package de.anniversarycounter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/**
 * Aktionen fuer einen Zaehler, gemeinsam genutzt von {@link CounterSettingsActivity} und dem
 * Schnellmenue ({@link QuickMenuController}). Jede Aktion speichert selbst und meldet die
 * Aenderung ueber den uebergebenen Rueckruf.
 */
final class CounterActions {

    private CounterActions() {
    }

    static void editDate(Activity activity, AnniversaryCounter counter, Runnable changed) {
        DateTimePickerFlow.pick(activity, counter.start, start -> {
            counter.start = start;
            AnniversaryCounterStore.update(activity, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(activity);
            // Meilenstein-Zieldaten haengen am Startzeitpunkt und muessen neu geplant werden
            MilestoneScheduler.scheduleForCounter(activity, counter);
            changed.run();
        });
    }

    static void editMessage(Activity activity, AnniversaryCounter counter, Runnable changed) {
        promptText(activity, R.string.custom_message_dialog_title, R.string.custom_message_hint,
                counter.message, value -> {
                    counter.message = value;
                    AnniversaryCounterStore.update(activity, counter);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(activity);
                    changed.run();
                });
    }

    static void editName(Activity activity, AnniversaryCounter counter, Runnable changed) {
        promptText(activity, R.string.name_dialog_title, R.string.name_dialog_hint,
                counter.name, value -> {
                    counter.name = value;
                    AnniversaryCounterStore.update(activity, counter);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(activity);
                    changed.run();
                });
    }

    private interface TextResult {
        void onText(String valueOrNull);
    }

    private static void promptText(Activity activity, int titleRes, int hintRes, String current,
                                   TextResult result) {
        EditText input = new EditText(activity);
        input.setHint(hintRes);
        if (current != null) {
            input.setText(current);
            input.setSelection(current.length());
        }

        int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(activity)
                .setTitle(titleRes)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    result.onText(value.isEmpty() ? null : value);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Oeffnet den Kontaktwaehler; das Ergebnis kommt in onActivityResult der Activity an. */
    static void pickContact(Activity activity, int requestCode) {
        // Der Kontaktwaehler gibt Lesezugriff auf genau den gewaehlten Eintrag, READ_CONTACTS
        // ist dafuer nicht noetig.
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        try {
            activity.startActivityForResult(pick, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.contact_pick_failed, Toast.LENGTH_LONG).show();
        }
    }

    static void saveContactFromPicker(Activity activity, AnniversaryCounter counter, Uri uri, Runnable changed) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
        };
        try (Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(1)) {
                saveContact(activity, counter, cursor.getString(0), cursor.getString(1), changed);
                return;
            }
        } catch (SecurityException e) {
            // faellt unten auf die Fehlermeldung durch
        }
        Toast.makeText(activity, R.string.contact_pick_failed, Toast.LENGTH_LONG).show();
    }

    static void saveContact(Activity activity, AnniversaryCounter counter, String name, String phone,
                            Runnable changed) {
        counter.contactName = name;
        counter.contactPhone = phone;
        AnniversaryCounterStore.update(activity, counter);
        changed.run();
    }

    static void delete(Activity activity, AnniversaryCounter counter, Runnable deleted) {
        String label = counter.displayLabel(activity);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.delete_counter_title)
                .setMessage(activity.getString(R.string.delete_counter_message, label))
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> {
                    MilestoneScheduler.cancelForCounter(activity, counter);
                    BackgroundImageStore.clear(activity, counter.id);
                    AnniversaryCounterStore.delete(activity, counter.id);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(activity);
                    deleted.run();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Oeffnet eine Unterseite fuer diesen Zaehler; alle nutzen dasselbe Extra {@code counter_id}. */
    static void openForCounter(Activity activity, Class<? extends Activity> page, AnniversaryCounter counter) {
        Intent intent = new Intent(activity, page);
        intent.putExtra(CounterSettingsActivity.EXTRA_COUNTER_ID, counter.id);
        activity.startActivity(intent);
    }
}
