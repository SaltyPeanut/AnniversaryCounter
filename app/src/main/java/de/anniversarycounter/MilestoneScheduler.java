package de.anniversarycounter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Plant/storniert Meilenstein-Alarme ueber den {@link AlarmManager}.
 *
 * Zustellung bewusst ueber {@code setAndAllowWhileIdle} (inexakt, Doze-tauglich) statt
 * {@code setExactAndAllowWhileIdle} - fuer eine Jahrestags-Erinnerung reicht "ungefaehr am
 * Morgen" und es wird keine SCHEDULE_EXACT_ALARM-Berechtigung benoetigt.
 */
public final class MilestoneScheduler {

    static final String EXTRA_COUNTER_ID = "counter_id";
    static final String EXTRA_MILESTONE_KEY = "milestone_key";
    static final String EXTRA_MILESTONE_LABEL = "milestone_label";

    private static final String CUSTOM_PREFIX = "custom:";

    private MilestoneScheduler() {
    }

    public static void scheduleAll(Context context) {
        for (AnniversaryCounter counter : AnniversaryCounterStore.loadAll(context)) {
            scheduleForCounter(context, counter);
        }
    }

    public static void scheduleForCounter(Context context, AnniversaryCounter counter) {
        cancelForCounter(context, counter);

        for (Milestone.Standard standard : Milestone.Standard.values()) {
            if (!counter.enabledStandardMilestones.contains(standard.name())) {
                continue;
            }
            if (standard == Milestone.Standard.YEARLY) {
                int years = nextYearlyAmount(counter);
                // Bei ausgeblendetem Alter darf es auch in der Benachrichtigung nicht auftauchen.
                String label = counter.showAge
                        ? context.getResources().getQuantityString(R.plurals.years, years, years)
                        : context.getString(R.string.milestone_yearly_label);
                schedule(context, counter, standard.name(), years, Milestone.Unit.YEARS, label);
            } else {
                schedule(context, counter, standard.name(), standard.amount, standard.unit,
                        context.getString(standard.labelRes));
            }
        }
        for (CustomMilestone custom : counter.customMilestones) {
            schedule(context, counter, CUSTOM_PREFIX + custom.id, custom.amount, custom.unit,
                    formatCustomLabel(context, custom));
        }
    }

    /** Storniert alle moeglichen Alarme dieses Zaehlers (Standard-Set + dessen eigene Meilensteine). */
    public static void cancelForCounter(Context context, AnniversaryCounter counter) {
        for (Milestone.Standard standard : Milestone.Standard.values()) {
            cancel(context, counter.id, standard.name());
        }
        for (CustomMilestone custom : counter.customMilestones) {
            cancel(context, counter.id, CUSTOM_PREFIX + custom.id);
        }
    }

    /**
     * Ob der Meilenstein noch zum Zaehler gehoert. Faengt Alarme ab, die nicht mehr storniert
     * werden konnten, etwa aus aelteren App-Versionen oder nach einem Backup-Import.
     */
    static boolean isActive(AnniversaryCounter counter, String milestoneKey) {
        if (!milestoneKey.startsWith(CUSTOM_PREFIX)) {
            return counter.enabledStandardMilestones.contains(milestoneKey);
        }
        for (CustomMilestone custom : counter.customMilestones) {
            if (milestoneKey.equals(CUSTOM_PREFIX + custom.id)) {
                return true;
            }
        }
        return false;
    }

    /** Anzahl Jahre am naechsten Jahrestag; immer ab dem Start gerechnet, damit der 29.02. nicht wandert. */
    private static int nextYearlyAmount(AnniversaryCounter counter) {
        LocalDateTime now = LocalDateTime.now();
        int years = Math.max(1, now.getYear() - counter.start.getYear());
        while (!targetTime(counter, years, Milestone.Unit.YEARS).isAfter(now)) {
            years++;
        }
        return years;
    }

    private static LocalDateTime targetTime(AnniversaryCounter counter, int amount, Milestone.Unit unit) {
        return applyUnit(counter.start, amount, unit).toLocalDate().atTime(9, 0);
    }

    private static void schedule(Context context, AnniversaryCounter counter, String milestoneKey,
                                  int amount, Milestone.Unit unit, String label) {
        LocalDateTime target = targetTime(counter, amount, unit);
        if (!target.isAfter(LocalDateTime.now())) {
            return;
        }
        long triggerAtMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        PendingIntent pendingIntent = buildPendingIntent(context, counter.id, milestoneKey, label,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private static void cancel(Context context, String counterId, String milestoneKey) {
        PendingIntent pendingIntent = buildPendingIntent(context, counterId, milestoneKey, null,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent == null) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private static PendingIntent buildPendingIntent(Context context, String counterId, String milestoneKey,
                                                      String label, int flags) {
        Intent intent = new Intent(context, MilestoneAlarmReceiver.class);
        intent.putExtra(EXTRA_COUNTER_ID, counterId);
        intent.putExtra(EXTRA_MILESTONE_KEY, milestoneKey);
        if (label != null) {
            intent.putExtra(EXTRA_MILESTONE_LABEL, label);
        }
        int requestCode = (counterId + "|" + milestoneKey).hashCode();
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private static LocalDateTime applyUnit(LocalDateTime start, int amount, Milestone.Unit unit) {
        switch (unit) {
            case DAYS:
                return start.plusDays(amount);
            case MONTHS:
                return start.plusMonths(amount);
            case YEARS:
                return start.plusYears(amount);
            default:
                throw new IllegalArgumentException("Unknown unit: " + unit);
        }
    }

    private static String formatCustomLabel(Context context, CustomMilestone custom) {
        int pluralsRes;
        switch (custom.unit) {
            case DAYS:
                pluralsRes = R.plurals.days;
                break;
            case MONTHS:
                pluralsRes = R.plurals.months;
                break;
            case YEARS:
                pluralsRes = R.plurals.years;
                break;
            default:
                throw new IllegalArgumentException("Unknown unit: " + custom.unit);
        }
        return context.getResources().getQuantityString(pluralsRes, custom.amount, custom.amount);
    }
}
