package com.daimajia.swipedemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class NestedExample extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nested);
        findViewById(R.id.hhhhh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Tag","got");
            }
        });
    }
}
