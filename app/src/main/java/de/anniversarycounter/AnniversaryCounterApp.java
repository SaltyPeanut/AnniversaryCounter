package de.anniversarycounter;

import android.app.Application;

/**
 * Wendet den gespeicherten Hell/Dunkel-Modus an, bevor die erste Activity aufgebaut wird -
 * geschieht das erst spaeter (z. B. nur beim Toggle in den Einstellungen), wuerde die zuerst
 * gezeigte Activity kurz im falschen Design aufblitzen.
 */
public class AnniversaryCounterApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppSettings.applyNightMode(this);
        AppSettings.applyLanguage(this);
    }
}
