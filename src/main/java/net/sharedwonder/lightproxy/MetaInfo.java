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

package net.sharedwonder.lightproxy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.stream.JsonReader;

public final class MetaInfo {
    private MetaInfo() {}

    public static final String ID = "lightproxy";

    public static final String NAME = "LightProxy";

    public static final String VERSION;

    static {
        var input = MetaInfo.class.getResourceAsStream("/version.json");
        if (input == null) {
            throw new Error("'version.json' not found");
        }
        try (var reader = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String version = null;
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("version")) {
                    version = reader.nextString();
                    break;
                }
                reader.skipValue();
            }
            if (version == null) {
                throw new Error("version not found in 'version.json'");
            }
            VERSION = version;
        } catch (IOException exception) {
            throw new Error("I/O error while reading 'version.json'", exception);
        } catch (IllegalStateException exception) {
            throw new Error("Invalid 'version.json'", exception);
        }
    }
}
