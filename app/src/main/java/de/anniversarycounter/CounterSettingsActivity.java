package de.anniversarycounter;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Eigene Seite mit allen Aktionen fuer einen Zaehler, erreichbar ueber das Zahnrad in
 * {@link MainActivity}. Ersetzt die frueher dort einklappbare Button-Leiste, damit die
 * Hauptseite strukturell konstant (und damit zentriert) bleibt.
 */
public class CounterSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int REQUEST_IMPORT_BACKUP = 300;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 301;
    private static final int REQUEST_CALENDAR_IMPORT = 302;
    private static final int REQUEST_PICK_CONTACT = 303;

    private AnniversaryCounter counter;

    private LinearLayout counterTabGroup;
    private TextView tvTitle;
    private View perCounterActions;
    private Button btnSetDate;
    private Button btnSetMessage;
    private Button btnSetName;
    private CheckBox cbShowMessage;
    private CheckBox cbShowName;
    private CheckBox cbCustomAppearance;
    private CheckBox cbCountdownToNext;
    private CheckBox cbShowAge;
    private Button btnSetContact;
    private Button btnNotifyContact;
    private Button btnRemoveContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter_settings);

        counterTabGroup = findViewById(R.id.counterTabGroup);
        tvTitle = findViewById(R.id.tvCounterSettingsTitle);
        perCounterActions = findViewById(R.id.perCounterActions);
        btnSetDate = findViewById(R.id.btnSetDate);
        btnSetMessage = findViewById(R.id.btnSetMessage);
        btnSetName = findViewById(R.id.btnSetName);
        cbShowMessage = findViewById(R.id.cbShowMessage);
        cbShowName = findViewById(R.id.cbShowName);
        cbCustomAppearance = findViewById(R.id.cbCustomAppearance);
        cbCountdownToNext = findViewById(R.id.cbCountdownToNext);
        cbShowAge = findViewById(R.id.cbShowAge);
        cbCountdownToNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (counter == null || counter.countdownToNext == isChecked) {
                return;
            }
            counter.countdownToNext = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        });
        cbShowAge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (counter == null || counter.showAge == isChecked) {
                return;
            }
            counter.showAge = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
            // Der Text der jaehrlichen Erinnerung steht schon im geplanten Alarm.
            MilestoneScheduler.scheduleForCounter(this, counter);
        });
        Button btnCounterAppearance = findViewById(R.id.btnCounterAppearance);
        Button btnNotifications = findViewById(R.id.btnNotifications);
        Button btnChangeBackground = findViewById(R.id.btnChangeBackground);
        Button btnDeleteCounter = findViewById(R.id.btnDeleteCounter);
        Button btnAddCounter = findViewById(R.id.btnAddCounter);
        ImageButton btnBack = findViewById(R.id.btnBack);
        CheckBox cbUseLightTheme = findViewById(R.id.cbUseLightTheme);

        cbUseLightTheme.setChecked(AppSettings.useLightTheme(this));
        cbUseLightTheme.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppSettings.setUseLightTheme(this, isChecked));

        Button btnAppearance = findViewById(R.id.btnAppearance);
        btnAppearance.setOnClickListener(v ->
                startActivity(new Intent(this, AppearanceSettingsActivity.class)));

        Button btnReorderCounters = findViewById(R.id.btnReorderCounters);
        btnReorderCounters.setOnClickListener(v ->
                startActivity(new Intent(this, CounterOrderActivity.class)));

        Button btnExportBackup = findViewById(R.id.btnExportBackup);
        Button btnImportBackup = findViewById(R.id.btnImportBackup);
        Button btnImportCalendar = findViewById(R.id.btnImportCalendar);
        btnImportCalendar.setOnClickListener(v -> startActivityForResult(
                new Intent(this, CalendarImportActivity.class), REQUEST_CALENDAR_IMPORT));

        btnSetContact = findViewById(R.id.btnSetContact);
        btnNotifyContact = findViewById(R.id.btnNotifyContact);
        btnRemoveContact = findViewById(R.id.btnRemoveContact);
        btnSetContact.setOnClickListener(v -> pickContact());
        btnNotifyContact.setOnClickListener(v -> {
            if (counter != null && counter.hasContact()) {
                CounterMessage.sendToContact(this, counter);
            }
        });
        btnRemoveContact.setOnClickListener(v -> saveContact(null, null));

        btnExportBackup.setOnClickListener(v -> exportBackup());
        btnImportBackup.setOnClickListener(v -> confirmImportBackup());

        btnBack.setOnClickListener(v -> finish());
        btnSetDate.setOnClickListener(v -> editDate());
        btnSetMessage.setOnClickListener(v -> editMessage());
        cbShowMessage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (counter == null || counter.showMessage == isChecked) {
                return;
            }
            counter.showMessage = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        });
        cbShowName.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (counter == null || counter.showName == isChecked) {
                return;
            }
            counter.showName = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        });
        cbCustomAppearance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (counter == null || counter.customAppearance == isChecked) {
                return;
            }
            counter.customAppearance = isChecked;
            AnniversaryCounterStore.update(this, counter);
        });
        btnCounterAppearance.setOnClickListener(v -> {
            if (counter == null) {
                return;
            }
            Intent intent = new Intent(this, AppearanceSettingsActivity.class);
            intent.putExtra(AppearanceSettingsActivity.EXTRA_COUNTER_ID, counter.id);
            startActivity(intent);
        });
        btnSetName.setOnClickListener(v -> editName());
        btnNotifications.setOnClickListener(v -> openNotificationSettings());
        btnChangeBackground.setOnClickListener(v -> openBackgroundSettings());
        btnDeleteCounter.setOnClickListener(v -> deleteCounter());
        btnAddCounter.setOnClickListener(v -> CounterCreationFlow.start(this, created -> {
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
            buildCounterTabs(created.id);
        }));

        // Nach dem Neuaufbau derselbe Zaehler: das Ergebnis des Kontaktwaehlers muss bei dem
        // Zaehler landen, fuer den er geoeffnet wurde.
        String counterId = savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_COUNTER_ID)
                : getIntent().getStringExtra(EXTRA_COUNTER_ID);
        buildCounterTabs(counterId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (counter != null) {
            outState.putString(EXTRA_COUNTER_ID, counter.id);
        }
    }

    /**
     * Baut die Reiterleiste ganz oben neu auf, mit der sich der Zaehler waehlen laesst, fuer
     * den die Einstellungen unten gelten. preferredId waehlt (falls vorhanden) den Reiter aus,
     * sonst wird der erste Zaehler vorausgewaehlt.
     */
    private void buildCounterTabs(String preferredId) {
        List<AnniversaryCounter> all = AnniversaryCounterStore.loadAll(this);
        AnniversaryCounter toSelect = null;
        if (preferredId != null) {
            for (AnniversaryCounter c : all) {
                if (c.id.equals(preferredId)) {
                    toSelect = c;
                    break;
                }
            }
        }
        if (toSelect == null && !all.isEmpty()) {
            toSelect = all.get(0);
        }
        renderCounterTabs(all, toSelect);
    }

    private void renderCounterTabs(List<AnniversaryCounter> all, AnniversaryCounter selected) {
        counterTabGroup.removeAllViews();
        int hPad = dp(16);
        int vPad = dp(8);
        int margin = dp(8);
        for (AnniversaryCounter c : all) {
            boolean isSelected = selected != null && c.id.equals(selected.id);
            TextView tab = new TextView(this);
            tab.setText(c.displayLabel(this));
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tab.setPadding(hPad, vPad, hPad, vPad);
            tab.setBackgroundResource(isSelected ? R.drawable.btn_outline_selected : R.drawable.btn_outline);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(margin);
            tab.setLayoutParams(params);
            tab.setOnClickListener(v -> renderCounterTabs(all, c));
            counterTabGroup.addView(tab);
        }
        bindCounter(selected);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    /** Zeigt die Seite fuer den uebergebenen Zaehler an; null blendet die Pro-Zaehler-Aktionen aus. */
    private void bindCounter(AnniversaryCounter counter) {
        this.counter = counter;
        if (counter == null) {
            perCounterActions.setVisibility(View.GONE);
            tvTitle.setText(R.string.counter_settings_title_generic);
        } else {
            perCounterActions.setVisibility(View.VISIBLE);
            tvTitle.setText(getString(R.string.counter_settings_title, counter.displayLabel(this)));
            cbShowMessage.setChecked(counter.showMessage);
            cbShowName.setChecked(counter.showName);
            cbCustomAppearance.setChecked(counter.customAppearance);
            cbCountdownToNext.setChecked(counter.countdownToNext);
            cbShowAge.setChecked(counter.showAge);
            bindContact();
        }
    }

    private void bindContact() {
        boolean hasContact = counter.hasContact();
        btnSetContact.setText(hasContact
                ? getString(R.string.btn_change_contact, counter.contactLabel())
                : getString(R.string.btn_set_contact));
        btnNotifyContact.setVisibility(hasContact ? View.VISIBLE : View.GONE);
        btnRemoveContact.setVisibility(hasContact ? View.VISIBLE : View.GONE);
    }

    private void pickContact() {
        if (counter == null) {
            return;
        }
        // Der Kontaktwaehler gibt Lesezugriff auf genau den gewaehlten Eintrag, READ_CONTACTS
        // ist dafuer nicht noetig.
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        try {
            startActivityForResult(pick, REQUEST_PICK_CONTACT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.contact_pick_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void saveContactFromPicker(Uri uri) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
        };
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(1)) {
                saveContact(cursor.getString(0), cursor.getString(1));
                return;
            }
        } catch (SecurityException e) {
            // faellt unten auf die Fehlermeldung durch
        }
        Toast.makeText(this, R.string.contact_pick_failed, Toast.LENGTH_LONG).show();
    }

    private void saveContact(String name, String phone) {
        if (counter == null) {
            return;
        }
        counter.contactName = name;
        counter.contactPhone = phone;
        AnniversaryCounterStore.update(this, counter);
        bindContact();
    }

    private void editDate() {
        if (counter == null) {
            return;
        }
        LocalDateTime base = counter.start;
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> pickTime(year, month + 1, dayOfMonth),
                base.getYear(),
                base.getMonthValue() - 1,
                base.getDayOfMonth());
        dialog.setTitle(R.string.pick_date);
        dialog.show();
    }

    private void pickTime(int year, int month, int day) {
        LocalDateTime base = counter.start;
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    counter.start = LocalDateTime.of(year, month, day, hourOfDay, minute, 0);
                    AnniversaryCounterStore.update(this, counter);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(this);
                    // Meilenstein-Zieldaten haengen am Startzeitpunkt und muessen neu geplant werden
                    MilestoneScheduler.scheduleForCounter(this, counter);
                },
                base.getHour(),
                base.getMinute(),
                true);
        dialog.setTitle(R.string.pick_time);
        dialog.show();
    }

    private void editMessage() {
        if (counter == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setHint(R.string.custom_message_hint);
        if (counter.message != null) {
            input.setText(counter.message);
            input.setSelection(counter.message.length());
        }

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.custom_message_dialog_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    counter.message = value.isEmpty() ? null : value;
                    AnniversaryCounterStore.update(this, counter);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(this);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void editName() {
        if (counter == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setHint(R.string.name_dialog_hint);
        if (counter.name != null) {
            input.setText(counter.name);
            input.setSelection(counter.name.length());
        }

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.name_dialog_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    counter.name = value.isEmpty() ? null : value;
                    AnniversaryCounterStore.update(this, counter);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(this);
                    buildCounterTabs(counter.id);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openNotificationSettings() {
        if (counter == null) {
            return;
        }
        Intent intent = new Intent(this, MilestoneSettingsActivity.class);
        intent.putExtra(MilestoneSettingsActivity.EXTRA_COUNTER_ID, counter.id);
        startActivity(intent);
    }

    private void openBackgroundSettings() {
        if (counter == null) {
            return;
        }
        Intent intent = new Intent(this, BackgroundSettingsActivity.class);
        intent.putExtra(BackgroundSettingsActivity.EXTRA_COUNTER_ID, counter.id);
        startActivity(intent);
    }

    private void exportBackup() {
        try {
            String json = BackupManager.export(this);
            File dir = new File(getCacheDir(), "backups");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "anniversary_counter_backup.json");
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.backup_share_title)));
        } catch (IOException e) {
            Toast.makeText(this, R.string.backup_export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmImportBackup() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_confirm_title)
                .setMessage(R.string.backup_restore_confirm_message)
                .setPositiveButton(R.string.backup_restore_confirm_button, (dialog, which) -> {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.setType("application/json");
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(pick, REQUEST_IMPORT_BACKUP);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CALENDAR_IMPORT) {
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
            buildCounterTabs(counter != null ? counter.id : null);
        } else if (data != null && data.getData() != null) {
            if (requestCode == REQUEST_IMPORT_BACKUP) {
                restoreBackup(data.getData());
            } else if (requestCode == REQUEST_PICK_CONTACT) {
                saveContactFromPicker(data.getData());
            }
        }
    }

    private void restoreBackup(Uri uri) {
        try {
            String json = readText(uri);
            BackupManager.Result result = BackupManager.restore(this, json);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
            Toast.makeText(this, getString(R.string.backup_import_success, result.counterCount), Toast.LENGTH_LONG)
                    .show();
            if (result.anyMilestonesEnabled && requestNotificationPermissionIfNeeded()) {
                return; // finish() erfolgt in onRequestPermissionsResult
            }
            finish();
        } catch (IOException e) {
            Toast.makeText(this, R.string.backup_import_failed, Toast.LENGTH_LONG).show();
        }
    }

    private String readText(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Datei konnte nicht geoeffnet werden");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private boolean requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            finish();
        }
    }

    private void deleteCounter() {
        if (counter == null) {
            return;
        }
        String label = counter.displayLabel(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_counter_title)
                .setMessage(getString(R.string.delete_counter_message, label))
                .setPositiveButton(R.string.delete_confirm, (dialog, which) -> {
                    MilestoneScheduler.cancelForCounter(this, counter);
                    BackgroundImageStore.clear(this, counter.id);
                    AnniversaryCounterStore.delete(this, counter.id);
                    AnniversaryCounterWidgetProvider.updateAllWidgets(this);
                    buildCounterTabs(null);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
