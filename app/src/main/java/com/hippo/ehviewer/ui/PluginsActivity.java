/*
 * Copyright 2018 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

/*
 * Created by Hippo on 2018/3/23.
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.axlecho.api.MHPlugin;
import com.axlecho.api.MHPluginManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hippo.android.resource.AttrResources;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.R;
import com.hippo.ripple.Ripple;
import com.hippo.yorozuya.LayoutUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PluginsActivity extends ToolbarActivity
        implements EasyRecyclerView.OnItemClickListener, View.OnClickListener {


    private List<MHPlugin> data;

    private EasyRecyclerView recyclerView;
    private View tip;
    private HostsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = MHPluginManager.Companion.getINSTANCE().plugins();

        setContentView(R.layout.activity_plugins);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
        recyclerView = findViewById(R.id.recycler_view);
        tip = findViewById(R.id.tip);
        FloatingActionButton fab = findViewById(R.id.fab);

        adapter = new HostsAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL,
                AttrResources.getAttrColor(this, R.attr.dividerColor),
                LayoutUtils.dp2pix(this, 1));
        decoration.setShowLastDivider(true);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setSelector(Ripple.generateRippleDrawable(this, !AttrResources.getAttrBoolean(this, R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnItemClickListener(this);
        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                recyclerView.getPaddingBottom() + getResources().getDimensionPixelOffset(R.dimen.gallery_padding_bottom_fab));

        fab.setOnClickListener(this);

        recyclerView.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
        tip.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView easyRecyclerView, View view, int position, long id) {
        MHPlugin pair = data.get(position);
        return true;
    }

    @Override
    public void onClick(View v) {
        importData(this);
    }

    private void notifyHostsChanges() {
        data = MHPluginManager.Companion.getINSTANCE().plugins();
        recyclerView.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
        tip.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private class HostsHolder extends RecyclerView.ViewHolder {

        public final TextView host;
        public final TextView ip;

        public HostsHolder(View itemView) {
            super(itemView);
            host = itemView.findViewById(R.id.host);
            ip = itemView.findViewById(R.id.ip);
        }
    }

    private class HostsAdapter extends RecyclerView.Adapter<HostsHolder> {

        private final LayoutInflater inflater = getLayoutInflater();

        @NonNull
        @Override
        public HostsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new HostsHolder(inflater.inflate(R.layout.item_hosts, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull HostsHolder holder, int position) {
            MHPlugin plugin = data.get(position);
            holder.host.setText(plugin.getName());
            holder.ip.setText(plugin.isEnable() ? "enable" : "disable");
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static void importData(final Context context) {
        final File dir = AppConfig.getExternalDataPlugins();
        if (null == dir) {
            Toast.makeText(context, R.string.cant_get_data_dir, Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] files = dir.list();
        if (null == files || files.length <= 0) {
            Toast.makeText(context, R.string.cant_find_any_data, Toast.LENGTH_SHORT).show();
            return;
        }
        Arrays.sort(files);
        new AlertDialog.Builder(context).setItems(files, (dialog, which) -> {
            File file = new File(dir, files[which]);
            try {
                MHPlugin plugin = MHPluginManager.Companion.getINSTANCE().parserPlugin(file.getAbsolutePath());
                MHPluginManager.Companion.getINSTANCE().savePlugin(plugin);
            } catch (Exception e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(context, R.string.add_plugin_successful, Toast.LENGTH_SHORT).show();
        }).show();
    }
}
