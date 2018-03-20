package sword.langbook3.android.db;

public abstract class DbTable implements DbView {
    private final String _name;
    private final DbColumn[] _columns;

    protected DbTable(String name, DbColumn... columns) {
        _name = name;
        _columns = new DbColumn[columns.length + 1];

        System.arraycopy(columns, 0, _columns, 1, columns.length);
        _columns[0] = new DbIdColumn();
    }

    public String getName() {
        return _name;
    }

    @Override
    public int getColumnCount() {
        return _columns.length;
    }

    public int getIdColumnIndex() {
        return 0;
    }

    @Override
    public DbColumn getColumn(int index) {
        return _columns[index];
    }

    public String getColumnName(int index) {
        return getColumn(index).getName();
    }

    public String getColumnType(int index) {
        return _columns[index].getSqlType();
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
