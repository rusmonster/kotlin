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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.editor.JetEditorOptions
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.IdeaReferenceSearcher
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.awt.datatransfer.Transferable
import java.util.ArrayList

public class ConvertJavaCopyPastePostProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.conversion.copy.ConvertJavaCopyPastePostProcessor")

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(CopiedCode.DATA_FLAVOR)) {
                return listOf(content.getTransferData(CopiedCode.DATA_FLAVOR) as TextBlockTransferableData)
            }
        }
        catch (e: Throwable) {
            LOG.error(e)
        }
        return listOf()
    }

    public override fun collectTransferableData(file: PsiFile, editor: Editor, startOffsets: IntArray, endOffsets: IntArray): List<TextBlockTransferableData> {
        if (file !is PsiJavaFile) return listOf()

        return listOf(CopiedCode(file.getName(), file.getText()!!, startOffsets, endOffsets))
    }

    public override fun processTransferableData(project: Project, editor: Editor, bounds: RangeMarker, caretOffset: Int, indented: Ref<Boolean>, values: List<TextBlockTransferableData>) {
        if (DumbService.getInstance(project).isDumb()) return

        val data = values.single()

        if (data !is CopiedCode) return

        val sourceFile = PsiFileFactory.getInstance(project).
                createFileFromText(data.fileName, JavaLanguage.INSTANCE, data.fileText) as? PsiJavaFile ?: return

        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as? JetFile ?: return

        val jetEditorOptions = JetEditorOptions.getInstance()!!
        val needConvert = jetEditorOptions.isEnableJavaToKotlinConversion() && (jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project))
        if (needConvert) {
            val text = convertCopiedCodeToKotlin(data, sourceFile, data.fileText, targetFile)
            if (!text.isEmpty()) {
                ApplicationManager.getApplication()!!.runWriteAction {
                    val startOffset = bounds.getStartOffset()
                    editor.getDocument().replaceString(startOffset, bounds.getEndOffset(), text)

                    val endOffsetAfterCopy = startOffset + text.length()
                    editor.getCaretModel().moveToOffset(endOffsetAfterCopy)

                    CodeStyleManager.getInstance(project)!!.reformatText(targetFile, startOffset, endOffsetAfterCopy)
                }
            }
        }
    }

    private fun convertCopiedCodeToKotlin(code: CopiedCode, sourceFile: PsiJavaFile, sourceFileText: String, targetFile: JetFile): String {
        assert(code.startOffsets.size() == code.endOffsets.size(), "Must have the same size")

        val list = ArrayList<Any>()
        for (i in code.startOffsets.indices) {
            list.collectElementsToConvert(sourceFile, sourceFileText, TextRange(code.startOffsets[i], code.endOffsets[i]))
        }

        val converter = JavaToKotlinConverter(
                sourceFile.getProject(),
                ConverterSettings.defaultSettings,
                IdeaReferenceSearcher,
                IdeaResolverForConverter
        )

        val results = converter.elementsToKotlin(list.filterIsInstance<PsiElement>().map { it to J2kPostProcessor(targetFile, formatCode = false) })

        var resultIndex = 0
        val text = StringBuilder {
            for (o in list) {
                if (o is PsiElement) {
                    val result = results[resultIndex++]
                    if (!result.isEmpty()) {
                        append(result)
                    }
                    else { // failed to convert element to Kotlin, insert "as is"
                        append(o.getText())
                    }
                }
                else {
                    append(o as String)
                }
            }
        }.toString()

        return StringUtil.convertLineSeparators(text)
    }

    // builds list consisting of PsiElement's to convert and plain String's
    private fun MutableList<Any>.collectElementsToConvert(
            file: PsiJavaFile,
            fileText: String,
            range: TextRange
    ) {
        var currentRange = range
        while (!currentRange.isEmpty()) {
            val leaf = findFirstLeafWhollyInRange(file, currentRange)
            if (leaf == null) {
                val unconvertedSuffix = fileText.substring(currentRange.start, currentRange.end)
                add(unconvertedSuffix)
                break
            }

            val elementToConvert = leaf
                    .parents(withItself = true)
                    .first {
                        val parent = it.getParent()
                        parent == null || parent.range !in currentRange
                    }
            val elementToConvertRange = elementToConvert.range

            val unconvertedPrefix = fileText.substring(currentRange.start, elementToConvertRange.start)
            add(unconvertedPrefix)

            add(elementToConvert)

            currentRange = TextRange(elementToConvertRange.end, currentRange.end)
        }
    }

    private fun findFirstLeafWhollyInRange(file: PsiJavaFile, range: TextRange): PsiElement? {
        var element = file.findElementAt(range.start) ?: return null
        var elementRange = element.range
        if (elementRange.start < range.start) {
            element = file.findElementAt(elementRange.end) ?: return null
            elementRange = element.range
        }
        assert(elementRange.start >= range.start)
        return if (elementRange.end <= range.end) element else null
    }

    private fun okFromDialog(project: Project): Boolean {
        val dialog = KotlinPasteFromJavaDialog(project)
        dialog.show()
        return dialog.isOK()
    }
}
