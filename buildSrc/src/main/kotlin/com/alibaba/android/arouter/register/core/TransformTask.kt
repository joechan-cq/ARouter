package com.alibaba.android.arouter.register.core

import RegisterCodeGenerator
import com.alibaba.android.arouter.register.utils.ScanSetting
import com.alibaba.android.arouter.register.utils.ScanUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

abstract class TransformTask : DefaultTask() {

    companion object {
        var registerList: ArrayList<ScanSetting> = arrayListOf()
        var fileContainsInitClass: File? = null
    }

    // This property will be set to all Jar files available in scope
    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    // Gradle will set this property with all class directories that available in scope
    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    // Task will put all classes from directories and jars after optional modification into single jar
    @get:OutputFile
    abstract val output: RegularFileProperty

    @Internal
    val jarPaths = mutableSetOf<String>()

    @TaskAction
    fun taskAction(inputChanges: InputChanges) {

        val jarOutput = JarOutputStream(
            BufferedOutputStream(
                FileOutputStream(
                    output.get().asFile
                )
            )
        )

        val leftSlash = File.separator == "/"
        allDirectories.get().forEach { directory ->
            directory.asFile.walk().forEach { file ->
                if (file.isFile) {
                    val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
//                    println("***********handling $relativePath")
                    var tPath = relativePath
                    if (!leftSlash) {
                        tPath = tPath.replace("\\", "/")
                    }
                    if (ScanUtil.shouldProcessClass(tPath)) {
                        ScanUtil.scanClass(file)
                    }
                    jarOutput.writeEntity(
                        relativePath.replace(File.separatorChar, '/'),
                        file.inputStream()
                    )
                }
            }
        }

        allJars.get().forEach { file ->
            var isInitClassJar = false
            val src = file.asFile
            if (ScanUtil.shouldProcessPreDexJar(src.absolutePath)) {
                isInitClassJar = ScanUtil.scanJar(src, src)
            }

            if (!isInitClassJar) {
//                println("**********handling copy:" + src.absolutePath)
                val jarFile = JarFile(src)
                jarFile.entries().iterator().forEach { jarEntry ->
                    jarOutput.writeEntity(jarEntry.name, jarFile.getInputStream(jarEntry))
                }
                jarFile.close()
            } else {
                println("**********handling find InitClass Jar:" + src.absolutePath)
            }
        }

        fileContainsInitClass?.let { initClass ->
            registerList.forEach { ext ->
                println("Insert register code to file ${initClass.absolutePath}")

                if (ext.classList.isEmpty()) {
                    println("No class implements found for interface:${ext.interfaceName}")
                } else {
                    RegisterCodeGenerator.insertInitCodeTo(ext)
                }
            }

            val jarFile = JarFile(initClass)
            jarFile.entries().iterator().forEach { jarEntry ->
                jarOutput.writeEntity(jarEntry.name, jarFile.getInputStream(jarEntry))
            }
            jarFile.close()
        }

        jarOutput.close()
    }

    // writeEntity methods check if the file has name that already exists in output jar
    private fun JarOutputStream.writeEntity(name: String, inputStream: InputStream) {
        // check for duplication name first
        if (jarPaths.contains(name)) {
            printDuplicatedMessage(name)
        } else {
            putNextEntry(JarEntry(name))
            inputStream.copyTo(this)
            closeEntry()
            jarPaths.add(name)
        }
    }

    private fun JarOutputStream.writeEntity(relativePath: String, byteArray: ByteArray) {
        // check for duplication name first
        if (jarPaths.contains(relativePath)) {
            printDuplicatedMessage(relativePath)
        } else {
            putNextEntry(JarEntry(relativePath))
            write(byteArray)
            closeEntry()
            jarPaths.add(relativePath)
        }
    }

    private fun printDuplicatedMessage(name: String) =
        println("Cannot add ${name}, because output Jar already has file with the same name.")
}