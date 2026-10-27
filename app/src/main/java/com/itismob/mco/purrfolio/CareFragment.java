package com.itismob.mco.purrfolio;

public class CareFragment extends BaseListFragment {
    @Override protected void onReady() { titleTv.setText("Care"); subtitleTv.setText("Keep up with your pet's care"); }
    @Override protected void load() { showRows(java.util.Collections.emptyList()); }
}
