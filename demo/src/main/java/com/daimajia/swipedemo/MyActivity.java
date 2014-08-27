package com.daimajia.swipedemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.nineoldandroids.view.ViewHelper;

public class MyActivity extends Activity {

    private SwipeLayout sample1, sample2,sample3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

//        SwipeLayout swipeLayout = (SwipeLayout)findViewById(R.id.godfather);
//        swipeLayout.setDragEdge(SwipeLayout.DragEdge.Bottom); // Set in XML

        //sample1

        sample1 = (SwipeLayout)findViewById(R.id.sample1);
        sample1.setShowMode(SwipeLayout.ShowMode.LayDown);
        sample1.setDragEdge(SwipeLayout.DragEdge.Left);
        sample1.addRevealListener(R.id.delete, new SwipeLayout.OnRevealListener() {
            @Override
            public void onReveal(View child, SwipeLayout.DragEdge edge, float fraction, int distance) {

            }
        });

        //sample2

        sample2 = (SwipeLayout)findViewById(R.id.sample2);
        sample2.setShowMode(SwipeLayout.ShowMode.LayDown);
//        sample2.setShowMode(SwipeLayout.ShowMode.PullOut);
        sample2.findViewById(R.id.star).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MyActivity.this, "Star", Toast.LENGTH_SHORT).show();
            }
        });

        sample2.findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MyActivity.this, "Trash Bin", Toast.LENGTH_SHORT).show();
            }
        });

        sample2.findViewById(R.id.magnifier).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MyActivity.this, "Magnifier", Toast.LENGTH_SHORT).show();
            }
        });

        sample2.findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MyActivity.this, "Yo",Toast.LENGTH_SHORT).show();
            }
        });
        sample1.addSwipeListener(new SwipeLayout.SwipeListener() {
            @Override
            public void onClose(SwipeLayout layout) {

            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

            }

            @Override
            public void onOpen(SwipeLayout layout) {

            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

            }
        });

        //sample3

        sample3 = (SwipeLayout)findViewById(R.id.sample3);
        sample3.setDragEdge(SwipeLayout.DragEdge.Top);
        sample3.addRevealListener(R.id.bottom_wrapper_child1, new SwipeLayout.OnRevealListener() {
            @Override
            public void onReveal(View child, SwipeLayout.DragEdge edge, float fraction, int distance) {
                View star = child.findViewById(R.id.star);
                float d = child.getHeight() / 2 - star.getHeight() / 2;
                ViewHelper.setTranslationY(star, d * fraction);
                ViewHelper.setScaleX(star, fraction + 0.6f);
                ViewHelper.setScaleY(star, fraction + 0.6f);
                int c = (Integer)evaluate(fraction, Color.parseColor("#dddddd"), Color.parseColor("#4C535B"));
                child.setBackgroundColor(c);
            }
        });
        sample3.findViewById(R.id.star).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MyActivity.this, "Yo!", Toast.LENGTH_SHORT).show();
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
            return true;
        }else if(id == R.id.action_gridview){
            startActivity(new Intent(this, GridViewExample.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    Color transition method.
     */
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }
}
