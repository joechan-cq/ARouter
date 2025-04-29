package com.alibaba.android.arouter.register.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Format log
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/12/18 下午2:43
 */
object Logger {
    private var logger: Logger? = null

    fun make(project: Project) {
        logger = project.logger
    }

    fun i(info: String?) {
        if (!info.isNullOrEmpty() && logger != null) {
            logger?.info("ARouter::Register >>> $info")
        }
    }

    fun e(error: String?) {
        if (!error.isNullOrEmpty() && logger != null) {
            logger?.error("ARouter::Register >>> $error")
        }
    }

    fun w(warning: String?) {
        if (!warning.isNullOrEmpty() && logger != null) {
            logger?.warn("ARouter::Register >>> $warning")
        }
    }
}