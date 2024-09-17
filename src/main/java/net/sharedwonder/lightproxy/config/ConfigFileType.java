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

package net.sharedwonder.lightproxy.config;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Properties;
import net.sharedwonder.lightproxy.util.JsonUtils;

public enum ConfigFileType {
    JSON {
        @Override
        public String getFileExtension() {
            return "json";
        }

        @Override
        public <T> T parse(Class<T> type, Reader reader) {
            return JsonUtils.fromJson(reader, type);
        }
    },
    PROPERTIES {
        @Override
        public String getFileExtension() {
            return "properties";
        }

        @Override
        public <T> T parse(Class<T> type, Reader reader) {
            var properties = new Properties();
            try {
                properties.load(reader);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to read the configuration file", exception);
            }

            T obj;
            try {
                obj = type.getConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException("Failed to instantiate the configuration class", exception);
            }
            for (var field : type.getDeclaredFields()) {
                var annotation = field.getDeclaredAnnotation(PropertyName.class);
                var name = annotation != null ? annotation.value() : field.getName();
                var value = properties.get(name);
                if (value == null) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    field.set(obj, value);
                } catch (ReflectiveOperationException | InaccessibleObjectException exception) {
                    throw new IllegalArgumentException("Failed to set the field", exception);
                }
            }
            return obj;
        }
    };

    public abstract String getFileExtension();

    public abstract <T> T parse(Class<T> type, Reader reader) throws Exception;
}
