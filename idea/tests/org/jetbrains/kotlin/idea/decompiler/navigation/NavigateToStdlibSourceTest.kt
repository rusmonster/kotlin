/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.ConfigureTestUtils
import org.jetbrains.kotlin.idea.ConfigureTestUtils.ModuleKind
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.KotlinCodeInsightTestCase

public class NavigateToStdlibSourceTest : KotlinCodeInsightTestCase() {

    private val FILE_TEXT = "fun foo() { <caret>println() )"

    public fun testRefToPrintlnWithJVM() {
        val navigationElement = configureAndResolve(FILE_TEXT, ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES)
        TestCase.assertEquals("Console.kt", navigationElement.getContainingFile().getName())
    }

    public fun testRefToPrintlnWithJVMAndJSModule() {
        val navigationElement = configureAndResolve(FILE_TEXT, ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES, ModuleKind.KOTLIN_JAVASCRIPT)
        TestCase.assertEquals("Console.kt", navigationElement.getContainingFile().getName())
    }

    public fun testRefToPrintlnWithJS() {
        val navigationElement = configureAndResolve(FILE_TEXT, ModuleKind.KOTLIN_JAVASCRIPT)
        TestCase.assertEquals("core.kt", navigationElement.getContainingFile().getName())
    }

    public fun testRefToPrintlnWithJSAndJVM() {
        val navigationElement = configureAndResolve(FILE_TEXT, ModuleKind.KOTLIN_JAVASCRIPT, ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES)
        TestCase.assertEquals("core.kt", navigationElement.getContainingFile().getName())
    }

    override fun tearDown() {
        // Copied verbatim from NavigateToStdlibSourceRegressionTest.
        // Workaround for IDEA's bug during tests.
        // After tests IDEA disposes VirtualFiles within LocalFileSystem, but doesn't rebuild indices.
        // This causes library source files to be impossible to find via indices
        super.tearDown()
        ApplicationManager.getApplication().runWriteAction(object : Runnable {
            override fun run() {
                LightPlatformTestCase.closeAndDeleteProject()
            }
        })
    }

    protected fun configureAndResolve(
            text: String,
            mainModuleKind: ModuleKind,
            additionalModuleKind: ModuleKind? = null
    ): PsiElement {
        configureByText(JetFileType.INSTANCE, text)
        ConfigureTestUtils.configureModule(getModule(), mainModuleKind)

        if (additionalModuleKind != null) {
            val additionalModule = this.createModule("additional-module")
            ConfigureTestUtils.configureModule(additionalModule, additionalModuleKind)
        }

        val ref = getFile().findReferenceAt(getEditor().getCaretModel().getOffset())
        return ref!!.resolve()!!.getNavigationElement()
    }
}