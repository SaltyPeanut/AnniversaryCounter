package de.anniversarycounter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

/** Untermenü des Zahnrad-Menues: legt per Opt-in/Opt-out fest, was im Schnellmenue erscheint. */
public class QuickMenuSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_menu_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        LinearLayout rows = findViewById(R.id.quickMenuSettingsRows);
        Set<QuickMenuItem> selected = AppSettings.quickMenuItems(this);
        QuickMenuItem.Page currentPage = null;
        for (QuickMenuItem item : QuickMenuItem.values()) {
            if (item.page != currentPage) {
                currentPage = item.page;
                rows.addView(pageHeading(rows, currentPage));
            }
            rows.addView(itemCheckBox(rows, item, selected.contains(item)));
        }
    }

    private TextView pageHeading(LinearLayout parent, QuickMenuItem.Page page) {
        TextView heading = new TextView(this);
        heading.setText(page.titleRes);
        heading.setTextColor(getColor(R.color.text_secondary));
        heading.setTextSize(13);
        heading.setTypeface(heading.getTypeface(), android.graphics.Typeface.BOLD);
        int density = (int) getResources().getDisplayMetrics().density;
        heading.setPadding(0, 20 * density, 0, 8 * density);
        return heading;
    }

    private CheckBox itemCheckBox(LinearLayout parent, QuickMenuItem item, boolean checked) {
        CheckBox check = (CheckBox) LayoutInflater.from(this)
                .inflate(R.layout.item_quick_menu_check, parent, false);
        check.setText(item.labelRes);
        check.setChecked(checked);
        check.setOnCheckedChangeListener((button, on) -> {
            // Frisch lesen, damit jeder Haken auf dem aktuellen Stand aufbaut.
            Set<QuickMenuItem> items = AppSettings.quickMenuItems(this);
            if (on) {
                items.add(item);
            } else {
                items.remove(item);
            }
            AppSettings.setQuickMenuItems(this, items);
        });
        return check;
    }
}
