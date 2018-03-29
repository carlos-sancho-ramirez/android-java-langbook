package sword.langbook3.android.db;

import java.util.Iterator;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableList;
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
                query.getRestrictionCount() != 1) {
            throw new UnsupportedOperationException("Unimplemented");
        }

        final DbView view = query.getView(0);
        final MutableIntKeyMap<ImmutableList<Object>> content = _tableMap.get(view);

        // Apply id restriction if found
        final ImmutableIntKeyMap<DbValue> restrictions = query.restrictions();
        final MutableList<ImmutableList<Object>> unselectedResult;
        if (restrictions.keySet().contains(0)) {
            final int id = restrictions.get(0).toInt();
            ImmutableList<Object> register = content.get(id).prepend(id);
            unselectedResult = new MutableList.Builder<ImmutableList<Object>>().add(register).build();
        }
        else {
            final MutableList.Builder<ImmutableList<Object>> builder = new MutableList.Builder<>();
            for (MutableIntKeyMap.Entry<ImmutableList<Object>> entry : content) {
                final ImmutableList<Object> register = entry.getValue().prepend(entry.getKey());
                builder.add(register);
            }
            unselectedResult = builder.build();
        }

        // Apply restrictions
        for (ImmutableIntKeyMap.Entry<DbValue> restriction : restrictions) {
            final DbValue value = restriction.getValue();
            final Object rawValue = value.isText()? value.toText() : value.toInt();

            final Iterator<ImmutableList<Object>> it = unselectedResult.iterator();
            while (it.hasNext()) {
                final ImmutableList<Object> register = it.next();
                if (!rawValue.equals(register.get(restriction.getKey()))) {
                    it.remove();
                }
            }
        }

        // Apply column selection
        final ImmutableList.Builder<ImmutableList<Object>> builder = new ImmutableList.Builder<>(unselectedResult.size());
        final ImmutableIntSet selection = query.selection();
        for (ImmutableList<Object> register : unselectedResult) {
            final ImmutableList.Builder<Object> regBuilder = new ImmutableList.Builder<>();
            for (int columnIndex : selection) {
                regBuilder.add(register.get(columnIndex));
            }
            builder.add(regBuilder.build());
        }
        return new Result(builder.build());
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
