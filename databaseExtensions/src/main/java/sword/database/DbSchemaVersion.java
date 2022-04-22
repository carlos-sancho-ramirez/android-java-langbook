package sword.database;

import sword.collections.ImmutableList;

public interface DbSchemaVersion extends DbSchema {
    /**
     * Return the previous database schema version.
     * <p>
     * This method should never return null. If this is the first version of a
     * database, return {@link #EMPTY} instead.
     *
     * @return The previous version of the database schema
     */
    DbSchema previousVersion();

    /**
     * Return the new tables included in this schema version.
     * <p>
     * This method should never return null. If there is no new tables, then return {@link ImmutableList#empty()} instead.
     *
     * @return A list containing the new tables for this schema version.
     */
    ImmutableList<DbTable> newTables();

    /**
     * Return the new indexes included in this schema version.
     * <p>
     * This method should never return null. If there is no new indexes, then return {@link ImmutableList#empty()} instead.
     *
     * @return A list containing the new indexes for this schema version.
     */
    ImmutableList<DbIndex> newIndexes();

    @Override
    default ImmutableList<DbTable> tables() {
        return previousVersion().tables().appendAll(newTables());
    }

    @Override
    default ImmutableList<DbIndex> indexes() {
        return previousVersion().indexes().appendAll(newIndexes());
    }

    // TODO: Move this to the DbSchema instead
    DbSchema EMPTY = new DbSchema() {

        @Override
        public ImmutableList<DbTable> tables() {
            return ImmutableList.empty();
        }

        @Override
        public ImmutableList<DbIndex> indexes() {
            return ImmutableList.empty();
        }
    };
}
