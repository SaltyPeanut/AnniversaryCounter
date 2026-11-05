package de.anniversarycounter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Alle Einstellungen, erreichbar ueber das Zahnrad in {@link MainActivity}. Sie sind in dieselben
 * Gruppen wie das Schnellmenue aufgeteilt, zwischen denen man seitlich wischt; die Reiter oben
 * waehlen das Ereignis fuer die ereignisbezogenen Seiten.
 */
public class CounterSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int REQUEST_IMPORT_BACKUP = 300;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 301;
    private static final int REQUEST_CALENDAR_IMPORT = 302;
    private static final int REQUEST_PICK_CONTACT = 303;

    /** Eine Gruppe von Einstellungen; ohne Ereignis bleiben nur die Seiten ohne {@code needsCounter}. */
    private enum Page {
        EDIT(R.layout.page_settings_edit, R.string.menu_page_edit, true, true),
        DISPLAY(R.layout.page_settings_display, R.string.menu_page_display, true, true),
        REMINDERS(R.layout.page_settings_reminders, R.string.menu_page_notifications, true, true),
        MANAGE(R.layout.page_settings_manage, R.string.menu_page_manage, false, true),
        GENERAL(R.layout.page_settings_general, R.string.settings_section_global, false, false);

        final int layoutRes;
        final int titleRes;
        final boolean needsCounter;
        final boolean forCounter;

        Page(int layoutRes, int titleRes, boolean needsCounter, boolean forCounter) {
            this.layoutRes = layoutRes;
            this.titleRes = titleRes;
            this.needsCounter = needsCounter;
            this.forCounter = forCounter;
        }
    }

    private final Map<Page, View> pageViews = new EnumMap<>(Page.class);
    private final List<Page> visiblePages = new ArrayList<>();
    private AnniversaryCounter counter;

    private LinearLayout counterTabGroup;
    private View tabs;
    private TextView tvPageTitle;
    private TextView tvTitle;
    private ViewPager2 pager;
    private LinearLayout dots;
    private Button btnDeleteCounter;
    private Button btnSetDate;
    private Button btnSetMessage;
    private Button btnSetName;
    private CheckBox cbShowMessage;
    private CheckBox cbShowName;
    private CheckBox cbCustomAppearance;
    private CheckBox cbCountdownToNext;
    private CheckBox cbIsBirthday;
    private Button btnBirthdaySettings;
    private Button btnSetContact;
    private Button btnNotifyContact;
    private Button btnRemoveContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter_settings);

        counterTabGroup = findViewById(R.id.counterTabGroup);
        tabs = findViewById(R.id.settingsTabs);
        tvPageTitle = findViewById(R.id.tvSettingsPageTitle);
        tvTitle = findViewById(R.id.tvCounterSettingsTitle);
        dots = findViewById(R.id.settingsDots);
        pager = findViewById(R.id.settingsPager);
        pager.setOffscreenPageLimit(Page.values().length - 1);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                showPageInfo(position);
            }
        });
        for (Page page : Page.values()) {
            pageViews.put(page, getLayoutInflater().inflate(page.layoutRes, pager, false));
        }
        btnSetDate = findViewById(R.id.btnSetDate);
        btnSetMessage = findViewById(R.id.btnSetMessage);
        btnSetName = findViewById(R.id.btnSetName);
        cbShowMessage = findViewById(R.id.cbShowMessage);
        cbShowName = findViewById(R.id.cbShowName);
        cbCustomAppearance = findViewById(R.id.cbCustomAppearance);
        cbCountdownToNext = findViewById(R.id.cbCountdownToNext);
        cbIsBirthday = findViewById(R.id.cbIsBirthday);
        btnBirthdaySettings = findViewById(R.id.btnBirthdaySettings);
        cbCountdownToNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (counter == null || counter.countdownToNext == isChecked) {
                return;
            }
            counter.countdownToNext = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        });
        cbIsBirthday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnBirthdaySettings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (counter == null || counter.isBirthday == isChecked) {
                return;
            }
            counter.isBirthday = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
            // Der Text der jaehrlichen Erinnerung steht schon im geplanten Alarm.
            MilestoneScheduler.scheduleForCounter(this, counter);
        });
        btnBirthdaySettings.setOnClickListener(v -> openBirthdaySettings());
        Button btnCounterAppearance = findViewById(R.id.btnCounterAppearance);
        Button btnNotifications = findViewById(R.id.btnNotifications);
        Button btnChangeBackground = findViewById(R.id.btnChangeBackground);
        btnDeleteCounter = findViewById(R.id.btnDeleteCounter);
        Button btnAddCounter = findViewById(R.id.btnAddCounter);
        ImageButton btnBack = findViewById(R.id.btnBack);
        CheckBox cbUseLightTheme = findViewById(R.id.cbUseLightTheme);

        cbUseLightTheme.setChecked(AppSettings.useLightTheme(this));
        cbUseLightTheme.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppSettings.setUseLightTheme(this, isChecked));

        Button btnAppearance = findViewById(R.id.btnAppearance);
        btnAppearance.setOnClickListener(v ->
                startActivity(new Intent(this, AppearanceSettingsActivity.class)));

        Button btnFormatSettings = findViewById(R.id.btnFormatSettings);
        btnFormatSettings.setOnClickListener(v ->
                startActivity(new Intent(this, FormatSettingsActivity.class)));

        Button btnReorderCounters = findViewById(R.id.btnReorderCounters);
        btnReorderCounters.setOnClickListener(v ->
                startActivity(new Intent(this, CounterOrderActivity.class)));

        Button btnQuickMenuSettings = findViewById(R.id.btnQuickMenuSettings);
        btnQuickMenuSettings.setOnClickListener(v ->
                startActivity(new Intent(this, QuickMenuSettingsActivity.class)));

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

    /** Die Seiten haengen erst spaeter am Fenster; so finden die Aufrufe unten auch ihre Elemente. */
    @Override
    public <T extends View> T findViewById(int id) {
        T view = super.findViewById(id);
        if (view != null) {
            return view;
        }
        for (View page : pageViews.values()) {
            view = page.findViewById(id);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    /** Stellt die Seiten nur bei Bedarf um, damit der Pager nicht bei jedem Neuladen neu beginnt. */
    private void showPages(boolean hasCounter) {
        List<Page> wanted = new ArrayList<>();
        for (Page page : Page.values()) {
            if (hasCounter || !page.needsCounter) {
                wanted.add(page);
            }
        }
        if (!wanted.equals(visiblePages) || pager.getAdapter() == null) {
            visiblePages.clear();
            visiblePages.addAll(wanted);
            pager.setAdapter(new PagesAdapter());
            buildDots();
        }
        showPageInfo(pager.getCurrentItem());
    }

    private void buildDots() {
        dots.removeAllViews();
        int size = dp(8);
        int margin = dp(4);
        for (int i = 0; i < visiblePages.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dots.addView(dot);
        }
    }

    private void showPageInfo(int position) {
        if (position < 0 || position >= visiblePages.size()) {
            return;
        }
        Page page = visiblePages.get(position);
        tvPageTitle.setText(page.titleRes);
        boolean forCounter = page.forCounter && counter != null;
        tabs.setVisibility(forCounter ? View.VISIBLE : View.GONE);
        tvTitle.setVisibility(forCounter ? View.VISIBLE : View.GONE);
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setBackgroundResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    /** Gibt die fertig erzeugten Seiten an den Pager; jede Seite hat ihren eigenen Typ. */
    private final class PagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return visiblePages.get(position).ordinal();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = pageViews.get(Page.values()[viewType]);
            if (page.getParent() instanceof ViewGroup) {
                ((ViewGroup) page.getParent()).removeView(page);
            }
            return new RecyclerView.ViewHolder(page) { };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return visiblePages.size();
        }
    }

    /**
     * Unterseiten speichern ihre eigene Kopie des Zaehlers. Ohne Neuladen wuerde die naechste
     * Aenderung hier deren Stand mit dem veralteten {@link #counter} ueberschreiben.
     */
    @Override
    protected void onResume() {
        super.onResume();
        buildCounterTabs(counter != null ? counter.id : null);
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
        showPages(counter != null);
        btnDeleteCounter.setVisibility(counter != null ? View.VISIBLE : View.GONE);
        if (counter == null) {
            tvTitle.setText(R.string.counter_settings_title_generic);
        } else {
            tvTitle.setText(getString(R.string.counter_settings_title, counter.displayLabel(this)));
            cbShowMessage.setChecked(counter.showMessage);
            cbShowName.setChecked(counter.showName);
            cbCustomAppearance.setChecked(counter.customAppearance);
            cbCountdownToNext.setChecked(counter.countdownToNext);
            cbIsBirthday.setChecked(counter.isBirthday);
            btnBirthdaySettings.setVisibility(counter.isBirthday ? View.VISIBLE : View.GONE);
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
        if (counter != null) {
            CounterActions.pickContact(this, REQUEST_PICK_CONTACT);
        }
    }

    private void saveContact(String name, String phone) {
        if (counter != null) {
            CounterActions.saveContact(this, counter, name, phone, this::bindContact);
        }
    }

    private void editDate() {
        if (counter != null) {
            CounterActions.editDate(this, counter, () -> { });
        }
    }

    private void editMessage() {
        if (counter != null) {
            CounterActions.editMessage(this, counter, () -> { });
        }
    }

    private void editName() {
        if (counter != null) {
            CounterActions.editName(this, counter, () -> buildCounterTabs(counter.id));
        }
    }

    private void openNotificationSettings() {
        if (counter == null) {
            return;
        }
        Intent intent = new Intent(this, MilestoneSettingsActivity.class);
        intent.putExtra(MilestoneSettingsActivity.EXTRA_COUNTER_ID, counter.id);
        startActivity(intent);
    }

    private void openBirthdaySettings() {
        if (counter == null) {
            return;
        }
        Intent intent = new Intent(this, BirthdaySettingsActivity.class);
        intent.putExtra(BirthdaySettingsActivity.EXTRA_COUNTER_ID, counter.id);
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
                if (counter != null) {
                    CounterActions.saveContactFromPicker(this, counter, data.getData(), this::bindContact);
                }
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
        if (counter != null) {
            CounterActions.delete(this, counter, () -> buildCounterTabs(null));
        }
    }
}
