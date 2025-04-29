package com.alibaba.android.arouter.register.utils

/**
 * Register setting
 * @author billy.qi email: qiyilike@163.com
 * @since 17/3/28 11:48
 */
class ScanSetting(interfaceName: String) {
    companion object {
        const val PLUGIN_NAME = "com.alibaba.arouter"
        const val GENERATE_TO_CLASS_NAME = "com/alibaba/android/arouter/core/LogisticsCenter"
        const val GENERATE_TO_CLASS_FILE_NAME = "$GENERATE_TO_CLASS_NAME.class"
        const val GENERATE_TO_METHOD_NAME = "loadRouterMap"
        const val ROUTER_CLASS_PACKAGE_NAME = "com/alibaba/android/arouter/routes/"
        const val REGISTER_METHOD_NAME = "register"
        private const val INTERFACE_PACKAGE_NAME = "com/alibaba/android/arouter/facade/template/"
    }

    /**
     * Scan for classes which implements this interface
     */
    var interfaceName: String = INTERFACE_PACKAGE_NAME + interfaceName

    /**
     * Scan result for [interfaceName]
     * Class names in this list
     */
    val classList: MutableList<String> = ArrayList()

    /**
     * Constructor for arouter-auto-register settings
     * @param interfaceName Interface to scan
     */
    init {
        this.interfaceName = INTERFACE_PACKAGE_NAME + interfaceName
    }
}