package de.anniversarycounter;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Zentrale Formatierung von Datum, Uhrzeit und Zahlen fuer die Anzeige.
 *
 * Standardmaessig ({@code AUTO}) richtet sich alles nach der gewaehlten App-Sprache
 * ({@link AppSettings#languageLocale}), die Uhrzeit nach der 12h/24h-Systemeinstellung.
 * Ueber {@link FormatSettingsActivity} kann der Nutzer jedes Format einzeln festlegen.
 */
public final class DisplayFormats {

    private DisplayFormats() {
    }

    public static String formatDateTime(Context context, LocalDateTime dateTime) {
        return formatDateTime(context, dateTime,
                AppSettings.dateStyle(context), AppSettings.timeStyle(context));
    }

    /** Variante mit explizitem Stil, z. B. fuer die Vorschau in den Einstellungen. */
    public static String formatDateTime(Context context, LocalDateTime dateTime,
                                        AppSettings.DateStyle dateStyle, AppSettings.TimeStyle timeStyle) {
        return context.getString(R.string.date_time_format,
                formatDate(context, dateTime.toLocalDate(), dateStyle),
                formatTime(context, dateTime.toLocalTime(), timeStyle));
    }

    public static String formatDate(Context context, LocalDate date, AppSettings.DateStyle style) {
        Locale locale = AppSettings.languageLocale(context);
        DateTimeFormatter formatter;
        switch (style) {
            case DMY_DOT:
                formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale);
                break;
            case DMY_SLASH:
                formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale);
                break;
            case MDY_SLASH:
                formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", locale);
                break;
            case ISO:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale);
                break;
            case LONG:
                formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);
                break;
            case AUTO:
            default:
                formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
                break;
        }
        return date.format(formatter);
    }

    public static boolean use24Hour(Context context) {
        return use24Hour(context, AppSettings.timeStyle(context));
    }

    public static boolean use24Hour(Context context, AppSettings.TimeStyle style) {
        switch (style) {
            case H24:
                return true;
            case H12:
                return false;
            case AUTO:
            default:
                return android.text.format.DateFormat.is24HourFormat(context);
        }
    }

    public static String formatTime(Context context, LocalTime time, AppSettings.TimeStyle style) {
        String pattern = use24Hour(context, style) ? "HH:mm" : "h:mm a";
        return time.format(DateTimeFormatter.ofPattern(pattern, AppSettings.languageLocale(context)));
    }

    public static String formatNumber(Context context, long value) {
        return formatNumber(context, value, AppSettings.numberStyle(context));
    }

    public static String formatNumber(Context context, long value, AppSettings.NumberStyle style) {
        Locale locale = AppSettings.languageLocale(context);
        if (style == AppSettings.NumberStyle.AUTO) {
            return NumberFormat.getIntegerInstance(locale).format(value);
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        switch (style) {
            case DOT:
                symbols.setGroupingSeparator('.');
                break;
            case COMMA:
                symbols.setGroupingSeparator(',');
                break;
            case SPACE:
                // geschuetztes Leerzeichen, damit Zahlen nicht umbrechen
                symbols.setGroupingSeparator(' ');
                break;
            case NONE:
            default:
                format.setGroupingUsed(false);
                break;
        }
        format.setDecimalFormatSymbols(symbols);
        return format.format(value);
    }
}
