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

package net.sharedwonder.mc.ptbridge.config;

import java.io.File;
import java.io.FileReader;
import java.util.Objects;

public final class ConfigManager {
    private ConfigManager() {}

    private static File configDir;

    public static void init(File configDir) {
        if (ConfigManager.configDir != null) {
            throw new IllegalStateException("ConfigManager is already initialized");
        }
        ConfigManager.configDir = Objects.requireNonNull(configDir);
    }

    public static <T> T getConfig(Class<T> type) {
        var annotation = type.getDeclaredAnnotation(Config.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class missing @Config annotation");
        }

        var name = annotation.name();
        if (!annotation.withoutExtension() && name.indexOf('.') == -1) {
            name = name + '.' + annotation.type().getFileExtension();
        }
        var file = new File(configDir, name);
        try (var reader = new FileReader(file)) {
            return annotation.type().parse(type, reader);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to get the configuration", exception);
        }
    }
}
