package com.snail.test.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LeakTestActivity extends BaseActivity {

    public static List<Activity> sActivity=new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity .add(this);
        TextView textView = new TextView(this);
        textView.setText("LeakTestActivity");
        setContentView(textView);
    }
}
