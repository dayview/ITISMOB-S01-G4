package com.itismob.mco.purrfolio;

import android.content.Intent;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class CareLogActivity extends BaseListActivity {
    @Override
    protected void onScreenCreated() {
        titleTv.setText("Care Log");
        subtitleTv.setText("Tap to edit · long-press to delete");
    }

    @Override
    protected void load() {
        Repo.sub(petId(), "logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(200).get().addOnSuccessListener(snap -> {
            List<Row> out = new ArrayList<>();
            String lastDay = "";
            for (DocumentSnapshot d : snap) {
                Long ts = d.getLong("timestamp");
                long t = ts == null ? 0 : ts;
                String day = Ui.dayLabel(t);
                if (!day.equals(lastDay)) { out.add(Row.header(day)); lastDay = day; }

                String amount = Repo.str(d, "amount");
                String sub = (amount.isEmpty() ? "" : amount + " · ") + Ui.time(t) + " · by " + Repo.str(d, "authorName");
                out.add(new Row(d.getId(), Repo.str(d, "type"), sub, "Edit"));
            }
            showRows(out);
        });
    }

    @Override
    protected void onRowClick(Row row) {
        Repo.can(petId(), "canLog", ok -> {
            if (!ok) { Ui.toast(this, "You don't have permission to delete logs"); return; }
            Ui.confirm(this, "Delete this log?", row.title + " · " + row.subtitle, () -> {
                Repo.sub(petId(), "logs").document(row.id).delete();
                Repo.activity(petId(), "Deleted " + row.title + " log", "");
                rows.remove(row);
                adapter.notifyDataSetChanged();
            });
        });
    }
}
