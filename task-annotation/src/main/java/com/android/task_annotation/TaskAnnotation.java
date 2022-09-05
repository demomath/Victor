package com.android.task_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Name: TaskAnnotation
 * Description: 任务的注解
 * Author: wudi41
 * Date: 2022/08/31
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface TaskAnnotation {
    /** 任务名称，需唯一 */
    String name();

    /** 是否在后台线程执行 */
    boolean background() default true;

    /** 优先级，越小优先级越高 */
    int priority() default ITaskConstant.PRIORITY_NORM;

    /** 任务执行进程，支持主进程、非主进程、所有进程、:xxx、特定进程名 */
    String[] process() default {ITaskConstant.PROCESS_MAIN};

    /** 依赖的任务 */
    String[] depends() default {} ;
}
