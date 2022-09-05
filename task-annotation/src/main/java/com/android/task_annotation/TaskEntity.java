package com.android.task_annotation;

import java.util.Arrays;
import java.util.Objects;

/**
 * ClassName: TaskEntity Description: 任务的实体类 Author: wudi41 Date: 2022/8/31 14:39
 */
public class TaskEntity implements Comparable {
    /** 任务名称，需唯一 */
    public String name;
    /** 是否在后台线程执行 */
    public boolean background;
    /** 优先级，越小优先级越高 */
    public int priority;
    /** 任务执行进程，支持主进程、非主进程、所有进程、:xxx、特定进程名 */
    public String[] process;
    /** 依赖的任务 */
    public String[] depends;
    /** 要执行的任务工作 */
    public ITask task;
    /** 是否已经执行 */
    public boolean executed;

    public TaskEntity(String name, boolean background, int priority, String[] process, String[] depends, ITask task) {
        this.name = name;
        this.background = background;
        this.priority = priority;
        this.process = process;
        this.depends = depends;
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskEntity that = (TaskEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof TaskEntity) {
            TaskEntity p = (TaskEntity) o;
            return p.priority - this.priority;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "TaskEntity{" +
                "name='" + name + '\'' +
                ", background=" + background +
                ", priority=" + priority +
                ", process=" + Arrays.toString(process) +
                ", depends=" + Arrays.toString(depends) +
                '}';
    }
}