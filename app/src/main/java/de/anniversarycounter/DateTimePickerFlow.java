package de.anniversarycounter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Dialog-Kette Datum -> Uhrzeit, gemeinsam genutzt von {@link CounterCreationFlow} und
 * {@link CounterSettingsActivity}.
 *
 * Beide Dialoge nutzen den Style ThemeOverlay.AnniversaryCounter.PickerDialog und folgen damit
 * den App-Farben im hellen und dunklen Design. Der Datumsdialog laeuft im Spinner-Modus (Raeder
 * fuer Tag/Monat/Jahr), damit sich auch weit zurueckliegende Jahre direkt einstellen lassen, ohne
 * Monat fuer Monat zu blaettern.
 */
public final class DateTimePickerFlow {

    private DateTimePickerFlow() {
    }

    public static void pick(Activity activity, LocalDateTime initial, Consumer<LocalDateTime> onPicked) {
        DatePickerDialog dialog = new DatePickerDialog(
                activity,
                R.style.ThemeOverlay_AnniversaryCounter_PickerDialog,
                (view, year, month, dayOfMonth) ->
                        pickTime(activity, initial, year, month + 1, dayOfMonth, onPicked),
                initial.getYear(),
                initial.getMonthValue() - 1,
                initial.getDayOfMonth());
        dialog.setTitle(R.string.pick_date);
        dialog.show();
    }

    private static void pickTime(Activity activity, LocalDateTime initial, int year, int month, int day,
                                 Consumer<LocalDateTime> onPicked) {
        TimePickerDialog dialog = new TimePickerDialog(
                activity,
                R.style.ThemeOverlay_AnniversaryCounter_PickerDialog,
                (view, hourOfDay, minute) ->
                        onPicked.accept(LocalDateTime.of(year, month, day, hourOfDay, minute, 0)),
                initial.getHour(),
                initial.getMinute(),
                DisplayFormats.use24Hour(activity));
        dialog.setTitle(R.string.pick_time);
        dialog.show();
    }
}
