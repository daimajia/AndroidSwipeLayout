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

public class MyActivity extends Activity {

    private SwipeLayout sample1, sample2,sample3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SwipeLayout swipeLayout = (SwipeLayout)findViewById(R.id.godfather);
        swipeLayout.setDragEdge(SwipeLayout.DragEdge.Bottom);

        //
        sample1 = (SwipeLayout)findViewById(R.id.sample1);
        sample1.setShowMode(SwipeLayout.ShowMode.LayDown);
        sample1.setDragEdge(SwipeLayout.DragEdge.Left);
        sample1.addRevealListener(R.id.delete, new SwipeLayout.OnRevealListener() {
            @Override
            public void onReveal(View child, SwipeLayout.DragEdge edge, float fraction, int distance) {

            }
        });

        sample2 = (SwipeLayout)findViewById(R.id.sample2);
        sample2.setShowMode(SwipeLayout.ShowMode.LayDown);
        sample2.setShowMode(SwipeLayout.ShowMode.PullOut);
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
//        sample2.addRevealListener(new int[]{R.id.magnifier, R.id.star, R.id.trash}, new SwipeLayout.OnRevealListener() {
//            @Override
//            public void onReveal(View child, SwipeLayout.DragEdge edge, float fraction, int distance) {
//                child.setScaleX(fraction);
//            }
//        });

        sample2.findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MyActivity.this, "Yo",Toast.LENGTH_SHORT).show();
            }
        });


        sample3 = (SwipeLayout)findViewById(R.id.sample3);
        sample3.setDragEdge(SwipeLayout.DragEdge.Top);
        sample3.addRevealListener(R.id.bottom_wrapper_child1, new SwipeLayout.OnRevealListener() {
            @Override
            public void onReveal(View child, SwipeLayout.DragEdge edge, float fraction, int distance) {
                View star = child.findViewById(R.id.star);
                float d = child.getHeight() / 2 - star.getHeight() / 2;
                star.setTranslationY(d * fraction);
                star.setScaleX(fraction+0.6f);
                star.setScaleY(fraction+0.6f);
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

//        final int[] res = new int[]{R.id.changeLeft, R.id.changeRight, R.id.changeTop, R.id.changeBottom};
//        final SwipeLayout.DragEdge[] edges = new SwipeLayout.DragEdge[]{SwipeLayout.DragEdge.Left, SwipeLayout.DragEdge.Right, SwipeLayout.DragEdge.Top, SwipeLayout.DragEdge.Bottom};
//        final SwipeLayout[] layouts = new SwipeLayout[]{sample1,sample2,sample3};
//        for(int i = 0; i < res.length; i++){
//            findViewById(res[i]).setTag(edges[i]);
//            findViewById(res[i]).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    for (SwipeLayout l : layouts) {
//                        l.setDragEdge((SwipeLayout.DragEdge) v.getTag());
//                    }
//                }
//            });
//        }
//
//        findViewById(R.id.pullout).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                for (SwipeLayout l : layouts) {
//                    l.setShowMode(SwipeLayout.ShowMode.PullOut);
//                }
//            }
//        });
//
//        findViewById(R.id.laydown).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                for(SwipeLayout l : layouts){
//                    l.setShowMode(SwipeLayout.ShowMode.LayDown);
//                }
//            }
//        });

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
