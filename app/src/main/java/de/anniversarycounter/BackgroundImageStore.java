package de.anniversarycounter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Speichert pro Zaehler optional ein eigenes Hintergrundbild im internen App-Speicher.
 *
 * Das Bild wird beim Auswaehlen einmalig auf {@link #MAX_DIMENSION} herunterskaliert und
 * als JPEG unter {@code filesDir/backgrounds/<counterId>.jpg} abgelegt - ob ein eigenes
 * Bild existiert, wird einfach ueber {@link File#exists()} geprueft, kein zusaetzliches
 * Feld im {@link AnniversaryCounter}-JSON noetig.
 *
 * Zusaetzlich wird unter {@code <counterId>_widget.jpg} eine kleinere Kopie fuer das
 * Homescreen-Widget gepflegt (siehe {@link #widgetBackgroundFile}): Das Widget bindet sie
 * per FileProvider-Uri ein statt als eingebettetes Bitmap, da eingebettete Bitmaps in
 * RemoteViews Android's Speicherlimit fuer Widgets sprengen koennen, was dann im Launcher
 * als "Problem beim Laden des Widgets" statt als fangbare Exception in unserem Prozess endet.
 */
public final class BackgroundImageStore {

    private static final int MAX_DIMENSION = 1080;
    private static final int WIDGET_MAX_DIMENSION = 400;
    private static final String DIR_NAME = "backgrounds";

    private BackgroundImageStore() {
    }

    public static File backgroundFile(Context context, String counterId) {
        return new File(new File(context.getFilesDir(), DIR_NAME), counterId + ".jpg");
    }

    public static File widgetBackgroundFile(Context context, String counterId) {
        return new File(new File(context.getFilesDir(), DIR_NAME), counterId + "_widget.jpg");
    }

    public static boolean hasCustomBackground(Context context, String counterId) {
        return backgroundFile(context, counterId).exists();
    }

    /** Liest das Bild von der gewaehlten Uri, skaliert es herunter und speichert es lokal. */
    public static void saveFromUri(Context context, String counterId, Uri sourceUri) throws IOException {
        Bitmap bitmap = decodeDownsampled(context, sourceUri, MAX_DIMENSION);
        if (bitmap == null) {
            throw new IOException("Bild konnte nicht gelesen werden");
        }
        try {
            writeJpeg(bitmap, backgroundFile(context, counterId));
        } finally {
            bitmap.recycle();
        }
        regenerateWidgetCopy(context, counterId);
    }

    /**
     * Erzeugt bzw. erneuert die kleine Widget-Kopie aus dem aktuellen Hintergrundbild.
     * Wird sowohl nach {@link #saveFromUri} als auch nach einem Backup-Restore aufgerufen,
     * da Backups nur das Hauptbild enthalten.
     */
    public static void regenerateWidgetCopy(Context context, String counterId) {
        File source = backgroundFile(context, counterId);
        if (!source.exists()) {
            return;
        }
        Bitmap bitmap = decodeDownsampledFile(source, WIDGET_MAX_DIMENSION);
        if (bitmap == null) {
            return;
        }
        try {
            writeJpeg(bitmap, widgetBackgroundFile(context, counterId));
        } catch (IOException ignored) {
            // Widget zeigt dann eben keinen Hintergrund - das Hauptbild bleibt davon unberuehrt
        } finally {
            bitmap.recycle();
        }
    }

    public static void clear(Context context, String counterId) {
        File file = backgroundFile(context, counterId);
        if (file.exists()) {
            file.delete();
        }
        File widgetFile = widgetBackgroundFile(context, counterId);
        if (widgetFile.exists()) {
            widgetFile.delete();
        }
    }

    private static void writeJpeg(Bitmap bitmap, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
    }

    private static Bitmap decodeDownsampled(Context context, Uri uri, int maxDimension) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            return BitmapFactory.decodeStream(in, null, options);
        }
    }

    private static Bitmap decodeDownsampledFile(File file, int maxDimension) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);

        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
}
