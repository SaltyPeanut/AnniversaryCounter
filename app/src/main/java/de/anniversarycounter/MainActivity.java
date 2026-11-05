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
 * gewechselt wird. Haeufige Aktionen liegen im ausfahrbaren {@link QuickMenuController}, das
 * Zahnrad oeffnet {@link CounterSettingsActivity} mit allen Einstellungen fuer den sichtbaren Zaehler.
 *
 * Die Daten liegen in {@link AnniversaryCounterStore}, ueberleben also Neustarts. Ein
 * Handler aktualisiert die sichtbaren Seiten jede Sekunde, solange die Activity
 * im Vordergrund ist. Jeder Rueckkehr aus einer Untermenue-Activity (onResume)
 * laedt {@link #refreshCounters()} den aktuellen Stand neu.
 */
public class MainActivity extends AppCompatActivity implements QuickMenuController.Host {

    /** Extra, ueber das eine Meilenstein-Benachrichtigung direkt zum passenden Zaehler springt. */
    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int MAX_DOTS = 15;
    private static final int REQUEST_PICK_CONTACT = 400;
    private static final int REQUEST_CALENDAR_IMPORT = 401;
    private static final String STATE_CONTACT_COUNTER_ID = "contact_counter_id";
    private static final String STATE_MENU_PAGE = "menu_page";

    private ViewPager2 pager;
    private CounterPagerAdapter adapter;
    private LinearLayout dotsRow;
    private ImageView ivBackground;
    private View vScrim;
    private TextView tvAppTitle;
    private QuickMenuController quickMenu;

    /** Zaehler, fuer den der Kontaktwaehler offen ist; uebersteht einen Neuaufbau waehrenddessen. */
    private String contactCounterId;

    /** Zaehler vor einem Neuaufbau (z. B. Designwechsel); die Liste des Adapters ist dann noch leer. */
    private String restoredCounterId;

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
        if (savedInstanceState != null) {
            // Neuaufbau: Android liefert das urspruengliche Intent, der Sprung ist aber schon erfolgt.
            getIntent().removeExtra(EXTRA_COUNTER_ID);
            restoredCounterId = savedInstanceState.getString(EXTRA_COUNTER_ID);
            contactCounterId = savedInstanceState.getString(STATE_CONTACT_COUNTER_ID);
        }
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
        quickMenu = new QuickMenuController(this, this);
        if (savedInstanceState != null) {
            quickMenu.restorePage(savedInstanceState.getString(STATE_MENU_PAGE));
        }
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                highlightDot(position);
                updateBackground(position);
                // Laeuft oft mitten im Layout-Durchlauf; dort hinzugefuegte Views blieben ohne Groesse.
                pager.post(quickMenu::rebuild);
            }
        });

        btnSettings.setOnClickListener(v -> openCounterSettings());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AnniversaryCounter counter = currentCounter();
        if (counter != null) {
            outState.putString(EXTRA_COUNTER_ID, counter.id);
        }
        outState.putString(STATE_CONTACT_COUNTER_ID, contactCounterId);
        outState.putString(STATE_MENU_PAGE, quickMenu.currentPageName());
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
        // Gesprungen wird im folgenden onResume, wenn die Zaehler-Liste aktuell ist.
        setIntent(intent);
    }

    /**
     * Laedt die Zaehler-Liste neu aus dem Store (z. B. nach Rueckkehr aus
     * {@link CounterSettingsActivity}), versucht denselben Zaehler wieder anzuzeigen und
     * startet bei einer leeren Liste die Anlege-Dialogkette.
     */
    @Override
    public void refreshCounters() {
        tvAppTitle.setTypeface(AppSettings.typeface(this, AppSettings.Target.TEXT));
        tvAppTitle.setTextColor(AppSettings.textColor(this, AppSettings.Target.TEXT));

        String keepId = currentCounter() != null ? currentCounter().id : restoredCounterId;
        restoredCounterId = null;

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
        quickMenu.rebuild();

        if (counters.isEmpty()) {
            // frische Installation bzw. alle Zaehler geloescht: direkt fragen
            CounterCreationFlow.start(this, counter -> {
                AnniversaryCounterWidgetProvider.updateAllWidgets(this);
                refreshCounters();
            });
        }
    }

    @Override
    public AnniversaryCounter currentCounter() {
        return adapter.getCounter(pager.getCurrentItem());
    }

    @Override
    public void onCounterCreated(AnniversaryCounter counter) {
        refreshCounters();
        jumpToCounter(counter.id, false);
    }

    @Override
    public void pickContact(AnniversaryCounter counter) {
        contactCounterId = counter.id;
        CounterActions.pickContact(this, REQUEST_PICK_CONTACT);
    }

    @Override
    public void openCalendarImport() {
        startActivityForResult(new Intent(this, CalendarImportActivity.class), REQUEST_CALENDAR_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CALENDAR_IMPORT) {
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        } else if (requestCode == REQUEST_PICK_CONTACT && data != null && data.getData() != null) {
            AnniversaryCounter counter = AnniversaryCounterStore.find(this, contactCounterId);
            if (counter != null) {
                CounterActions.saveContactFromPicker(this, counter, data.getData(), this::refreshCounters);
            }
        }
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

    /**
     * Tippt man auf eine Meilenstein-Benachrichtigung oder das Widget, springt der Pager einmalig
     * zum Zaehler. Das Extra wird entfernt, sonst ginge es bei jedem onResume wieder dorthin zurueck.
     */
    private void jumpToCounterFromIntent(Intent intent) {
        String counterId = intent.getStringExtra(EXTRA_COUNTER_ID);
        if (counterId != null) {
            intent.removeExtra(EXTRA_COUNTER_ID);
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
