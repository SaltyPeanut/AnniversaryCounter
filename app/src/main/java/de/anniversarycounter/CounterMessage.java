package de.anniversarycounter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.time.LocalDateTime;

/** Text zum aktuellen Stand eines Zaehlers, fuers Teilen und fuer Nachrichten an den Kontakt. */
final class CounterMessage {

    private CounterMessage() {
    }

    /** Eine ausgeblendete Nachricht bleibt auch hier weg, damit der Text der Anzeige entspricht. */
    static String text(Context context, AnniversaryCounter counter) {
        LocalDateTime now = LocalDateTime.now();
        DateDiff diff = DateDiff.between(counter.displayTarget(now), now);
        String text = diff.formatBreakdown(context).replace("\n", " ");
        if (counter.showMessage) {
            text = AnniversaryCounterStore.subtitleText(context, counter, diff.future) + "\n" + text;
        }
        String age = counter.ageText(context, now);
        return age != null ? text + "\n" + age : text;
    }

    /** Oeffnet die SMS-App mit Nummer und Text; abgeschickt wird dort vom Nutzer selbst. */
    static Intent smsIntent(Context context, AnniversaryCounter counter) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", counter.contactPhone, null));
        intent.putExtra("sms_body", text(context, counter));
        return intent;
    }

    static void sendToContact(Context context, AnniversaryCounter counter) {
        try {
            context.startActivity(smsIntent(context, counter));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.contact_no_sms_app, Toast.LENGTH_LONG).show();
        }
    }
}
