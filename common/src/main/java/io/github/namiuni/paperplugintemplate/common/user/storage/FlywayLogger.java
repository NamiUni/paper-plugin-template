package io.github.namiuni.paperplugintemplate.common.user.storage;

import jakarta.inject.Inject;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FlywayLogger implements LogCreator {

    private final ComponentLogger logger;

    @Inject
    private FlywayLogger(ComponentLogger logger) {
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
