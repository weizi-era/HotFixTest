package com.example.hotfixtest;

import android.util.Log;

public class Test {

    public static void test() {
     //   Log.d("TAG", "test: bug修复");
        throw new IllegalArgumentException("出bug啦");
    }
}
