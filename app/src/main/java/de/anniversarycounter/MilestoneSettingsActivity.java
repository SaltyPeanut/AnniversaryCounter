package de.anniversarycounter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Meilenstein-Einstellungen (Standard-Toggles + eigene Meilensteine) fuer einen einzelnen Zaehler. */
public class MilestoneSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;

    private AnniversaryCounter counter;
    private LinearLayout customContainer;
    private final List<CustomRow> customRows = new ArrayList<>();

    private CheckBox cbYearly;
    private CheckBox cbDays100;
    private CheckBox cbMonths6;
    private CheckBox cbYears1;
    private CheckBox cbYears2;
    private CheckBox cbDays1000;
    private CheckBox cbYears5;
    private CheckBox cbYears10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milestone_settings);

        String counterId = getIntent().getStringExtra(EXTRA_COUNTER_ID);
        counter = AnniversaryCounterStore.find(this, counterId);
        if (counter == null) {
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvMilestoneTitle);
        tvTitle.setText(getString(R.string.milestone_settings_title, counter.displayLabel(this)));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        cbYearly = findViewById(R.id.cbYearly);
        cbYearly.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.YEARLY.name()));

        cbDays100 = findViewById(R.id.cbDays100);
        cbMonths6 = findViewById(R.id.cbMonths6);
        cbYears1 = findViewById(R.id.cbYears1);
        cbYears2 = findViewById(R.id.cbYears2);
        cbDays1000 = findViewById(R.id.cbDays1000);
        cbYears5 = findViewById(R.id.cbYears5);
        cbYears10 = findViewById(R.id.cbYears10);

        cbDays100.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.DAYS_100.name()));
        cbMonths6.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.MONTHS_6.name()));
        cbYears1.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.YEARS_1.name()));
        cbYears2.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.YEARS_2.name()));
        cbDays1000.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.DAYS_1000.name()));
        cbYears5.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.YEARS_5.name()));
        cbYears10.setChecked(counter.enabledStandardMilestones.contains(Milestone.Standard.YEARS_10.name()));

        customContainer = findViewById(R.id.customMilestonesContainer);
        for (CustomMilestone custom : counter.customMilestones) {
            addCustomRow(custom.amount, custom.unit);
        }

        Button btnAdd = findViewById(R.id.btnAddMilestoneRow);
        btnAdd.setOnClickListener(v -> addCustomRow(null, Milestone.Unit.DAYS));

        Button btnSave = findViewById(R.id.btnSaveMilestones);
        btnSave.setOnClickListener(v -> save());
    }

    private void addCustomRow(Integer amount, Milestone.Unit unit) {
        View row = getLayoutInflater().inflate(R.layout.item_custom_milestone, customContainer, false);
        EditText etAmount = row.findViewById(R.id.etAmount);
        Spinner spUnit = row.findViewById(R.id.spUnit);
        Button btnRemove = row.findViewById(R.id.btnRemoveMilestone);

        if (amount != null) {
            etAmount.setText(String.valueOf(amount));
        }

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.milestone_units, android.R.layout.simple_spinner_dropdown_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);
        spUnit.setSelection(unit.ordinal());

        CustomRow customRow = new CustomRow(etAmount, spUnit);
        customRows.add(customRow);
        btnRemove.setOnClickListener(v -> {
            customContainer.removeView(row);
            customRows.remove(customRow);
        });

        customContainer.addView(row);
    }

    private void save() {
        Set<String> standard = new HashSet<>();
        if (cbYearly.isChecked()) {
            standard.add(Milestone.Standard.YEARLY.name());
        }
        if (cbDays100.isChecked()) {
            standard.add(Milestone.Standard.DAYS_100.name());
        }
        if (cbMonths6.isChecked()) {
            standard.add(Milestone.Standard.MONTHS_6.name());
        }
        if (cbYears1.isChecked()) {
            standard.add(Milestone.Standard.YEARS_1.name());
        }
        if (cbYears2.isChecked()) {
            standard.add(Milestone.Standard.YEARS_2.name());
        }
        if (cbDays1000.isChecked()) {
            standard.add(Milestone.Standard.DAYS_1000.name());
        }
        if (cbYears5.isChecked()) {
            standard.add(Milestone.Standard.YEARS_5.name());
        }
        if (cbYears10.isChecked()) {
            standard.add(Milestone.Standard.YEARS_10.name());
        }

        List<CustomMilestone> custom = new ArrayList<>();
        for (CustomRow row : customRows) {
            String amountText = row.etAmount.getText().toString().trim();
            if (amountText.isEmpty()) {
                continue;
            }
            int amount;
            try {
                amount = Integer.parseInt(amountText);
            } catch (NumberFormatException e) {
                continue;
            }
            if (amount <= 0) {
                continue;
            }
            Milestone.Unit unit = Milestone.Unit.values()[row.spUnit.getSelectedItemPosition()];
            custom.add(CustomMilestone.create(amount, unit));
        }

        // Solange die alte Liste noch gilt: eigene Meilensteine bekommen unten neue IDs, ihre
        // alten Alarme liessen sich danach nicht mehr finden.
        MilestoneScheduler.cancelForCounter(this, counter);

        counter.enabledStandardMilestones = standard;
        counter.customMilestones = custom;
        AnniversaryCounterStore.update(this, counter);
        MilestoneScheduler.scheduleForCounter(this, counter);

        boolean anyEnabled = !standard.isEmpty() || !custom.isEmpty();
        if (anyEnabled && requestNotificationPermissionIfNeeded()) {
            return; // finish() erfolgt in onRequestPermissionsResult
        }
        finish();
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
        finish();
    }

    private static class CustomRow {
        final EditText etAmount;
        final Spinner spUnit;

        CustomRow(EditText etAmount, Spinner spUnit) {
            this.etAmount = etAmount;
            this.spUnit = spUnit;
        }
    }
}
