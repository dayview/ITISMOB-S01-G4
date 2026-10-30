package com.itismob.mco.purrfolio;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import java.util.ArrayList;
import java.util.List;

public class OfflineSyncActivity extends BaseListActivity {
    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback netCallback;
    private ListenerRegistration registration;

    @Override
    protected void onScreenCreated() {
        titleTv.setText("Offline & Sync");
        subtitleTv.setText("Never stop recording, even offline");
        show(bannerTv, true);
        actionBtn.setText("Sync Now");
        show(actionBtn, true);
        actionBtn.setOnClickListener(v -> {
            Repo.db().enableNetwork();
            Repo.db().waitForPendingWrites().addOnSuccessListener(r -> Ui.toast(this, "Everything is synced"));
            Ui.toast(this, "Syncing…");
        });

        cm = getSystemService(ConnectivityManager.class);
        netCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network n) { runOnUiThread(() -> updateBanner(true)); }
            @Override public void onLost(Network n) { runOnUiThread(() -> updateBanner(false)); }
        };
        cm.registerDefaultNetworkCallback(netCallback);
        updateBanner(isOnline());
    }

    private boolean isOnline() {
        NetworkCapabilities c = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void updateBanner(boolean online) {
        bannerTv.setText(online ? "You’re online. Entries sync automatically."
                : "You’re offline. Your entries will be saved on this phone and synced later.");
    }

    @Override
    protected void load() {
        if (registration != null) registration.remove();
        registration = Repo.sub(petId(), "logs").addSnapshotListener(MetadataChanges.INCLUDE, (snap, e) -> {
            if (snap == null) return;
            List<Row> pending = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                if (!d.getMetadata().hasPendingWrites()) continue;
                Long ts = d.getLong("timestamp");
                pending.add(new Row(d.getId(), Repo.str(d, "type"),
                        Ui.dateTime(ts == null ? 0 : ts) + " · " + Repo.str(d, "petName"), "⏳"));
            }
            List<Row> out = new ArrayList<>();
            out.add(Row.header("Pending Sync (" + pending.size() + ")"));
            out.addAll(pending);
            showRows(out);
            emptyTv.setText("All caught up ✔");
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (registration != null) { registration.remove(); registration = null; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cm != null && netCallback != null) cm.unregisterNetworkCallback(netCallback);
    }
}
