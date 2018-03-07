package sword.langbook3.android.db;

import java.util.ArrayList;

public final class DbQuery {

    // TODO: Remove all SQLite related code from here

    private final DbTable[] _tables;
    private final int[] _joinPairs;
    private final int[] _restrictionKeys;
    private final DbValue[] _restrictionValues;
    private final int[] _selectedColumns;

    private final transient DbColumn[] _joinColumns;

    private DbQuery(DbTable[] tables, int[] joinPairs, int[] restrictionKeys, DbValue[] restrictionValues, int[] selectedColumns) {

        if (tables == null || tables.length == 0 || selectedColumns == null || selectedColumns.length == 0) {
            throw new IllegalArgumentException();
        }

        int joinColumnCount = tables[0].getColumnCount();
        for (int i = 1; i < tables.length; i++) {
            joinColumnCount += tables[i].getColumnCount();
        }

        final DbColumn[] joinColumns = new DbColumn[joinColumnCount];
        int index = 0;
        for (int i = 0; i < tables.length; i++) {
            final int columnCount = tables[i].getColumnCount();
            for (int j = 0; j < columnCount; j++) {
                joinColumns[index++] = tables[i].getColumn(j);
            }
        }

        if (joinPairs == null) {
            joinPairs = new int[0];
        }

        if ((joinPairs.length & 1) == 1 || !hasValidValues(joinPairs, 0, joinColumnCount - 1)) {
            throw new IllegalArgumentException("Invalid column join pairs");
        }

        if (!hasValidSetValues(selectedColumns, 0, joinColumnCount - 1) || selectedColumns.length > joinColumnCount) {
            throw new IllegalArgumentException("Invalid column selection");
        }

        if (restrictionKeys == null) {
            restrictionKeys = new int[0];
        }

        if (restrictionValues == null) {
            restrictionValues = new DbValue[0];
        }

        final int restrictionCount = restrictionKeys.length;
        if (restrictionCount != restrictionValues.length) {
            throw new IllegalArgumentException("Restriction key and value should match in length");
        }

        if (!hasValidSetValues(restrictionKeys, 0, joinColumnCount - 1)) {
            throw new IllegalArgumentException("Selected columns has repeated values");
        }

        for (int i = 0; i < restrictionCount; i++) {
            final DbValue value = restrictionValues[i];
            if (value == null || joinColumns[restrictionKeys[i]].isText() != value.isText()) {
                throw new IllegalArgumentException();
            }
        }

        _tables = tables;
        _joinPairs = joinPairs;
        _restrictionKeys = restrictionKeys;
        _restrictionValues = restrictionValues;
        _selectedColumns = selectedColumns;

        _joinColumns = joinColumns;
    }

    private int getTableIndexFromColumnIndex(int column) {
        int count = 0;
        int tableIndex = 0;
        while (column >= count) {
            count += _tables[tableIndex++].getColumnCount();
        }
        return tableIndex - 1;
    }

    private DbTable getTable(int index) {
        return _tables[index];
    }

    public DbColumn[] getSelectedColumns() {
        final DbColumn[] result = new DbColumn[_selectedColumns.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = _joinColumns[_selectedColumns[i]];
        }

        return result;
    }

    private String getSqlColumnName(int index) {
        return "J" + getTableIndexFromColumnIndex(index) + '.' + _joinColumns[index].getName();
    }

    private String getSqlSelectedColumnName(int index) {
        return getSqlColumnName(_selectedColumns[index]);
    }

    private String getSqlSelectedColumnNames() {
        final StringBuilder sb = new StringBuilder(getSqlSelectedColumnName(0));
        for (int i = 1; i < _selectedColumns.length; i++) {
            sb.append(',').append(getSqlSelectedColumnName(i));
        }

        return sb.toString();
    }

