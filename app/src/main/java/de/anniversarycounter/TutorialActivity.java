package de.anniversarycounter;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Erklaert die App in Wisch-Seiten, einmal beim ersten Start und erneut ueber das Zahnrad-Menue.
 * Jede Seite zeigt eine Skizze der Hauptseite, in der der erklaerte Bereich markiert ist.
 */
public class TutorialActivity extends AppCompatActivity {

    /** Aus den Einstellungen gestartet: am Ende nur dorthin zurueck, statt die Hauptseite zu oeffnen. */
    static final String EXTRA_REPLAY = "replay";

    private static final String ARROW_SIDEWAYS = "←  →";

    /** Was die Skizze einer Seite hervorhebt; {@code menuRows} zeigt das offene Schnellmenue. */
    private enum Slide {
        WELCOME(R.string.tutorial_welcome_title, R.string.tutorial_welcome_body, false, false),
        EVENTS(R.string.tutorial_events_title, R.string.tutorial_events_body, true, false),
        MENU(R.string.tutorial_menu_title, R.string.tutorial_menu_body, false, false,
                R.string.btn_set_date, R.string.btn_set_name, R.string.btn_set_message),
        SETTINGS(R.string.tutorial_settings_title, R.string.tutorial_settings_body, false, true),
        REMINDERS(R.string.tutorial_reminders_title, R.string.tutorial_reminders_body, false, true,
                R.string.is_birthday, R.string.countdown_to_next, R.string.btn_notifications),
        MORE(R.string.tutorial_more_title, R.string.tutorial_more_body, false, true,
                R.string.btn_export_backup, R.string.btn_import_backup);

        final int titleRes;
        final int bodyRes;
        final boolean highlightTime;
        final boolean highlightGear;
        final int[] menuRows;

        Slide(int titleRes, int bodyRes, boolean highlightTime, boolean highlightGear, int... menuRows) {
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
            this.highlightTime = highlightTime;
            this.highlightGear = highlightGear;
            this.menuRows = menuRows;
        }
    }

    private ViewPager2 pager;
    private LinearLayout dots;
    private Button next;
    private View skip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        pager = findViewById(R.id.tutorialPager);
        dots = findViewById(R.id.tutorialDots);
        next = findViewById(R.id.btnTutorialNext);
        skip = findViewById(R.id.btnTutorialSkip);
        pager.setAdapter(new SlideAdapter());
        buildDots();

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                showPosition(position);
            }
        });
        showPosition(pager.getCurrentItem());

        skip.setOnClickListener(v -> finishTutorial());
        next.setOnClickListener(v -> {
            int position = pager.getCurrentItem();
            if (position == Slide.values().length - 1) {
                finishTutorial();
            } else {
                pager.setCurrentItem(position + 1);
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int position = pager.getCurrentItem();
                if (position > 0) {
                    pager.setCurrentItem(position - 1);
                } else {
                    finishTutorial();
                }
            }
        });
    }

    private void finishTutorial() {
        AppSettings.setTutorialSeen(this);
        if (!getIntent().getBooleanExtra(EXTRA_REPLAY, false)) {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private void buildDots() {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        int margin = (int) (4 * density);
        for (int i = 0; i < Slide.values().length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dots.addView(dot);
        }
    }

    private void showPosition(int position) {
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setBackgroundResource(
                    i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
        boolean last = position == Slide.values().length - 1;
        next.setText(last ? R.string.tutorial_done : R.string.tutorial_next);
        skip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
    }

    private final class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tutorial_page, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(Slide.values()[position]);
        }

        @Override
        public int getItemCount() {
            return Slide.values().length;
        }

        final class Holder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final TextView body;
            private final View gear;
            private final View time;
            private final TextView arrow;
            private final TextView mockPosition;
            private final LinearLayout sheet;

            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvTutorialTitle);
                body = itemView.findViewById(R.id.tvTutorialBody);
                gear = itemView.findViewById(R.id.mockGear);
                time = itemView.findViewById(R.id.mockTime);
                arrow = itemView.findViewById(R.id.mockArrow);
                mockPosition = itemView.findViewById(R.id.mockPosition);
                sheet = itemView.findViewById(R.id.mockSheet);
            }

            void bind(Slide slide) {
                title.setText(slide.titleRes);
                body.setText(slide.bodyRes);
                gear.setBackgroundResource(slide.highlightGear ? R.drawable.bg_tutorial_highlight : R.drawable.btn_circle);
                time.setBackgroundResource(slide.highlightTime ? R.drawable.bg_tutorial_highlight : 0);
                arrow.setText(slide.highlightTime ? ARROW_SIDEWAYS : "");
                mockPosition.setText(getString(R.string.quick_menu_counter_position, 1, 3));

                boolean menuOpen = slide.menuRows.length > 0;
                mockPosition.setVisibility(menuOpen ? View.GONE : View.VISIBLE);
                sheet.setVisibility(menuOpen ? View.VISIBLE : View.GONE);
                sheet.removeAllViews();
                for (int row : slide.menuRows) {
                    sheet.addView(mockRow(row));
                }
            }

            private TextView mockRow(int labelRes) {
                TextView row = new TextView(itemView.getContext());
                row.setText(labelRes);
                row.setTextSize(10);
                row.setTextColor(getColor(R.color.text_primary));
                row.setGravity(android.view.Gravity.CENTER);
                row.setBackgroundResource(R.drawable.btn_outline);
                int padding = (int) (6 * getResources().getDisplayMetrics().density);
                row.setPadding(padding, padding, padding, padding);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = padding;
                row.setLayoutParams(params);
                return row;
            }
        }
    }
}
