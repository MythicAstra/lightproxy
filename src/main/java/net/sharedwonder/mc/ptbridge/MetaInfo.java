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

package net.sharedwonder.mc.ptbridge;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import com.google.gson.Gson;

public final class MetaInfo {
    private MetaInfo() {}

    public static final String ID = "ptbridge";

    public static final String VERSION;

    private static final Gson GSON = new Gson();

    static {
        try (var reader = new InputStreamReader(Objects.requireNonNull(MetaInfo.class.getResourceAsStream("/version.json")), StandardCharsets.UTF_8)) {
            VERSION = (String) GSON.fromJson(reader, Map.class).get("version");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
