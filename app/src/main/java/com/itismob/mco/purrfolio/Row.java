package com.itismob.mco.purrfolio;

public class Row {
    public String id, title, subtitle, trailing, extra;
    public boolean header, danger;

    public Row(String id, String title, String subtitle, String trailing) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.trailing = trailing;
    }

    // section title such as "Today" or "Upcoming" (not clickable)
    public static Row header(String title) {
        Row r = new Row(null, title, null, null);
        r.header = true;
        return r;
    }
}

