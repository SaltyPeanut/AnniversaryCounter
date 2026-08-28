# Anniversary Counter

Android-App in Java: Du gibst ein Datum mit Uhrzeit ein, die App zählt ab
diesem Zeitpunkt sekundengenau weiter – Jahre, Monate, Tage, Stunden,
Minuten, Sekunden.

## Funktionen

### Zähler

- Datum + Uhrzeit über Dialoge auswählen, jederzeit änderbar
- Läuft live weiter (Aktualisierung jede Sekunde, ohne Drift)
- Speichert alle Zähler dauerhaft (SharedPreferences), übersteht Neustarts
- Einheiten mit Wert 0 werden übersprungen
- Zähler antippen → Gesamtwerte (z. B. „112 Tage gesamt")
- Zähler lange drücken → Stand teilen
- Zukünftige Daten funktionieren auch: dann läuft ein Countdown
- Mehrere Zähler gleichzeitig: durch Wischen (ViewPager) zwischen ihnen wechseln,
  Seiten-Punkte zeigen die aktuelle Position
- Jeder Zähler frei benennbar, mit eigener Nachricht als Überschrift
  (ein-/ausblendbar) und optional eigenem Hintergrundbild
- Zähler-Reihenfolge per Drag & Drop änderbar
- App-Titel wird immer auf der Startseite angezeigt

### Meilenstein-Benachrichtigungen

- Fest vorgegebene Meilensteine (100 Tage, 6 Monate, 1/2/5/10 Jahre, 1000 Tage)
  pro Zähler einzeln an-/abwählbar
- Zusätzlich beliebig viele eigene Meilensteine (Anzahl + Einheit) definierbar
- Zustellung über `AlarmManager`, übersteht Geräte-Neustarts

### Homescreen-Widget

- Greift auf denselben Zeitpunkt wie die App zu, pro Widget frei wählbar,
  welcher Zähler angezeigt wird
- Einzeln konfigurierbar: welche Einheiten (Jahre/Monate/Tage/Stunden),
  eigene Nachricht als Überschrift, eigenes Hintergrundbild
- Antippen öffnet die App direkt beim dort konfigurierten Zähler
- Aktualisiert sich stündlich sowie sofort nach Änderungen in der App

### Aussehen

- Schriftart und Textfarbe getrennt einstellbar für Text (Name/Nachricht/Titel)
  und Zahlen (die große Zähler-Anzeige), gilt app-weit für alle Zähler
- Helles Design als Alternative zum dunklen Standard-Design

### Backup

- Alle Zähler samt eigener Hintergrundbilder als eine JSON-Datei über den
  Systemfreigabe-Dialog exportieren
- An anderer Stelle (oder nach Neuinstallation) wieder einlesen, ersetzt dabei
  alle vorhandenen Zähler

## Dateien

```
app/src/main/java/de/anniversarycounter/MainActivity.java               Zähler-Seiten (ViewPager), Sekundentakt
app/src/main/java/de/anniversarycounter/DateDiff.java                    Rechenlogik + Formatierung
app/src/main/java/de/anniversarycounter/AnniversaryCounter.java                 Datenmodell eines Zählers
app/src/main/java/de/anniversarycounter/AnniversaryCounterStore.java             Persistierung aller Zähler als JSON
app/src/main/java/de/anniversarycounter/CounterPagerAdapter.java          RecyclerView-Adapter für den ViewPager
app/src/main/java/de/anniversarycounter/CounterCreationFlow.java          Dialogkette zum Anlegen eines Zählers
app/src/main/java/de/anniversarycounter/CounterSettingsActivity.java      Einstellungen pro Zähler + globale Aktionen
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
