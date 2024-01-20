/*
 * Copyright (C) 2024 sharedwonder (Liu Baihao).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sharedwonder.mc.ptbridge.addon

import net.sharedwonder.mc.ptbridge.utils.GSON
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import org.apache.logging.log4j.LogManager

object AddonLoader {
    private const val INFO_FILE_NAME: String = "ptbridge-addon.json"

    private val LOGGER = LogManager.getLogger(AddonLoader::class.java)

    private val ADDONS = HashMap<String, AddonInfo>()

    @JvmStatic
    fun getAddonInfo(id: String): AddonInfo {
        val info = ADDONS[id]
        checkNotNull(info) { "The addon of $id is not loaded" }
        return info
    }

    @JvmStatic
    @PublishedApi
    internal fun init(addonsDir: File) {
        if (addonsDir.exists()) {
            val addons = addonsDir.walk().maxDepth(1).filter { it.isFile && it.extension == "jar" }.toList()
            if (addons.isNotEmpty()) {
                if (LOGGER.isInfoEnabled) {
                    LOGGER.info("The following addon(s) will be loaded:\n${addons.joinToString("\n - ", prefix = " - ") { it.name }}")
                }
                addons.forEach { load(it) }
            }
        }
    }

    @JvmStatic
    private fun load(file: File): Boolean {
        if (!file.exists()) {
            LOGGER.error("Addon file not found: {}", file.name)
            return false
        }

        val info: AddonInfo
        try {
            JarFile(file).use {
                it.getInputStream(it.getJarEntry(INFO_FILE_NAME)).reader().use { reader ->
                    info = GSON.fromJson(reader, AddonInfo::class.java)
                }
            }
        } catch (exception: Exception) {
            LOGGER.error("Failed to read addon info: {}", file.name, exception)
            return false
        }

        val initializer: AddonInitializer
        try {
            val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()))
            initializer = classLoader.loadClass(info.initializer).getConstructor().newInstance() as AddonInitializer
        } catch (exception: NullPointerException) {
            LOGGER.error("Invalid addon info: {}", file.name, exception)
            return false
        } catch (exception: ClassNotFoundException) {
            LOGGER.error("Addon initializer class not found: {}", file.name, exception)
            return false
        } catch (exception: ReflectiveOperationException) {
            LOGGER.error("Invalid addon initializer class: {}", file.name, exception)
            return false
        } catch (exception: ClassCastException) {
            LOGGER.error("Invalid addon initializer class: {}", file.name, exception)
            return false
        } catch (exception: Exception) {
            LOGGER.error("Unknown error while loading the addon: {}", file.name, exception)
            return false
        }

        try {
            ADDONS[info.id] = info
            initializer.init()
        } catch (exception: Throwable) {
            LOGGER.error("Exception thrown while initializing the addon: {}", file.name, exception)
            ADDONS.remove(info.id)
            return false
        }

        return true
    }
}
