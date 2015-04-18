package com.daimajia.swipedemo;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.util.Attributes;
import com.daimajia.swipedemo.adapter.ListViewAdapter;

public class ListViewExample extends Activity {

    private ListView mListView;
    private ListViewAdapter mAdapter;
    private Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);
        mListView = (ListView) findViewById(R.id.listview);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setTitle("ListView");
            }
        }

        /**
         * The following comment is the sample usage of ArraySwipeAdapter.
         */
//        String[] adapterData = new String[]{"Activity", "Service", "Content Provider", "Intent", "BroadcastReceiver", "ADT", "Sqlite3", "HttpClient",
//                "DDMS", "Android Studio", "Fragment", "Loader", "Activity", "Service", "Content Provider", "Intent",
//                "BroadcastReceiver", "ADT", "Sqlite3", "HttpClient", "Activity", "Service", "Content Provider", "Intent",
//                "BroadcastReceiver", "ADT", "Sqlite3", "HttpClient"};
//        mListView.setAdapter(new ArraySwipeAdapterSample<String>(this, R.layout.listview_item, R.id.position, adapterData));

        mAdapter = new ListViewAdapter(this);
        mListView.setAdapter(mAdapter);
        mAdapter.setMode(Attributes.Mode.Single);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((SwipeLayout)(mListView.getChildAt(position - mListView.getFirstVisiblePosition()))).open(true);
            }
        });
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e("ListView", "OnTouch");
                return false;
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(mContext, "OnItemLongClickListener", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.e("ListView", "onScrollStateChanged");
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.e("ListView", "onItemSelected:" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e("ListView", "onNothingSelected:");
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_listview) {
            startActivity(new Intent(this, ListViewExample.class));
            finish();
            return true;
        } else if (id == R.id.action_gridview) {
            startActivity(new Intent(this, GridViewExample.class));
            finish();
            return true;
        } else if (id == R.id.action_recycler) {
            startActivity(new Intent(this, RecyclerViewExample.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
