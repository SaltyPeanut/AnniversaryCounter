package de.anniversarycounter;

/**
 * Per Opt-in/Opt-out waehlbare Eintraege des Schnellmenues ({@link QuickMenuController}). Die
 * Reihenfolge hier ist auch die Reihenfolge im Menue. Backup ist bewusst nicht waehlbar: selten
 * gebraucht, und das Wiederherstellen ersetzt alle Zaehler.
 */
enum QuickMenuItem {
    SET_DATE(Page.EDIT, R.string.btn_set_date, true, true),
    RENAME(Page.EDIT, R.string.btn_set_name, true, true),
    EDIT_MESSAGE(Page.EDIT, R.string.btn_set_message, true, true),
    SHOW_NAME(Page.EDIT, R.string.show_name, true, true),
    SHOW_MESSAGE(Page.EDIT, R.string.show_message, true, true),
    COUNTDOWN(Page.EDIT, R.string.countdown_to_next, false, true),

    BIRTHDAY(Page.DISPLAY, R.string.is_birthday, false, true),
    CUSTOM_APPEARANCE(Page.DISPLAY, R.string.custom_appearance_checkbox, false, true),
    COUNTER_APPEARANCE(Page.DISPLAY, R.string.btn_counter_appearance, false, true),
    BACKGROUND(Page.DISPLAY, R.string.btn_change_background, false, true),

    MILESTONES(Page.NOTIFICATIONS, R.string.btn_notifications, true, true),
    CONTACT(Page.NOTIFICATIONS, R.string.btn_set_contact, true, true),

    NEW_COUNTER(Page.MANAGE, R.string.btn_add_counter, true, false),
    DELETE(Page.MANAGE, R.string.btn_delete_counter, true, true),

    LIGHT_THEME(Page.GENERAL, R.string.use_light_theme, false, false),
    GLOBAL_APPEARANCE(Page.GENERAL, R.string.btn_appearance, false, false),
    FORMAT(Page.GENERAL, R.string.btn_format_settings, false, false),
    REORDER(Page.GENERAL, R.string.btn_reorder_counters, false, false),
    CALENDAR_IMPORT(Page.GENERAL, R.string.btn_import_calendar, false, false);

    enum Page {
        EDIT(R.string.menu_page_edit),
        DISPLAY(R.string.menu_page_display),
        NOTIFICATIONS(R.string.menu_page_notifications),
        MANAGE(R.string.menu_page_manage),
        GENERAL(R.string.settings_section_global);

        final int titleRes;

        Page(int titleRes) {
            this.titleRes = titleRes;
        }
    }

    final Page page;
    final int labelRes;
    final boolean enabledByDefault;
    /** Braucht einen aktuellen Zaehler und entfaellt, solange es keinen gibt. */
    final boolean needsCounter;

    QuickMenuItem(Page page, int labelRes, boolean enabledByDefault, boolean needsCounter) {
        this.page = page;
        this.labelRes = labelRes;
        this.enabledByDefault = enabledByDefault;
        this.needsCounter = needsCounter;
    }
}
