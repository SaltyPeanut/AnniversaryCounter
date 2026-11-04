package de.anniversarycounter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

/**
 * Anniversary Counter – zeigt mehrere Zaehler, zwischen denen per Wischen (ViewPager2)
 * gewechselt wird. Das Zahnrad oeffnet {@link CounterSettingsActivity} fuer den
 * sichtbaren Zaehler; die Hauptseite selbst bleibt dadurch strukturell konstant.
 *
 * Die Daten liegen in {@link AnniversaryCounterStore}, ueberleben also Neustarts. Ein
 * Handler aktualisiert die sichtbaren Seiten jede Sekunde, solange die Activity
 * im Vordergrund ist. Jeder Rueckkehr aus einer Untermenue-Activity (onResume)
 * laedt {@link #refreshCounters()} den aktuellen Stand neu.
 */
public class MainActivity extends AppCompatActivity {

    /** Extra, ueber das eine Meilenstein-Benachrichtigung direkt zum passenden Zaehler springt. */
    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int MAX_DOTS = 15;

    private ViewPager2 pager;
    private CounterPagerAdapter adapter;
    private LinearLayout dotsRow;
    private ImageView ivBackground;
    private View vScrim;
    private TextView tvAppTitle;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            adapter.tick();
            // exakt zur naechsten vollen Sekunde neu planen, damit die Anzeige nicht driftet
            handler.postDelayed(this, 1000L - (System.currentTimeMillis() % 1000L));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSettings.hasLanguage(this)) {
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        dotsRow = findViewById(R.id.dotsRow);
        ivBackground = findViewById(R.id.ivBackground);
        vScrim = findViewById(R.id.vScrim);
        tvAppTitle = findViewById(R.id.tvAppTitle);
        ImageButton btnSettings = findViewById(R.id.btnSettings);

        adapter = new CounterPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                highlightDot(position);
                updateBackground(position);
            }
        });

        btnSettings.setOnClickListener(v -> openCounterSettings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(ticker);
        refreshCounters();
        jumpToCounterFromIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        jumpToCounterFromIntent(intent);
    }

    /**
     * Laedt die Zaehler-Liste neu aus dem Store (z. B. nach Rueckkehr aus
     * {@link CounterSettingsActivity}), versucht denselben Zaehler wieder anzuzeigen und
     * startet bei einer leeren Liste die Anlege-Dialogkette.
     */
    private void refreshCounters() {
        tvAppTitle.setTypeface(AppSettings.typeface(this, AppSettings.Target.TEXT));
        tvAppTitle.setTextColor(AppSettings.textColor(this, AppSettings.Target.TEXT));

        String keepId = currentCounter() != null ? currentCounter().id : null;

        List<AnniversaryCounter> counters = AnniversaryCounterStore.loadAll(this);
        adapter.setCounters(counters);
        buildDots(counters.size());

        int position = 0;
        if (keepId != null) {
            for (int i = 0; i < counters.size(); i++) {
                if (counters.get(i).id.equals(keepId)) {
                    position = i;
                    break;
                }
            }
        }
        if (position < counters.size()) {
            pager.setCurrentItem(position, false);
        }
        updateBackground(pager.getCurrentItem());

        if (counters.isEmpty()) {
            // frische Installation bzw. alle Zaehler geloescht: direkt fragen
            CounterCreationFlow.start(this, counter -> {
                AnniversaryCounterWidgetProvider.updateAllWidgets(this);
                refreshCounters();
            });
        }
    }

    private AnniversaryCounter currentCounter() {
        return adapter.getCounter(pager.getCurrentItem());
    }

    private void jumpToCounter(String counterId, boolean smooth) {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            AnniversaryCounter counter = adapter.getCounter(i);
            if (counter != null && counter.id.equals(counterId)) {
                pager.setCurrentItem(i, smooth);
                updateBackground(i);
                break;
            }
        }
    }

    /** Tippt man auf eine Meilenstein-Benachrichtigung, springt der Pager direkt zum Zaehler. */
    private void jumpToCounterFromIntent(Intent intent) {
        String counterId = intent.getStringExtra(EXTRA_COUNTER_ID);
        if (counterId != null) {
            jumpToCounter(counterId, false);
        }
    }

    private void openCounterSettings() {
        Intent intent = new Intent(this, CounterSettingsActivity.class);
        AnniversaryCounter counter = currentCounter();
        if (counter != null) {
            intent.putExtra(CounterSettingsActivity.EXTRA_COUNTER_ID, counter.id);
        }
        startActivity(intent);
    }

    /** Zeigt das eigene Hintergrundbild des Zaehlers an dieser Position, sonst das Standard-Muster. */
    private void updateBackground(int position) {
        AnniversaryCounter counter = adapter.getCounter(position);
        if (counter != null && BackgroundImageStore.hasCustomBackground(this, counter.id)) {
            ivBackground.setImageURI(Uri.fromFile(BackgroundImageStore.backgroundFile(this, counter.id)));
            vScrim.setVisibility(View.VISIBLE);
        } else {
            ivBackground.setImageResource(R.drawable.bg_hearts);
            vScrim.setVisibility(View.GONE);
        }
    }

    private void buildDots(int count) {
        dotsRow.removeAllViews();
        // Die Punktereihe scrollt nicht: bei sehr vielen Zaehlern (z. B. nach einem
        // Kalender-Import) liefe sie aus dem Bild, daher dann ganz ausblenden.
        if (count <= 1 || count > MAX_DOTS) {
            return;
        }
        int size = (int) (8 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        int current = pager.getCurrentItem();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == current ? R.drawable.dot_active : R.drawable.dot_inactive);
            dotsRow.addView(dot);
        }
    }

    private void highlightDot(int position) {
        for (int i = 0; i < dotsRow.getChildCount(); i++) {
            dotsRow.getChildAt(i).setBackgroundResource(
                    i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }
}
