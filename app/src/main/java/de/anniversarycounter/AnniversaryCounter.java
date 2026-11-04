package de.anniversarycounter;

import android.content.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ein einzelner Zähler: Startzeitpunkt, optionaler Name, optionale eigene Nachricht. */
public class AnniversaryCounter {

    public static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm", Locale.GERMANY);

    public final String id;
    public String name;
    public LocalDateTime start;
    public String message;
    public boolean showMessage = true;
    public boolean showName = true;

    /** Eigene Schriftart/Farbe fuer diesen Zaehler statt der globalen Einstellung aus {@link AppSettings}. */
    public boolean customAppearance = false;
    public String fontFamilyText = AppSettings.FONT_DEFAULT;
    public int colorValueText = AppSettings.COLOR_DEFAULT;
    public String fontFamilyNumbers = AppSettings.FONT_DEFAULT;
    public int colorValueNumbers = AppSettings.COLOR_DEFAULT;

    /** Namen (name()) der aktivierten {@link Milestone.Standard}-Eintraege, opt-in, leer per Default. */
    public Set<String> enabledStandardMilestones = new HashSet<>();
    public List<CustomMilestone> customMilestones = new ArrayList<>();

    /** Optional hinterlegter Kontakt, den man direkt benachrichtigen kann. */
    public String contactName;
    public String contactPhone;

    /** true: zaehlt bis zum naechsten Jahrestag herunter und springt danach aufs Folgejahr. */
    public boolean countdownToNext = false;
    public boolean showAge = false;

    public AnniversaryCounter(String id, String name, LocalDateTime start, String message) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.message = message;
    }

    /** Anzeigename fuer Auswahllisten (z. B. Widget-Konfiguration): eigener Name, sonst Startdatum. */
    public String displayLabel(Context context) {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return context.getString(R.string.counter_fallback_label, start.format(DISPLAY_FORMAT));
    }

    /** Zeitpunkt, gegen den die Anzeige rechnet: der Start oder der naechste Jahrestag. */
    public LocalDateTime displayTarget(LocalDateTime now) {
        if (!countdownToNext || start.isAfter(now)) {
            return start;
        }
        // Immer vom Start aus rechnen, damit ein 29.02. nicht dauerhaft zum 28.02. wird.
        long years = Math.max(1, now.getYear() - start.getYear());
        while (!start.plusYears(years).isAfter(now)) {
            years++;
        }
        return start.plusYears(years);
    }

    /** Text wie "Alter: 36 Jahre", oder null wenn ausgeblendet bzw. der Start noch bevorsteht. */
    public String ageText(Context context, LocalDateTime now) {
        if (!showAge || start.isAfter(now)) {
            return null;
        }
        int age = (int) ChronoUnit.YEARS.between(start, now);
        return context.getString(R.string.age_label,
                context.getResources().getQuantityString(R.plurals.years, age, age));
    }

    public boolean hasContact() {
        return contactPhone != null && !contactPhone.isEmpty();
    }

    /** Kontaktname fuer die Anzeige; Kontakte ohne Namen werden ueber die Nummer angezeigt. */
    public String contactLabel() {
        return contactName != null && !contactName.isEmpty() ? contactName : contactPhone;
    }
}
