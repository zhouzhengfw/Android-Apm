package com.snail.test.activity;

import android.os.Bundle;
import android.os.Handler;

import com.snail.test.app.MyButton;
import com.snail.test.databinding.ActivitySecondBinding;


public class LauncherTestActivity extends BaseActivity {

    private int count;
    private ActivitySecondBinding mActivitySecondBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        SystemClock.sleep(1000);
        MyButton textView = new MyButton(this);
        textView.setText("LauncherTestActivity");
        setContentView(textView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {


            }
        });
//        SystemClock.sleep(2000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        SystemClock.sleep(1000);
    }
}
