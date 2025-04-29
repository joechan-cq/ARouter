package com.alibaba.android.arouter.register.utils

import com.alibaba.android.arouter.register.core.TransformTask
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.jar.JarFile

/**
 * Scan all class in the package: com/alibaba/android/arouter/
 * find out all routers, interceptors and providers
 * @author billy.qi email: qiyilike@163.com
 * @since 17/3/20 11:48
 */
object ScanUtil {

    /**
     * scan jar file
     * @param jarFile All jar files that are compiled into apk
     * @param destFile dest file after this transform
     */
    fun scanJar(jarFile: File?, destFile: File) : Boolean {
        var result = false
        jarFile?.let {
            JarFile(it).use { file ->
                val enumeration = file.entries()
                while (enumeration.hasMoreElements()) {
                    val jarEntry = enumeration.nextElement()
                    if (jarEntry.isDirectory) {
                        continue
                    }
                    val entryName = jarEntry.name
                    when {
                        entryName.startsWith(ScanSetting.ROUTER_CLASS_PACKAGE_NAME) -> {
                            file.getInputStream(jarEntry).use { inputStream ->
                                scanClass(inputStream)
                            }
                        }
                        ScanSetting.GENERATE_TO_CLASS_FILE_NAME == entryName -> {
                            // mark this jar file contains LogisticsCenter.class
                            // After the scan is complete, we will generate register code into this file
                            TransformTask.fileContainsInitClass = destFile
                            result = true
                        }
                    }
                }
            }
        }
        return result
    }

    fun shouldProcessPreDexJar(path: String): Boolean {
        return !path.contains("com.android.support") &&
                !path.contains("/android/m2repository") &&
                !path.contains("androidx.")
    }

    fun shouldProcessClass(entryName: String?): Boolean {
        return entryName != null && entryName.startsWith(ScanSetting.ROUTER_CLASS_PACKAGE_NAME)
    }

    /**
     * scan class file
     * @param file class file
     */
    fun scanClass(file: File) {
        FileInputStream(file).use { inputStream ->
            scanClass(inputStream)
        }
    }

    fun scanClass(inputStream: InputStream) {
        val cr = ClassReader(inputStream)
        val cw = ClassWriter(cr, 0)
        val cv = ScanClassVisitor(Opcodes.ASM7, cw)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
    }

    class ScanClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)

            interfaces?.forEach { itName ->
                TransformTask.registerList.forEach { ext ->
                    if (ext.interfaceName != null && itName == ext.interfaceName) {
                        // fix repeated inject init code when Multi-channel packaging
                        if (!ext.classList.contains(name)) {
                            ext.classList.add(name!!)
                        }
                    }
                }
            }
        }
    }
}