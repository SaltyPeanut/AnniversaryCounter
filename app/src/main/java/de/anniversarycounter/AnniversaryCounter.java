package de.anniversarycounter;

import android.content.Context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Ein einzelner Zähler: Startzeitpunkt, optionaler Name, optionale eigene Nachricht. */
public class AnniversaryCounter {

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
        return context.getString(R.string.counter_fallback_label, DisplayFormats.formatDateTime(context, start));
    }
}
