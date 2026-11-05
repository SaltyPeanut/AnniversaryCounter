package de.anniversarycounter;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ausfahrbares Schnellmenue der Hauptseite. Eingefahren ist es unsichtbar, nur die Positionsanzeige
 * der Zaehler bleibt stehen; Hochwischen vom unteren Rand oder Antippen dieses Bereichs oeffnet es.
 * Offen reicht es bis zur Bildschirmmitte, zeigt die Eintraege seitenweise und laesst sich ueber
 * Herunterwischen, den Griff, den Zurueck-Button oder die Zurueck-Geste schliessen.
 * Welche Eintraege erscheinen, legt {@link QuickMenuSettingsActivity} fest.
 */
final class QuickMenuController {

    /** Was das Menue von der {@link MainActivity} braucht. */
    interface Host {
        AnniversaryCounter currentCounter();

        void refreshCounters();

        void onCounterCreated(AnniversaryCounter counter);

        void pickContact(AnniversaryCounter counter);

        void openCalendarImport();
    }

    private static final float MIN_COUNTER_SCALE = 0.35f;

    private final AppCompatActivity activity;
    private final Host host;
    private final View coordinator;
    private final View mainContent;
    private final ViewPager2 counterPager;
    private final View hint;
    private final View sheet;
    private final Drawable sheetBackground;
    private final float sheetElevation;
    private final View grip;
    private final View counterDots;
    private final TextView counterPosition;
    private final TextView title;
    private final ViewPager2 menuPager;
    private final View empty;
    private final LinearLayout menuDots;
    private final View back;
    private final BottomSheetBehavior<View> behavior;
    private final PageAdapter pageAdapter = new PageAdapter();
    private boolean hadCounter;
    private QuickMenuItem.Page restoredPage;
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            close();
        }
    };

    QuickMenuController(AppCompatActivity activity, Host host) {
        this.activity = activity;
        this.host = host;
        coordinator = activity.findViewById(R.id.mainCoordinator);
        mainContent = activity.findViewById(R.id.mainContent);
        counterPager = activity.findViewById(R.id.pager);
        hint = activity.findViewById(R.id.tvHint);
        sheet = activity.findViewById(R.id.quickMenuSheet);
        sheetBackground = sheet.getBackground().mutate();
        sheetElevation = sheet.getElevation();
        grip = activity.findViewById(R.id.quickMenuGrip);
        counterDots = activity.findViewById(R.id.dotsRow);
        counterPosition = activity.findViewById(R.id.tvCounterPosition);
        title = activity.findViewById(R.id.tvQuickMenuTitle);
        menuPager = activity.findViewById(R.id.quickMenuPager);
        empty = activity.findViewById(R.id.quickMenuEmpty);
        menuDots = activity.findViewById(R.id.quickMenuDots);
        back = activity.findViewById(R.id.btnQuickMenuBack);
        behavior = BottomSheetBehavior.from(sheet);

        // Nach Tastatureingaben laege sonst ein grauer Fokus-Kasten ueber dem ganzen Zaehler.
        counterPager.setDefaultFocusHighlightEnabled(false);
        counterPager.getChildAt(0).setDefaultFocusHighlightEnabled(false);

        menuPager.setAdapter(pageAdapter);
        // Sonst haelt das Sheet die horizontale Liste des Pagers fuer sein Scroll-Kind und laesst
        // sich ueber den Seiten nicht mehr herunterwischen.
        menuPager.getChildAt(0).setNestedScrollingEnabled(false);
        menuPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                showPageInfo(position);
                menuPager.post(QuickMenuController.this::updateDraggable);
            }
        });

        activity.findViewById(R.id.quickMenuHandle).setOnClickListener(v -> toggle());
        back.setOnClickListener(v -> close());
        activity.findViewById(R.id.btnQuickMenuCustomize).setOnClickListener(v ->
                activity.startActivity(new Intent(activity, QuickMenuSettingsActivity.class)));
        activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);

        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                backCallback.setEnabled(newState != BottomSheetBehavior.STATE_COLLAPSED);
                updateDraggable();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                applySlide(slideOffset);
            }
        });

        // Hoehe und Skalierung haengen von der Fenstergroesse ab, also auch von der Drehung.
        coordinator.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int wanted = (b - t) / 2;
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            if (params.height != wanted) {
                params.height = wanted;
                sheet.setLayoutParams(params);
            }
            coordinator.post(this::applyCurrentState);
        });
    }

    /** Name der aktuellen Menue-Seite, damit sie einen Neuaufbau der Activity uebersteht. */
    String currentPageName() {
        QuickMenuItem.Page page = pageAdapter.pageAt(menuPager.getCurrentItem());
        return page != null ? page.name() : null;
    }

    void restorePage(String name) {
        for (QuickMenuItem.Page page : QuickMenuItem.Page.values()) {
            if (page.name().equals(name)) {
                restoredPage = page;
            }
        }
    }

    /** Baut die Eintraege neu auf: geaenderte Auswahl, anderer Zaehler oder geaenderte Daten. */
    void rebuild() {
        boolean hasCounter = host.currentCounter() != null;
        // Ohne Zaehler gibt es nur "Verwalten"; sobald einer da ist, beginnt das Menue vorne.
        QuickMenuItem.Page shown = hadCounter == hasCounter
                ? pageAdapter.pageAt(menuPager.getCurrentItem())
                : restoredPage;
        hadCounter = hasCounter;
        restoredPage = null;
        pageAdapter.setPages(buildPages());
        empty.setVisibility(pageAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        menuPager.setVisibility(pageAdapter.getItemCount() == 0 ? View.INVISIBLE : View.VISIBLE);

        int position = Math.max(0, pageAdapter.indexOf(shown));
        menuPager.setCurrentItem(position, false);
        buildMenuDots();
        showPageInfo(menuPager.getCurrentItem());
        showCounterPosition();
        menuPager.post(this::updateDraggable);
        // Der neue Zaehler kann mehr oder weniger Zeilen haben als der vorige.
        coordinator.post(this::applyCurrentState);
    }

    private List<PageRows> buildPages() {
        Set<QuickMenuItem> selected = AppSettings.quickMenuItems(activity);
        boolean hasCounter = host.currentCounter() != null;
        Map<QuickMenuItem.Page, PageRows> byPage = new EnumMap<>(QuickMenuItem.Page.class);
        for (QuickMenuItem item : QuickMenuItem.values()) {
            if (selected.contains(item) && (hasCounter || !item.needsCounter)) {
                PageRows rows = byPage.get(item.page);
                if (rows == null) {
                    rows = new PageRows(item.page);
                    byPage.put(item.page, rows);
                }
                rows.items.add(item);
            }
        }
        return new ArrayList<>(byPage.values());
    }

    private void buildMenuDots() {
        int count = pageAdapter.getItemCount();
        int wanted = count > 1 ? count : 0;
        if (menuDots.getChildCount() == wanted) {
            return;
        }
        menuDots.removeAllViews();
        if (wanted == 0) {
            return;
        }
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int size = (int) (8 * metrics.density);
        int margin = (int) (4 * metrics.density);
        for (int i = 0; i < count; i++) {
            View dot = new View(activity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            menuDots.addView(dot);
        }
    }

    private void showPageInfo(int position) {
        QuickMenuItem.Page page = pageAdapter.pageAt(position);
        title.setText(page != null ? activity.getString(page.titleRes) : "");
        for (int i = 0; i < menuDots.getChildCount(); i++) {
            menuDots.getChildAt(i).setBackgroundResource(
                    i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void toggle() {
        if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            close();
        } else {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void close() {
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void applyCurrentState() {
        int state = behavior.getState();
        backCallback.setEnabled(state != BottomSheetBehavior.STATE_COLLAPSED);
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            applySlide(1f);
        } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            applySlide(0f);
        }
        updateDraggable();
    }

    /** Verkleinert den Zaehler passend zum Sheet, damit er oberhalb vollstaendig sichtbar bleibt. */
    private void applySlide(float offset) {
        float slide = Math.max(0f, Math.min(1f, offset));
        // Eingefahren ist vom Menue nichts zu sehen, nur die Positionsanzeige bleibt stehen.
        sheetBackground.setAlpha((int) (255 * slide));
        sheet.setElevation(sheetElevation * slide);
        grip.setAlpha(slide);
        counterDots.setAlpha(1f - slide);
        counterPosition.setAlpha(1f - slide);
        hint.setAlpha(1f - slide);
        title.setAlpha(slide);
        back.setVisibility(slide > 0f ? View.VISIBLE : View.INVISIBLE);
        back.setAlpha(slide);

        int pagerHeight = counterPager.getHeight();
        if (pagerHeight > 0) {
            int pagerTop = mainContent.getTop() + counterPager.getTop();
            int openSheetTop = coordinator.getHeight() - sheet.getLayoutParams().height;
            int available = Math.max(1, openSheetTop - pagerTop);
            float openScale = Math.max(MIN_COUNTER_SCALE, Math.min(1f, available / (float) counterContentHeight()));
            float scale = 1f - (1f - openScale) * slide;
            // Der Inhalt sitzt mittig im Pager: Dessen Mitte wandert in die Mitte des freien Bereichs.
            float shift = available / 2f - pagerHeight / 2f;
            counterPager.setPivotX(counterPager.getWidth() / 2f);
            counterPager.setPivotY(pagerHeight / 2f);
            counterPager.setScaleX(scale);
            counterPager.setScaleY(scale);
            counterPager.setTranslationY(shift * slide);
        }
    }

    /** Hoehe des sichtbaren Inhalts der aktuellen Zaehler-Seite, nicht der ganzen Seite. */
    private int counterContentHeight() {
        RecyclerView list = (RecyclerView) counterPager.getChildAt(0);
        RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(counterPager.getCurrentItem());
        if (!(holder != null && holder.itemView instanceof ViewGroup)) {
            return counterPager.getHeight();
        }
        ViewGroup page = (ViewGroup) holder.itemView;
        int top = Integer.MAX_VALUE;
        int bottom = 0;
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                top = Math.min(top, child.getTop());
                bottom = Math.max(bottom, child.getBottom());
            }
        }
        return bottom > top ? bottom - top : counterPager.getHeight();
    }

    private void showCounterPosition() {
        RecyclerView.Adapter<?> counters = counterPager.getAdapter();
        int count = counters != null ? counters.getItemCount() : 0;
        counterPosition.setText(count > 0
                ? activity.getString(R.string.quick_menu_counter_position, counterPager.getCurrentItem() + 1, count)
                : "");
    }

    /**
     * Scrollt eine offene Seite selbst, darf das Sheet die Geste nicht abfangen. Schliessen geht
     * dann ueber den Griff, den Zurueck-Button oder die Zurueck-Geste.
     */
    private void updateDraggable() {
        boolean pageScrolls = false;
        RecyclerView list = (RecyclerView) menuPager.getChildAt(0);
        RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(menuPager.getCurrentItem());
        if (holder != null && holder.itemView instanceof NestedScrollView) {
            NestedScrollView scroll = (NestedScrollView) holder.itemView;
            pageScrolls = scroll.canScrollVertically(1) || scroll.canScrollVertically(-1);
        }
        behavior.setDraggable(!(pageScrolls && behavior.getState() == BottomSheetBehavior.STATE_EXPANDED));
    }

    private void addRows(LinearLayout parent, QuickMenuItem item) {
        AnniversaryCounter counter = host.currentCounter();
        Runnable changed = this::afterChange;
        switch (item) {
            case SET_DATE:
                addButton(parent, item.labelRes, v -> CounterActions.editDate(activity, counter, changed));
                break;
            case RENAME:
                addButton(parent, item.labelRes, v -> CounterActions.editName(activity, counter, changed));
                break;
            case EDIT_MESSAGE:
                addButton(parent, item.labelRes, v -> CounterActions.editMessage(activity, counter, changed));
                break;
            case SHOW_NAME:
                addCheck(parent, item.labelRes, counter.showName, (b, on) -> {
                    counter.showName = on;
                    saveCounter(counter, false);
                });
                break;
            case SHOW_MESSAGE:
                addCheck(parent, item.labelRes, counter.showMessage, (b, on) -> {
                    counter.showMessage = on;
                    saveCounter(counter, false);
                });
                break;
            case COUNTDOWN:
                addCheck(parent, item.labelRes, counter.countdownToNext, (b, on) -> {
                    counter.countdownToNext = on;
                    saveCounter(counter, false);
                });
                break;
            case BIRTHDAY:
                addCheck(parent, item.labelRes, counter.isBirthday, (b, on) -> {
                    counter.isBirthday = on;
                    // Der Text der jaehrlichen Erinnerung steht schon im geplanten Alarm.
                    saveCounter(counter, true);
                });
                if (counter.isBirthday) {
                    addButton(parent, R.string.btn_birthday_settings, v ->
                            CounterActions.openForCounter(activity, BirthdaySettingsActivity.class, counter));
                }
                break;
            case CUSTOM_APPEARANCE:
                addCheck(parent, item.labelRes, counter.customAppearance, (b, on) -> {
                    counter.customAppearance = on;
                    saveCounter(counter, false);
                });
                break;
            case COUNTER_APPEARANCE:
                addButton(parent, item.labelRes, v ->
                        CounterActions.openForCounter(activity, AppearanceSettingsActivity.class, counter));
                break;
            case BACKGROUND:
                addButton(parent, item.labelRes, v ->
                        CounterActions.openForCounter(activity, BackgroundSettingsActivity.class, counter));
                break;
            case MILESTONES:
                addButton(parent, item.labelRes, v ->
                        CounterActions.openForCounter(activity, MilestoneSettingsActivity.class, counter));
                break;
            case CONTACT:
                addContactRows(parent, counter);
                break;
            case NEW_COUNTER:
                addButton(parent, item.labelRes, v -> CounterCreationFlow.start(activity, created -> {
                    AnniversaryCounterWidgetProvider.updateAllWidgets(activity);
                    host.onCounterCreated(created);
                }));
                break;
            case DELETE:
                addButton(parent, item.labelRes, v -> CounterActions.delete(activity, counter, changed));
                break;
            case LIGHT_THEME:
                addCheck(parent, item.labelRes, AppSettings.useLightTheme(activity),
                        (b, on) -> AppSettings.setUseLightTheme(activity, on));
                break;
            case GLOBAL_APPEARANCE:
                addPage(parent, item.labelRes, AppearanceSettingsActivity.class);
                break;
            case FORMAT:
                addPage(parent, item.labelRes, FormatSettingsActivity.class);
                break;
            case REORDER:
                addPage(parent, item.labelRes, CounterOrderActivity.class);
                break;
            case CALENDAR_IMPORT:
                addButton(parent, item.labelRes, v -> host.openCalendarImport());
                break;
            default:
                throw new IllegalArgumentException("Unknown item: " + item);
        }
    }

    private void addContactRows(LinearLayout parent, AnniversaryCounter counter) {
        if (!counter.hasContact()) {
            addButton(parent, R.string.btn_set_contact, v -> host.pickContact(counter));
            return;
        }
        Button change = addButton(parent, R.string.btn_set_contact, v -> host.pickContact(counter));
        change.setText(activity.getString(R.string.btn_change_contact, counter.contactLabel()));
        addButton(parent, R.string.btn_notify_contact, v -> CounterMessage.sendToContact(activity, counter));
        addButton(parent, R.string.btn_remove_contact, v ->
                CounterActions.saveContact(activity, counter, null, null, this::afterChange));
    }

    private void addPage(LinearLayout parent, int labelRes, Class<? extends AppCompatActivity> page) {
        addButton(parent, labelRes, v -> activity.startActivity(new Intent(activity, page)));
    }

    private Button addButton(LinearLayout parent, int labelRes, View.OnClickListener listener) {
        Button button = (Button) LayoutInflater.from(activity)
                .inflate(R.layout.item_quick_menu_button, parent, false);
        button.setText(labelRes);
        button.setOnClickListener(listener);
        parent.addView(button);
        return button;
    }

    private void addCheck(LinearLayout parent, int labelRes, boolean checked,
                          CompoundButton.OnCheckedChangeListener listener) {
        CheckBox check = (CheckBox) LayoutInflater.from(activity)
                .inflate(R.layout.item_quick_menu_check, parent, false);
        check.setText(labelRes);
        check.setChecked(checked);
        check.setOnCheckedChangeListener(listener);
        parent.addView(check);
    }

    private void saveCounter(AnniversaryCounter counter, boolean reschedule) {
        AnniversaryCounterStore.update(activity, counter);
        AnniversaryCounterWidgetProvider.updateAllWidgets(activity);
        if (reschedule) {
            MilestoneScheduler.scheduleForCounter(activity, counter);
        }
        afterChange();
    }

    private void afterChange() {
        host.refreshCounters();
    }

    private static final class PageRows {
        final QuickMenuItem.Page page;
        final List<QuickMenuItem> items = new ArrayList<>();

        PageRows(QuickMenuItem.Page page) {
            this.page = page;
        }
    }

    private final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        private final List<PageRows> pages = new ArrayList<>();

        void setPages(List<PageRows> newPages) {
            pages.clear();
            pages.addAll(newPages);
            notifyDataSetChanged();
        }

        QuickMenuItem.Page pageAt(int position) {
            return position >= 0 && position < pages.size() ? pages.get(position).page : null;
        }

        int indexOf(QuickMenuItem.Page page) {
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).page == page) {
                    return i;
                }
            }
            return -1;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quick_menu_page, parent, false);
            // Nur per Code moeglich: NestedScrollView stuerzt beim Setzen aus der XML-Datei ab.
            page.setNestedScrollingEnabled(false);
            return new Holder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.rows.removeAllViews();
            for (QuickMenuItem item : pages.get(position).items) {
                addRows(holder.rows, item);
            }
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final LinearLayout rows;

            Holder(View itemView) {
                super(itemView);
                rows = itemView.findViewById(R.id.quickMenuRows);
            }
        }
    }
}
