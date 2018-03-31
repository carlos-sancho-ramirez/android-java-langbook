package sword.langbook3.android.db;

import java.util.ArrayList;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.MutableIntKeyMap;

public final class DbQuery implements DbView {

    private static final int FLAG_COLUMN_FUNCTION_MAX = 0x10000;
    private static final int FLAG_COLUMN_FUNCTION_CONCAT = 0x20000;

    private final DbView[] _tables;
    private final int[] _joinPairs;
    private final int[] _columnValueMatchingPairs;
    private final ImmutableIntKeyMap<DbValue> _restrictions;
    private final int[] _groupBy;
    private final int[] _orderBy;

    // keys of this maps are the column indexes, while value is the FLAG for the column selection function, or 0 if not aggregate function is required.
    private final ImmutableIntPairMap _selection;

    private final transient DbColumn[] _joinColumns;
    private transient ImmutableList<DbColumn> _columns;

    private DbQuery(DbView[] tables, int[] joinPairs, int[] columnValueMatchPairs,
            ImmutableIntKeyMap<DbValue> restrictions, int[] groupBy, int[] orderBy, ImmutableIntPairMap selection) {

        if (tables == null || tables.length == 0 || selection == null || selection.isEmpty()) {
            throw new IllegalArgumentException();
        }

        int joinColumnCount = tables[0].columns().size();
        for (int i = 1; i < tables.length; i++) {
            joinColumnCount += tables[i].columns().size();
        }

        final DbColumn[] joinColumns = new DbColumn[joinColumnCount];
        int index = 0;
        for (int i = 0; i < tables.length; i++) {
            for (DbColumn column : tables[i].columns()) {
                joinColumns[index++] = column;
            }
        }

        if (joinPairs == null) {
            joinPairs = new int[0];
        }

        if ((joinPairs.length & 1) == 1 || !hasValidValues(joinPairs, 0, joinColumnCount - 1)) {
            throw new IllegalArgumentException("Invalid column join pairs");
        }

        if (columnValueMatchPairs == null) {
            columnValueMatchPairs = new int[0];
        }

        if ((columnValueMatchPairs.length & 1) == 1 || !hasValidValues(columnValueMatchPairs, 0, joinColumnCount - 1)) {
            throw new IllegalArgumentException("Invalid column value matching pairs");
        }

        if (selection.keySet().min() < 0 || selection.keySet().max() >= joinColumnCount) {
            throw new IllegalArgumentException("Selected column indexes out of bounds");
        }

        for (ImmutableIntPairMap.Entry entry : selection) {
            final int func = entry.getValue();
            if (func != 0 && func != FLAG_COLUMN_FUNCTION_MAX && func != FLAG_COLUMN_FUNCTION_CONCAT) {
                throw new IllegalArgumentException("Unexpected aggregate function");
            }
        }

        if (restrictions == null) {
            restrictions = ImmutableIntKeyMap.empty();
        }

        final int restrictionCount = restrictions.size();
        final ImmutableIntSet restrictedColumns = restrictions.keySet();
        if (restrictionCount > 0 && (restrictedColumns.min() < 0 || restrictedColumns.max() >= joinColumnCount)) {
            throw new IllegalArgumentException("Restricted column indexes out of bounds");
        }

        for (int i = 0; i < restrictionCount; i++) {
            final DbValue value = restrictions.valueAt(i);
            if (value == null || joinColumns[restrictions.keyAt(i)].isText() != value.isText()) {
                throw new IllegalArgumentException();
            }
        }

        if (groupBy == null) {
            groupBy = new int[0];
        }

        if (!hasValidSetValues(groupBy, 0, joinColumnCount - 1)) {
            throw new IllegalArgumentException("Invalid grouping parameters");
        }

        if (orderBy == null) {
            orderBy = new int[0];
        }

        if (!hasValidSetValues(orderBy, 0, joinColumnCount - 1)) {
            throw new IllegalArgumentException("Invalid ordering parameters");
        }

        _tables = tables;
        _joinPairs = joinPairs;
        _columnValueMatchingPairs = columnValueMatchPairs;
        _restrictions = restrictions;
        _groupBy = groupBy;
        _orderBy = orderBy;
        _selection = selection;

        _joinColumns = joinColumns;
    }

    public int getTableCount() {
        return _tables.length;
    }

    public DbView getView(int index) {
        return _tables[index];
    }

    public int getTableIndexFromColumnIndex(int column) {
        int count = 0;
        int tableIndex = 0;
        while (column >= count) {
            count += _tables[tableIndex++].columns().size();
        }
        return tableIndex - 1;
    }

    public DbColumn getJoinColumn(int index) {
        return _joinColumns[index];
    }

    public ImmutableIntSet selection() {
        return _selection.keySet();
    }

    @Override
    public ImmutableList<DbColumn> columns() {
        if (_columns == null) {
            final ImmutableList.Builder<DbColumn> builder = new ImmutableList.Builder<>();
            for (int selected : _selection.keySet()) {
                builder.add(_joinColumns[selected]);
            }

            _columns = builder.build();
        }

        return _columns;
    }

    @Override
    public DbTable asTable() {
        return null;
    }

