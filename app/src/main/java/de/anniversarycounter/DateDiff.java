package de.anniversarycounter;

import android.content.Context;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Kalendergenaue Differenz zwischen zwei Zeitpunkten.
 *
 * Es wird schrittweise gerechnet (erst Jahre abziehen, dann Monate, dann Tage ...),
 * damit unterschiedlich lange Monate und Schaltjahre korrekt behandelt werden.
 * Eine simple Division durch 30 Tage waere hier falsch.
 */
public class DateDiff {

    /** Fuer die Widget-Konfiguration waehlbare Einheiten (bewusst ohne Minuten/Sekunden). */
    public enum Unit {
        YEARS, MONTHS, DAYS, HOURS
    }

    public final long years;
    public final long months;
    public final long days;
    public final long hours;
    public final long minutes;
    public final long seconds;

    /** true, wenn der eingegebene Zeitpunkt noch in der Zukunft liegt. */
    public final boolean future;

    private final LocalDateTime from;
    private final LocalDateTime to;

    private DateDiff(long years, long months, long days, long hours, long minutes, long seconds,
                     boolean future, LocalDateTime from, LocalDateTime to) {
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.future = future;
        this.from = from;
        this.to = to;
    }

    public static DateDiff between(LocalDateTime start, LocalDateTime now) {
        boolean future = start.isAfter(now);
        LocalDateTime from = future ? now : start;
        LocalDateTime to = future ? start : now;

        LocalDateTime cursor = from;

        long years = cursor.until(to, ChronoUnit.YEARS);
        cursor = cursor.plusYears(years);

        long months = cursor.until(to, ChronoUnit.MONTHS);
        cursor = cursor.plusMonths(months);

        long days = cursor.until(to, ChronoUnit.DAYS);
        cursor = cursor.plusDays(days);

        long hours = cursor.until(to, ChronoUnit.HOURS);
        cursor = cursor.plusHours(hours);

        long minutes = cursor.until(to, ChronoUnit.MINUTES);
        cursor = cursor.plusMinutes(minutes);

        long seconds = cursor.until(to, ChronoUnit.SECONDS);

        return new DateDiff(years, months, days, hours, minutes, seconds, future, from, to);
    }

    /**
     * Mehrzeilige Aufschluesselung wie in der Vorlage: Einheiten mit Wert 0 werden
     * uebersprungen, Sekunden stehen immer am Ende.
     */
    public String formatBreakdown(Context context) {
        List<String> parts = new ArrayList<>();
        if (years > 0) {
            parts.add(context.getResources().getQuantityString(R.plurals.years, (int) years, years));
        }
        if (months > 0) {
            parts.add(context.getResources().getQuantityString(R.plurals.months, (int) months, months));
        }
        if (days > 0) {
            parts.add(context.getResources().getQuantityString(R.plurals.days, (int) days, days));
        }
        if (hours > 0) {
            parts.add(context.getResources().getQuantityString(R.plurals.hours, (int) hours, hours));
        }
        if (minutes > 0) {
            parts.add(context.getResources().getQuantityString(R.plurals.minutes, (int) minutes, minutes));
        }
        parts.add(context.getResources().getQuantityString(R.plurals.seconds, (int) seconds, seconds));

        return join(parts);
    }

    /**
     * Aufschluesselung nur der ausgewaehlten Einheiten – fuer das Homescreen-Widget.
     * Anders als {@link #formatBreakdown} werden gewaehlte Einheiten auch bei Wert 0
     * angezeigt, da der Nutzer sie bewusst ausgesucht hat.
     */
    public String formatUnits(Context context, Set<Unit> units) {
        List<String> parts = new ArrayList<>();
        if (units.contains(Unit.YEARS)) {
            parts.add(context.getResources().getQuantityString(R.plurals.years, (int) years, years));
        }
        if (units.contains(Unit.MONTHS)) {
            parts.add(context.getResources().getQuantityString(R.plurals.months, (int) months, months));
        }
        if (units.contains(Unit.DAYS)) {
            parts.add(context.getResources().getQuantityString(R.plurals.days, (int) days, days));
        }
        if (units.contains(Unit.HOURS)) {
            parts.add(context.getResources().getQuantityString(R.plurals.hours, (int) hours, hours));
        }
        return join(parts);
    }

    /** Gesamtwerte in jeweils einer Einheit – erscheint beim Antippen des Zaehlers. */
    public String formatTotals(Context context) {
        long totalDays = ChronoUnit.DAYS.between(from, to);
        long totalMonths = ChronoUnit.MONTHS.between(from, to);

        List<String> parts = new ArrayList<>();
        if (totalMonths > 0) {
            parts.add(format(context, R.string.total_months, totalMonths));
        }
        parts.add(format(context, R.string.total_weeks, ChronoUnit.WEEKS.between(from, to)));
        parts.add(format(context, R.string.total_days, totalDays));
        parts.add(format(context, R.string.total_hours, ChronoUnit.HOURS.between(from, to)));
        parts.add(format(context, R.string.total_minutes, ChronoUnit.MINUTES.between(from, to)));
        parts.add(format(context, R.string.total_seconds, ChronoUnit.SECONDS.between(from, to)));

        return join(parts);
    }

    private static String format(Context context, int stringRes, long value) {
        return context.getString(stringRes, DisplayFormats.formatNumber(context, value));
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
