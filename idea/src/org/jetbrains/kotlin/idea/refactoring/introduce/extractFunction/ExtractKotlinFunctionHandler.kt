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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction

import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiFile
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui.KotlinExtractFunctionDialog
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetBlockExpression
import kotlin.test.fail
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.*

public class ExtractKotlinFunctionHandler(
        public val allContainersEnabled: Boolean = false,
        private val helper: ExtractionEngineHelper = ExtractKotlinFunctionHandler.InteractiveExtractionHelper) : RefactoringActionHandler {

    object InteractiveExtractionHelper : ExtractionEngineHelper() {
        override fun configureInteractively(
                project: Project,
                editor: Editor,
                descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
                continuation: (ExtractionGeneratorConfiguration) -> Unit
        ) {
            KotlinExtractFunctionDialog(descriptorWithConflicts.descriptor.extractionData.project, descriptorWithConflicts) {
                continuation(it.getCurrentConfiguration())
            }.show()
        }
    }

    fun doInvoke(
            editor: Editor,
            file: JetFile,
            elements: List<PsiElement>,
            targetSibling: PsiElement
    ) {
        fun adjustElements(elements: List<PsiElement>): List<PsiElement> {
            if (elements.size() != 1) return elements

            val e = elements.first()
            if (e is JetBlockExpression && e.getParent() is JetFunctionLiteral) return e.getStatements()

            return elements
        }

        val extractionData = ExtractionData(file, adjustElements(elements).toRange(false), targetSibling)
        ExtractionEngine(EXTRACT_FUNCTION, helper).run(editor, extractionData) {
            processDuplicates(it.duplicateReplacers, file.getProject(), editor)
        }
    }

    fun selectElements(editor: Editor, file: PsiFile, continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit) {
        selectElementsWithTargetSibling(
                EXTRACT_FUNCTION,
                editor,
                file,
                { elements, parent -> parent.getExtractionContainers(elements.size() == 1, allContainersEnabled) },
                continuation
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is JetFile) return
        selectElements(editor, file) { elements, targetSibling -> doInvoke(editor, file, elements, targetSibling) }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        fail("Extract Function can only be invoked from editor")
    }
}

private val EXTRACT_FUNCTION: String = JetRefactoringBundle.message("extract.function")
