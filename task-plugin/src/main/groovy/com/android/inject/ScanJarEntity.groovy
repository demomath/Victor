package com.android.inject

/**
 * ClassName: ScanJarHarvest
 * Description: 已扫描到接口或者codeInsertToClassName jar的信息
 * Author: wudi41
 * Date: 2022/8/31 15:56
 */
class ScanJarEntity {
    List<Harvest> harvestList = new ArrayList<>()
    class Harvest {
        String className
        String interfaceName
        boolean isInitClass
    }
}