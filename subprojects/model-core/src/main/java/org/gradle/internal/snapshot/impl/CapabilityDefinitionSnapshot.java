/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.snapshot.impl;

import org.gradle.api.capabilities.Capability;
import org.gradle.internal.hash.Hasher;

import javax.annotation.Nullable;

public class CapabilityDefinitionSnapshot extends AbstractIsolatableScalarValue<Capability> {

    public CapabilityDefinitionSnapshot(Capability value) {
        super(value);
    }

    @Override
    public void appendToHasher(Hasher hasher) {
        hasher.putString(getValue().getGroup());
        hasher.putString(getValue().getName());
        @Nullable String version = getValue().getVersion();
        if (version == null) {
            hasher.putNull();
        } else {
            hasher.putString(version);
        }
    }
}
