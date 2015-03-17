/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.psi

import com.intellij.core.CoreJavaFileManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.PsiTestCase
import com.intellij.testFramework.PsiTestUtil
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.CoreJavaFileManagerExt
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName


public class CoreJavaFileManagerTest : PsiTestCase() {

    throws(javaClass<Exception>())
    public fun testCommon() {
        val manager = configureManager("package foo;\n\n" + "public class TopLevel {\n" + "public class Inner {\n" + "   public class Inner {}\n" + "}\n" + "\n" + "}", "TopLevel")

        assertCanFind(manager, "foo/TopLevel")
        assertCanFind(manager, "org/jetbrains/TopLevel")
//        assertCanFind(manager, "foo/TopLevel.Inner")
//        assertCanFind(manager, "foo/TopLevel.Inner.Inner")
//
//        assertCannotFind(manager, "foo/TopLevel\$Inner.Inner")
//        assertCannotFind(manager, "foo/TopLevel.Inner\$Inner")
//        assertCannotFind(manager, "foo/TopLevel.Inner.Inner.Inner")
    }

    throws(javaClass<Exception>())
    public fun testInnerClassesWithDollars() {
        val manager = configureManager("package foo;\n\n" + "public class TopLevel {\n" +
                                       "public class I\$nner {" + "   public class I\$nner{}" + "   public class \$Inner{}" + "   public class In\$ne\$r\${}" + "   public class Inner\$\${}" + "   public class \$\$\$\$\${}" + "}\n" + "public class Inner\$ {" + "   public class I\$nner{}" + "   public class \$Inner{}" + "   public class In\$ne\$r\${}" + "   public class Inner\$\${}" + "   public class \$\$\$\$\${}" + "}\n" + "public class In\$ner\$\$ {" + "   public class I\$nner{}" + "   public class \$Inner{}" + "   public class In\$ne\$r\${}" + "   public class Inner\$\${}" + "   public class \$\$\$\$\${}" + "}\n" + "\n" + "}", "TopLevel")

        assertCanFind(manager, "foo.TopLevel")

        assertCanFind(manager, "foo.TopLevel.I\$nner")
        assertCanFind(manager, "foo.TopLevel.I\$nner.I\$nner")
        assertCanFind(manager, "foo.TopLevel.I\$nner.\$Inner")
        assertCanFind(manager, "foo.TopLevel.I\$nner.In\$ne\$r\$")
        assertCanFind(manager, "foo.TopLevel.I\$nner.Inner\$\$")
        assertCanFind(manager, "foo.TopLevel.I\$nner.\$\$\$\$\$")

        assertCannotFind(manager, "foo.TopLevel.I.nner.\$\$\$\$\$")

        assertCanFind(manager, "foo.TopLevel.Inner\$")
        assertCanFind(manager, "foo.TopLevel.Inner\$.I\$nner")
        assertCanFind(manager, "foo.TopLevel.Inner\$.\$Inner")
        assertCanFind(manager, "foo.TopLevel.Inner\$.In\$ne\$r\$")
        assertCanFind(manager, "foo.TopLevel.Inner\$.Inner\$\$")
        assertCanFind(manager, "foo.TopLevel.Inner\$.\$\$\$\$\$")

        assertCannotFind(manager, "foo.TopLevel.Inner..\$\$\$\$\$")

        assertCanFind(manager, "foo.TopLevel.In\$ner\$\$")
        assertCanFind(manager, "foo.TopLevel.In\$ner\$\$.I\$nner")
        assertCanFind(manager, "foo.TopLevel.In\$ner\$\$.\$Inner")
        assertCanFind(manager, "foo.TopLevel.In\$ner\$\$.In\$ne\$r\$")
        assertCanFind(manager, "foo.TopLevel.In\$ner\$\$.Inner\$\$")
        assertCanFind(manager, "foo.TopLevel.In\$ner\$\$.\$\$\$\$\$")

        assertCannotFind(manager, "foo.TopLevel.In.ner\$\$.\$\$\$\$\$")
    }

    throws(javaClass<Exception>())
    public fun testTopLevelClassesWithDollars() {
        val inTheMiddle = configureManager("package foo;\n\n public class Top\$Level {}", "Top\$Level")
        assertCanFind(inTheMiddle, "foo.Top\$Level")

        val doubleAtTheEnd = configureManager("package foo;\n\n public class TopLevel\$\$ {}", "TopLevel\$\$")
        assertCanFind(doubleAtTheEnd, "foo.TopLevel\$\$")

        val multiple = configureManager("package foo;\n\n public class Top\$Lev\$el\$ {}", "Top\$Lev\$el\$")
        assertCanFind(multiple, "foo.Top\$Lev\$el\$")
        assertCannotFind(multiple, "foo.Top.Lev\$el\$")

        val twoBucks = configureManager("package foo;\n\n public class \$\$ {}", "\$\$")
        assertCanFind(twoBucks, "foo.\$\$")
    }

