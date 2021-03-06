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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetProperty;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ChangeVariableMutabilityFix implements IntentionAction {
    private boolean isVar;

    public ChangeVariableMutabilityFix(boolean isVar) {
        this.isVar = isVar;
    }

    public ChangeVariableMutabilityFix() {
        this(false);
    }

    @NotNull
    @Override
    public String getText() {
        return isVar ? JetBundle.message("make.variable.immutable") : JetBundle.message("make.variable.mutable");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.variable.mutability.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof JetFile)) return false;
        JetProperty property = getCorrespondingProperty(editor, (JetFile) file);
        return property != null && !property.isVar();
    }

    private static JetProperty getCorrespondingProperty(Editor editor, JetFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        JetProperty property = PsiTreeUtil.getParentOfType(elementAtCaret, JetProperty.class);
        if (property != null) return property;
        JetSimpleNameExpression simpleNameExpression = PsiTreeUtil.getParentOfType(elementAtCaret, JetSimpleNameExpression.class);
        if (simpleNameExpression != null) {
            BindingContext bindingContext = ResolvePackage.analyzeFully(file);
            VariableDescriptor descriptor = BindingContextUtils.extractVariableDescriptorIfAny(bindingContext, simpleNameExpression, true);
            if (descriptor != null) {
                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
                if (declaration instanceof JetProperty) {
                    return (JetProperty) declaration;
                }
            }
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetProperty property = getCorrespondingProperty(editor, (JetFile)file);
        assert property != null;
        property.getValOrVarNode().getPsi().replace(JetPsiFactory(property).createValOrVarNode(isVar ? "val" : "var").getPsi());
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
