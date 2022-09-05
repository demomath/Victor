package com.android.task_demo;

import android.app.Application;
import android.util.Log;

import com.android.task_annotation.TaskEntity;
import com.android.task_impl.TaskRegisterManager;
import com.android.task_interf.IVictor;
import com.android.task_interf.RunTaskListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ClassName: VictorApplication Description: VictorApplication Author: wudi41 Date: 2022/9/1 15:53
 */
public class VictorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ArrayList<TaskEntity> taskList = TaskRegisterManager.getInstance().getTaskList();
        Log.i(IVictor.TAG, "收集到任务 " + taskList.size() + " 个，分别如下：");
        for (TaskEntity task :  taskList) {
            Log.i(IVictor.TAG, "entity = " + task.toString());
        }
        TaskRegisterManager.getInstance().start(this, new RunTaskListener() {
            @Override
            public void onSingleComplete(TaskEntity task) {
                Log.i(IVictor.TAG, "task.name = " + task.name + "执行完成");

            }

            @Override
            public void onAllComplete() {
                Log.i(IVictor.TAG, "全部任务执行完成");
            }
        });
    }
}