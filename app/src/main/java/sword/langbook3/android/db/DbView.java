package sword.langbook3.android.db;

import sword.collections.ImmutableList;

public interface DbView {
    ImmutableList<DbColumn> columns();

    /**
     * Return this instance typed as DbTable, or null if it's not a table
     */
    DbTable asTable();

    /**
     * Return this instance typed as DbQuery, or null if it's not a query
     */
    DbQuery asQuery();
}
