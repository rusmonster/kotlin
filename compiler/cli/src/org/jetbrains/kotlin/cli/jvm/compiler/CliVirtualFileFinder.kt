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

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

public class CliVirtualFileFinder(private val packagesCache: PackagesCache) : VirtualFileKotlinClassFinder(), VirtualFileFinder {

    override fun findVirtualFileWithHeader(className: FqName): VirtualFile? {
        //TODO_R:
        return findVirtualFileByClassId(ClassId.topLevel(className))
    }

    override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
        val file = findVirtualFileByClassId(classId) ?: return null

        return KotlinBinaryClassCache.getKotlinBinaryClass(file)
    }

    override fun findVirtualFile(internalName: String): VirtualFile? {
        //TODO_R: this is ridiculous
        val jvmClassName = JvmClassName.byInternalName(internalName)
        val packageFqName = jvmClassName.getPackageFqName()
        val fullNameSegments = jvmClassName.getFqNameForClassNameWithoutDollars().pathSegments().map { it.asString() }
        val relativeClassName = FqName.fromSegments(fullNameSegments.subList(packageFqName.pathSegments().size(), fullNameSegments.size()))
        val classId = ClassId(packageFqName, relativeClassName, false)

        return findVirtualFileByClassId(classId)
    }

    private fun findVirtualFileByClassId(classId: ClassId): VirtualFile? {
        val relativeClassName = classId.getRelativeClassName().asString().replace('.', '$')
        return packagesCache.searchPackages(classId.getPackageFqName(), cacheByClassId = classId) {
            it.findChild("$relativeClassName.class")?.let {
                if (it.isValid()) it else null
            }
        }
    }
}
