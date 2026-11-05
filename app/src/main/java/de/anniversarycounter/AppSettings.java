package de.anniversarycounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Globale, zaehlerunabhaengige App-Einstellungen. */
public final class AppSettings {

    /** Schriftart/Farbe lassen sich fuer Text (Name/Nachricht/Titel) und Zahlen getrennt einstellen. */
    public enum Target {
        TEXT("font_family", "text_color"),
        NUMBERS("font_family_numbers", "text_color_numbers");

        final String fontKey;
        final String colorKey;

        Target(String fontKey, String colorKey) {
            this.fontKey = fontKey;
            this.colorKey = colorKey;
        }
    }

    /** Datumsformat; AUTO = abgeleitet aus der App-Sprache (siehe {@link DisplayFormats}). */
    public enum DateStyle {
        AUTO, DMY_DOT, DMY_SLASH, MDY_SLASH, ISO, LONG
    }

    /** Uhrzeitformat; AUTO = Systemeinstellung des Geraets (12h/24h). */
    public enum TimeStyle {
        AUTO, H24, H12
    }

    /** Tausendertrennzeichen; AUTO = abgeleitet aus der App-Sprache. */
    public enum NumberStyle {
        AUTO, DOT, COMMA, SPACE, NONE
    }

    private static final String PREFS = "app_settings_prefs";
    private static final String KEY_USE_LIGHT_THEME = "use_light_theme";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DATE_STYLE = "date_style";
    private static final String KEY_TIME_STYLE = "time_style";
    private static final String KEY_NUMBER_STYLE = "number_style";
    private static final String KEY_QUICK_MENU_ITEMS = "quick_menu_items";
    private static final String KEY_TUTORIAL_SEEN = "tutorial_seen";

    public static final String LANG_DE = "de";
    public static final String LANG_EN = "en";
    public static final String LANG_ES = "es";
    public static final String LANG_FR = "fr";

    /** Leerer Family-Name = Systemstandard ({@link Typeface#DEFAULT}). */
    public static final String FONT_DEFAULT = "";

    /** Auswahl an eingebauten Android-Schriftfamilien, keine Extra-Assets noetig. */
    public static final FontOption[] FONT_OPTIONS = {
            new FontOption(FONT_DEFAULT, R.string.font_default),
            new FontOption("sans-serif-light", R.string.font_light),
            new FontOption("sans-serif-medium", R.string.font_medium),
            new FontOption("sans-serif-condensed", R.string.font_condensed),
            new FontOption("serif", R.string.font_serif),
            new FontOption("monospace", R.string.font_monospace),
            new FontOption("cursive", R.string.font_cursive),
    };

    /** Sentinel: kein eigener Farbwert gesetzt, es gilt @color/text_primary. */
    public static final int COLOR_DEFAULT = 0;

    public static final int[] COLOR_PALETTE = {
            0xFFFFFFFF, // Standard (Weiss)
            0xFFFF4D6D, // Pink
            0xFFFFD54F, // Gold
            0xFF64B5F6, // Hellblau
            0xFF81C784, // Gruen
            0xFFCE93D8, // Lavendel
            0xFFFFB74D, // Orange
            0xFFEF5350, // Rot
    };

    private AppSettings() {
    }

    /** true sobald einmal eine Sprache gewaehlt wurde (Gate fuer den Auswahl-Dialog beim ersten Start). */
    public static boolean hasLanguage(Context context) {
        return prefs(context).contains(KEY_LANGUAGE);
    }

    public static void setLanguage(Context context, String languageCode) {
        prefs(context).edit().putString(KEY_LANGUAGE, languageCode).apply();
        applyLanguage(context);
    }

    /**
     * Muss beim App-Start (siehe {@link AnniversaryCounterApp}) erneut aufgerufen werden, da
     * AppCompatDelegate die Auswahl nicht von sich aus ueber Prozess-Neustarts hinweg merkt.
     */
    public static void applyLanguage(Context context) {
        String language = prefs(context).getString(KEY_LANGUAGE, null);
        if (language != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
        }
    }

