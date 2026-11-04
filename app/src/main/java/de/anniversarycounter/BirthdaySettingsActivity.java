package de.anniversarycounter;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Untermenü fuer Zaehler, die als Geburtstag markiert sind: legt fest, ob das Alter erscheint. */
public class BirthdaySettingsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNTER_ID = "counter_id";

    private AnniversaryCounter counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday_settings);

        counter = AnniversaryCounterStore.find(this, getIntent().getStringExtra(EXTRA_COUNTER_ID));
        if (counter == null) {
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvBirthdayTitle);
        tvTitle.setText(getString(R.string.birthday_settings_title, counter.displayLabel(this)));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        CheckBox cbShowAge = findViewById(R.id.cbShowAge);
        cbShowAge.setChecked(counter.showAge);
        cbShowAge.setOnCheckedChangeListener((buttonView, isChecked) -> {
            counter.showAge = isChecked;
            AnniversaryCounterStore.update(this, counter);
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
            // Der Text der jaehrlichen Erinnerung steht schon im geplanten Alarm.
            MilestoneScheduler.scheduleForCounter(this, counter);
        });
    }
}
