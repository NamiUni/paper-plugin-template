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
package io.github.namiuni.paperplugintemplate.common.user.storage;

import jakarta.inject.Inject;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;
import org.jspecify.annotations.NullMarked;

/// Flyway [LogCreator] that routes migration log output through Adventure's
/// [ComponentLogger].
///
/// Flyway uses its own logging abstraction and resolves a [LogCreator] at
/// startup via [org.flywaydb.core.api.logging.LogFactory#setLogCreator].
/// Registering this implementation redirects all Flyway log output —
/// schema validation messages, applied migration notices, and repair
/// diagnostics — into the plugin's logger hierarchy, keeping the console
/// output consistent and avoiding the default Flyway SLF4J binding from
/// leaking into the server classpath.
///
/// ## Activation
///
/// Pass an instance to [org.flywaydb.core.api.logging.LogFactory#setLogCreator]
/// before calling [org.flywaydb.core.Flyway#migrate] and restore the
/// default by passing `null` afterward:
///
/// ```java
/// LogFactory.setLogCreator(flywayLogger);
/// flyway.migrate();
/// LogFactory.setLogCreator(null);
/// ```
///
/// ## Thread safety
///
/// [ComponentLogger] implementations are required to be thread-safe by the
/// SLF4J contract. This class carries no mutable state beyond the injected
/// logger reference; [#createLogger] is therefore safe to call from any
/// thread, including Flyway's internal migration executor.
@NullMarked
public final class FlywayLogger implements LogCreator {

    private final ComponentLogger logger;

    /// Constructs a new [FlywayLogger] backed by the given [ComponentLogger].
    ///
    /// @param logger the plugin's component-aware logger; must not be `null`
    @Inject
    private FlywayLogger(final ComponentLogger logger) {
        this.logger = logger;
    }

    /// Creates and returns a [Log] instance that prefixes each message with
    /// the simple name of `clazz` and forwards it to the underlying
    /// [ComponentLogger] at the corresponding log level.
    ///
    /// The prefix format is `" [ClassName] message"`, matching Flyway's
    /// conventional output style while keeping messages attributable to
    /// their originating Flyway component.
    ///
    /// @param  clazz the Flyway class requesting a logger; used as a prefix
    ///               label — never `null`
    /// @return a stateless [Log] implementation bound to `clazz`; the
    ///         returned instance is safe to use from any thread
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
