package com.itismob.mco.purrfolio;

public class HomeFragment extends BaseListFragment {
    @Override protected void onReady() { titleTv.setText("Home"); subtitleTv.setText("Welcome to Purrfolio"); }
    @Override protected void load() { showRows(java.util.Collections.emptyList()); }
}