    private String getSqlFromClause() {
        final StringBuilder sb = new StringBuilder(" FROM ");
        sb.append(_tables[0].getName()).append(" AS J0");

        for (int i = 1; i < _tables.length; i++) {
            final int pairCount = _joinPairs.length / 2;
            int left = -1;
            int right = -1;
            for (int j = 0; j < pairCount; i++) {
                if (getTableIndexFromColumnIndex(_joinPairs[2 * j + 1]) == i && getTableIndexFromColumnIndex(_joinPairs[2 * j]) < i) {
                    left = _joinPairs[2 * j];
                    right = _joinPairs[2 * j + 1];
                    break;
                }
            }

            if (left < 0) {
                throw new AssertionError("No pair found for table " + i);
            }

            sb.append(" JOIN ").append(_tables[i].getName()).append(" AS J").append(i);
            sb.append(" ON J").append(getTableIndexFromColumnIndex(left)).append('.');
            sb.append(_joinColumns[left].getName()).append("=J").append(i);
            sb.append('.').append(_joinColumns[right].getName());
        }

        return sb.toString();
    }

    private String getSqlWhereClause() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < _restrictionKeys.length; i++) {
            if (i == 0) {
                sb.append(" WHERE ");
            }
            else {
                sb.append(" AND ");
            }
            sb.append(getSqlColumnName(_restrictionKeys[i]))
                    .append('=').append(_restrictionValues[i].toSql());
        }

        return sb.toString();
    }

    public String toSql() {
        return "SELECT " + getSqlSelectedColumnNames() + getSqlFromClause() + getSqlWhereClause();
    }

    private static boolean hasValidValues(int[] values, int min, int max) {
        final int length = (values != null)? values.length : 0;
        if (length < 1) {
            return true;
        }

        for (int i = 0; i < length; i++) {
            final int iValue = values[i];
            if (iValue < min || iValue > max) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasValidSetValues(int[] values, int min, int max) {
        final int length = (values != null)? values.length : 0;
        if (length < 1) {
            return true;
        }
        else if (length == 1) {
            return values[0] >= min && values[0] <= max;
        }

        for (int i = 0; i < length - 1; i++) {
            final int iValue = values[i];
            if (iValue < min || iValue > max) {
                return false;
            }

            for (int j = i + 1; j < length; j++) {
                if (values[i] == values[j]) {
                    return false;
                }
            }
        }

        return values[length - 1] >= min && values[length - 1] <= max;
    }

    public static final class Builder {
        private final ArrayList<DbTable> _tables = new ArrayList<>();
        private final ArrayList<Integer> _joinPairs = new ArrayList<>();
        private final ArrayList<Integer> _restrictionKeys = new ArrayList<>();
        private final ArrayList<DbValue> _restrictionValues = new ArrayList<>();
        private int _joinColumnCount;

        public Builder(DbTable table) {
            _tables.add(table);
            _joinColumnCount = table.getColumnCount();
        }

        public Builder where(int columnIndex, DbValue value) {
            _restrictionKeys.add(columnIndex);
            _restrictionValues.add(value);
            return this;
        }

        public Builder where(int columnIndex, int value) {
            return where(columnIndex, new DbIntValue(value));
        }

        public Builder join(DbTable table, int left, int newTableColumnIndex) {
            final int tableColumnCount = table.getColumnCount();
            if (left < 0 || left >= _joinColumnCount || newTableColumnIndex < 0 || newTableColumnIndex >= tableColumnCount) {
                throw new IndexOutOfBoundsException();
            }

            _joinPairs.add(left);
            _joinPairs.add(_joinColumnCount + newTableColumnIndex);
            _tables.add(table);
            _joinColumnCount += tableColumnCount;

            return this;
        }

        public DbQuery select(int... selection) {
            final DbTable[] tables = new DbTable[_tables.size()];
            final int[] joinPairs = new int[_joinPairs.size()];
            final int restrictionPairs = _restrictionKeys.size();
            final int[] keys = new int[restrictionPairs];
            final DbValue[] values = new DbValue[restrictionPairs];

            for (int i = 0; i < restrictionPairs; i++) {
                keys[i] = _restrictionKeys.get(i);
            }
            _restrictionValues.toArray(values);
            _tables.toArray(tables);

            for (int i = 0; i < _joinPairs.size(); i++) {
                joinPairs[i] = _joinPairs.get(i);
            }

            return new DbQuery(tables, joinPairs, keys, values, selection);
        }
    }
}