    /**
     * Locale der gewaehlten App-Sprache (ohne Region) fuer Datums- und Zahlenformate. Bewusst aus
     * den Prefs statt aus der Context-Konfiguration gelesen, damit auch Widget und Receiver
     * stimmen, fuer die AppCompat die App-Sprache unter API 33 nicht setzt.
     */
    public static Locale languageLocale(Context context) {
        String language = prefs(context).getString(KEY_LANGUAGE, null);
        if (language != null) {
            return Locale.forLanguageTag(language);
        }
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    public static DateStyle dateStyle(Context context) {
        return readEnum(context, KEY_DATE_STYLE, DateStyle.class, DateStyle.AUTO);
    }

    public static void setDateStyle(Context context, DateStyle style) {
        prefs(context).edit().putString(KEY_DATE_STYLE, style.name()).apply();
    }

    public static TimeStyle timeStyle(Context context) {
        return readEnum(context, KEY_TIME_STYLE, TimeStyle.class, TimeStyle.AUTO);
    }

    public static void setTimeStyle(Context context, TimeStyle style) {
        prefs(context).edit().putString(KEY_TIME_STYLE, style.name()).apply();
    }

    public static NumberStyle numberStyle(Context context) {
        return readEnum(context, KEY_NUMBER_STYLE, NumberStyle.class, NumberStyle.AUTO);
    }

    public static void setNumberStyle(Context context, NumberStyle style) {
        prefs(context).edit().putString(KEY_NUMBER_STYLE, style.name()).apply();
    }

    static boolean hasSeenTutorial(Context context) {
        return prefs(context).getBoolean(KEY_TUTORIAL_SEEN, false);
    }

    static void setTutorialSeen(Context context) {
        prefs(context).edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply();
    }

    /** Im Schnellmenue gewaehlte Eintraege; ohne gespeicherte Auswahl gelten die Standardwerte. */
    static Set<QuickMenuItem> quickMenuItems(Context context) {
        Set<QuickMenuItem> items = EnumSet.noneOf(QuickMenuItem.class);
        Set<String> stored = prefs(context).getStringSet(KEY_QUICK_MENU_ITEMS, null);
        for (QuickMenuItem item : QuickMenuItem.values()) {
            if (stored == null ? item.enabledByDefault : stored.contains(item.name())) {
                items.add(item);
            }
        }
        return items;
    }

    static void setQuickMenuItems(Context context, Set<QuickMenuItem> items) {
        Set<String> names = new HashSet<>();
        for (QuickMenuItem item : items) {
            names.add(item.name());
        }
        prefs(context).edit().putStringSet(KEY_QUICK_MENU_ITEMS, names).apply();
    }

    /** Liest einen als name() gespeicherten Enum-Wert; unbekannte/fehlende Werte ergeben den Fallback. */
    private static <E extends Enum<E>> E readEnum(Context context, String key, Class<E> type, E fallback) {
        String stored = prefs(context).getString(key, null);
        if (stored == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, stored);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Das dunkle Design ist der historische Standard dieser App und bleibt es fuer
     * bestehende Nutzer, bis sie aktiv das helle Design waehlen.
     */
    public static boolean useLightTheme(Context context) {
        return prefs(context).getBoolean(KEY_USE_LIGHT_THEME, false);
    }

    public static void setUseLightTheme(Context context, boolean useLight) {
        prefs(context).edit().putBoolean(KEY_USE_LIGHT_THEME, useLight).apply();
        applyNightMode(context);
    }

    /**
     * Erzwingt hell/dunkel unabhaengig vom Systemschalter (statt MODE_NIGHT_FOLLOW_SYSTEM),
     * damit dieser Einstellung allein die Nutzerwahl zaehlt. Muss beim App-Start (siehe
     * {@link AnniversaryCounterApp}) sowie bei jeder Aenderung erneut aufgerufen werden; AppCompat
     * loest daraufhin automatisch ein Recreate aller offenen Activities aus.
     */
    public static void applyNightMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(
                useLightTheme(context) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
    }

    public static String fontFamily(Context context, Target target) {
        return prefs(context).getString(target.fontKey, FONT_DEFAULT);
    }

    public static void setFontFamily(Context context, Target target, String family) {
        prefs(context).edit().putString(target.fontKey, family).apply();
    }

    public static Typeface typeface(Context context, Target target) {
        String family = fontFamily(context, target);
        if (family == null || family.isEmpty()) {
            return Typeface.DEFAULT;
        }
        return Typeface.create(family, Typeface.NORMAL);
    }

    public static int textColorValue(Context context, Target target) {
        return prefs(context).getInt(target.colorKey, COLOR_DEFAULT);
    }

    public static void setTextColorValue(Context context, Target target, int color) {
        prefs(context).edit().putInt(target.colorKey, color).apply();
    }

    public static int textColor(Context context, Target target) {
        int value = textColorValue(context, target);
        if (value == COLOR_DEFAULT) {
            return ContextCompat.getColor(context, R.color.text_primary);
        }
        return value;
    }

    /**
     * Wie {@link #typeface(Context, Target)}, nutzt aber die eigene Schriftart des Zaehlers,
     * falls {@link AnniversaryCounter#customAppearance} gesetzt ist; sonst gilt die globale Einstellung.
     */
    public static Typeface typeface(Context context, AnniversaryCounter counter, Target target) {
        if (counter == null || !counter.customAppearance) {
            return typeface(context, target);
        }
        String family = target == Target.TEXT ? counter.fontFamilyText : counter.fontFamilyNumbers;
        if (family == null || family.isEmpty()) {
            return Typeface.DEFAULT;
        }
        return Typeface.create(family, Typeface.NORMAL);
    }

    /** Wie {@link #textColor(Context, Target)}, aber mit eigener Farbe des Zaehlers als Vorrang. */
    public static int textColor(Context context, AnniversaryCounter counter, Target target) {
        if (counter == null || !counter.customAppearance) {
            return textColor(context, target);
        }
        int value = target == Target.TEXT ? counter.colorValueText : counter.colorValueNumbers;
        if (value == COLOR_DEFAULT) {
            return ContextCompat.getColor(context, R.color.text_primary);
        }
        return value;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Eine waehlbare Schriftfamilie fuer die Einstellungen-Liste. */
    public static final class FontOption {
        public final String family;
        public final int labelRes;

        public FontOption(String family, int labelRes) {
            this.family = family;
            this.labelRes = labelRes;
        }
    }
}
