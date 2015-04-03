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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ExtendedCallable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetPrefixExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type

public class Not : LazyIntrinsicMethod() {
    override fun generateImpl(codegen: ExpressionCodegen, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): StackValue {
        val stackValue: StackValue
        if (arguments.size() == 1) {
            stackValue = codegen.gen(arguments.get(0))
        }
        else {
            stackValue = receiver
        }
        return StackValue.not(StackValue.coercion(stackValue, Type.BOOLEAN_TYPE))
    }

    override fun supportCallable(): Boolean {
        return true
    }

    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): ExtendedCallable {
        val callable = codegen.getState().getTypeMapper().mapToCallableMethod(fd, false, codegen.getContext())
        return object : MappedCallable(callable, {}) {
            override fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, returnType: Type, codegen: ExpressionCodegen): StackValue {
                val element = resolvedCall.getCall().getCallElement()
                val stackValue =
                        if (element is JetPrefixExpression) {
                            codegen.gen(element.getBaseExpression())
                        }
                        else {
                            StackValue.receiver(resolvedCall, receiver, codegen, this)
                        }
                return StackValue.not(StackValue.coercion(stackValue, Type.BOOLEAN_TYPE))
            }
        }
    }
}
