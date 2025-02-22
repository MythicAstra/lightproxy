/*
 * Copyright (C) 2025 MythicAstra
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

package net.sharedwonder.lightproxy.addon

import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import net.sharedwonder.lightproxy.util.JsonUtils
import org.apache.logging.log4j.LogManager

object AddonLoader {
    private const val INFO_FILE_NAME: String = "lightproxy-addon.json"

    private val logger = LogManager.getLogger(AddonLoader::class.java)

    private val addons = HashMap<String, AddonInfo>()

    private var initialized = false

    @JvmStatic
    fun init(addonDir: File) {
        synchronized(this) {
            check(!initialized) { "AddonLoader has already been initialized" }
            initialized = true
        }

        if (addonDir.exists()) {
            val addons = addonDir.walk().maxDepth(1).filter { it.isFile && it.extension == "jar" }.toList()
            if (addons.isNotEmpty()) {
                logger.info(fun(): String = "The following addon(s) will be loaded:\n${addons.joinToString("\n - ", prefix = " - ") { it.name }}")
                addons.forEach { load(it) }
            }
        }
    }

    @JvmStatic
    fun load(file: File): Boolean {
        if (!file.exists()) {
            logger.error("Addon file not found: ${file.name}")
            return false
        }

        val info: AddonInfo
        try {
            JarFile(file).use {
                it.getInputStream(it.getJarEntry(INFO_FILE_NAME)).reader().use { reader ->
                    info = JsonUtils.fromJson<AddonInfo>(reader)
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed to read addon info: ${file.name}", exception)
            return false
        }

        val initializer: AddonInitializer
        try {
            val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()))
            initializer = classLoader.loadClass(info.initializer).getConstructor().newInstance() as AddonInitializer
        } catch (exception: NullPointerException) {
            logger.error("Invalid addon info: ${file.name}", exception)
            return false
        } catch (exception: ClassNotFoundException) {
            logger.error("Addon initializer class not found: ${file.name}", exception)
            return false
        } catch (exception: ReflectiveOperationException) {
            logger.error("Invalid addon initializer class: ${file.name}", exception)
            return false
        } catch (exception: ClassCastException) {
            logger.error("Not an addon initializer class: ${file.name}", exception)
            return false
        } catch (exception: Exception) {
            logger.error("Unknown error while loading the addon: ${file.name}", exception)
            return false
        }

        try {
            addons[info.id] = info
            initializer.init()
        } catch (exception: Throwable) {
            logger.error("Exception thrown while initializing the addon: ${file.name}", exception)
            addons.remove(info.id)
            return false
        }

        return true
    }

    @JvmStatic
    fun getAddonInfo(id: String): AddonInfo {
        val info = addons[id]
        checkNotNull(info) { "The addon of $id is not loaded" }
        return info
    }
}
