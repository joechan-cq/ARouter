package com.alibaba.android.arouter.register.launch

import com.alibaba.android.arouter.register.core.TransformTask
import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Simple version of AutoRegister plugin for ARouter
 * @author billy.qi email: qiyilike@163.com
 * @since 17/12/06 15:35
 */
class PluginLaunch : Plugin<Project> {
    override fun apply(project: Project) {
        val isApp = project.plugins.hasPlugin(AppPlugin::class.java)
        // only application module needs this plugin to generate register code
        if (isApp) {
            Logger.make(project)
            val list = ArrayList<ScanSetting>(3)
            list.add(ScanSetting("IRouteRoot"))
            list.add(ScanSetting("IInterceptorGroup"))
            list.add(ScanSetting("IProviderGroup"))
            TransformTask.registerList = list

            project.plugins.withType(AppPlugin::class.java) {
                Logger.i("Project enable arouter-register plugin")
                val androidComponents = project.extensions.getByType(
                    ApplicationAndroidComponentsExtension::class.java
                )
                androidComponents.onVariants { variant: Variant ->
                    val taskProvider = project.tasks.register(
                        "${variant.name}TransformTask",
                        TransformTask::class.java
                    )

                    variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(taskProvider)
                        .toTransform(
                            ScopedArtifact.CLASSES,
                            TransformTask::allJars,
                            TransformTask::allDirectories,
                            TransformTask::output
                        )
                }
            }
        }
    }
}