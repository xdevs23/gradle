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
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * The special-cased implementation of {@link Set} that tracks all accesses to its elements.
 *
 * @param <E> the type of elements
 */
class AccessTrackingSet<E> implements Set<E> {
    // TODO(https://github.com/gradle/configuration-cache/issues/337) Only a limited subset of entrySet/keySet methods are currently tracked.
    private final Set<E> delegate;
    private final Consumer<Object> onAccess;
    private final Runnable onAggregatingAccess;

    public AccessTrackingSet(Set<E> delegate, Consumer<Object> onAccess, Runnable onAggregatingAccess) {
        this.delegate = delegate;
        this.onAccess = onAccess;
        this.onAggregatingAccess = onAggregatingAccess;
    }

    @Override
    public boolean contains(Object o) {
        boolean result = delegate.contains(o);
        onAccess.accept(o);
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        boolean result = delegate.containsAll(collection);
        for (Object o : collection) {
            onAccess.accept(o);
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        // We cannot perform modification before notifying because the listener may want to query the state of the delegate prior to that.
        onAccess.accept(o);
        return delegate.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        // We cannot perform modification before notifying because the listener may want to query the state of the delegate prior to that.
        for (Object o : collection) {
            onAccess.accept(o);
        }
        return delegate.removeAll(collection);
    }

    @Override
    public Iterator<E> iterator() {
        reportAggregatingAccess();
        return delegate.iterator();
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
    public boolean equals(Object object) {
        reportAggregatingAccess();
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        reportAggregatingAccess();
        return super.hashCode();
    }

    @Override
    public Object[] toArray() {
        reportAggregatingAccess();
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        reportAggregatingAccess();
        return delegate.toArray(array);
    }

    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Spliterator<E> spliterator() {
        return Set.super.spliterator();
    }

    private void reportAggregatingAccess() {
        onAggregatingAccess.run();
    }
}
