package com.daimajia.swipedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.daimajia.swipe.SwipeAdapter;
import com.daimajia.swipedemo.adapter.GridViewAdapter;

public class GridViewExample extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gridview);
        final GridView gridView = (GridView)findViewById(R.id.gridview);
        final GridViewAdapter adapter = new GridViewAdapter(this);
        adapter.setMode(SwipeAdapter.Mode.Multiple);
        gridView.setAdapter(adapter);
        gridView.setSelected(false);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("onItemLongClick","onItemLongClick:" + position);
                return false;
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("onItemClick","onItemClick:" + position);
            }
        });


        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.e("onItemSelected","onItemSelected:" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Handler handler = (new Handler());
        // Open up some items in the list
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.openItem(1);
                adapter.openItem(2);
                adapter.openItem(3);
                adapter.openItem(1);
            }
        }, 1000);


        // Close some items in the list
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.closeItem(1);
                adapter.closeItem(1);
                adapter.closeItem(3);
            }
        }, 3000);

    }
}
