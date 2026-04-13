/*
 * PaperPluginTemplate
 *
 * Copyright (c) 2026. Namiu (うにたろう)
 *                     Contributors []
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.namiuni.paperplugintemplate.common.component;

import io.github.namiuni.paperplugintemplate.common.component.components.Component;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/// Thread-safe, heterogeneous data store that associates [Component] instances
/// with entities, keyed by ([UUID], [ComponentType]).
///
/// ## Memory management
///
/// Components are retained until explicitly removed. Callers must call
/// [#removeAll] when an entity's lifecycle ends to prevent unbounded memory
/// growth.
///
/// ## Thread safety
///
/// All methods are safe to call concurrently from any number of threads.
@Singleton
@NullMarked
public final class ComponentRegistry {

    private static final int DEFAULT_CAPACITY = 256;

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Class<? extends Component>, Component>> store;

    /// Constructs a new empty registry with default initial capacity.
    @Inject
    private ComponentRegistry() {
        this.store = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
    }

    /// Stores or replaces the component for the given uuid and type.
    ///
    /// @param <C>       the component type
    /// @param uuid    the target uuid; must not be `null`
    /// @param type      the component type token; must not be `null`
    /// @param component the component to store; must not be `null`
    public <C extends Component> void set(final UUID uuid, final ComponentType<C> type, final C component) {
        this.bucket(uuid).put(type.rawType(), component);
    }

    /// Returns the component of the given type for `uuid`, if present.
    ///
    /// @param <C>  the component type
    /// @param uuid the uuid to query
    /// @param type the component type token
    /// @return the component wrapped in [Optional], or [Optional#empty()] on a miss
    @SuppressWarnings("unchecked")
    public <C extends Component> Optional<C> get(final UUID uuid, final ComponentType<C> type) {
        final var bucket = this.store.get(uuid);
        if (bucket == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((C) bucket.get(type.rawType()));
    }

    /// Returns the component of the given type for `uuid`.
    ///
    /// @param <C>  the component type
    /// @param uuid the uuid to query
    /// @param type the component type token
    /// @return the component, never `null`
    /// @throws ComponentNotFoundException if no component of `type` exists for `uuid`
    public <C extends Component> C getOrThrow(final UUID uuid, final ComponentType<C> type) {
        return this.get(uuid, type)
                .orElseThrow(() -> new ComponentNotFoundException(uuid, type));
    }

    /// Returns `true` if `uuid` currently has a component of the given type.
    ///
    /// @param uuid the uuid to query
    /// @param type the component type token
    /// @return `true` if the component is present
    public boolean has(final UUID uuid, final ComponentType<?> type) {
        final var bucket = this.store.get(uuid);
        return bucket != null && bucket.containsKey(type.rawType());
    }

    /// Atomically replaces the stored component by applying `operator` to the
    /// current value and persisting the result.
    ///
    /// `operator` executes inside `ConcurrentHashMap#compute` and may be
    /// invoked more than once under high contention. It must therefore be a
    /// pure, side-effect-free function.
    ///
    /// @param <C>      the component type
    /// @param uuid     the target uuid
    /// @param type     the component type token
    /// @param operator pure function from current to updated component;
    ///                 must not return `null`
    /// @return the updated component, never `null`
    /// @throws ComponentNotFoundException if no component of `type` exists for `uuid`
    @SuppressWarnings("unchecked")
    public <C extends Component> C updateAndGet(
            final UUID uuid,
            final ComponentType<C> type,
            final UnaryOperator<C> operator
    ) {
        // Wrapper array gives us the typed return value from inside the lambda,
        // which is the idiomatic approach for ConcurrentHashMap#compute.
        final C[] holder = (C[]) new Component[1];
        this.bucket(uuid).compute(type.rawType(), (_, current) -> {
            if (current == null) {
                throw new ComponentNotFoundException(uuid, type);
            }
            final C updated = operator.apply((C) current);
            holder[0] = updated;
            return updated;
        });
        return Objects.requireNonNull(holder[0]);
    }

    /// Removes the component of the given type for `uuid`.
    ///
    /// Idempotent: removing an absent component is a no-op.
    ///
    /// @param uuid the target uuid
    /// @param type the component type token
    public void remove(final UUID uuid, final ComponentType<?> type) {
        final var bucket = this.store.get(uuid);
        if (bucket != null) {
            bucket.remove(type.rawType());
            if (bucket.isEmpty()) {
                this.store.remove(uuid, bucket);
            }
        }
    }

    /// Removes **all** components for `entity`.
    ///
    /// Must be called when the entity's lifecycle ends (e.g. on player
    /// disconnect after cache eviction) to prevent unbounded memory growth.
    /// Idempotent.
    ///
    /// @param entity the entity whose components should be purged
    public void removeAll(final UUID entity) {
        this.store.remove(entity);
    }

    /// Returns a snapshot stream of all (entity, component) pairs for the
    /// given type at the moment of invocation.
    ///
    /// The stream does **not** reflect concurrent modifications made after
    /// it is created. Materialize it into a collection before performing any
    /// further registry operations to avoid stale reads.
    ///
    /// This method is designed for system-wide processing (e.g. world-save
    /// checkpoints that iterate every online player's [PersistenceComponent]).
    ///
    /// @param <C>  the component type
    /// @param type the component type token
    /// @return an ordered stream of (entityId, component) pairs
    @SuppressWarnings("unchecked")
    public <C extends Component> Stream<Map.Entry<UUID, C>> query(final ComponentType<C> type) {
        return this.store.entrySet().stream()
                .filter(uuid -> uuid.getValue().containsKey(type.rawType()))
                .map(uuid -> Map.entry(uuid.getKey(), (C) uuid.getValue().get(type.rawType())));
    }

    // -------------------------------------------------------------------------

    private ConcurrentHashMap<Class<? extends Component>, Component> bucket(final UUID uuid) {
        return this.store.computeIfAbsent(uuid, _ -> new ConcurrentHashMap<>());
    }
}
