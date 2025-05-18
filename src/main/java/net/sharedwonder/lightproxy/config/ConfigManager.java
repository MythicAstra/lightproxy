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

package net.sharedwonder.lightproxy.config;

import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class ConfigManager {
    private ConfigManager() {}

    @Nullable
    private static File configDir;

    public static synchronized void init(File configDir) {
        if (ConfigManager.configDir != null) {
            throw new IllegalStateException("ConfigManager has already been initialized");
        }
        ConfigManager.configDir = Objects.requireNonNull(configDir);
    }

    public static <T> T getConfig(Class<T> type) {
        var annotation = type.getDeclaredAnnotation(Config.class);
        if (annotation == null) {
            throw new IllegalArgumentException("The specified class is missing @net.sharedwonder.lightproxy.config.Config");
        }

        var filename = annotation.filename();
        var file = new File(configDir, filename);
        if (file.exists()) {
            try (var reader = new FileReader(file)) {
                return annotation.format().parse(type, reader);
            } catch (Exception exception) {
                throw new RuntimeException("Unable to get the configuration", exception);
            }
        }
        try {
            return type.getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Unable to instantiate the configuration class: " + type.getName(), exception);
        }
    }
}
