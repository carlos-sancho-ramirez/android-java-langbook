package sword.langbook3.android.db;

import java.util.Iterator;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableList;
import sword.collections.MutableMap;

import static sword.langbook3.android.EqualUtils.equal;

/**
 * Implementation for an in-memory non-permanent database.
 * <p>
 * This class can be used for data that is valid while the process is still
 * alive, as a cache, or just for testing purposes.
 */
public final class MemoryDatabase implements Database {

    private final MutableMap<DbView, MutableIntKeyMap<ImmutableList<Object>>> _tableMap = MutableMap.empty();
    private final MutableMap<DbColumn, MutableMap<Object, Integer>> _indexes = MutableMap.empty();

    private static final class Result implements DbResult {

        private final ImmutableList<ImmutableList<Object>> _content;
        private int _index;

        Result(ImmutableList<ImmutableList<Object>> content) {
            _content = content;
        }

        @Override
        public void close() {
            _index = _content.size();
        }

        @Override
        public int getRemainingRows() {
            return _content.size() - _index;
        }

        @Override
        public boolean hasNext() {
            return _index < _content.size();
        }

        @Override
        public Row next() {
            return new Row(_content.get(_index++));
        }

        private static final class Row implements DbResult.Row {

            private final ImmutableList<Object> _list;

            Row(ImmutableList<Object> list) {
                _list = list;
            }

            @Override
            public DbValue get(int index) {
                final Object raw = _list.get(index);
                return (raw instanceof Integer)? new DbIntValue((Integer) raw) : new DbStringValue((String) raw);
            }
        }
    }

    private void applyJoins(MutableList<ImmutableList<Object>> result, DbQuery query) {
        final int tableCount = query.getTableCount();
        for (int viewIndex = 1; viewIndex < tableCount; viewIndex++) {
            final DbView view = query.getView(viewIndex);
            final DbQuery.JoinColumnPair joinPair = query.getJoinPair(viewIndex - 1);

            for (int row = 0; row < result.size(); row++) {
                final ImmutableList<Object> oldRow = result.get(row);
                final Object rawValue = oldRow.get(joinPair.left());
                final MutableIntKeyMap<ImmutableList<Object>> viewContent = _tableMap.get(view);
                final int targetJoinColumnIndex = joinPair.right() - oldRow.size();

                if (targetJoinColumnIndex == 0) {
                    final int id = (Integer) rawValue;
                    ImmutableList<Object> newRow = oldRow.append(id).appendAll(viewContent.get(id));
                    result.put(row, newRow);
                }
                else {
                    boolean somethingReplaced = false;
                    for (MutableIntKeyMap.Entry<ImmutableList<Object>> entry : viewContent.entries()) {
                        if (equal(entry.value().get(targetJoinColumnIndex - 1), rawValue)) {
                            final ImmutableList<Object> newRow = oldRow.append(entry.key())
                                    .appendAll(entry.value());
                            if (!somethingReplaced) {
                                result.put(row, newRow);
                                somethingReplaced = true;
                            }
                            else {
                                result.insert(++row, newRow);
                            }
                        }
                    }

                    if (!somethingReplaced) {
                        result.removeAt(row--);
                    }
                }
            }
        }
    }

    private void applyColumnMatchRestrictions(
            MutableList<ImmutableList<Object>> result, Iterable<DbQuery.JoinColumnPair> pairs) {
        for (DbQuery.JoinColumnPair pair : pairs) {
            final Iterator<ImmutableList<Object>> it = result.iterator();
            while (it.hasNext()) {
                final ImmutableList<Object> row = it.next();
                final boolean matchValue = equal(row.get(pair.left()), row.get(pair.right()));
                if (pair.mustMatch() && !matchValue || !pair.mustMatch() && matchValue) {
                    it.remove();
                }
            }
        }
    }

    private void applyRestrictions(
            MutableList<ImmutableList<Object>> result,
            ImmutableIntKeyMap<DbQuery.Restriction> restrictions) {
        for (ImmutableIntKeyMap.Entry<DbQuery.Restriction> entry : restrictions.entries()) {
            final DbValue value = entry.value().value;
            if (!value.isText() || entry.value().type == DbQuery.RestrictionTypes.EXACT) {
                final Object rawValue = value.isText()? value.toText() : value.toInt();

                final Iterator<ImmutableList<Object>> it = result.iterator();
                while (it.hasNext()) {
                    final ImmutableList<Object> register = it.next();
                    if (!rawValue.equals(register.get(entry.key()))) {
                        it.remove();
                    }
                }
            }
            else {
                final int type = entry.value().type;
                final Predicate2<String, String> cmpFunc =
                        (type == DbQuery.RestrictionStringTypes.ENDS_WITH)? String::endsWith :
                        (type == DbQuery.RestrictionStringTypes.STARTS_WITH)? String::startsWith :
                        String::contains;

                final Iterator<ImmutableList<Object>> it = result.iterator();
                while (it.hasNext()) {
                    final ImmutableList<Object> register = it.next();
                    if (!cmpFunc.apply(register.get(entry.key()).toString(), value.toText())) {
                        it.remove();
                    }
                }
            }
        }
    }

