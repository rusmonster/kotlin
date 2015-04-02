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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.CoreJavaFileManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.GlobalSearchScope
import kotlin.Function1
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

import java.util.ArrayList
import kotlin.properties.Delegates

//TODO_R: packages cache injection
public class CoreJavaFileManagerExt(private val myPsiManager: PsiManager) : CoreJavaFileManager(myPsiManager) {

    private val myClasspath = ArrayList<VirtualFile>()
    private var packagesCache: JvmDependenciesIndex by Delegates.notNull()

    public fun initCache(packagesCache: JvmDependenciesIndex) {
        this.packagesCache = packagesCache
    }

    public fun findClass(classId: ClassId, scope: GlobalSearchScope): PsiClass? {
        val classNameWithInnerClasses = classId.getRelativeClassName().asString()
        return packagesCache.findClass(classId) { dir, type ->
            findClassGivenPackage(scope, dir, classNameWithInnerClasses, type)
        }
    }

    override fun findClass(qName: String, scope: GlobalSearchScope): PsiClass? {
        //TODO_R: comment
        val classIdAsTopLevelClass = ClassId.topLevel(FqName(qName))
        return findClass(classIdAsTopLevelClass, scope) ?: super.findClass(qName, scope)
    }

    override fun findClasses(qName: String, scope: GlobalSearchScope): Array<PsiClass> {
        val result = ArrayList<PsiClass>()
        val classIdAsTopLevelClass = ClassId.topLevel(FqName(qName))
        val classNameWithInnerClasses = classIdAsTopLevelClass.getRelativeClassName().asString()
        packagesCache.traverseDirectoriesInPackage(classIdAsTopLevelClass.getPackageFqName()) { dir, rootType ->
            val psiClass = findClassGivenPackage(scope, dir, classNameWithInnerClasses, rootType)
            if (psiClass != null) {
                result.add(psiClass)
            }
            // traverse all
            true
        }
        if (result.isEmpty()) {
            return super.findClasses(qName, scope)
        }
        return result.toArray<PsiClass>(arrayOfNulls<PsiClass>(result.size()))
    }

    public fun findClassGivenPackage(
            scope: GlobalSearchScope, packageDir: VirtualFile,
            classNameWithInnerClasses: String, rootType: JavaRoot.RootType
    ): PsiClass? {
        val topLevelClassName = classNameWithInnerClasses.substringBefore('.')

        val vFile = when (rootType) {
            JavaRoot.RootType.BINARY -> packageDir.findChild("$topLevelClassName.class")
            JavaRoot.RootType.SOURCE -> packageDir.findChild("$topLevelClassName.java")
        }

        if (vFile == null) return null

        if (!vFile.isValid()) {
            LOG.error("Invalid child of valid parent: ${vFile.getPath()}; ${packageDir.isValid()} path=${packageDir.getPath()}")
            return null
        }
        if (vFile !in scope) {
            return null
        }

        val file = myPsiManager.findFile(vFile) as? PsiClassOwner ?: return null
        return findClassInPsiFile(classNameWithInnerClasses, file)
    }


    override fun addToClasspath(root: VirtualFile) {
        super.addToClasspath(root)
        myClasspath.add(root)
    }

    override fun findPackage(packageName: String): PsiPackage? {
        var found = false
        packagesCache.traverseDirectoriesInPackage(FqName(packageName)) { _, __ ->
            found = true
            //abort on first found
            false
        }
        if (found) {
            return PsiPackageImpl(myPsiManager, packageName)
        }
        return null
    }

    companion object {
        //TODO_R:
        private val LOG = Logger.getInstance("#com.intellij.core.CoreJavaFileManager")

        private fun findClassInPsiFile(classNameWithInnerClassesDotSeparated: String, file: PsiClassOwner): PsiClass? {
            for (topLevelClass in file.getClasses()) {
                val candidate = findClassByTopLevelClass(classNameWithInnerClassesDotSeparated, topLevelClass)
                if (candidate != null) {
                    return candidate
                }
            }
            return null
        }

        private fun findClassByTopLevelClass(className: String, topLevelClass: PsiClass): PsiClass? {
            if (className.indexOf('.') < 0) {
                return if (className == topLevelClass.getName()) topLevelClass else null
            }

            val segments = StringUtil.split(className, ".").iterator()
            if (!segments.hasNext() || segments.next() != topLevelClass.getName()) {
                return null
            }
            var curClass = topLevelClass
            while (segments.hasNext()) {
                val innerClassName = segments.next()
                val innerClass = curClass.findInnerClassByName(innerClassName, false)
                if (innerClass == null) {
                    return null
                }
                curClass = innerClass
            }
            return curClass
        }
    }
}
