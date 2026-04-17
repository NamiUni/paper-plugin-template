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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage;

import io.github.namiuni.paperplugintemplate.common.infrastructure.configuration.configurations.PrimaryConfiguration;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.json.JsonUserRepository;
import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.sql.JdbiUserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UserRepositoryProvider implements Provider<UserRepository> {

    private final Provider<PrimaryConfiguration> primaryConfig;
    private final Provider<JsonUserRepository> jsonRepository;
    private final Provider<JdbiUserRepository> jdbiRepository;

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

    @Override
    public UserRepository get() {
        final PrimaryConfiguration.Storage storage = this.primaryConfig.get().storage();
        return switch (storage.type()) {
            case JSON -> this.jsonRepository.get();
            case H2, MYSQL, POSTGRESQL -> this.jdbiRepository.get();
        };
    }
}
