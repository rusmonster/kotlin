/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.android

import org.jetbrains.jet.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.JetTestUtils
import java.util.Collections
import com.intellij.util.ArrayUtil
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.util.ArrayList
import com.intellij.util.Processor
import org.jetbrains.jet.codegen.CodegenTestFiles
import java.util.regex.Pattern
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.kotlin.android.AndroidConfigurationKeys
import org.jetbrains.jet.JetTestCaseBuilder
import com.intellij.openapi.application.PathManager
import org.jetbrains.jet.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.android.AndroidExpressionCodegen
import org.jetbrains.jet.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.jet.cli.jvm.compiler.EnvironmentConfigFiles

public abstract class AbstractAndroidBoxTest : AbstractBlackBoxCodegenTest() {

    private fun createAndroidAPIEnvironment(path: String) {
        return createEnvironmentForConfiguration(JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.ANDROID_API), path)
    }

    private fun createFakeAndroidEnvironment(path: String) {
        return createEnvironmentForConfiguration(JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK), path)
    }

    private fun createEnvironmentForConfiguration(configuration: CompilerConfiguration, path: String) {
        val resPath = path + "layout/"
        val manifestPath = path + "AndroidManifest.xml"
        configuration.put(AndroidConfigurationKeys.ANDROID_RES_PATH, resPath)
        configuration.put(AndroidConfigurationKeys.ANDROID_MANIFEST, manifestPath)
        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = myEnvironment.getProject()
        ExternalDeclarationsProvider.registerExtension(project, AndroidTestDeclarationsProvider(project, resPath, manifestPath))
        ExpressionCodegenExtension.registerExtension(project, AndroidExpressionCodegen())
    }

    public fun doCompileAgainstAndroidSdkTest(path: String) {
        createAndroidAPIEnvironment(path)
        doMultiFileTest(path)
    }

    public fun doFakeInvocationTest(path: String) {
        if (needsInvocationTest(path)) {
            createFakeAndroidEnvironment(path)
            doMultiFileTest(path, getFakeFiles(path))
        }
    }

    private fun getFakeFiles(path: String): Collection<String> {
        return FileUtil.findFilesByMask(Pattern.compile("^Fake.*\\.kt$"), File(path.replace(getTestName(true), ""))) map { relativePath(it) }
    }

    private fun needsInvocationTest(path: String): Boolean {
        return !FileUtil.findFilesByMask(Pattern.compile("^0.kt$"), File(path)).empty
    }

    override fun relativePath(file: File): String {
        val stringToCut = "plugins/android-compiler-plugin/testData/codegen/"
        val systemIndependentPath = file.getPath().replace(File.separatorChar, '/')
        assert(systemIndependentPath.startsWith(stringToCut), "File path is not absolute: $file ")
        return systemIndependentPath.substring(stringToCut.length())
    }

    private fun doMultiFileTest(path: String, additionalFiles: Collection<String>? = null) {
        val files = ArrayList<String>(2)
        FileUtil.processFilesRecursively(File(path), object : Processor<File> {
            override fun process(file: File?): Boolean {
                when (file!!.getName()) {
                    "1.kt" -> { if (additionalFiles == null) files.add(relativePath(file)) }
                    "0.kt" -> { if (additionalFiles != null) files.add(relativePath(file)) }
                    else   -> { if (file.getName().endsWith(".kt")) files.add(relativePath(file)) }
                }
                return true
            }
        })
        Collections.sort(files);
        if (additionalFiles != null) {
            files.addAll(additionalFiles)
        }
        myFiles = CodegenTestFiles.create(
                myEnvironment!!.getProject(),
                ArrayUtil.toStringArray(files),
                JetTestCaseBuilder.getHomeDirectory() + "/plugins/android-compiler-plugin/testData"
        )
        blackBox();
    }
}