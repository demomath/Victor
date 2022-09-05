package com.android.demo1;

import android.content.Context;
import android.util.Log;

import com.android.task_annotation.TaskAnnotation;
import com.android.task_interf.IRunTask;
import com.android.task_interf.IVictor;

/**
 * Name: TaskB Description: 测试任务B Author: wudi41 Date: 2022/8/31 14:30
 */
@TaskAnnotation(name = "TaskB", depends = {"TaskC"})
public class TaskB implements IRunTask {
    @Override
    public void execute(Context context) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(IVictor.TAG, "任务B 对 「交易」 进行了初始化，耗时 2s");
    }
}