package com.android.demo3;

import android.content.Context;
import android.util.Log;

import com.android.task_annotation.TaskAnnotation;
import com.android.task_annotation.TaskEntity;
import com.android.task_interf.IRunTask;
import com.android.task_interf.IVictor;

/**
 * Name: TaskD Description: 测试任务D Author: wudi41 Date: 2022/8/31 14:30
 */
@TaskAnnotation(name = "TaskD", background = false)
public class TaskD implements IRunTask {
    @Override
    public void execute(Context context) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(IVictor.TAG, "任务D 对 「Pass」 进行了初始化");
    }
}