    private ImmutableList<Object> getGroup(ImmutableList<Object> reg, ImmutableIntSet grouping) {
        final ImmutableList.Builder<Object> groupBuilder = new ImmutableList.Builder<>();
        for (int columnIndex : grouping) {
            groupBuilder.add(reg.get(columnIndex));
        }
        return groupBuilder.build();
    }

    @Override
    public DbResult select(DbQuery query) {
        if (!query.ordering().isEmpty()) {
            throw new UnsupportedOperationException("Unimplemented");
        }

        final DbView view = query.getView(0);
        final MutableIntKeyMap<ImmutableList<Object>> content;
        if (_tableMap.containsKey(view)) {
            content = _tableMap.get(view);
        }
        else {
            content = MutableIntKeyMap.empty();
            _tableMap.put(view, content);
        }

        // Apply id restriction if found
        final ImmutableIntKeyMap<DbQuery.Restriction> restrictions = query.restrictions();
        final MutableList<ImmutableList<Object>> unselectedResult;
        final MutableList.Builder<ImmutableList<Object>> unselectedResultBuilder = new MutableList.Builder<>();
        if (restrictions.keySet().contains(0)) {
            final int id = restrictions.get(0).value.toInt();
            ImmutableList<Object> rawRegister = content.get(id, null);
            if (rawRegister != null) {
                unselectedResultBuilder.add(rawRegister.prepend(id));
            }
        }
        else {
            for (MutableIntKeyMap.Entry<ImmutableList<Object>> entry : content.entries()) {
                final ImmutableList<Object> register = entry.value().prepend(entry.key());
                unselectedResultBuilder.add(register);
            }
        }
        unselectedResult = unselectedResultBuilder.build();

        applyJoins(unselectedResult, query);
        applyColumnMatchRestrictions(unselectedResult, query.columnValueMatchPairs());
        applyRestrictions(unselectedResult, restrictions);

        // Apply column selection
        final int selectionCount = query.selection().size();
        boolean groupedSelection = query.getGroupingCount() != 0;
        if (!groupedSelection) {
            for (int i = 0; i < selectionCount; i++) {
                if (query.isMaxAggregateFunctionSelection(i) || query.isConcatAggregateFunctionSelection(i)) {
                    groupedSelection = true;
                    break;
                }
            }
        }

        if (groupedSelection) {
            final MutableMap<ImmutableList<Object>, Integer> groups = MutableMap.empty();
            for (int resultRow = 0; resultRow < unselectedResult.size(); resultRow++) {
                final ImmutableList<Object> reg = unselectedResult.get(resultRow);
                final ImmutableList<Object> group = getGroup(reg, query.grouping());
                if (!groups.containsKey(group)) {
                    groups.put(group, resultRow);
                    final ImmutableList.Builder<Object> rowBuilder = new ImmutableList.Builder<>();
                    for (int selectedColumn : query.selection()) {
                        rowBuilder.add(reg.get(selectedColumn));
                    }
                    unselectedResult.put(resultRow, rowBuilder.build());
                }
                else {
                    final int oldRowIndex = groups.get(group);
                    final ImmutableList<Object> oldRow = unselectedResult.get(oldRowIndex);
                    final ImmutableList.Builder<Object> rowBuilder = new ImmutableList.Builder<>();
                    for (int selectionIndex = 0; selectionIndex < selectionCount; selectionIndex++) {
                        Object rawValue = reg.get(query.selection().valueAt(selectionIndex));
                        if (query.isMaxAggregateFunctionSelection(selectionIndex)) {
                            int oldMax = (Integer) oldRow.get(selectionIndex);
                            int value = (Integer) rawValue;
                            rowBuilder.add(value > oldMax? value : oldMax);
                        }
                        else if (query.isConcatAggregateFunctionSelection(selectionIndex)) {
                            String oldText = (String) oldRow.get(selectionIndex);
                            String value = (String) rawValue;
                            rowBuilder.add(oldText + value);
                        }
                        else {
                            rowBuilder.add(rawValue);
                        }
                    }

                    unselectedResult.put(oldRowIndex, rowBuilder.build());
                    unselectedResult.removeAt(resultRow);
                    --resultRow;
                }
            }
            return new Result(unselectedResult.toImmutable());
        }
        else {
            final ImmutableList.Builder<ImmutableList<Object>> builder = new ImmutableList.Builder<>(unselectedResult.size());
            final ImmutableIntList selection = query.selection();
            for (ImmutableList<Object> register : unselectedResult) {
                final ImmutableList.Builder<Object> regBuilder = new ImmutableList.Builder<>();
                for (int columnIndex : selection) {
                    regBuilder.add(register.get(columnIndex));
                }
                builder.add(regBuilder.build());
            }
            return new Result(builder.build());
        }
    }

