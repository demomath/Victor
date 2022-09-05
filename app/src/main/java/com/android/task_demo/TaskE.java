package com.android.task_demo;

import android.content.Context;
import android.util.Log;

import com.android.task_annotation.TaskAnnotation;
import com.android.task_interf.IRunTask;
import com.android.task_interf.IVictor;

/**
 * Name: DemoTask1 Description: 测试任务1 Author: wudi41 Date: 2022/8/31 14:30
 */
@TaskAnnotation(name = "TaskE", background = true, priority = 0)
public class TaskE implements IRunTask {
    @Override
    public void execute(Context context) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(IVictor.TAG, "任务E 对 「feed」 进行了初始化，耗时 1s");
    }
}