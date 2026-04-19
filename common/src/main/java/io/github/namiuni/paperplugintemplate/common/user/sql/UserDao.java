package io.github.namiuni.paperplugintemplate.common.user.sql;

import io.github.namiuni.paperplugintemplate.common.infrastructure.storage.StorageDialect;
import io.github.namiuni.paperplugintemplate.common.user.UserRecord;
import io.github.namiuni.paperplugintemplate.common.utilities.UUIDCodec;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UserDao extends SqlObject {

    @SqlQuery("SELECT uuid, name, last_seen FROM users WHERE uuid = :uuid")
    Optional<UserRecord> findByUuid(@Bind("uuid") UUID uuid);

    @SqlUpdate("INSERT INTO users (uuid, name, last_seen) VALUES (:uuid, :name, :lastSeen)")
    void insert(@BindMethods UserRecord userRecord);

    @SqlUpdate("UPDATE users SET name = :name, last_seen = :lastSeen WHERE uuid = :uuid")
    int update(@BindMethods UserRecord userRecord);

    @SqlUpdate("DELETE FROM users WHERE uuid = :uuid")
    void deleteByUuid(@Bind("uuid") UUID uuid);

    @Transaction
    default void upsert(final UserRecord userRecord) {
        if (this.update(userRecord) == 0) {
            try {
                this.insert(userRecord);
            } catch (final Exception _) {
                this.update(userRecord);
            }
        }
    }

    static RowMapper<UserRecord> rowMapper(final StorageDialect dialect) {
        return switch (dialect) {
            case StorageDialect.MySQL() -> (rs, _) -> new UserRecord(
                    UUIDCodec.uuidFromBytes(rs.getBytes("uuid")),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
            case StorageDialect.PostgreSQL() -> (rs, _) -> new UserRecord(
                    rs.getObject("uuid", UUID.class),
                    rs.getString("name"),
                    Instant.ofEpochMilli(rs.getLong("last_seen"))
            );
        };
    }
}
