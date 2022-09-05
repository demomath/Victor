package com.android.inject

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ClassName: InjectPlugin
 * Description: 注入代码插件
 * Author: wudi41
 * Date: 2022/8/31 15:56
 */
public class InjectPlugin implements Plugin<Project> {
    public static final String EXT_NAME = 'inject_plugin'

    @Override
    public void apply(Project project) {
        // 注册transform接口
        def isApp = project.plugins.hasPlugin(AppPlugin)
        project.extensions.create(EXT_NAME, InjectPluginConfig)
        if (isApp) {
            println 'project(' + project.name + ') apply inject-plugin plugin'
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new InjectPluginTransform(project)
            android.registerTransform(transformImpl)
            project.afterEvaluate {
                init(project, transformImpl) // 此处要先于transformImpl.transform方法执行
            }
        }
    }

    static void init(Project project, InjectPluginTransform transformImpl) {
        InjectPluginConfig config = project.extensions.findByName(EXT_NAME) as InjectPluginConfig
        config.project = project
        config.convertConfig()
        transformImpl.config = config
    }

}
