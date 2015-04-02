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
import com.intellij.util.containers.IntArrayList
import org.jetbrains.kotlin.name.FqName
import java.util.ArrayList
import java.util.EnumSet
import java.util.HashMap
import kotlin.properties.Delegates

//TODO_R: not public?
public data class JavaRoot(public val file: VirtualFile, public val type: JavaRoot.RootType) {
    public enum class RootType {
        SOURCE
        BINARY
    }
}

class PackagesCache(val roots: List<JavaRoot>) {
    //these fields are computed based on classpath which is filled in later
    private val maxIndex: Int by Delegates.lazy { roots.size() }

    private val rootCache: Cache by Delegates.lazy {
        with(Cache()) {
            roots.indices.forEach {
                rootIndices.add(it)
            }
            rootIndices.add(maxIndex)
            rootIndices.trimToSize()
            this
        }
    }

    fun <T : Any> searchPackages(
            packageFqName: FqName,
            acceptedRootTypes: EnumSet<JavaRoot.RootType> = EnumSet.allOf(javaClass<JavaRoot.RootType>()),
            handler: (VirtualFile) -> T?
    ): T? {
        fun handle(root: JavaRoot, targetDirInRoot: VirtualFile): T? {
            if (root.type in acceptedRootTypes) {
                return handler(targetDirInRoot)
            }
            return null
        }

        val packagesPath = packageFqName.pathSegments().map { it.getIdentifier() }
        if (packagesPath.isEmpty()) {
            roots.forEach { root ->
                val result = handle(root, root.file)
                if (result != null) return result
            }
        }

        val caches = cachesPath(packagesPath)

        var lastMaxIndex = -1
        for (cacheIndex in (caches.size() - 1) downTo 0) {
            val cache = caches[cacheIndex]
            for (i in cache.rootIndices.size().indices) {
                val rootIndex = cache.rootIndices[i]
                if (rootIndex <= lastMaxIndex) continue

                val dir = travelPath(rootIndex, packagesPath, cacheIndex, caches) ?: continue
                val result = handle(roots[rootIndex], dir)
                if (result != null) {
                    return result
                }
            }
            lastMaxIndex = cache.rootIndices.lastOrNull() ?: lastMaxIndex
        }
        return null
    }

    /**
     * root -> "org" -> "jet" -> "language"
     * [org, jet, language]
     */
    private fun travelPath(rootIndex: Int, packagesPath: List<String>, fillCachesAfter: Int, cachesPath: List<Cache>): VirtualFile? {
        if (oob(rootIndex)) {
            for (i in (fillCachesAfter + 1)..cachesPath.size() - 1) {
                cachesPath[i].rootIndices.add(maxIndex)
                cachesPath[i].rootIndices.trimToSize()
            }
            return null
        }

        var currentFile = roots[rootIndex].file
        for (pathIndex in packagesPath.indices) {
            currentFile = currentFile.findChild(packagesPath[pathIndex]) ?: return null
            val correspondingCacheIndex = pathIndex + 1
            if (correspondingCacheIndex > fillCachesAfter) {
                cachesPath[correspondingCacheIndex].rootIndices.add(rootIndex)
            }
        }
        return currentFile
    }

    private fun cachesPath(path: List<String>): ArrayList<Cache> {
        val caches = ArrayList<Cache>()
        caches.add(rootCache)
        var currentCache = rootCache
        for (subPackageName in path) {
            currentCache = currentCache[subPackageName]
            caches.add(currentCache)
        }
        return caches
    }

    private fun oob(index: Int): Boolean {
        return index >= maxIndex
    }

    class Cache {
        private val innerCaches = HashMap<String, Cache>()

        fun get(name: String) = innerCaches.getOrPut(name) { Cache() }

        val rootIndices = IntArrayList()
    }
}

fun IntArrayList.lastOrNull() = if (isEmpty()) null else get(size() - 1)