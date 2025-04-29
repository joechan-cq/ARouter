import com.alibaba.android.arouter.register.core.TransformTask
import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * generate register code into LogisticsCenter.class
 * @author billy.qi email: qiyilike@163.com
 */
class RegisterCodeGenerator(private val extension: ScanSetting) {

    companion object {
        fun insertInitCodeTo(registerSetting: ScanSetting?) {
            if (registerSetting != null && registerSetting.classList.isNotEmpty()) {
                val processor = RegisterCodeGenerator(registerSetting)
                val file = TransformTask.fileContainsInitClass
                if (file?.name?.endsWith(".jar") == true) {
                    processor.insertInitCodeIntoJarFile(file)
                }
            }
        }
    }

    /**
     * generate code into jar file
     * @param jarFile the jar file which contains LogisticsCenter.class
     * @return
     */
    private fun insertInitCodeIntoJarFile(jarFile: File?): File? {
        jarFile?.let { file ->
            val optJar = File(file.parent, "${file.name}.opt")
            if (optJar.exists()) {
                optJar.delete()
            }

            JarFile(file).use { jarFileObj ->
                JarOutputStream(FileOutputStream(optJar)).use { jarOutputStream ->
                    val enumeration = jarFileObj.entries()
                    while (enumeration.hasMoreElements()) {
                        val jarEntry = enumeration.nextElement()
                        val entryName = jarEntry.name
                        val zipEntry = ZipEntry(entryName)
                        jarFileObj.getInputStream(jarEntry).use { inputStream ->
                            jarOutputStream.putNextEntry(zipEntry)
                            if (ScanSetting.GENERATE_TO_CLASS_FILE_NAME == entryName) {
                                Logger.i("Insert init code to class >> $entryName")
                                val bytes = referHackWhenInit(inputStream)
                                jarOutputStream.write(bytes)
                            } else {
                                jarOutputStream.write(IOUtils.toByteArray(inputStream))
                            }
                            jarOutputStream.closeEntry()
                        }
                    }
                }
            }

            if (file.exists()) {
                file.delete()
            }
            optJar.renameTo(file)
            return file
        }
        return null
    }

    //refer hack class when object init
    private fun referHackWhenInit(inputStream: InputStream): ByteArray {
        val cr = ClassReader(inputStream)
        val cw = ClassWriter(cr, 0)
        val cv = MyClassVisitor(Opcodes.ASM7, cw)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    private inner class MyClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, desc, signature, exceptions)
            //generate code into this method
            return if (name == ScanSetting.GENERATE_TO_METHOD_NAME) {
                RouteMethodVisitor(Opcodes.ASM5, mv)
            } else {
                mv
            }
        }
    }

    private inner class RouteMethodVisitor(api: Int, mv: MethodVisitor?) : MethodVisitor(api, mv) {

        override fun visitInsn(opcode: Int) {
            //generate code before return
            if (opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                extension.classList.forEach { name ->
                    val className = name.replace("/", ".")
                    mv.visitLdcInsn(className) // 类名
                    // generate invoke register method into LogisticsCenter.loadRouterMap()
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ScanSetting.GENERATE_TO_CLASS_NAME,
                        ScanSetting.REGISTER_METHOD_NAME,
                        "(Ljava/lang/String;)V",
                        false
                    )
                }
            }
            super.visitInsn(opcode)
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}