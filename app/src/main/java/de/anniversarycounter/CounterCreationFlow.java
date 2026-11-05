package de.anniversarycounter;

import android.app.Activity;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dialog-Kette zum Anlegen eines neuen Zaehlers (optionaler Name, dann Datum + Uhrzeit).
 *
 * Wird von {@link MainActivity} (Erststart-Bootstrap, wenn noch kein Zaehler existiert),
 * {@link QuickMenuController} und {@link CounterSettingsActivity} ("+ Neuer Zähler") gemeinsam
 * genutzt, um die Dialogkette nicht zu duplizieren.
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
                    String name = value.isEmpty() ? null : value;
                    DateTimePickerFlow.pick(activity, LocalDateTime.now(), start -> {
                        AnniversaryCounter counter =
                                new AnniversaryCounter(UUID.randomUUID().toString(), name, start, null);
                        AnniversaryCounterStore.add(activity, counter);
                        callback.onCreated(counter);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
