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
package io.github.namiuni.paperplugintemplate.common.infrastructure.storage.sql;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.logging.LogFactory;
import org.jspecify.annotations.NullMarked;

@Singleton
@NullMarked
public final class DatabaseMigrator {

    private final Flyway flyway;
    private final FlywayLogger flywayLogger;
    private final ComponentLogger logger;

    @Inject
    private DatabaseMigrator(
            final Flyway flyway,
            final FlywayLogger flywayLogger,
            final ComponentLogger logger
    ) {
        this.flyway = flyway;
        this.flywayLogger = flywayLogger;
        this.logger = logger;
    }

    public void migrate() {
        this.logger.info("Running Flyway repair & migration...");
        LogFactory.setLogCreator(this.flywayLogger);
        try {
            this.flyway.repair();
            this.flyway.migrate();
        } finally {
            LogFactory.setLogCreator(null);
        }
    }
}
