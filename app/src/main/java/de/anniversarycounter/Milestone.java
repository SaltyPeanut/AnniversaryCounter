package de.anniversarycounter;

/** Gemeinsame Definitionen fuer Meilenstein-Benachrichtigungen. */
public final class Milestone {

    private Milestone() {
    }

    public enum Unit {
        DAYS, MONTHS, YEARS
    }

    /** Fest vorgegebene, pro Zaehler einzeln togglebare Meilensteine (chronologisch sortiert). */
    public enum Standard {
        DAYS_100(100, Unit.DAYS, R.string.milestone_days_100),
        MONTHS_6(6, Unit.MONTHS, R.string.milestone_months_6),
        YEARS_1(1, Unit.YEARS, R.string.milestone_years_1),
        YEARS_2(2, Unit.YEARS, R.string.milestone_years_2),
        DAYS_1000(1000, Unit.DAYS, R.string.milestone_days_1000),
        YEARS_5(5, Unit.YEARS, R.string.milestone_years_5),
        YEARS_10(10, Unit.YEARS, R.string.milestone_years_10);

        public final int amount;
        public final Unit unit;
        public final int labelRes;

        Standard(int amount, Unit unit, int labelRes) {
            this.amount = amount;
            this.unit = unit;
            this.labelRes = labelRes;
        }
    }
}
