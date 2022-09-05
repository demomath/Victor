package com.android.inject

import org.gradle.api.Project

/**
 * ClassName: InjectPluginConfig
 * Description: AOP的配置信息
 * Author: wudi41
 * Date: 2022/8/31 15:56
 */
class InjectPluginConfig {

    public ArrayList<Map<String, Object>> registerInfo = []

    ArrayList<InjectPluginInfo> list = new ArrayList<>()

    Project project
    def cacheEnabled = true

    InjectPluginConfig() {}

    void convertConfig() {
        registerInfo.each { map ->
            InjectPluginInfo info = new InjectPluginInfo()
            info.interfaceName = map.get('scanInterface')
            def superClasses = map.get('scanSuperClasses')
            if (!superClasses) {
                superClasses = new ArrayList<String>()
            } else if (superClasses instanceof String) {
                ArrayList<String> superList = new ArrayList<>()
                superList.add(superClasses)
                superClasses = superList
            }
            info.superClassNames = superClasses
            info.initClassName = map.get('codeInsertToClassName') //代码注入的类
            info.initMethodName = map.get('codeInsertToMethodName') //代码注入的方法（默认为static块）
            info.registerMethodName = map.get('registerMethodName') //生成的代码所调用的方法
            info.registerClassName = map.get('registerClassName') //注册方法所在的类
            info.include = map.get('include')
            info.exclude = map.get('exclude')
            info.init()
            if (info.validate())
                list.add(info)
            else {
                project.logger.error('inject-plugin config error: scanInterface, codeInsertToClassName and registerMethodName should not be null\n' + info.toString())
            }

        }

        if (cacheEnabled) {
            checkRegisterInfo()
        } else {
            deleteFile(InjectPluginHelper.getRegisterInfoCacheFile(project))
            deleteFile(InjectPluginHelper.getRegisterCacheFile(project))
        }
    }

    private void checkRegisterInfo() {
        def registerInfo = InjectPluginHelper.getRegisterInfoCacheFile(project)
        def listInfo = list.toString()
        def sameInfo = false

        if (!registerInfo.exists()) {
            registerInfo.createNewFile()
        } else if(registerInfo.canRead()) {
            def info = registerInfo.text
            sameInfo = info == listInfo
            if (!sameInfo) {
                project.logger.error("inject-plugin registerInfo has been changed since project(':$project.name') last build")
            }
        } else {
            project.logger.error('inject-plugin read registerInfo error--------')
        }
        if (!sameInfo) {
            deleteFile(InjectPluginHelper.getRegisterCacheFile(project))
        }
        if (registerInfo.canWrite()) {
            registerInfo.write(listInfo)
        } else {
            project.logger.error('inject-plugin write registerInfo error--------')
        }
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            //registerInfo 配置有改动就删除緩存文件
            file.delete()
        }
    }

    void reset() {
        list.each { info ->
            info.reset()
        }
    }

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder(InjectPlugin.EXT_NAME).append(' = {')
                .append('\n  cacheEnabled = ').append(cacheEnabled)
                .append('\n  registerInfo = [\n')
        def size = list.size()
        for (int i = 0; i < size; i++) {
            sb.append('\t' + list.get(i).toString().replaceAll('\n', '\n\t'))
            if (i < size - 1)
                sb.append(',\n')
        }
        sb.append('\n  ]\n}')
        return sb.toString()
    }
}