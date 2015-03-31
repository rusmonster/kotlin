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

package org.jetbrains.kotlin.codegen.stackvalue

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class BranchedValue(val arg1: StackValue, val arg2: StackValue? = null, val operandType: Type, val jumpInstr: Int, var genBooleanBranches: Boolean, val operationUpdate: (Int, InstructionAdapter) -> Int = { it, v -> it }) : StackValue(Type.BOOLEAN_TYPE) {

    private var jumpLabel: Label? = null

    constructor(or: BranchedValue, opcode: Int, label: Label?): this(or.arg1, or.arg2, or.operandType, opcode, or.genBooleanBranches, or.operationUpdate) {
        jumpLabel = label
        if (label != null) {
            genBooleanBranches = false
        }
    }

    override fun putSelector(type: Type, v: InstructionAdapter) {
        val branchJumpLabel = getJumpBranchLabel()
        condJump(branchJumpLabel, v)
        if (genBooleanBranches) {
            val endLabel = Label()
            v.iconst(1)
            v.visitJumpInsn(GOTO, endLabel)
            v.visitLabel(branchJumpLabel)
            v.iconst(0)
            v.visitLabel(endLabel)
            coerceTo(type, v);
        }
    }

    open fun condJump(jumpIfFalseLabel: Label, v: InstructionAdapter) {
        arg1.put(operandType, v)
        arg2?.put(operandType, v)
        v.visitJumpInsn(patchOpcode(jumpInstr, v), jumpIfFalseLabel);
    }

    protected open fun genNegate(): BranchedValue {
        return BranchedValue(this, negatedOperations.get(jumpInstr), this.jumpLabel)
    }

    protected open fun patchOpcode(opcode: Int, v: InstructionAdapter): Int {
        return operationUpdate(opcode, v)
    }

    open fun getJumpBranchLabel(): Label = jumpLabel ?: Label()

    companion object {
        val negatedOperations = hashMapOf<Int, Int>()

        val TRUE: BranchedValue = object : BranchedValue(StackValue.Constant(true, Type.BOOLEAN_TYPE), null, Type.BOOLEAN_TYPE, IFEQ, false) {
            override fun genNegate(): BranchedValue {
                return FALSE
            }

            override fun condJump(jumpIfFalseLabel: Label, v: InstructionAdapter) {
                super.condJump(jumpIfFalseLabel, v)
            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(1)
                coerceTo(type, v);
            }
        }
        val FALSE: BranchedValue = object : BranchedValue(StackValue.Constant(false, Type.BOOLEAN_TYPE), null, Type.BOOLEAN_TYPE, IFEQ, false) {
            override fun genNegate(): BranchedValue {
                return TRUE
            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(0)
                coerceTo(type, v);
            }
        }

        init {
            registerOperations(IFNE, IFEQ)
            registerOperations(IFLE, IFGT)
            registerOperations(IFLT, IFGE)
            registerOperations(IFGE, IFLT)
            registerOperations(IFGT, IFLE)
            registerOperations(IF_ACMPNE, IF_ACMPEQ)
        }

        private fun registerOperations(op: Int, negatedOp: Int) {
            negatedOperations.put(op, negatedOp)
            negatedOperations.put(negatedOp, op)
        }

        fun booleanConstant(value: Boolean): BranchedValue {
            return if (value) TRUE else FALSE
        }

        fun createInvertValue(argument: StackValue): StackValue {
            if (argument.type != Type.BOOLEAN_TYPE) {
                throw UnsupportedOperationException("operand of ! must be boolean")
            }

            if (argument is BranchedValue) {
                return argument.genNegate()
            }
            return BranchedValue(argument, null, Type.BOOLEAN_TYPE, IFNE, true)
        }

        fun condJump(condition: StackValue, label: Label, jumpIfFalse: Boolean, iv: InstructionAdapter) {
            val condJump: BranchedValue
            if (condition is BranchedValue) {
                val opcode = if (jumpIfFalse) condition else condition.genNegate()
                if (opcode == TRUE) {
                    return;
                } else if (opcode == FALSE) {
                    iv.goTo(label)
                }
                condJump = opcode;
            } else {
                condJump = BranchedValue(condition, null, Type.BOOLEAN_TYPE, if (!jumpIfFalse) IFNE else IFEQ, false)
            }
            condJump.condJump(label, iv)
        }

        public fun cmp(opToken: IElementType, operandType: Type, left: StackValue, right: StackValue): StackValue {
            val opCode = if (operandType.getSort() == Type.OBJECT) getObjectCompareOpcode(opToken) else getNumberCompareOpcode(opToken)
            return BranchedValue(left, right, operandType, opCode, true) {
                opcode, v ->
                if (operandType.getSort() == Type.OBJECT)
                    opcode
                else {
                    var newOpcode = opcode
                    if (operandType == Type.FLOAT_TYPE || operandType == Type.DOUBLE_TYPE) {
                        if (opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
                            v.cmpl(operandType)
                        }
                        else {
                            v.cmpg(operandType)
                        }
                    }
                    else if (operandType == Type.LONG_TYPE) {
                        v.lcmp()
                    }
                    else {
                        newOpcode += (IF_ICMPEQ - IFEQ)
                    }
                    newOpcode
                }
            }
        }

        protected fun getNumberCompareOpcode(opToken: IElementType): Int {
            val opcode: Int
            if (opToken == JetTokens.EQEQ || opToken == JetTokens.EQEQEQ) {
                opcode = IFNE
            }
            else if (opToken == JetTokens.EXCLEQ || opToken == JetTokens.EXCLEQEQEQ) {
                opcode = IFEQ
            }
            else if (opToken == JetTokens.GT) {
                opcode = IFLE
            }
            else if (opToken == JetTokens.GTEQ) {
                opcode = IFLT
            }
            else if (opToken == JetTokens.LT) {
                opcode = IFGE
            }
            else if (opToken == JetTokens.LTEQ) {
                opcode = IFGT
            }
            else {
                throw UnsupportedOperationException("Don't know how to generate this condJump: " + opToken)
            }

            return opcode
        }

        protected fun getObjectCompareOpcode(opToken: IElementType): Int {
            val opcode: Int
            if (opToken == JetTokens.EQEQEQ) {
                opcode = IF_ACMPNE
            }
            else if (opToken == JetTokens.EXCLEQEQEQ) {
                opcode = IF_ACMPEQ
            }
            else {
                throw UnsupportedOperationException("don't know how to generate this condjump")
            }
            return opcode
        }
    }
}