package de.anniversarycounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Speichert die Liste aller Zaehler als JSON in den SharedPreferences.
 *
 * Wird sowohl von {@link MainActivity} als auch vom {@link AnniversaryCounterWidgetProvider}
 * genutzt, damit beide exakt denselben Stand sehen.
 */
public final class AnniversaryCounterStore {

    private static final String PREFS = "anniversary_counter_prefs";
    private static final String KEY_COUNTERS = "counters_json";

    // Alte Einzel-Zaehler-Keys, nur fuer die einmalige Migration bestehender Nutzer.
    private static final String LEGACY_KEY_START = "start_date_time";
    private static final String LEGACY_KEY_MESSAGE = "custom_message";

    private AnniversaryCounterStore() {
    }

    public static List<AnniversaryCounter> loadAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_COUNTERS, null);
        if (json == null) {
            return migrateLegacy(context, prefs);
        }
        return decode(json);
    }

    public static void saveAll(Context context, List<AnniversaryCounter> counters) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_COUNTERS, encode(counters).toString())
                .apply();
    }

    public static void add(Context context, AnniversaryCounter counter) {
        List<AnniversaryCounter> all = loadAll(context);
        all.add(counter);
        saveAll(context, all);
    }

    public static void update(Context context, AnniversaryCounter counter) {
        List<AnniversaryCounter> all = loadAll(context);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(counter.id)) {
                all.set(i, counter);
                break;
            }
        }
        saveAll(context, all);
    }

    public static void delete(Context context, String id) {
        List<AnniversaryCounter> all = loadAll(context);
        for (Iterator<AnniversaryCounter> it = all.iterator(); it.hasNext(); ) {
            if (it.next().id.equals(id)) {
                it.remove();
                break;
            }
        }
        saveAll(context, all);
    }

    public static AnniversaryCounter find(Context context, String id) {
        if (id == null) {
            return null;
        }
        for (AnniversaryCounter counter : loadAll(context)) {
            if (counter.id.equals(id)) {
                return counter;
            }
        }
        return null;
    }

    /** Ueberschrift-Text: eigene Nachricht falls gesetzt, sonst Standardtext je nach future-Status. */
    public static String subtitleText(Context context, AnniversaryCounter counter, boolean future) {
        if (future) {
            return context.getString(R.string.subtitle_future);
        }
        if (counter.message != null && !counter.message.isEmpty()) {
            return counter.message;
        }
        return context.getString(R.string.subtitle_default);
    }

    private static List<AnniversaryCounter> migrateLegacy(Context context, SharedPreferences prefs) {
        List<AnniversaryCounter> counters = new ArrayList<>();
        String legacyStart = prefs.getString(LEGACY_KEY_START, null);
        if (legacyStart != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(legacyStart);
                String message = prefs.getString(LEGACY_KEY_MESSAGE, null);
                counters.add(new AnniversaryCounter(UUID.randomUUID().toString(), null, start, message));
            } catch (DateTimeParseException e) {
                Toast.makeText(context, R.string.date_read_failed, Toast.LENGTH_LONG).show();
            }
        }
        saveAll(context, counters);
        prefs.edit().remove(LEGACY_KEY_START).remove(LEGACY_KEY_MESSAGE).apply();
        return counters;
    }

    /** Package-sichtbar, damit {@link BackupManager} dieselbe Serialisierung fuer Backups nutzen kann. */
    static JSONArray encode(List<AnniversaryCounter> counters) {
        JSONArray array = new JSONArray();
        try {
            for (AnniversaryCounter counter : counters) {
                JSONObject obj = new JSONObject();
                obj.put("id", counter.id);
                obj.put("name", counter.name == null ? JSONObject.NULL : counter.name);
                obj.put("start", counter.start.toString());
                obj.put("message", counter.message == null ? JSONObject.NULL : counter.message);
                obj.put("showMessage", counter.showMessage);
                obj.put("showName", counter.showName);
                obj.put("customAppearance", counter.customAppearance);
                obj.put("fontFamilyText", counter.fontFamilyText);
                obj.put("colorValueText", counter.colorValueText);
                obj.put("fontFamilyNumbers", counter.fontFamilyNumbers);
                obj.put("colorValueNumbers", counter.colorValueNumbers);
                obj.put("standardMilestones", new JSONArray(counter.enabledStandardMilestones));

                JSONArray customArray = new JSONArray();
                for (CustomMilestone custom : counter.customMilestones) {
                    JSONObject customObj = new JSONObject();
                    customObj.put("id", custom.id);
                    customObj.put("amount", custom.amount);
                    customObj.put("unit", custom.unit.name());
                    customArray.put(customObj);
                }
                obj.put("customMilestones", customArray);
                obj.put("countdownToNext", counter.countdownToNext);
                obj.put("isBirthday", counter.isBirthday);
                obj.put("showAge", counter.showAge);
                obj.put("contactName", counter.contactName == null ? JSONObject.NULL : counter.contactName);
                obj.put("contactPhone", counter.contactPhone == null ? JSONObject.NULL : counter.contactPhone);

                array.put(obj);
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return array;
    }

    private static List<AnniversaryCounter> decode(String json) {
        try {
            return decode(new JSONArray(json));
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    /** Package-sichtbar, damit {@link BackupManager} dieselbe Deserialisierung fuer Backups nutzen kann. */
    static List<AnniversaryCounter> decode(JSONArray array) {
        List<AnniversaryCounter> result = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");
                String name = obj.isNull("name") ? null : obj.getString("name");
                LocalDateTime start = LocalDateTime.parse(obj.getString("start"));
                String message = obj.isNull("message") ? null : obj.getString("message");
                AnniversaryCounter counter = new AnniversaryCounter(id, name, start, message);
                counter.showMessage = obj.optBoolean("showMessage", true);
                counter.showName = obj.optBoolean("showName", true);
                counter.customAppearance = obj.optBoolean("customAppearance", false);
                counter.fontFamilyText = obj.optString("fontFamilyText", AppSettings.FONT_DEFAULT);
                counter.colorValueText = obj.optInt("colorValueText", AppSettings.COLOR_DEFAULT);
                counter.fontFamilyNumbers = obj.optString("fontFamilyNumbers", AppSettings.FONT_DEFAULT);
                counter.colorValueNumbers = obj.optInt("colorValueNumbers", AppSettings.COLOR_DEFAULT);
                counter.countdownToNext = obj.optBoolean("countdownToNext", false);
                counter.showAge = obj.optBoolean("showAge", false);
                // Aeltere Staende kennen isBirthday nicht: eingeschaltetes Alter bleibt so sichtbar.
                counter.isBirthday = obj.optBoolean("isBirthday", counter.showAge);
                counter.contactName = obj.isNull("contactName") ? null : obj.getString("contactName");
                counter.contactPhone = obj.isNull("contactPhone") ? null : obj.getString("contactPhone");

                if (obj.has("standardMilestones")) {
                    JSONArray standardArray = obj.getJSONArray("standardMilestones");
                    for (int s = 0; s < standardArray.length(); s++) {
                        counter.enabledStandardMilestones.add(standardArray.getString(s));
                    }
                }

                if (obj.has("customMilestones")) {
                    JSONArray customArray = obj.getJSONArray("customMilestones");
                    for (int c = 0; c < customArray.length(); c++) {
                        JSONObject customObj = customArray.getJSONObject(c);
                        counter.customMilestones.add(new CustomMilestone(
                                customObj.getString("id"),
                                customObj.getInt("amount"),
                                Milestone.Unit.valueOf(customObj.getString("unit"))));
                    }
                }

                result.add(counter);
            }
        } catch (JSONException | DateTimeParseException e) {
            return new ArrayList<>();
        }
        return result;
    }
}
