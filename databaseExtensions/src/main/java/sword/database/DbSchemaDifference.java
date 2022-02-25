package sword.database;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;

public final class DbSchemaDifference {
    private final DbSchema _oldSchema;
    private final DbSchema _newSchema;

    DbSchemaDifference(DbSchema oldSchema, DbSchema newSchema) {
        if (oldSchema == null || newSchema == null) {
            throw new IllegalArgumentException();
        }

        _oldSchema = oldSchema;
        _newSchema = newSchema;
    }

    public ImmutableList<DbTable> newTables() {
        final ImmutableSet<DbTable> oldTables = _oldSchema.tables().toSet();
        return _newSchema.tables().filterNot(oldTables::contains);
    }

    public ImmutableList<DbIndex> newIndexes() {
        final ImmutableSet<DbIndex> oldIndexes = _oldSchema.indexes().toSet();
        return _newSchema.indexes().filterNot(oldIndexes::contains);
    }
}
