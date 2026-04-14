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

@Singleton
@NullMarked
public final class ComponentStore {

    private static final int DEFAULT_CAPACITY = 256;

    private final Map<UUID, Map<Class<? extends Component>, Component>> store;

    @Inject
    private ComponentStore() {
        this.store = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
    }

    public <C extends Component> void set(final UUID uuid, final ComponentType<C> type, final C component) {
        this.bucket(uuid).put(type.rawType(), component);
    }

    public <C extends Component> Optional<C> get(final UUID uuid, final ComponentType<C> type) {
        final Map<Class<? extends Component>, Component> bucket = this.store.get(uuid);
        if (bucket == null) {
            return Optional.empty();
        }
        return Optional.of(type.rawType()
                .cast(bucket.get(type.rawType())));
    }

    public <C extends Component> C getOrThrow(final UUID uuid, final ComponentType<C> type) {
        return this.get(uuid, type)
                .orElseThrow(() -> new ComponentNotFoundException(uuid, type));
    }

    public boolean has(final UUID uuid, final ComponentType<?> type) {
        final Map<Class<? extends Component>, Component> bucket = this.store.get(uuid);
        return bucket != null && bucket.containsKey(type.rawType());
    }

    public <C extends Component> C updateAndGet(
            final UUID uuid,
            final ComponentType<C> type,
            final UnaryOperator<C> operator
    ) {
        @SuppressWarnings("unchecked") final C[] holder = (C[]) new Component[1];
        this.bucket(uuid).compute(type.rawType(), (_, current) -> {
            if (current == null) {
                throw new ComponentNotFoundException(uuid, type);
            }
            final C updated = operator.apply(type.rawType().cast(current));
            holder[0] = updated;
            return updated;
        });
        return Objects.requireNonNull(holder[0]);
    }

    public void remove(final UUID uuid, final ComponentType<?> type) {
        final Map<Class<? extends Component>, Component> bucket = this.store.get(uuid);
        if (bucket != null) {
            bucket.remove(type.rawType());
            if (bucket.isEmpty()) {
                this.store.remove(uuid, bucket);
            }
        }
    }

    public void removeAll(final UUID entity) {
        this.store.remove(entity);
    }

    public <C extends Component> Stream<Map.Entry<UUID, C>> query(final ComponentType<C> type) {
        return this.store.entrySet().stream()
                .flatMap(entry -> {
                    final Component raw = entry.getValue().get(type.rawType());
                    if (raw == null) {
                        return Stream.empty();
                    }
                    return Stream.of(Map.entry(entry.getKey(), type.rawType().cast(raw)));
                });
    }

    private Map<Class<? extends Component>, Component> bucket(final UUID uuid) {
        return this.store.computeIfAbsent(uuid, _ -> new ConcurrentHashMap<>());
    }
}
