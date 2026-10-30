package com.itismob.mco.purrfolio;

import java.util.ArrayList;
import java.util.List;

public class NotificationSettingsActivity extends BaseListActivity {
    @Override
    protected void onScreenCreated() {
        titleTv.setText("Notifications");
        subtitleTv.setText("We’ll only notify you about what matters.");
    }

    @Override
    protected void load() {
        List<Row> out = new ArrayList<>();
        out.add(Row.header("Care Reminders"));
        out.add(item("care", "Care Reminders", "Medication, feeding, playtime, etc."));
        out.add(Row.header("Updates"));
        out.add(item("updates", "App Updates", "New features and improvements"));
        out.add(item("tips", "Tips & Insights", "General pet care tips (not medical advice)"));
        out.add(Row.header("Shared Care"));
        out.add(item("circle", "Care Circle Activity", "When someone updates a record"));
        out.add(Row.header("Marketing"));
        out.add(item("marketing", "Product News", "Occasional updates and offers"));
        showRows(out);
    }

    private Row item(String key, String title, String sub) {
        return toggleRow(key, title, sub, Session.notif(this, key));
    }

    @Override
    protected void onRowClick(Row row) {
        Session.setNotif(this, row.id, !Session.notif(this, row.id));
        load();
    }
}