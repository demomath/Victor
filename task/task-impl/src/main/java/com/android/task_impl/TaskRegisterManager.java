package com.android.task_impl;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.task_annotation.ITaskRegister;
import com.android.task_annotation.TaskEntity;
import com.android.task_interf.IRunTask;
import com.android.task_interf.IVictor;
import com.android.task_interf.RunTaskListener;
import com.android.task_interf.TaskProcessUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;

import androidx.annotation.NonNull;

/**
 * ClassName: TaskRegisterManager Description: 任务注册管理类 Author: wudi41 Date: 2022/8/31 19:09
 */
public class TaskRegisterManager {

    /** application上下文 */
    private Application mApplication;
    /** 执行监听 */
    private RunTaskListener mListener;
    /** 启动任务集合 */
    private final ArrayList<TaskEntity> mTaskList = new ArrayList<>();
    /** 没有依赖的任务的启动任务队列 */
    private final PriorityQueue<TaskEntity> mNoDependencyQueue = new PriorityQueue<>();
    /** 任务执行后通知执行结果 */
    private Handler mHandler;
    /** 任务拓扑关系表  x > y 表示有向图依赖关系  y > x 表示当前任务的子任务 */
    private boolean[][] mTaskRelationalTable;

    /**
     * 静态内部类单例
     */
    private static class TaskRegisterManagerHolder {
        private static final TaskRegisterManager SINGLE_TON = new TaskRegisterManager();
    }

    private TaskRegisterManager() {
        init();
    }

    public static TaskRegisterManager getInstance() {
        return TaskRegisterManagerHolder.SINGLE_TON;
    }

    /**
     * 这个方法会通过代码注入，将要注册类注入进来，并调用 register(ITaskRegister register)
     */
    private void init() {

    }

    /**
     * 注册任务，将任务添加到集合
     *
     * @param register 注册类，通过javapoet生成
     */
    private void register(ITaskRegister register) {
        register.register(mTaskList);
    }

    /**
     * 获取任务集合
     *
     * @return mTaskList
     */
    public ArrayList<TaskEntity> getTaskList() {
        return mTaskList;
    }

