package sword.database;

/**
 * Database that includes the new methods to change the current database
 * schema.
 *
 * Implementations of this class are intended for schema upgrading purposes,
 * like adding or removing tables or indexes. This can be required to upgrade databases with previous
 */
public interface MutableSchemaDatabase extends Database {
    void createTable(DbTable table);
    void createIndex(DbIndex index);
}
