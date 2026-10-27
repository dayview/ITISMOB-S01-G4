package com.itismob.mco.purrfolio;

public class LogFragment extends BaseListFragment {
    @Override protected void onReady() { titleTv.setText("Log"); subtitleTv.setText("Track your pet's activities"); }
    @Override protected void load() { showRows(java.util.Collections.emptyList()); }
}
