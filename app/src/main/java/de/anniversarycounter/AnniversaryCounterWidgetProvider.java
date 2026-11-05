package de.anniversarycounter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;

import java.io.File;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Homescreen-Widget, das denselben Startzeitpunkt wie {@link MainActivity} anzeigt.
 *
 * Jede Widget-Instanz hat ihre eigene Konfiguration (welche Einheiten, Ueberschrift
 * ja/nein), gesetzt ueber {@link WidgetConfigActivity} und abgelegt in
 * {@link #WIDGET_PREFS}. Aktualisiert wird stuendlich (siehe
 * res/xml/anniversary_counter_widget_info.xml) sowie sofort nach Aenderungen in der App.
 */
public class AnniversaryCounterWidgetProvider extends AppWidgetProvider {

    private static final String WIDGET_PREFS = "anniversary_counter_widget_prefs";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit();
        for (int appWidgetId : appWidgetIds) {
            editor.remove(unitsKey(appWidgetId));
            editor.remove(subtitleKey(appWidgetId));
            editor.remove(counterIdKey(appWidgetId));
            editor.remove(backgroundKey(appWidgetId));
        }
        editor.apply();
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        AnniversaryCounter counter = resolveCounter(context, appWidgetId);
        try {
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, counter, true));
        } catch (RuntimeException e) {
            // Irgendwas rund um das Hintergrundbild ist schiefgelaufen (z. B. beim Freigeben der
            // Uri an den Launcher-Prozess) - lieber ein Widget ohne Hintergrund als eines, das der
            // Launcher gar nicht laden kann.
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, counter, false));
        }
    }

    private static RemoteViews buildViews(
            Context context, int appWidgetId, AnniversaryCounter counter, boolean allowBackground) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_anniversary_counter);

        Intent openApp = new Intent(context, MainActivity.class);
        if (counter != null) {
            openApp.putExtra(MainActivity.EXTRA_COUNTER_ID, counter.id);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

        if (counter == null) {
            views.setViewVisibility(R.id.widgetSubtitle, View.GONE);
            views.setTextViewText(R.id.widgetCounter, context.getString(
                    loadCounterId(context, appWidgetId) == null
                            ? R.string.widget_no_date
                            : R.string.widget_counter_deleted));
            return views;
        }

        LocalDateTime now = LocalDateTime.now();
        DateDiff diff = DateDiff.between(counter.displayTarget(now), now);

        String age = counter.ageText(context, now);
        if (age != null) {
            views.setTextViewText(R.id.widgetAge, age);
            views.setViewVisibility(R.id.widgetAge, View.VISIBLE);
        }

        if (loadShowSubtitle(context, appWidgetId)) {
            views.setViewVisibility(R.id.widgetSubtitle, View.VISIBLE);
            views.setTextViewText(R.id.widgetSubtitle, AnniversaryCounterStore.subtitleText(context, counter, diff.future));
        } else {
            views.setViewVisibility(R.id.widgetSubtitle, View.GONE);
        }

        views.setTextViewText(R.id.widgetCounter, diff.formatUnits(context, loadUnits(context, appWidgetId)));

        boolean withBackground = false;
        if (allowBackground && loadShowBackground(context, appWidgetId)) {
            File file = BackgroundImageStore.widgetBackgroundFile(context, counter.id);
            if (!file.exists()) {
                // Zaehler, deren Hintergrundbild schon vor Einfuehrung der Widget-Kopie gesetzt
                // wurde, haben diese Datei noch nicht - hier einmalig nachholen.
                BackgroundImageStore.regenerateWidgetCopy(context, counter.id);
            }
            if (file.exists()) {
                // Als Bitmap statt als FileProvider-Uri einbetten: der Launcher-Prozess darf
                // unseren nicht-exportierten Provider nicht oeffnen (SecurityException trotz
                // grantUriPermission). Die Widget-Kopie ist bereits auf WIDGET_MAX_DIMENSION
                // herunterskaliert, bleibt also klar unter dem RemoteViews-Transaktionslimit.
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widgetBackground, bitmap);
                    views.setViewVisibility(R.id.widgetBackground, View.VISIBLE);
                    views.setViewVisibility(R.id.widgetScrim, View.VISIBLE);
                    withBackground = true;
                }
            }
        }
        if (!withBackground) {
            views.setViewVisibility(R.id.widgetBackground, View.GONE);
            views.setViewVisibility(R.id.widgetScrim, View.GONE);
        }

        return views;
    }

    /**
     * Loest den fuer dieses Widget konfigurierten Zaehler auf. Aeltere, vor der
     * Mehrzaehler-Unterstuetzung konfigurierte Widgets (kein counter_id gespeichert)
     * fallen auf den ersten vorhandenen Zaehler zurueck.
     */
    private static AnniversaryCounter resolveCounter(Context context, int appWidgetId) {
        String counterId = loadCounterId(context, appWidgetId);
        if (counterId == null) {
            List<AnniversaryCounter> all = AnniversaryCounterStore.loadAll(context);
            return all.isEmpty() ? null : all.get(0);
        }
        return AnniversaryCounterStore.find(context, counterId);
    }

    static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, AnniversaryCounterWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static Set<DateDiff.Unit> loadUnits(Context context, int appWidgetId) {
        String stored = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .getString(unitsKey(appWidgetId), null);
        if (stored == null || stored.isEmpty()) {
            return EnumSet.allOf(DateDiff.Unit.class);
        }
        Set<DateDiff.Unit> units = EnumSet.noneOf(DateDiff.Unit.class);
        for (String name : stored.split(",")) {
            units.add(DateDiff.Unit.valueOf(name));
        }
        return units;
    }

    static void saveUnits(Context context, int appWidgetId, Set<DateDiff.Unit> units) {
        StringBuilder joined = new StringBuilder();
        for (DateDiff.Unit unit : units) {
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(unit.name());
        }
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(unitsKey(appWidgetId), joined.toString())
                .apply();
    }

    static String loadCounterId(Context context, int appWidgetId) {
        return context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .getString(counterIdKey(appWidgetId), null);
    }

    static void saveCounterId(Context context, int appWidgetId, String counterId) {
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(counterIdKey(appWidgetId), counterId)
                .apply();
    }

    static boolean loadShowSubtitle(Context context, int appWidgetId) {
        return context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .getBoolean(subtitleKey(appWidgetId), true);
    }

    static void saveShowSubtitle(Context context, int appWidgetId, boolean show) {
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(subtitleKey(appWidgetId), show)
                .apply();
    }

    static boolean loadShowBackground(Context context, int appWidgetId) {
        return context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .getBoolean(backgroundKey(appWidgetId), false);
    }

    static void saveShowBackground(Context context, int appWidgetId, boolean show) {
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(backgroundKey(appWidgetId), show)
                .apply();
    }

    private static String backgroundKey(int appWidgetId) {
        return "widget_" + appWidgetId + "_show_background";
    }

    private static String unitsKey(int appWidgetId) {
        return "widget_" + appWidgetId + "_units";
    }

    private static String subtitleKey(int appWidgetId) {
        return "widget_" + appWidgetId + "_show_subtitle";
    }

    private static String counterIdKey(int appWidgetId) {
        return "widget_" + appWidgetId + "_counter_id";
    }
}
