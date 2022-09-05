package com.android.task_annotation;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * ClassName: TaskRegister
 * Description: 任务注册
 * Author: wudi41
 * Date: 2022/8/31 15:56
 */
public interface ITaskRegister {
    void register(ArrayList<TaskEntity> tasks);
}