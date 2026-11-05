# Anniversary Counter

Android-App in Java: Du gibst ein Datum mit Uhrzeit ein, die App zählt ab
diesem Zeitpunkt sekundengenau weiter – Jahre, Monate, Tage, Stunden,
Minuten, Sekunden.

## Funktionen

### Ereignisse

- Datum + Uhrzeit über Dialoge auswählen, jederzeit änderbar; das Datum wird
  über Räder (Tag/Monat/Jahr) gewählt, auch weit zurückliegende Jahre sind so
  direkt einstellbar
- Läuft live weiter (Aktualisierung jede Sekunde, ohne Drift)
- Speichert alle Ereignisse dauerhaft (SharedPreferences), übersteht Neustarts
- Einheiten mit Wert 0 werden übersprungen
- Ereignis antippen → Gesamtzeit (z. B. „112 Tage gesamt")
- Ereignis gedrückt halten → Stand teilen
- Zukünftige Daten funktionieren auch: dann läuft ein Countdown
- Mehrere Ereignisse gleichzeitig: durch Wischen (ViewPager) zwischen ihnen wechseln,
  Punkte und „Ereignis 2 von 3“ zeigen die aktuelle Position
- Jedes Ereignis frei benennbar, mit eigener Überschrift
  (ein-/ausblendbar) und optional eigenem Hintergrundbild
- Reihenfolge per Drag & Drop änderbar
- Pro Ereignis wählbar: dauerhaft hochzählen oder Countdown zum nächsten Jahrestag
  (springt danach automatisch aufs Folgejahr)
- Pro Ereignis als Geburtstag markierbar; in den Geburtstagsoptionen lässt sich dann die
  Altersanzeige einschalten
- App-Titel wird immer auf der Startseite angezeigt

### Schnellmenü

- Ausfahrbares Menü auf der Hauptseite: eingefahren unsichtbar, nur die Position des Ereignisses bleibt
  stehen. Hochwischen vom unteren Rand oder Antippen dieses Bereichs öffnet es bis zur Bildschirmmitte,
  das Ereignis bleibt darüber sichtbar
- Mehrere Seiten, durch Wischen nach links und rechts erreichbar; die Punkte zeigen dann die Menü-Seite
- Schließen durch Herunterwischen, den Griff, den Zurück-Button oder die Zurück-Geste
- Im Zahnrad-Menü unter „Schnellmenü anpassen“ per Opt-in/Opt-out wählbar, was erscheint; alle
  Einstellungen bleiben im Zahnrad-Menü erreichbar

### Aus Kalender hinzufügen

- Jährliche Termine (z. B. Geburtstage) aus den auf dem Gerät synchronisierten
  Kalendern lesen und auswählen, welche als Ereignisse übernommen werden
- Bereits vorhandene Einträge sind markiert und nicht erneut wählbar
- Jeder Eintrag wird ein eigenes Ereignis; der Kalender wird danach nicht mehr gelesen
- Kein Google-Login: nutzt nur den Android-Kalender (`READ_CALENDAR`)

### Kontakt pro Ereignis

- Optional bei jedem Ereignis einen Kontakt aus dem Adressbuch hinzufügen
- „SMS an Kontakt schreiben“ öffnet die SMS-App mit Nummer und Text, gesendet wird
  von Hand; die Aktion steht auch in den Erinnerungen bereit

### Erinnerungen

- Fest vorgegebene Erinnerungen (100 Tage, 6 Monate, 1/2/5/10 Jahre, 1000 Tage)
  pro Ereignis einzeln an-/abwählbar
- Zusätzlich beliebig viele eigene Erinnerungen (Anzahl + Einheit) definierbar
- Optional jährliche Erinnerung am Jahrestag, plant sich selbst immer neu; bei
  aus dem Kalender hinzugefügten Ereignissen automatisch aktiv
- Zustellung über `AlarmManager`, übersteht Geräte-Neustarts

### Homescreen-Widget

- Greift auf denselben Zeitpunkt wie die App zu, pro Widget frei wählbar,
  welches Ereignis angezeigt wird
- Einzeln konfigurierbar: welche Einheiten (Jahre/Monate/Tage/Stunden),
  eigene Überschrift, eigenes Hintergrundbild
- Antippen öffnet die App direkt beim dort konfigurierten Ereignis
- Aktualisiert sich stündlich sowie sofort nach Änderungen in der App

### Aussehen

- Schriftart und Textfarbe getrennt einstellbar für Text (Name/Überschrift/Titel)
  und Zahlen (die große Zeitanzeige), gilt app-weit für alle Ereignisse
- Helles Design als Alternative zum dunklen Standard-Design
- Datum & Zahlen: standardmäßig nach App-Sprache (Datum, Tausender-
  trennzeichen) bzw. Systemeinstellung (12h/24h), in den Einstellungen
  einzeln änderbar

### Daten sichern

- Alle Ereignisse samt eigener Hintergrundbilder als eine JSON-Datei über den
  Systemfreigabe-Dialog exportieren
- An anderer Stelle (oder nach Neuinstallation) wieder einlesen, ersetzt dabei
  alle vorhandenen Ereignisse

## Dateien

```
app/src/main/java/de/anniversarycounter/MainActivity.java               Zähler-Seiten (ViewPager), Sekundentakt
app/src/main/java/de/anniversarycounter/DateDiff.java                    Rechenlogik + Formatierung
app/src/main/java/de/anniversarycounter/DisplayFormats.java              Datums-/Uhrzeit-/Zahlenformatierung
app/src/main/java/de/anniversarycounter/FormatSettingsActivity.java      Einstellung Datum & Zahlenformat
app/src/main/java/de/anniversarycounter/DateTimePickerFlow.java          Dialogkette Datum (Räder) → Uhrzeit
app/src/main/java/de/anniversarycounter/AnniversaryCounter.java                 Datenmodell eines Zählers
app/src/main/java/de/anniversarycounter/AnniversaryCounterStore.java             Persistierung aller Zähler als JSON
app/src/main/java/de/anniversarycounter/CounterPagerAdapter.java          RecyclerView-Adapter für den ViewPager
app/src/main/java/de/anniversarycounter/CounterCreationFlow.java          Dialogkette zum Anlegen eines Zählers
app/src/main/java/de/anniversarycounter/CounterSettingsActivity.java      Einstellungen pro Zähler + globale Aktionen
app/src/main/java/de/anniversarycounter/BirthdaySettingsActivity.java     Geburtstags-Untermenü (Altersanzeige)
app/src/main/java/de/anniversarycounter/QuickMenuController.java         Ausfahrbares Schnellmenü der Hauptseite
app/src/main/java/de/anniversarycounter/QuickMenuItem.java               Wählbare Schnellmenü-Einträge
app/src/main/java/de/anniversarycounter/QuickMenuSettingsActivity.java   Opt-in/Opt-out der Schnellmenü-Einträge
app/src/main/java/de/anniversarycounter/CounterActions.java              Gemeinsame Aktionen für einen Zähler
app/src/main/java/de/anniversarycounter/CounterOrderActivity.java         Zähler-Reihenfolge per Drag & Drop
app/src/main/java/de/anniversarycounter/CounterOrderAdapter.java          RecyclerView-Adapter für die Sortierliste
app/src/main/java/de/anniversarycounter/AppSettings.java                  Globale Einstellungen (Schrift, Farbe, Theme)
app/src/main/java/de/anniversarycounter/AppearanceSettingsActivity.java   Schriftart/Farbe für Text und Zahlen
app/src/main/java/de/anniversarycounter/AnniversaryCounterApp.java               Wendet den Hell/Dunkel-Modus beim App-Start an
app/src/main/java/de/anniversarycounter/BackgroundImageStore.java         Speicherung eigener Hintergrundbilder
app/src/main/java/de/anniversarycounter/BackgroundSettingsActivity.java   Hintergrundbild pro Zähler setzen/entfernen
app/src/main/java/de/anniversarycounter/Milestone.java                    Standard-Meilenstein-Definitionen
app/src/main/java/de/anniversarycounter/CustomMilestone.java              Eigener Meilenstein (Anzahl + Einheit)
app/src/main/java/de/anniversarycounter/MilestoneSettingsActivity.java    Meilenstein-Einstellungen pro Zähler
app/src/main/java/de/anniversarycounter/MilestoneScheduler.java           Plant/storniert AlarmManager-Alarme
app/src/main/java/de/anniversarycounter/MilestoneAlarmReceiver.java       Stellt fällige Meilenstein-Benachrichtigungen zu
app/src/main/java/de/anniversarycounter/BootReceiver.java                 Plant Alarme nach Geräte-Neustart neu
app/src/main/java/de/anniversarycounter/BackupManager.java                Export/Import aller Zähler als JSON
app/src/main/java/de/anniversarycounter/CalendarBirthdayReader.java       Liest jährliche Termine aus dem Android-Kalender
app/src/main/java/de/anniversarycounter/CalendarImportActivity.java       Auswahlmaske für den Kalender-Import
app/src/main/java/de/anniversarycounter/CalendarImportAdapter.java        RecyclerView-Adapter für die Auswahlmaske
app/src/main/java/de/anniversarycounter/CounterMessage.java               Text zum Teilen + SMS an den hinterlegten Kontakt
app/src/main/java/de/anniversarycounter/AnniversaryCounterWidgetProvider.java    Homescreen-Widget
app/src/main/java/de/anniversarycounter/WidgetConfigActivity.java         Widget-Konfiguration pro Instanz
app/src/main/res/layout/activity_main.xml                          Layout der App
app/src/main/res/layout/widget_anniversary_counter.xml                    Layout des Widgets
app/src/main/res/layout/activity_widget_config.xml                 Layout der Widget-Konfiguration
app/src/main/res/xml/anniversary_counter_widget_info.xml                  Widget-Metadaten
app/src/main/res/values/strings.xml                                 Texte inkl. Plural-Formen
app/src/main/res/values/colors.xml, values-night/colors.xml         Farben für helles/dunkles Design
app/src/main/res/drawable/bg_hearts.xml                             Herzmuster im Hintergrund
```
