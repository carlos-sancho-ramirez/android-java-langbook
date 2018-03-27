package sword.langbook3.android.db;

import sword.collections.ImmutableList;

public class DbTable implements DbView {
    private final String _name;
    private final ImmutableList<DbColumn> _columns;

    public DbTable(String name, DbColumn... columns) {
        final ImmutableList.Builder<DbColumn> builder = new ImmutableList.Builder<>();
        builder.add(new DbIdColumn());
        for (DbColumn column : columns) {
            builder.add(column);
        }

        _name = name;
        _columns = builder.build();
    }

    public String name() {
        return _name;
    }

    @Override
    public ImmutableList<DbColumn> columns() {
        return _columns;
    }

    public int getIdColumnIndex() {
        return 0;
    }

    @Override
    public DbTable asTable() {
        return this;
    }

    @Override
    public DbQuery asQuery() {
        return null;
    }
}
