package de.anniversarycounter;

import java.util.UUID;

/** Ein vom Nutzer selbst angelegter Meilenstein (Anzahl + Einheit), pro Zaehler. */
public class CustomMilestone {

    public final String id;
    public int amount;
    public Milestone.Unit unit;

    public CustomMilestone(String id, int amount, Milestone.Unit unit) {
        this.id = id;
        this.amount = amount;
        this.unit = unit;
    }

    public static CustomMilestone create(int amount, Milestone.Unit unit) {
        return new CustomMilestone(UUID.randomUUID().toString(), amount, unit);
    }
}
