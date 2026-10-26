package com.itismob.mco.purrfolio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseListFragment extends Fragment {
    protected TextView titleTv, subtitleTv, bannerTv;
    protected RecyclerView rv;
    protected MaterialButton actionBtn;
    protected final ArrayList<Row> rows = new ArrayList<>();
    protected RowAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.titleTv = view.findViewById(R.id.titleTv);
        this.subtitleTv = view.findViewById(R.id.subtitleTv);
        this.bannerTv = view.findViewById(R.id.bannerTv);
        this.rv = view.findViewById(R.id.rv);
        this.actionBtn = view.findViewById(R.id.actionBtn);
        this.adapter = new RowAdapter(this.rows, this::onRowClick, this::onRowLongClick);
        this.rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        this.rv.setAdapter(this.adapter);
        onReady();
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    protected abstract void onReady();
    protected abstract void load();
    protected void onRowClick(Row row) { }
    protected void onRowLongClick(Row row) { }

    protected void showRows(List<Row> list) {
        if (!isAdded()) return;
        this.rows.clear();
        this.rows.addAll(list);
        this.adapter.notifyDataSetChanged();
    }
}
