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

import jakarta.inject.Inject;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FlywayLogger implements LogCreator {

    private final ComponentLogger logger;

    @Inject
    FlywayLogger(final ComponentLogger logger) {
        this.logger = logger;
    }

    @Override
    public Log createLogger(final Class<?> clazz) {
        return new Log() {

            @Override
            public void debug(final String message) {
                FlywayLogger.this.logger.debug(" [{}] {}", clazz.getSimpleName(), message);
            }

            @Override
            public void info(final String message) {
                FlywayLogger.this.logger.info(" [{}] {}", clazz.getSimpleName(), message);
            }

            @Override
            public void warn(final String message) {
                FlywayLogger.this.logger.warn(" [{}] {}", clazz.getSimpleName(), message);
            }

            @Override
            public void error(final String message) {
                FlywayLogger.this.logger.error(" [{}] {}", clazz.getSimpleName(), message);
            }

            @Override
            public void error(final String message, final Exception exception) {
                FlywayLogger.this.logger.error(" [{}] {}", clazz.getSimpleName(), message, exception);
            }

            @Override
            public void notice(final String message) {
                FlywayLogger.this.logger.info(" [{}] (Notice) {}", clazz.getSimpleName(), message);
            }
        };
    }
}
