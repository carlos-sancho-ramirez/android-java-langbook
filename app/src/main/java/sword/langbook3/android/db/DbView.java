package sword.langbook3.android.db;

public interface DbView {
    int getColumnCount();
    DbColumn getColumn(int index);

    /**
     * Return this instance typed as DbTable, or null if it's not a table
     */
    DbTable asTable();

    /**
     * Return this instance typed as DbQuery, or null if it's not a query
     */
    DbQuery asQuery();
}
