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

package org.jetbrains.kotlin.idea.kdoc

import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils

public abstract class AbstractKDocTypingTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String = JetTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = JetLightProjectDescriptor.INSTANCE

    protected fun doTest(fileName: String) {
        myFixture.configureByFile(fileName)
        val textToType = InTextDirectivesUtils.findStringWithPrefixes(myFixture.getFile().getText(), "// TYPE:")
        if (textToType == null) {
            throw IllegalArgumentException("Cannot find directive TYPE in input file")
        }
        myFixture.type(textToType)
        myFixture.checkResultByFile(fileName + ".after")
    }
}
