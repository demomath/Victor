package com.android.demo2;

import android.content.Context;
import android.util.Log;

import com.android.task_annotation.TaskAnnotation;
import com.android.task_interf.IRunTask;
import com.android.task_interf.IVictor;

/**
 * Name: TaskC Description: 测试任务C Author: wudi41 Date: 2022/8/31 14:30
 */
@TaskAnnotation(name = "TaskC", depends = {"TaskD"})
public class TaskC implements IRunTask {
    @Override
    public void execute(Context context) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(IVictor.TAG, "TaskC 对 「账号」 进行了初始化，耗时 3s");
    }
}