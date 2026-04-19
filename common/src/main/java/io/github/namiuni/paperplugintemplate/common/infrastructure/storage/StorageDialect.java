package io.github.namiuni.paperplugintemplate.common.infrastructure.storage;

import io.github.namiuni.paperplugintemplate.common.utilities.UUIDCodec;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface StorageDialect permits StorageDialect.MySQL, StorageDialect.PostgreSQL {

    String migrationLocation();

    QualifiedArgumentFactory uuidArgumentFactory();

    @NullMarked
    record MySQL() implements StorageDialect {

        private static final String LOCATION = "storage/migration/mysql";

        @Override
        public String migrationLocation() {
            return LOCATION;
        }

        @Override
        public QualifiedArgumentFactory uuidArgumentFactory() {
            return (_, value, _) -> {
                if (!(value instanceof final UUID uuid)) {
                    return Optional.empty();
                }
                final byte[] bytes = UUIDCodec.uuidToBytes(uuid);
                return Optional.of((pos, stmt, _) -> stmt.setBytes(pos, bytes));
            };
        }
    }

    @NullMarked
    record PostgreSQL() implements StorageDialect {

        private static final String LOCATION = "storage/migration/postgresql";

        @Override
        public String migrationLocation() {
            return LOCATION;
        }

        @Override
        public QualifiedArgumentFactory uuidArgumentFactory() {
            return (_, value, _) -> {
                if (!(value instanceof UUID)) {
                    return Optional.empty();
                }
                return Optional.of((pos, stmt, _) -> stmt.setObject(pos, value));
            };
        }
    }
}
