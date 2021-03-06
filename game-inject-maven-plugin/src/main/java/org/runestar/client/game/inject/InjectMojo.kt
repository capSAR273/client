package org.runestar.client.game.inject

import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.bytecode.Multiplication
import net.bytebuddy.implementation.bytecode.StackManipulation
import net.bytebuddy.implementation.bytecode.assign.Assigner
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant
import net.bytebuddy.implementation.bytecode.constant.LongConstant
import net.bytebuddy.implementation.bytecode.member.FieldAccess
import net.bytebuddy.implementation.bytecode.member.MethodReturn
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess
import net.bytebuddy.pool.TypePool
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.runestar.client.game.raw.access.XClient
import org.runestar.client.updater.GAMEPACK_CLEAN
import org.runestar.client.updater.HOOKS
import org.runestar.client.updater.common.FieldHook
import org.runestar.client.updater.common.MethodHook
import org.runestar.client.updater.common.decoderNarrowed
import org.runestar.client.updater.common.finalArgumentNarrowed
import org.zeroturnaround.zip.ZipUtil
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Mojo(name = "inject")
class InjectMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project}")
    private lateinit var project: MavenProject

    private val cleanJar by lazy { Paths.get(project.build.directory, "gamepack.clean.jar") }

    private val classesDir by lazy { Paths.get(project.build.directory, "classes") }

    override fun execute() {
        Files.createDirectories(cleanJar.parent)
        GAMEPACK_CLEAN.openStream().use { input ->
            Files.copy(input, cleanJar, StandardCopyOption.REPLACE_EXISTING)
        }
        inject(cleanJar, classesDir)
    }

    private fun inject(sourceJar: Path, destinationFolder: Path) {
        val classFileLocator = ClassFileLocator.Compound(
                ClassFileLocator.ForClassLoader.ofClassPath(),
                ClassFileLocator.ForJarFile.of(sourceJar.toFile())
        )
        val typePool = TypePool.Default.of(classFileLocator)
        ZipUtil.unpack(sourceJar.toFile(), destinationFolder.toFile())
        hookClassNames().forEach { cn ->
            val typeDescription = typePool.describe(cn).resolve()
            var typeBuilder = BYTEBUDDY.rebase<Any>(typeDescription, classFileLocator, METHOD_NAME_TRANSFORMER)
            HOOKS.forEach { ch ->
                val xClass = Class.forName("$ACCESS_PKG.X${ch.`class`}")
                if (cn == ch.name) {
                    typeBuilder = typeBuilder.implement(xClass)
                    log.info("Injected interface: ${xClass.simpleName} -> ${ch.name}")
                    ch.fields.forEach { f ->
                        typeBuilder = typeBuilder.method(f.getterMatcher())
                                .intercept(createFieldGetter(typePool, f))
                        log.info("Injected getter: ${f.getterName} -> ${ch.name} (${xClass.simpleName})")
                        if (!Modifier.isFinal(f.access)) {
                            typeBuilder = typeBuilder.method(f.setterMatcher())
                                    .intercept(createFieldSetter(typePool, f))
                            log.info("Injected setter: ${f.setterName} -> ${ch.name} (${xClass.simpleName})" )
                        }
                    }
                    ch.methods.forEach { m ->
                        if (m.parameters != null && !Modifier.isAbstract(m.access)) {
                            typeBuilder = typeBuilder.method(m.proxyMatcher())
                                    .intercept(createMethodProxy(typePool, m))
                            log.info("Injected method: ${m.method} -> ${ch.name} (${xClass.simpleName})")
                        }
                    }
                }
                ch.methods.forEach { m ->
                    if (m.parameters != null && m.owner == cn && !Modifier.isAbstract(m.access)) {
                        typeBuilder = typeBuilder.method(m.matcher())
                                .intercept(
                                        Advice.withCustomMapping()
                                                .bind(MethodAdvice.Execution::class.java, xClass.getDeclaredField(m.method))
                                                .to(MethodAdvice::class.java)
                                )
                        log.info("Injected callbacks -> ${m.owner}.${m.name} (X${ch.`class`}.${m.method})")
                    }
                }
            }
            typeBuilder.make().saveIn(destinationFolder.toFile())
        }
    }

    private companion object {

        val ACCESS_PKG = XClient::class.java.`package`.name

        val BYTEBUDDY = ByteBuddy().with(TypeValidation.DISABLED)

        val METHOD_NAME_TRANSFORMER = MethodNameTransformer.Suffixing("0")

        fun createMethodProxy(typePool: TypePool, methodHook: MethodHook): Implementation {
            val sourceMethodDescription = typePool.describe(methodHook.owner).resolve().declaredMethods.filter(methodHook.matcher()).only
            var methodCall = MethodCall.invoke(sourceMethodDescription).withAllArguments()
            if (methodHook.argumentsCount == methodHook.actualArgumentsCount + 1) {
                methodCall = methodCall.with(methodHook.finalArgumentNarrowed ?: 0.toByte())
            } else {
                check(methodHook.argumentsCount == methodHook.actualArgumentsCount)
            }
            return methodCall.withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
        }

        fun createFieldGetter(typePool: TypePool, fieldHook: FieldHook): Implementation {
            val fieldOwnerDescription = typePool.describe(fieldHook.owner).resolve()
            val fieldDescription = fieldOwnerDescription.declaredFields.filter(fieldHook.matcher()).only
            val fieldAccess = FieldAccess.forField(fieldDescription)
            val decoder = fieldHook.decoderNarrowed
            return when(decoder) {
                is Int -> {
                    Implementation.Simple(
                            if (fieldDescription.isStatic) StackManipulation.Trivial.INSTANCE else MethodVariableAccess.loadThis(),
                            fieldAccess.read(),
                            IntegerConstant.forValue(decoder),
                            Multiplication.INTEGER,
                            MethodReturn.INTEGER
                    )
                }
                is Long -> {
                    Implementation.Simple(
                            if (fieldDescription.isStatic) StackManipulation.Trivial.INSTANCE else MethodVariableAccess.loadThis(),
                            fieldAccess.read(),
                            LongConstant.forValue(decoder),
                            Multiplication.LONG,
                            MethodReturn.LONG
                    )
                }
                null -> {
                    FieldAccessor.ofField(fieldHook.name).`in`(fieldOwnerDescription)
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                }
                else -> error(decoder)
            }
        }

        fun createFieldSetter(typePool: TypePool, fieldHook: FieldHook): Implementation {
            val fieldOwnerDescription = typePool.describe(fieldHook.owner).resolve()
            val fieldDescription = fieldOwnerDescription.declaredFields.filter(fieldHook.matcher()).only
            val fieldAccess = FieldAccess.forField(fieldDescription)
            val encoder = invert(fieldHook.decoderNarrowed)
            return when(encoder) {
                is Int -> {
                    Implementation.Simple(
                            if (fieldDescription.isStatic) StackManipulation.Trivial.INSTANCE else MethodVariableAccess.loadThis(),
                            IntegerConstant.forValue(encoder),
                            MethodVariableAccess.INTEGER.loadFrom(1),
                            Multiplication.INTEGER,
                            fieldAccess.write(),
                            MethodReturn.VOID
                    )
                }
                is Long -> {
                    Implementation.Simple(
                            if (fieldDescription.isStatic) StackManipulation.Trivial.INSTANCE else MethodVariableAccess.loadThis(),
                            LongConstant.forValue(encoder),
                            MethodVariableAccess.LONG.loadFrom(1),
                            Multiplication.LONG,
                            fieldAccess.write(),
                            MethodReturn.VOID
                    )
                }
                null -> {
                    FieldAccessor.ofField(fieldHook.name).`in`(fieldOwnerDescription)
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                }
                else -> error(encoder)
            }
        }
    }
}