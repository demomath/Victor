package com.android.task_interf;

import android.content.Context;

import com.android.task_annotation.ITask;

/**
 * Name: TaskAnnotation
 * Description: 任务的接口定义
 * Author: wudi41
 * Date: 2022/08/31
 */
public interface IRunTask extends ITask {
    /**
     * 执行任务
     * @param context 执行任务的上下文
     */
    void execute(Context context);
}
