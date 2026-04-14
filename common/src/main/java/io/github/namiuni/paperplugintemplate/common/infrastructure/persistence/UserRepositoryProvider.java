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
package io.github.namiuni.paperplugintemplate.common.infrastructure.persistence;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.infrastructure.persistence.sql.JdbiUserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.NullMarked;

/// Guice [Provider] that selects and returns the active [UserRepository] implementation
/// based on the configured [StorageType].
///
/// The selection is deferred to first access rather than performed at injector-creation
/// time. This allows the [PrimaryConfiguration] to be fully loaded before the repository
/// is instantiated, and ensures that provider-scoped sub-providers for SQL and JSON
/// backends are only constructed when actually required by the configured type.
///
/// ## Selection logic
///
/// | [StorageType] | Implementation |
/// |---|---|
/// | `JSON`       | [JsonUserRepository]  |
/// | `H2`         | [JdbiUserRepository]  |
/// | `MYSQL`      | [JdbiUserRepository]  |
/// | `POSTGRESQL` | [JdbiUserRepository]  |
///
/// ## Thread safety
///
/// This class carries no mutable state after construction. The provider is invoked
/// on a single thread during Guice eager-singleton initialization, so no concurrent
/// access occurs.
@NullMarked
public final class UserRepositoryProvider implements Provider<UserRepository> {

    private final Provider<PrimaryConfiguration> primaryConfig;
    private final Provider<JsonUserRepository> jsonRepository;
    private final Provider<JdbiUserRepository> jdbiRepository;

    /// Constructs a new provider.
    ///
    /// @param primaryConfig   the configuration provider used to read the active storage type
    /// @param jsonRepository  the provider for the JSON flat-file backend
    /// @param jdbiRepository  the provider for the SQL (JDBI) backend
    @Inject
    private UserRepositoryProvider(
            final Provider<PrimaryConfiguration> primaryConfig,
            final Provider<JsonUserRepository> jsonRepository,
            final Provider<JdbiUserRepository> jdbiRepository
    ) {
        this.primaryConfig = primaryConfig;
        this.jsonRepository = jsonRepository;
        this.jdbiRepository = jdbiRepository;
    }

    /// Returns the [UserRepository] implementation for the configured [StorageType].
    ///
    /// @return the active repository implementation, never `null`
    @Override
    public UserRepository get() {
        final PrimaryConfiguration.Storage storage = this.primaryConfig.get().storage();
        return switch (storage.type()) {
            case JSON -> this.jsonRepository.get();
            case H2, MYSQL, POSTGRESQL -> this.jdbiRepository.get();
        };
    }
}
