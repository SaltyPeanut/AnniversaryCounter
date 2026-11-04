package de.anniversarycounter;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liest jaehrlich wiederkehrende Termine aus den auf dem Geraet synchronisierten Kalendern.
 *
 * Geburtstage sind im Kalender-Provider nicht als eigener Typ erkennbar, daher liefert der
 * Reader alle jaehrlichen Serien - die Auswahl trifft der Nutzer in {@link CalendarImportActivity}.
 * Setzt die Berechtigung READ_CALENDAR voraus.
 */
final class CalendarBirthdayReader {

    /** Ein importierbarer Termin; {@link #key()} identifiziert ihn ueber Titel + Datum. */
    static final class Candidate {
        final String title;
        final LocalDate date;
        final String calendarName;
        boolean alreadyImported;

        Candidate(String title, LocalDate date, String calendarName) {
            this.title = title;
            this.date = date;
            this.calendarName = calendarName;
        }

        String key() {
            return key(title, date);
        }

        LocalDateTime start() {
            return date.atStartOfDay();
        }

        static String key(String title, LocalDate date) {
            return date + "|" + title;
        }
    }

    private static final String[] PROJECTION = {
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
    };

    private static final String SELECTION =
            CalendarContract.Events.DELETED + " = 0 AND "
                    + CalendarContract.Events.VISIBLE + " = 1 AND "
                    + CalendarContract.Events.RRULE + " LIKE '%FREQ=YEARLY%'";

    private CalendarBirthdayReader() {
    }

    /** Alphabetisch sortiert; gleicher Titel + gleiches Datum aus mehreren Kalendern nur einmal. */
    static List<Candidate> read(Context context) {
        Map<String, Candidate> unique = new LinkedHashMap<>();
        try (Cursor cursor = context.getContentResolver().query(
                CalendarContract.Events.CONTENT_URI, PROJECTION, SELECTION, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                String title = cursor.getString(0);
                if (title == null || title.trim().isEmpty() || cursor.isNull(1)) {
                    continue;
                }
                LocalDate date = toLocalDate(cursor.getLong(1), cursor.getInt(2) == 1);
                Candidate candidate = new Candidate(title.trim(), date, cursor.getString(3));
                unique.putIfAbsent(candidate.key(), candidate);
            }
        }

        List<Candidate> result = new ArrayList<>(unique.values());
        Collator collator = Collator.getInstance();
        result.sort((a, b) -> collator.compare(a.title, b.title));
        return result;
    }

    /** Ganztaegige Termine liegen als Mitternacht in UTC vor; lokal umgerechnet verrutschte der Tag. */
    private static LocalDate toLocalDate(long startMillis, boolean allDay) {
        ZoneId zone = allDay ? ZoneOffset.UTC : ZoneId.systemDefault();
        return Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate();
    }
}
