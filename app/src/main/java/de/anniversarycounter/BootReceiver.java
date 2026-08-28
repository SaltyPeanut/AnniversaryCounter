package de.anniversarycounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** AlarmManager-Alarme ueberleben einen Geraete-Neustart nicht - hier werden sie neu geplant. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MilestoneScheduler.scheduleAll(context);
        }
    }
}
