package com.example.hotfixtest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionX.init(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                .onExplainRequestReason((scope, deniedList) -> scope.showRequestReasonDialog(deniedList, "需要您同意以下授权才能正常使用","同意","拒绝"))
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        Toast.makeText(MainActivity.this, "您同意了所有权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "您拒绝了以下权限" + deniedList, Toast.LENGTH_SHORT).show();
                    }
                });

        Test.test();
    }

}