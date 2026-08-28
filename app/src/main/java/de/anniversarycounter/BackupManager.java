package de.anniversarycounter;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Exportiert/importiert alle Zaehler samt eigener Hintergrundbilder als ein einzelnes
 * JSON-Dokument, damit bei Geraetewechsel oder Deinstallation nichts verloren geht.
 * Die Hintergrundbilder (bereits von {@link BackgroundImageStore} auf max. 1080px
 * herunterskalierte JPEGs) werden Base64-kodiert eingebettet.
 */
public final class BackupManager {

    private static final int VERSION = 1;
    private static final String KEY_VERSION = "version";
    private static final String KEY_COUNTERS = "counters";
    private static final String KEY_BACKGROUNDS = "backgrounds";

    private BackupManager() {
    }

    public static String export(Context context) {
        List<AnniversaryCounter> counters = AnniversaryCounterStore.loadAll(context);
        JSONObject root = new JSONObject();
        try {
            root.put(KEY_VERSION, VERSION);
            root.put(KEY_COUNTERS, AnniversaryCounterStore.encode(counters));

            JSONObject backgrounds = new JSONObject();
            for (AnniversaryCounter counter : counters) {
                File file = BackgroundImageStore.backgroundFile(context, counter.id);
                if (file.exists()) {
                    backgrounds.put(counter.id, Base64.encodeToString(readFile(file), Base64.NO_WRAP));
                }
            }
            root.put(KEY_BACKGROUNDS, backgrounds);
        } catch (JSONException | IOException e) {
            throw new IllegalStateException(e);
        }
        return root.toString();
    }

    /** Ersetzt alle vorhandenen Zaehler durch den Inhalt des Backups. */
    public static Result restore(Context context, String json) throws IOException {
        JSONObject root;
        JSONArray countersArray;
        try {
            root = new JSONObject(json);
            countersArray = root.getJSONArray(KEY_COUNTERS);
        } catch (JSONException e) {
            throw new IOException("Ungueltiges Backup-Format", e);
        }

        List<AnniversaryCounter> counters = AnniversaryCounterStore.decode(countersArray);
        if (counters.isEmpty()) {
            throw new IOException("Backup enthaelt keine gueltigen Zaehler");
        }

        JSONObject backgrounds = root.optJSONObject(KEY_BACKGROUNDS);
        if (backgrounds != null) {
            Iterator<String> ids = backgrounds.keys();
            while (ids.hasNext()) {
                String counterId = ids.next();
                try {
                    byte[] bytes = Base64.decode(backgrounds.getString(counterId), Base64.NO_WRAP);
                    File file = BackgroundImageStore.backgroundFile(context, counterId);
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (OutputStream out = new FileOutputStream(file)) {
                        out.write(bytes);
                    }
                    // Backups enthalten nur das Hauptbild - die kleinere Widget-Kopie muss
                    // aus diesem wiederhergestellten Bild neu erzeugt werden.
                    BackgroundImageStore.regenerateWidgetCopy(context, counterId);
                } catch (JSONException | IOException ignored) {
                    // einzelnes defektes Bild soll den Rest des Restores nicht abbrechen
                }
            }
        }

        AnniversaryCounterStore.saveAll(context, counters);

        boolean anyMilestones = false;
        for (AnniversaryCounter counter : counters) {
            if (!counter.enabledStandardMilestones.isEmpty() || !counter.customMilestones.isEmpty()) {
                anyMilestones = true;
                MilestoneScheduler.scheduleForCounter(context, counter);
            }
        }

        return new Result(counters.size(), anyMilestones);
    }

    private static byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
        }
        return buffer.toByteArray();
    }

    public static final class Result {
        public final int counterCount;
        public final boolean anyMilestonesEnabled;

        Result(int counterCount, boolean anyMilestonesEnabled) {
            this.counterCount = counterCount;
            this.anyMilestonesEnabled = anyMilestonesEnabled;
        }
    }
}
