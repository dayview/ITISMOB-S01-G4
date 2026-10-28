package com.itismob.mco.purrfolio;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseListActivity extends AppCompatActivity {
    protected TextView backBtn, addBtn, titleTv, subtitleTv, bannerTv, emptyTv;
    protected EditText searchEt;
    protected TabLayout filterTabs;
    protected Spinner dateSpinner;
    protected CalendarView calendarView;
    protected ImageView imageIv;
    protected RecyclerView rv;
    protected MaterialButton actionBtn, action2Btn;

    protected final ArrayList<Row> rows = new ArrayList<>();
    protected RowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.screen(this, R.layout.activity_list);
        this.backBtn = findViewById(R.id.backBtn);
        this.addBtn = findViewById(R.id.addBtn);
        this.titleTv = findViewById(R.id.titleTv);
        this.subtitleTv = findViewById(R.id.subtitleTv);
        this.bannerTv = findViewById(R.id.bannerTv);
        this.emptyTv = findViewById(R.id.emptyTv);
        this.searchEt = findViewById(R.id.searchEt);
        this.filterTabs = findViewById(R.id.filterTabs);
        this.dateSpinner = findViewById(R.id.dateSpinner);
        this.calendarView = findViewById(R.id.calendarView);
        this.imageIv = findViewById(R.id.imageIv);
        this.rv = findViewById(R.id.rv);
        this.actionBtn = findViewById(R.id.actionBtn);
        this.action2Btn = findViewById(R.id.action2Btn);
        this.adapter = new RowAdapter(this.rows, this::onRowClick, this::onRowLongClick);
        this.rv.setLayoutManager(new LinearLayoutManager(this));
        this.rv.setAdapter(this.adapter);
        this.backBtn.setOnClickListener(v -> finish());
        onScreenCreated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    protected void onScreenCreated() { }
    protected abstract void load();
    protected void onRowClick(Row row) { }
    protected void onRowLongClick(Row row) { }
    protected void onFilterChanged() { }

    protected void showRows(List<Row> list) {
        this.rows.clear();
        this.rows.addAll(list);
        this.adapter.notifyDataSetChanged();
        this.emptyTv.setVisibility(this.rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    protected void show(View v, boolean visible) { v.setVisibility(visible ? View.VISIBLE : View.GONE); }
    protected void addTabs(String... names) {
        for (String n : names) this.filterTabs.addTab(this.filterTabs.newTab().setText(n));
        this.filterTabs.setVisibility(View.VISIBLE);
        this.filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { onFilterChanged(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    protected String selectedTab() {
        TabLayout.Tab t = this.filterTabs.getTabAt(this.filterTabs.getSelectedTabPosition());
        return t == null || t.getText() == null ? "All" : t.getText().toString();
    }

    // pet is shown on this screen: passed for the intent, otherwise the currently selected pet
    protected String petId() {
        String id = getIntent().getStringExtra(Ui.PET_ID);
        return id != null ? id : Session.petId(this);
    }

    protected Row toggleRow(String key, String title, String subtitle, boolean on) {
        return new Row(key, title, subtitle, on ? "ON" : "OFF");
    }
}

