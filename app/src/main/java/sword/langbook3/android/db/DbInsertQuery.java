package sword.langbook3.android.db;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class DbInsertQuery {

    private final DbTable _table;
    private final int[] _columns;
    private final DbValue[] _values;

    private DbInsertQuery(DbTable table, int[] columns, DbValue[] values) {
        _table = table;
        _columns = columns;
        _values = values;
    }

    public DbTable getTable() {
        return _table;
    }

    public int getColumnCount() {
        return _columns.length;
    }

    public DbColumn getColumn(int index) {
        return _table.getColumn(_columns[index]);
    }

    public DbValue getValue(int index) {
        return _values[index];
    }

    public static final class Builder {
        private final DbTable _table;
        private final Map<Integer, DbValue> _map = new HashMap<>();

        public Builder(DbTable table) {
            if (table == null) {
                throw new IllegalArgumentException();
            }

            _table = table;
        }

        private DbInsertQuery.Builder put(int column, DbValue value) {
            if (column < 0 || column >= _table.getColumnCount() || _map.containsKey(column)) {
                throw new IllegalArgumentException();
            }

            _map.put(column, value);
            return this;
        }

        public Builder put(int column, int value) {
            return put(column, new DbIntValue(value));
        }

        public Builder put(int column, String value) {
            return put(column, new DbStringValue(value));
        }

        public DbInsertQuery build() {
            final int size = _map.size();
            final int[] keys = new int[size];

            int i = 0;
            for (int key : _map.keySet()) {
                keys[i++] = key;
            }
            Arrays.sort(keys);

            final DbValue[] values = new DbValue[size];
            for (i = 0; i < size; i++) {
                values[i] = _map.get(keys[i]);
            }

            return new DbInsertQuery(_table, keys, values);
        }
    }
}
