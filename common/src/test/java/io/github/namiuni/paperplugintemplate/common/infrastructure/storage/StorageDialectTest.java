package io.github.namiuni.paperplugintemplate.common.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.namiuni.paperplugintemplate.common.utilities.UUIDCodec;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.jdbi.v3.core.argument.QualifiedArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@NullMarked
class StorageDialectTest {

    private static final UUID TEST_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final ConfigRegistry CONFIG = mock(ConfigRegistry.class);
    private static final StatementContext STMT_CTX = mock(StatementContext.class);

    @Nested
    class MySQLDialectTest {

        private final StorageDialect.MySQL dialect = new StorageDialect.MySQL();

        @Test
        void migrationLocationPointsToMySQLDirectory() {
            assertEquals("storage/migration/mysql", this.dialect.migrationLocation());
        }

        @Test
        void uuidArgumentFactoryReturnsEmptyForNonUUIDValue() {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();

            assertTrue(factory.build(null, "not-a-uuid", CONFIG).isEmpty());
        }

        @Test
        void uuidArgumentFactoryReturnsEmptyForNullValue() {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();

            assertTrue(factory.build(null, null, CONFIG).isEmpty());
        }

        @Test
        void uuidArgumentFactoryReturnsArgumentForUUIDValue() {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();

            assertTrue(factory.build(null, TEST_UUID, CONFIG).isPresent());
        }

        @Test
        void uuidArgumentSetsBytesOnPreparedStatement() throws SQLException {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();
            final PreparedStatement stmt = mock(PreparedStatement.class);

            factory.build(null, TEST_UUID, CONFIG)
                    .orElseThrow()
                    .apply(1, stmt, STMT_CTX);

            verify(stmt).setBytes(1, UUIDCodec.uuidToBytes(TEST_UUID));
        }

        @Test
        void uuidArgumentPreservesUUIDBytesRoundtrip() throws SQLException {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();
            final byte[][] captured = new byte[1][];
            final PreparedStatement stmt = mock(PreparedStatement.class);
            Mockito.doAnswer(inv -> {
                captured[0] = inv.getArgument(1);
                return null;
            }).when(stmt).setBytes(1, UUIDCodec.uuidToBytes(TEST_UUID));

            factory.build(null, TEST_UUID, CONFIG)
                    .orElseThrow()
                    .apply(1, stmt, STMT_CTX);

            assertEquals(TEST_UUID, UUIDCodec.uuidFromBytes(UUIDCodec.uuidToBytes(TEST_UUID)));
        }
    }

    @Nested
    class PostgreSQLDialectTest {

        private final StorageDialect.PostgreSQL dialect = new StorageDialect.PostgreSQL();

        @Test
        void migrationLocationPointsToPostgreSQLDirectory() {
            assertEquals("storage/migration/postgresql", this.dialect.migrationLocation());
        }

        @Test
        void uuidArgumentFactoryReturnsEmptyForNonUUIDValue() {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();

            assertTrue(factory.build(null, "not-a-uuid", CONFIG).isEmpty());
        }

        @Test
        void uuidArgumentFactoryReturnsEmptyForNullValue() {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();

            assertTrue(factory.build(null, null, CONFIG).isEmpty());
        }

        @Test
        void uuidArgumentFactoryReturnsArgumentForUUIDValue() {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();

            assertTrue(factory.build(null, TEST_UUID, CONFIG).isPresent());
        }

        @Test
        void uuidArgumentSetsObjectOnPreparedStatement() throws SQLException {
            final QualifiedArgumentFactory factory = this.dialect.uuidArgumentFactory();
            final PreparedStatement stmt = mock(PreparedStatement.class);

            factory.build(null, TEST_UUID, CONFIG)
                    .orElseThrow()
                    .apply(1, stmt, STMT_CTX);

            verify(stmt).setObject(1, TEST_UUID);
        }
    }
}
