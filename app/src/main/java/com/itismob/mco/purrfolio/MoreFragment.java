package com.itismob.mco.purrfolio;

public class MoreFragment extends BaseListFragment {
    @Override protected void onReady() { titleTv.setText("More"); subtitleTv.setText("Manage your Purrfolio"); }
    @Override protected void load() { showRows(java.util.Collections.emptyList()); }
}
