package com.android.task_demo;

import android.os.Bundle;
import android.util.Log;

import com.android.task_annotation.TaskEntity;
import com.android.task_impl.TaskRegisterManager;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}