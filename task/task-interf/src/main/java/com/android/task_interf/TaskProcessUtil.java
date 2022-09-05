package com.android.task_interf;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.android.task_annotation.ITaskConstant;
import com.android.task_annotation.TaskEntity;

import java.lang.reflect.Method;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ClassName: TaskProcessUtil Description: 进程工具类 Author: wudi41 Date: 2022/9/2 14:38
 */
public class TaskProcessUtil {
    private static String currentProcessName;

    /**
     * 检查任务进程是否符合
     * @param context 上下文
     * @param task 任务
     * @return true 符合  false 不符合
     */
    public static boolean checkTaskProcess(@NonNull Context context, TaskEntity task) {
        String[] process = task.process;
        if (process == null || process.length == 0) {
            return true;
        }
        for (String p : process) {
            if (Objects.equals(p, ITaskConstant.PROCESS_MAIN)) {
                return context.getApplicationContext().getPackageName().equals
                        (getCurrentProcessName(context));
            } else {
                return Objects.equals(getCurrentProcessName(context), p);
            }
        }
        return false;
    }

    /**
     * @return 当前进程名
     */
    @Nullable
    private static String getCurrentProcessName(@NonNull Context context) {
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //1)通过Application的API获取当前进程名
        currentProcessName = getCurrentProcessNameByApplication();
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //2)通过反射ActivityThread获取当前进程名
        currentProcessName = getCurrentProcessNameByActivityThread();
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }
        return currentProcessName;
    }


    /**
     * 通过Application新的API获取进程名，无需反射，无需IPC，效率最高。
     */
    private static String getCurrentProcessNameByApplication() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        return null;
    }

    /**
     * 通过反射ActivityThread获取进程名，避免了ipc
     */
    private static String getCurrentProcessNameByActivityThread() {
        String processName = null;
        try {
            final Method declaredMethod = Class.forName("android.app.ActivityThread", false,
                            Application.class.getClassLoader())
                    .getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
            declaredMethod.setAccessible(true);
            final Object invoke = declaredMethod.invoke(null, new Object[0]);
            if (invoke instanceof String) {
                processName = (String) invoke;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return processName;
    }
}