    throws(javaClass<Exception>())
    public fun testTopLevelClassWithDollarsAndInners() {
        val manager = configureManager("package foo;\n\n" + "public class Top\$Level\$\$ {\n" +
                                       "public class I\$nner {" + "   public class I\$nner{}" + "   public class In\$ne\$r\${}" + "   public class Inner\$\$\$\$\${}" + "   public class \$Inner{}" + "   public class \${}" + "   public class \$\$\$\$\${}" + "}\n" + "public class Inner {" + "   public class Inner{}" + "}\n" + "\n" + "}", "Top\$Level\$\$")

        assertCanFind(manager, "foo.Top\$Level\$\$")

        assertCanFind(manager, "foo.Top\$Level\$\$.Inner")
        assertCanFind(manager, "foo.Top\$Level\$\$.Inner.Inner")

        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner")
        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner.I\$nner")
        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner.In\$ne\$r\$")
        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner.Inner\$\$\$\$\$")
        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner.\$Inner")
        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner.\$")
        assertCanFind(manager, "foo.Top\$Level\$\$.I\$nner.\$\$\$\$\$")

        assertCannotFind(manager, "foo.Top.Level\$\$.I\$nner.\$\$\$\$\$")
    }

    throws(javaClass<Exception>())
    public fun testDoNotThrowOnMalformedInput() {
        val fileWithEmptyName = configureManager("package foo;\n\n public class Top\$Level {}", "")
        assertCannotFind(fileWithEmptyName, "foo.")
        assertCannotFind(fileWithEmptyName, ".")
        assertCannotFind(fileWithEmptyName, "..")
        assertCannotFind(fileWithEmptyName, "")
        assertCannotFind(fileWithEmptyName, ".foo")
    }

    throws(javaClass<Exception>())
    public fun testSeveralClassesInOneFile() {
        val manager = configureManager("package foo;\n\n" + "public class One {}\n" + "class Two {}\n" + "class Three {}", "One")

        assertCanFind(manager, "foo.One")

        //NOTE: this is unsupported
        assertCannotFind(manager, "foo.Two")
        assertCannotFind(manager, "foo.Three")
    }

    throws(javaClass<Exception>())
    public fun testScopeCheck() {
        val manager = configureManager("package foo;\n\n" + "public class Test {}\n", "Test")

        TestCase.assertNotNull("Should find class in all scope", manager.findClass("foo.Test", GlobalSearchScope.allScope(getProject())))
        TestCase.assertNull("Should not find class in empty scope", manager.findClass("foo.Test", GlobalSearchScope.EMPTY_SCOPE))
    }

    throws(javaClass<Exception>())
    private fun configureManager(Language("JAVA") text: String, className: String): CoreJavaFileManagerExt {
        val root = PsiTestUtil.createTestProjectStructure(myProject, myModule, PlatformTestCase.myFilesToDelete)
//        val root2 = PsiTestUtil.createTestProjectStructure(myProject, myModule, PlatformTestCase.myFilesToDelete)
        val pkg1 = root.createChildDirectory(this, "foo")
        val pkg2 = root.createChildDirectory(this, "org").createChildDirectory(this, "jetbrains")
        for (pkg in listOf(pkg1, pkg2)) {
            val dir = myPsiManager.findDirectory(pkg)
            TestCase.assertNotNull(dir)
            dir.add(PsiFileFactory.getInstance(getProject()).createFileFromText(className + ".java", JavaFileType.INSTANCE, text))
        }
        //TODO:
        throw UnsupportedOperationException()
    }

    private fun assertCanFind(manager: CoreJavaFileManagerExt, qName: String) {
        val foundClass = manager.findClass(ClassId.fromString(qName), GlobalSearchScope.allScope(getProject()))
        TestCase.assertNotNull("Could not find:" + qName, foundClass)
//        TestCase.assertEquals("Found " + foundClass.getQualifiedName() + " instead of " + qName, qName, foundClass.getQualifiedName())
    }

    private fun assertCannotFind(manager: CoreJavaFileManager, qName: String) {
        val foundClass = manager.findClass(qName, GlobalSearchScope.allScope(getProject()))
        TestCase.assertNull("Found, but shouldn't have:" + qName, foundClass)
    }
}
