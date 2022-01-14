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

package org.gradle.internal.instrumented;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A wrapper for the {@link System#getenv()} result that notifies a listener about accesses.
 */
class AccessTrackingEnvMap implements Map<String, String> {
    private final Map<String, String> delegate;
    private final BiConsumer<? super String, ? super String> onAccess;

    public AccessTrackingEnvMap(BiConsumer<String, String> onAccess) {
        this(System.getenv(), onAccess);
    }

    AccessTrackingEnvMap(Map<String, String> delegate, BiConsumer<? super String, ? super String> onAccess) {
        this.delegate = delegate;
        this.onAccess = onAccess;
    }

    @Override
    public String get(Object key) {
        return getAndReport(key);
    }

    @Override
    public String put(String key, String value) {
        return delegate.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public String getOrDefault(Object key, String defaultValue) {
        String value = getAndReport(key);
        if (value == null && !delegate.containsKey(key)) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public boolean containsKey(Object key) {
        return getAndReport(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Set<String> keySet() {
        return new AccessTrackingSet<>(delegate.keySet(), this::getAndReport, this::reportAggregatingAccess);
    }

    @Override
    public Collection<String> values() {
        return delegate.values();
    }

    private String getAndReport(Object key) {
        String result = delegate.get(key);
        // The delegate will throw if something that isn't a string is used there. Do call delegate.get() first so the exception is thrown form the JDK code to avoid extra blame.
        onAccess.accept((String) key, result);
        return result;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AccessTrackingSet<>(delegate.entrySet(), this::onAccessEntrySetElement, this::reportAggregatingAccess);
    }

    private void onAccessEntrySetElement(Object potentialEntry) {
        Map.Entry<String, String> entry = AccessTrackingUtils.tryConvertingToTrackableEntry(potentialEntry);
        if (entry != null) {
            getAndReport(entry.getKey());
        }
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super String> action) {
        reportAggregatingAccess();
        delegate.forEach(action);
    }

    @Override
    public int size() {
        reportAggregatingAccess();
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        reportAggregatingAccess();
        return delegate.isEmpty();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object object) {
        reportAggregatingAccess();
        return delegate.equals(object);
    }

    @Override
    public int hashCode() {
        reportAggregatingAccess();
        return delegate.hashCode();
    }

    private void reportAggregatingAccess() {
        // Mark all map contents as inputs if some aggregating access is used.
        delegate.forEach(onAccess);
    }
}

