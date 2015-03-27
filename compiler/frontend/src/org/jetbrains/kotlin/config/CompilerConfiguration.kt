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

package org.jetbrains.kotlin.config

import com.intellij.openapi.util.Key

import java.util.*

SuppressWarnings("unchecked")
public class CompilerConfiguration {
    private val map = HashMap<Key<*>, Any?>()
    private var readOnly = false

    public fun <T: Any> get(key: CompilerConfigurationKey<T>): T? {
        val data = map[key.ideaKey] as? T
        return if (data == null) null else unmodifiable(data)
    }

    public fun <T> get(key: CompilerConfigurationKey<T>, defaultValue: T): T {
        val data = map[key.ideaKey] as? T
        return if (data == null) defaultValue else unmodifiable(data)
    }

    public fun <T> getList(key: CompilerConfigurationKey<MutableList<T>>): List<T> {
        val data = map[key.ideaKey] as? List<T>
        if (data == null) {
            return listOf()
        }
        else {
            return Collections.unmodifiableList<T>(data)
        }
    }

    public fun <T: Any> put(key: CompilerConfigurationKey<T>, value: T?) {
        checkReadOnly()
        map.put(key.ideaKey, value)
    }

    public fun <T> add(key: CompilerConfigurationKey<MutableList<T>>, value: T) {
        checkReadOnly()
        val ideaKey = key.ideaKey
        if (map.get(ideaKey) == null) {
            map.put(ideaKey, ArrayList<T>())
        }
        val list = map[ideaKey] as MutableList<T>
        list.add(value)
    }

    public fun <T> addAll(key: CompilerConfigurationKey<MutableList<T>>, values: Collection<T>) {
        checkReadOnly()
        checkForNullElements(values)
        val ideaKey = key.ideaKey
        if (map.get(ideaKey) == null) {
            map.put(ideaKey, ArrayList<T>())
        }
        val list = map[ideaKey] as MutableList<T>
        list.addAll(values)
    }

    public fun copy(): CompilerConfiguration {
        val copy = CompilerConfiguration()
        copy.map.putAll(map)
        return copy
    }

    private fun checkReadOnly() {
        if (readOnly) {
            throw IllegalStateException("CompilerConfiguration is read-only")
        }
    }

    public fun setReadOnly(readOnly: Boolean) {
        if (readOnly != this.readOnly) {
            checkReadOnly()
            this.readOnly = readOnly
        }
    }

    [suppress("UNCHECKED_CAST")]
    private fun <T> unmodifiable(o: T): T {
        if (o is List<*>) {
            return Collections.unmodifiableList(o) as T
        }
        else if (o is Map<*, *>) {
            return Collections.unmodifiableMap(o) as T
        }
        else if (o is Collection<*>) {
            return Collections.unmodifiableCollection(o) as T
        }
        else {
            return o
        }
    }

    override fun toString(): String {
        return map.toString()
    }

    private fun <T> checkForNullElements(values: Collection<T>) {
        var index = 0
        for (value in values) {
            if (value == null) {
                throw IllegalArgumentException("Element $index is null, while null values in compiler configuration are not allowed")
            }
            index++
        }
    }
}