    private MutableIntKeyMap<ImmutableList<Object>> obtainTableContent(DbTable table) {
        MutableIntKeyMap<ImmutableList<Object>> content = _tableMap.get(table, null);

        if (content == null) {
            content = MutableIntKeyMap.empty();
            _tableMap.put(table, content);
        }

        return content;
    }

    @Override
    public Integer insert(DbInsertQuery query) {
        final DbTable table = query.getTable();
        final MutableIntKeyMap<ImmutableList<Object>> content = obtainTableContent(table);

        final int queryColumnCount = query.getColumnCount();
        final ImmutableList<DbColumn> columns = table.columns();

        final ImmutableList.Builder<Object> builder = new ImmutableList.Builder<>(columns.size());
        final MutableMap<DbColumn, Object> uniqueMap = MutableMap.empty();
        Integer id = null;
        for (DbColumn column : columns) {
            boolean found = false;
            for (int i = 0; i < queryColumnCount; i++) {
                if (column == query.getColumn(i)) {
                    final DbValue value = query.getValue(i);
                    if (column.isPrimaryKey()) {
                        if (id != null) {
                            throw new AssertionError();
                        }
                        id = value.toInt();
                        if (content.keySet().contains(id)) {
                            // Let's avoid duplicates
                            return null;
                        }
                        found = true;
                    }
                    else {
                        final Object rawValue = value.isText()? value.toText() : value.toInt();
                        if (column.isUnique()) {
                            if (_indexes.containsKey(column) && _indexes.get(column).keySet().contains(rawValue)) {
                                // Let's avoid duplicates
                                return null;
                            }
                            uniqueMap.put(column, rawValue);
                        }

                        builder.add(rawValue);
                        found = true;
                        break;
                    }
                }
            }

            if (!found && column.isPrimaryKey()) {
                if (id != null) {
                    throw new AssertionError();
                }

                id = content.isEmpty()? 1 : content.keySet().max() + 1;
                found = true;
            }

            if (!found) {
                throw new IllegalArgumentException("Unable to find value for column " + column.name());
            }
        }
        final ImmutableList<Object> register = builder.build();
        content.put(id, register);

        for (MutableMap.Entry<DbColumn, Object> entry : uniqueMap.entries()) {
            final MutableMap<Object, Integer> map;
            if (_indexes.containsKey(entry.key())) {
                map = _indexes.get(entry.key());
            }
            else {
                map = MutableMap.empty();
                _indexes.put(entry.key(), map);
            }

            map.put(entry.value(), id);
        }

        return id;
    }

    @Override
    public boolean update(DbUpdateQuery query) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public boolean delete(DbDeleteQuery query) {
        final ImmutableIntKeyMap<DbValue> constraints = query.constraints();
        final int constraintCount = constraints.size();
        if (constraintCount == 0) {
            return false;
        }

        final MutableIntKeyMap<ImmutableList<Object>> table = _tableMap.get(query.table(), null);
        if (table != null) {
            if (constraints.keyAt(0) == 0) {
                final int id = constraints.valueAt(0).toInt();
                final ImmutableList<Object> register = table.get(id, null);
                if (register != null) {
                    boolean matches = true;
                    for (int i = 1; i < constraintCount; i++) {
                        final DbValue value = constraints.valueAt(i);
                        final Object rawValue = value.isText()? value.toText() : value.toInt();
                        if (!equal(register.get(constraints.keyAt(i) - 1), rawValue)) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        if (!table.remove(id)) {
                            throw new AssertionError();
                        }
                        return true;
                    }
                }
            }
            else {
                boolean removed = false;
                int index = 0;
                while (index < table.size()) {
                    final ImmutableList<Object> register = table.valueAt(index);
                    boolean matches = true;
                    for (IntKeyMap.Entry<DbValue> entry : constraints.entries()) {
                        final DbValue value = entry.value();
                        final Object rawValue = value.isText() ? value.toText() : value.toInt();
                        if (!equal(register.get(entry.key() - 1), rawValue)) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        table.removeAt(index);
                        removed = true;
                    } else {
                        index++;
                    }
                }

                return removed;
            }
        }

        return false;
    }
}
