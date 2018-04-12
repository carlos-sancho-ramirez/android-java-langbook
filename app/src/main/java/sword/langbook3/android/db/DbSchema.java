package sword.langbook3.android.db;

import sword.collections.ImmutableList;

public interface DbSchema {
    ImmutableList<DbTable> tables();
    ImmutableList<DbIndex> indexes();
}
