package com.android.task_interf;

import com.android.task_annotation.TaskEntity;

/**
 * ClassName: RunTaskListener Description: 任务执行监听 Author: wudi41 Date: 2022/9/1 19:27
 */
public interface RunTaskListener {
    /**
     * 单一任务执行结束
     * @param task 单一任务
     */
    void onSingleComplete(TaskEntity task);

    /**
     * 全部执行结束
     */
    void onAllComplete();
}