    /**
     * 执行任务
     */
    @SuppressLint("HandlerLeak")
    public void start(Application application, RunTaskListener listener) {
        if (checkCircularDependency()) {
            throw new RuntimeException("启动任务存在循环依赖!");
        }
        this.mApplication = application;
        this.mListener = listener;
        // 去除task map不符合当前进程的任务
        removeOtherProcessTask();
        if (mTaskList.size() == 0) {
            releaseResources();
            return;
        }
        // 创建任务关系表
        createTaskRelationalTable();
        // 首批没有依赖的模块任务入队
        NoDependsTaskQueueOffer();
        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                TaskEntity entity = (TaskEntity) msg.obj;
                taskRunComplete(entity);
                luncherTask();
            }
        };
        // 任务从队列出队，执行任务
        luncherTask();
    }

    /**
     * 移除不符合当前进程的任务
     */
    private void removeOtherProcessTask() {
        Iterator<TaskEntity> iterator = mTaskList.iterator();
        while (iterator.hasNext()) {
            TaskEntity next = iterator.next();
            if (!TaskProcessUtil.checkTaskProcess(mApplication, next)) {
                iterator.remove();
            }
        }
    }

    /**
     * 检查循环依赖
     *
     * @return true 有循环 false 无循环
     */
    private boolean checkCircularDependency() {
        return false;
    }

    /**
     * 为每个任务标记子任务，并且子任务会根据优先级排序
     */
    private void createTaskRelationalTable() {
        mTaskRelationalTable = new boolean[mTaskList.size()][mTaskList.size()];
        for (int x = 0; x < mTaskList.size(); x++) {
            TaskEntity task = mTaskList.get(x);
            if (task.depends == null) {
                continue;
            }
            String[] depends = task.depends;
            for (String dependTaskName : depends) {
                int y = getTaskIndexByNameFromList(dependTaskName);
                if (y == -1) {
                    continue;
                }
                mTaskRelationalTable[x][y] = true;
            }
        }
    }

    /**
     * 通过任务名寻找任务实体位置
     *
     * @param dependTaskName 任务名
     *
     * @return 位置
     */
    private int getTaskIndexByNameFromList(String dependTaskName) {
        for (int i = 0, mTaskListSize = mTaskList.size(); i < mTaskListSize; i++) {
            TaskEntity entity = mTaskList.get(i);
            if (entity == null) {
                continue;
            }
            if (Objects.equals(entity.name, dependTaskName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 首批没有依赖的模块任务集合入队
     */
    private void NoDependsTaskQueueOffer() {
        for (TaskEntity entity : mTaskList) {
            if (entity != null && (entity.depends == null || entity.depends.length == 0)) {
                mNoDependencyQueue.offer(entity);
                Log.i(IVictor.TAG, "首次没有依赖的任务进队 ： entity = " + entity.toString());
            }
        }
    }

    /**
     * 开启执行
     */
    private void luncherTask() {
        if (mNoDependencyQueue.size() == 0) {
            return;
        }
        TaskEntity entity = mNoDependencyQueue.poll();
        if (entity == null) {
            luncherTask();
            return;
        }
        if (entity.background) {
            Log.i(IVictor.TAG, "任务 " + entity.name + " 开启子线程开始执行");
            runTaskOnChildThread(entity);
        } else {
            Log.i(IVictor.TAG, "任务 " + entity.name + " 当前线程开始执行");
            runTaskOnUIThread(entity);
        }
    }

    /**
     * 在主线程执行问题
     *
     * @param entity 任务
     */
    private void runTaskOnUIThread(TaskEntity entity) {
        IRunTask task = (IRunTask) entity.task;
        task.execute(mApplication);
        Message msg = new Message();
        msg.obj = entity;
        mHandler.handleMessage(msg);
    }

    /**
     * 在子线程执行任务
     *
     * @param entity 任务
     */
    private void runTaskOnChildThread(TaskEntity entity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IRunTask task = (IRunTask) entity.task;
                try {
                    task.execute(mApplication);
                } catch (Exception e) {
                    Log.i(IVictor.TAG, e.getMessage());
                }
                Message msg = new Message();
                msg.obj = entity;
                mHandler.handleMessage(msg);
            }
        }).start();
        luncherTask();
    }

    /**
     * 任务执行完成
     *
     * @param entity 任务
     */
    private void taskRunComplete(TaskEntity entity) {
        if (mListener != null) {
            mListener.onSingleComplete(entity);
        }
        entity.executed = true;
        changeDependRelationalAndAddEntity2Queue(mTaskList.indexOf(entity));
        if (checkAllTaskRunComplete()) {
            if (mListener != null) {
                mListener.onAllComplete();
            }
            releaseResources();
        }
    }

    /**
     * 取消依赖关系
     *
     * @param i 被依赖的task index
     */
    private void changeDependRelationalAndAddEntity2Queue(int i) {
        if (i < 0) {
            return;
        }
        for (int x = 0; x < mTaskRelationalTable.length; x++) {
            if (mTaskRelationalTable[x][i]) {
                boolean hasDepend = false;
                for (int y = 0; y < mTaskRelationalTable.length; y++) {
                    if (y == i) {
                        mTaskRelationalTable[x][y] = false;
                    } else {
                        hasDepend = hasDepend || mTaskRelationalTable[x][y];
                    }
                }
                if (!hasDepend) {
                    mNoDependencyQueue.offer(mTaskList.get(x));
                }
            }
        }
    }

    /**
     * 检查任务是否全都完成
     *
     * @return 是否全部完成
     */
    private boolean checkAllTaskRunComplete() {
        for (TaskEntity task : mTaskList) {
            if (!task.executed) {
                return false;
            }
        }
        return true;
    }

    /**
     * 释放资源
     */
    private void releaseResources() {
        if (mHandler != null) {
            mHandler = null;
        }
        if (mApplication != null) {
            mApplication = null;
        }
        if (mTaskRelationalTable != null) {
            mTaskRelationalTable = null;
        }
        mNoDependencyQueue.clear();
    }
}