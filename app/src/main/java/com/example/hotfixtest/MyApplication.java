package com.example.hotfixtest;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.example.lib.HotFix;

import java.io.File;

public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        HotFix.install(this, new File("/sdcard/patch.jar"));
       // HotFix.install(this, new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch.jar"));
    }
}
