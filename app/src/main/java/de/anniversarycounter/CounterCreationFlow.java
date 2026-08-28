package de.anniversarycounter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dialog-Kette zum Anlegen eines neuen Zaehlers (optionaler Name, dann Datum + Uhrzeit).
 *
 * Wird von {@link MainActivity} (Erststart-Bootstrap, wenn noch kein Zaehler existiert) und
 * {@link CounterSettingsActivity} ("+ Neuer Zähler") gemeinsam genutzt, um die Dialogkette
 * nicht zu duplizieren.
 */
public final class CounterCreationFlow {

    public interface Callback {
        void onCreated(AnniversaryCounter counter);
    }

    private CounterCreationFlow() {
    }

    public static void start(Activity activity, Callback callback) {
        promptName(activity, callback);
    }

    private static void promptName(Activity activity, Callback callback) {
        EditText input = new EditText(activity);
        input.setHint(R.string.name_dialog_hint);
        int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.name_dialog_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    pickDate(activity, value.isEmpty() ? null : value, callback);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void pickDate(Activity activity, String name, Callback callback) {
        LocalDateTime now = LocalDateTime.now();
        DatePickerDialog dialog = new DatePickerDialog(
                activity,
                (view, year, month, dayOfMonth) -> pickTime(activity, name, year, month + 1, dayOfMonth, callback),
                now.getYear(),
                now.getMonthValue() - 1,
                now.getDayOfMonth());
        dialog.setTitle(R.string.pick_date);
        dialog.show();
    }

    private static void pickTime(Activity activity, String name, int year, int month, int day, Callback callback) {
        LocalDateTime now = LocalDateTime.now();
        TimePickerDialog dialog = new TimePickerDialog(
                activity,
                (view, hourOfDay, minute) -> {
                    LocalDateTime start = LocalDateTime.of(year, month, day, hourOfDay, minute, 0);
                    AnniversaryCounter counter = new AnniversaryCounter(UUID.randomUUID().toString(), name, start, null);
                    AnniversaryCounterStore.add(activity, counter);
                    callback.onCreated(counter);
                },
                now.getHour(),
                now.getMinute(),
                true /* 24-Stunden-Format */);
        dialog.setTitle(R.string.pick_time);
        dialog.show();
    }
}
