package com.android.task_processor;

import com.android.task_annotation.ITaskRegister;
import com.android.task_annotation.TaskAnnotation;
import com.android.task_annotation.TaskEntity;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Name: TaskAnnotationProcessor Description: 任务注解解析 Author: wudi41 Date: 2022/08/31
 */
@AutoService(Processor.class)
public class TaskAnnotationProcessor extends AbstractProcessor {

    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;
    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;
    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;
    // 文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件
    private Filer filer;
    // 模块名称
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        moduleName = formatModuleName(processingEnv.getOptions().get("moduleName"));
    }

    /**
     * 给出解析的注解范围
     *
     * @return Set<String>
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(TaskAnnotation.class.getCanonicalName());
    }

    /**
     * 解析注解后，并去生成 .java 文件
     *
     * @param annotations 注解
     * @param roundEnv    环境
     *
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(TaskAnnotation.class);
        if (elements == null || elements.size() == 0) {
            return false;
        }

        elementUtils.getTypeElement("com.android.task_interf.IRunTask");

        MethodSpec.Builder builder = MethodSpec.methodBuilder("register")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(ArrayList.class), ClassName.get(TaskEntity.class))
                        , "tasks");
        for (Element element : elements) {
            TaskAnnotation taskAnnotation = element.getAnnotation(TaskAnnotation.class);
            System.out.println("taskAnnotation.name() = " + taskAnnotation.name());
            CodeBlock codeBlock = CodeBlock.builder()
                    .addStatement("tasks.add(new TaskEntity($S, $L, $L, $L, $L ,new $T()));",
                            taskAnnotation.name(), taskAnnotation.background(),
                            taskAnnotation.priority(), strarr2String(taskAnnotation.process()),
                            strarr2String(taskAnnotation.depends()), ClassName.get(element.asType()))
                    .build();
            builder.addCode(codeBlock);
        }
        MethodSpec registerMethod = builder.build();
        TypeSpec registerClass = TypeSpec.classBuilder("TaskRegister$" + moduleName)
                .addSuperinterface(ITaskRegister.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(registerMethod)
                .build();
        JavaFile javaFile = JavaFile.builder("com.android.task_register", registerClass).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private String strarr2String(String[] stringArray) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("new String[]{");
        if (stringArray != null && stringArray.length > 0) {
            for (int i = 0; i < stringArray.length; i++) {
                String s = stringArray[i];
                if (i == 0) {
                    stringBuilder.append("\"").append(s).append("\"");
                } else {
                    stringBuilder.append(", ").append("\"").append(s).append("\"");
                }
            }
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    String formatModuleName(String moduleName) {
        return moduleName.replace('-', '_');
    }
}
