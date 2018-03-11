package sword.langbook3.android.db;

import java.util.ArrayList;

public final class DbQuery {

    private static final int FLAG_COLUMN_FUNCTION_MAX = 0x10000;

    private final DbTable[] _tables;
    private final int[] _joinPairs;
    private final int[] _restrictionKeys;
    private final DbValue[] _restrictionValues;
    private final int[] _selectedColumns;
    private final int[] _selectionFunctions;

    private final transient DbColumn[] _joinColumns;

    private DbQuery(DbTable[] tables, int[] joinPairs, int[] restrictionKeys, DbValue[] restrictionValues, int[] selectedColumns, int[] selectionFunctions) {

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

        if (selectionFunctions == null) {
            selectionFunctions = new int[selectedColumns.length];
        }

        if (selectedColumns.length != selectionFunctions.length) {
            throw new IllegalArgumentException();
        }

        for (int func : selectionFunctions) {
            if (func != 0 && func != FLAG_COLUMN_FUNCTION_MAX) {
                throw new IllegalArgumentException("Unexpected aggregate function");
            }
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
        _selectionFunctions = selectionFunctions;

        _joinColumns = joinColumns;
    }

    public int getTableCount() {
        return _tables.length;
    }

    public DbTable getTable(int index) {
        return _tables[index];
    }

    public int getTableIndexFromColumnIndex(int column) {
        int count = 0;
        int tableIndex = 0;
        while (column >= count) {
            count += _tables[tableIndex++].getColumnCount();
        }
        return tableIndex - 1;
    }

    public DbColumn getJoinColumn(int index) {
        return _joinColumns[index];
    }

    public int getSelectedColumnCount() {
        return _selectedColumns.length;
    }

    public int getSelectedColumnIndex(int index) {
        return _selectedColumns[index];
    }

    public static final class JoinColumnPair {

        private final int _left;
        private final int _right;

        private JoinColumnPair(int left, int right) {
            _left = left;
            _right = right;
        }

        public int getLeft() {
            return _left;
        }

        public int getRight() {
            return _right;
        }
    }

    public JoinColumnPair getJoinPair(int index) {
        final int pairCount = _joinPairs.length / 2;
        for (int j = 0; j < pairCount; j++) {
            if (getTableIndexFromColumnIndex(_joinPairs[2 * j + 1]) == index + 1 && getTableIndexFromColumnIndex(_joinPairs[2 * j]) < index + 1) {
                return new JoinColumnPair(_joinPairs[2 * j], _joinPairs[2 * j + 1]);
            }
        }

        throw new AssertionError("No pair found for table " + (index + 1));
    }

    public DbColumn getSelectedColumn(int index) {
        return _joinColumns[getSelectedColumnIndex(index)];
    }

    public DbColumn[] getSelectedColumns() {
        final DbColumn[] result = new DbColumn[getSelectedColumnCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = getSelectedColumn(i);
        }

        return result;
    }

    public int getRestrictionCount() {
        return _restrictionKeys.length;
    }

    public int getRestrictedColumnIndex(int index) {
        return _restrictionKeys[index];
    }

    public DbValue getRestriction(int index) {
        return _restrictionValues[index];
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

    public boolean isMaxAggregateFunctionSelection(int selectionIndex) {
        return (_selectionFunctions[selectionIndex] & FLAG_COLUMN_FUNCTION_MAX) != 0;
    }

    public static int max(int index) {
        return FLAG_COLUMN_FUNCTION_MAX | index;
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

        public Builder where(int columnIndex, String value) {
            return where(columnIndex, new DbStringValue(value));
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
            final int[] filteredSelection = new int[selection.length];
            final int[] funcSelection = new int[selection.length];
            final DbValue[] values = new DbValue[restrictionPairs];

            for (int i = 0; i < restrictionPairs; i++) {
                keys[i] = _restrictionKeys.get(i);
            }
            _restrictionValues.toArray(values);
            _tables.toArray(tables);

            for (int i = 0; i < _joinPairs.size(); i++) {
                joinPairs[i] = _joinPairs.get(i);
            }

            for (int i = 0; i < selection.length; i++) {
                filteredSelection[i] = (FLAG_COLUMN_FUNCTION_MAX - 1) & selection[i];
                if ((selection[i] & FLAG_COLUMN_FUNCTION_MAX) != 0) {
                    funcSelection[i] = FLAG_COLUMN_FUNCTION_MAX;
                }
            }

            return new DbQuery(tables, joinPairs, keys, values, filteredSelection, funcSelection);
        }
    }
}
