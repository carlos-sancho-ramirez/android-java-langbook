package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableMap;

public class MemoryDatabase implements DbInitializer.Database {

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

    @Override
    public DbResult select(DbQuery query) {
        if (query.getTableCount() != 1 || query.getColumnValueMatchPairCount() != 0 ||
                query.getGroupingCount() != 0 || query.getOrderingCount() != 0 ||
                query.getRestrictionCount() != 1 || query.getRestrictedColumnIndex(0) != 0) {
            throw new UnsupportedOperationException("Unimplemented");
        }

        final DbView view = query.getView(0);
        final MutableIntKeyMap<ImmutableList<Object>> content = _tableMap.get(view);
        final ImmutableList<Object> register = content.get(query.getRestriction(0).toInt());

        final int selectionCount = query.getSelectedColumnCount();
        final ImmutableList.Builder<Object> builder = new ImmutableList.Builder<>();
        for (int i = 0; i < selectionCount; i++) {
            builder.add(register.get(query.getSelectedColumnIndex(i) - 1));
        }

        final ImmutableList<ImmutableList<Object>> resultTable = new ImmutableList.Builder<ImmutableList<Object>>(1)
                .add(builder.build())
                .build();

        return new Result(resultTable);
    }

    private MutableIntKeyMap<ImmutableList<Object>> obtainTableContent(DbTable table) {
        MutableIntKeyMap<ImmutableList<Object>> content = _tableMap.get(table);

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

        for (MutableMap.Entry<DbColumn, Object> entry : uniqueMap) {
            final MutableMap<Object, Integer> map;
            if (_indexes.containsKey(entry.getKey())) {
                map = _indexes.get(entry.getKey());
            }
            else {
                map = MutableMap.empty();
                _indexes.put(entry.getKey(), map);
            }

            map.put(entry.getValue(), id);
        }

        return id;
    }
}
