package io.github.namiuni.paperplugintemplate.common.infrastructure.storage;

import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface JdbiConfigurer {

    void configure(Jdbi jdbi, StorageDialect dialect);
}
