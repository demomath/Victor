package com.android.task_annotation;

/**
 * ClassName: TaskConstant Description: java类作用描述 Author: wudi41 Date: 2022/9/2 14:55
 */
public interface ITaskConstant {
    int PRIORITY_MAX = Integer.MIN_VALUE;
    int PRIORITY_HIGH = -1000;
    int PRIORITY_NORM = 0;
    int PRIORITY_LOW = 1000;
    int PRIORITY_MIN = Integer.MAX_VALUE;

    String PROCESS_MAIN = "main";
}