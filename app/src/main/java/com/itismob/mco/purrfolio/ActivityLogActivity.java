package com.itismob.mco.purrfolio;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogActivity extends BaseListActivity {
    @Override
    protected void onScreenCreated() {
        titleTv.setText("Care Circle Activity Log");
        subtitleTv.setText("Track who did what and when");
    }

    @Override
    protected void load() {
        Repo.sub(petId(), "activity").orderBy("timestamp", Query.Direction.DESCENDING).limit(150).get().addOnSuccessListener(snap -> {
            List<Row> out = new ArrayList<>();
            String lastDay = "";
            for (DocumentSnapshot d : snap) {
                Long ts = d.getLong("timestamp");
                long t = ts == null ? 0 : ts;
                String day = Ui.dayLabel(t);
                if (!day.equals(lastDay)) { out.add(Row.header(day)); lastDay = day; }

                String detail = Repo.str(d, "detail");
                boolean me = Repo.uid().equals(d.getString("authorId"));
                String sub = Ui.time(t) + (detail.isEmpty() ? "" : " · " + detail) + " · by " + (me ? "You" : Repo.str(d, "authorName"));
                out.add(new Row(d.getId(), Repo.str(d, "action"), sub, ""));
            }
            showRows(out);
        });
    }
}