    @Override
    public DbQuery asQuery() {
        return this;
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

    public int getColumnValueMatchPairCount() {
        return _columnValueMatchingPairs.length / 2;
    }

    public JoinColumnPair getColumnValueMatchPair(int index) {
        return new JoinColumnPair(_columnValueMatchingPairs[index * 2], _columnValueMatchingPairs[index * 2 + 1]);
    }

    public ImmutableSet<JoinColumnPair> columnValueMatchPairs() {
        final int count = _columnValueMatchingPairs.length / 2;
        final ImmutableSet.Builder<JoinColumnPair> builder = new ImmutableSet.Builder<>();
        for (int index = 0; index < count; index++) {
            builder.add(new JoinColumnPair(_columnValueMatchingPairs[index * 2], _columnValueMatchingPairs[index * 2 + 1]));
        }

        return builder.build();
    }

    public int getRestrictionCount() {
        return _restrictions.size();
    }

    public int getRestrictedColumnIndex(int index) {
        return _restrictions.keyAt(index);
    }

    public DbValue getRestriction(int index) {
        return _restrictions.valueAt(index);
    }

    public ImmutableIntKeyMap<DbValue> restrictions() {
        return _restrictions;
    }

    public int getGroupingCount() {
        return _groupBy.length;
    }

    public int getGrouping(int index) {
        return _groupBy[index];
    }

    public int getOrderingCount() {
        return _orderBy.length;
    }

    public int getOrdering(int index) {
        return _orderBy[index];
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
        return (_selection.valueAt(selectionIndex) & FLAG_COLUMN_FUNCTION_MAX) != 0;
    }

    public boolean isConcatAggregateFunctionSelection(int selectionIndex) {
        return (_selection.valueAt(selectionIndex) & FLAG_COLUMN_FUNCTION_CONCAT) != 0;
    }

    public static int max(int index) {
        return FLAG_COLUMN_FUNCTION_MAX | index;
    }

    public static int concat(int index) {
        return FLAG_COLUMN_FUNCTION_CONCAT | index;
    }

    public static final class Builder {
        private final ArrayList<DbView> _tables = new ArrayList<>();
        private final ArrayList<Integer> _joinPairs = new ArrayList<>();
        private final MutableIntKeyMap<DbValue> _restrictions = MutableIntKeyMap.empty();
        private final ArrayList<Integer> _columnValueMatchPairs = new ArrayList<>();
        private int[] _groupBy;
        private int[] _orderBy;
        private int _joinColumnCount;

        public Builder(DbView table) {
            _tables.add(table);
            _joinColumnCount = table.columns().size();
        }

        public Builder where(int columnIndex, DbValue value) {
            _restrictions.put(columnIndex, value);
            return this;
        }

        public Builder where(int columnIndex, int value) {
            return where(columnIndex, new DbIntValue(value));
        }

        public Builder where(int columnIndex, String value) {
            return where(columnIndex, new DbStringValue(value));
        }

        public Builder whereColumnValueMatch(int leftColumnIndex, int rightColumnIndex) {
            if (leftColumnIndex >= rightColumnIndex) {
                throw new IllegalArgumentException("Wrong column matching condition");
            }

            _columnValueMatchPairs.add(leftColumnIndex);
            _columnValueMatchPairs.add(rightColumnIndex);
            return this;
        }

        public Builder join(DbTable table, int left, int newTableColumnIndex) {
            final int tableColumnCount = table.columns().size();
            if (left < 0 || left >= _joinColumnCount || newTableColumnIndex < 0 || newTableColumnIndex >= tableColumnCount) {
                throw new IndexOutOfBoundsException();
            }

            _joinPairs.add(left);
            _joinPairs.add(_joinColumnCount + newTableColumnIndex);
            _tables.add(table);
            _joinColumnCount += tableColumnCount;

            return this;
        }

        public Builder groupBy(int... columnIndexes) {
            if (_groupBy != null) {
                throw new UnsupportedOperationException("groupBy can only be called once per query");
            }

            _groupBy = columnIndexes;
            return this;
        }

        public Builder orderBy(int... columnIndexes) {
            if (_orderBy != null) {
                throw new UnsupportedOperationException("orderBy can only be called once per query");
            }

            _orderBy = columnIndexes;
            return this;
        }

        public DbQuery select(int... selection) {
            final DbView[] views = new DbView[_tables.size()];
            final int[] joinPairs = new int[_joinPairs.size()];
            final int[] columnValueMatchPairs = new int[_columnValueMatchPairs.size()];

            _tables.toArray(views);

            for (int i = 0; i < _joinPairs.size(); i++) {
                joinPairs[i] = _joinPairs.get(i);
            }

            for (int i = 0; i < _columnValueMatchPairs.size(); i++) {
                columnValueMatchPairs[i] = _columnValueMatchPairs.get(i);
            }

            final ImmutableIntPairMap.Builder selectionBuilder = new ImmutableIntPairMap.Builder();
            for (int i = 0; i < selection.length; i++) {
                int columnIndex = (FLAG_COLUMN_FUNCTION_MAX - 1) & selection[i];
                int func = selection[i] & ~(FLAG_COLUMN_FUNCTION_MAX - 1);
                selectionBuilder.put(columnIndex, func);
            }

            return new DbQuery(views, joinPairs, columnValueMatchPairs, _restrictions.toImmutable(),
                    _groupBy, _orderBy, selectionBuilder.build());
        }
    }
}
