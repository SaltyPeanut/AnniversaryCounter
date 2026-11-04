package de.anniversarycounter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/** Empfaengt einen faelligen Meilenstein-Alarm und stellt die Benachrichtigung zu. */
public class MilestoneAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "milestones";

    @Override
    public void onReceive(Context context, Intent intent) {
        String counterId = intent.getStringExtra(MilestoneScheduler.EXTRA_COUNTER_ID);
        String milestoneKey = intent.getStringExtra(MilestoneScheduler.EXTRA_MILESTONE_KEY);
        String milestoneLabel = intent.getStringExtra(MilestoneScheduler.EXTRA_MILESTONE_LABEL);
        if (counterId == null || milestoneKey == null) {
            return;
        }

        AnniversaryCounter counter = AnniversaryCounterStore.find(context, counterId);
        if (counter == null) {
            // Zaehler wurde inzwischen geloescht - der Alarm haette storniert sein sollen,
            // im Zweifel aber lieber keine Benachrichtigung ohne Ziel anzeigen.
            return;
        }

        if (!MilestoneScheduler.isActive(counter, milestoneKey)) {
            return;
        }

        if (Milestone.Standard.YEARLY.name().equals(milestoneKey)) {
            MilestoneScheduler.scheduleForCounter(context, counter);
        }

        ensureChannel(context);

        String title = counter.name != null && !counter.name.isEmpty()
                ? counter.name
                : context.getString(R.string.app_name);
        String text = context.getString(R.string.milestone_notification_text,
                milestoneLabel != null ? milestoneLabel : "");

        int requestCode = (counterId + "|" + milestoneKey).hashCode();

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.putExtra(MainActivity.EXTRA_COUNTER_ID, counterId);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, requestCode, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (counter.hasContact()) {
            Intent sms = CounterMessage.smsIntent(context, counter);
            sms.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent smsIntent = PendingIntent.getActivity(
                    context, requestCode + 1, sms,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_dialog_email,
                    context.getString(R.string.btn_notify_contact), smsIntent);
        }

        NotificationManagerCompat.from(context).notify(requestCode, builder.build());
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.milestone_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.milestone_channel_description));
            manager.createNotificationChannel(channel);
        }
    }
}
