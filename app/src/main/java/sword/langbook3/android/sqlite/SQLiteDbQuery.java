package sword.langbook3.android.sqlite;

import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.database.DbQuery;
import sword.database.DbTable;
import sword.database.DbView;

public final class SQLiteDbQuery {

    private final DbQuery _query;

    public SQLiteDbQuery(DbQuery query) {
        _query = query;
    }

    private String getSqlColumnName(int subQueryIndex, int columnIndex) {
        final int tableIndex = _query.getTableIndexFromColumnIndex(columnIndex);
        if (tableIndex == 0 && _query.getView(0).asQuery() != null) {
            return "S" + (subQueryIndex + 1) + "C" + columnIndex;
        }
        else {
            return "J" + _query.getTableIndexFromColumnIndex(columnIndex) + '.' + _query.getJoinColumn(columnIndex).name();
        }
    }

    private String getSqlSelectedColumnName(int subQueryIndex, int selectionIndex) {
        final String name = getSqlColumnName(subQueryIndex, _query.selection().valueAt(selectionIndex));
        return _query.isMaxAggregateFunctionSelection(selectionIndex)? "coalesce(max(" + name + "), 0)" :
                _query.isConcatAggregateFunctionSelection(selectionIndex)? "group_concat(" + name + ",'')" : name;
    }

    private String getSqlSelectedColumnNames(int subQueryIndex) {
        final int selectedColumnCount = _query.selection().size();
        final StringBuilder sb = new StringBuilder(getSqlSelectedColumnName(subQueryIndex, 0));
        if (subQueryIndex > 0) {
            sb.append(" AS S").append(subQueryIndex).append("C0");
        }

        for (int i = 1; i < selectedColumnCount; i++) {
            sb.append(',').append(getSqlSelectedColumnName(subQueryIndex, i));

            if (subQueryIndex > 0) {
                sb.append(" AS S").append(subQueryIndex).append('C').append(i);
            }
        }

        return sb.toString();
    }

    private String getSqlFromClause(int subQueryIndex) {
        final int tableCount = _query.getTableCount();
        final StringBuilder sb = new StringBuilder(" FROM ");

        final DbView firstView = _query.getView(0);
        final DbQuery query = firstView.asQuery();
        final DbTable table = firstView.asTable();
        if (query != null) {
            final SQLiteDbQuery sqlQuery = new SQLiteDbQuery(query);
            sb.append('(').append(sqlQuery.toSql(subQueryIndex + 1)).append(')');
        }
        else if (table != null) {
            sb.append(table.name());
        }
        sb.append(" AS J0");

        for (int i = 1; i < tableCount; i++) {
            final DbQuery.JoinColumnPair pair = _query.getJoinPair(i - 1);
            sb.append(" JOIN ").append(_query.getView(i).asTable().name()).append(" AS J").append(i);
            sb.append(" ON J").append(_query.getTableIndexFromColumnIndex(pair.left())).append('.');
            sb.append(_query.getJoinColumn(pair.left()).name()).append("=J").append(i);
            sb.append('.').append(_query.getJoinColumn(pair.right()).name());
        }

        return sb.toString();
    }

    private String getSqlWhereClause(int subQueryIndex) {
        final ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        for (IntKeyMap.Entry<DbQuery.Restriction> entry : _query.restrictions().entries()) {
            final String value;
            if (entry.value().value.isText()) {
                final int type = entry.value().type;
                value = ((type == DbQuery.RestrictionTypes.EXACT)? "=" : " LIKE ") +
                        ((type == DbQuery.RestrictionStringTypes.ENDS_WITH || type == DbQuery.RestrictionStringTypes.CONTAINS)? "'%" : "'") +
                        entry.value().value.toText() +
                        ((type == DbQuery.RestrictionStringTypes.STARTS_WITH || type == DbQuery.RestrictionStringTypes.CONTAINS)? "%'" : "'");
            }
            else {
                value = "=" + Integer.toString(entry.value().value.toInt());
            }

            builder.add(getSqlColumnName(subQueryIndex, entry.key()) + value);
        }

        for (DbQuery.JoinColumnPair pair : _query.columnValueMatchPairs()) {
            final String operator = pair.mustMatch()? "=" : "!=";
            builder.add(getSqlColumnName(subQueryIndex, pair.left()) + operator + getSqlColumnName(subQueryIndex, pair.right()));
        }

        final ImmutableList<String> conditions = builder.build();
        return !conditions.isEmpty()? " WHERE " + conditions.reduce((a,b) -> a + " AND " + b) : "";
    }

    private String getGroupingClause(int subQueryIndex) {
        final int count = _query.getGroupingCount();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                sb.append(" GROUP BY ");
            }
            else {
                sb.append(", ");
            }
            sb.append(getSqlColumnName(subQueryIndex, _query.getGrouping(i)));
        }

        return sb.toString();
    }

    private String getOrderingClause(int subQueryIndex) {
        final ImmutableList<DbQuery.Ordered> ordering = _query.ordering();
        final int count = ordering.size();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                sb.append(" ORDER BY ");
            }
            else {
                sb.append(", ");
            }
            sb.append(getSqlColumnName(subQueryIndex, ordering.get(i).columnIndex));

            if (ordering.get(i).descendantOrder) {
                sb.append(" DESC");
            }
        }

        return sb.toString();
    }

    private String toSql(int subQueryIndex) {
        return "SELECT " + getSqlSelectedColumnNames(subQueryIndex) +
                getSqlFromClause(subQueryIndex) + getSqlWhereClause(subQueryIndex) +
                getGroupingClause(subQueryIndex) + getOrderingClause(subQueryIndex);
    }

    public String toSql() {
        return toSql(0);
    }
}
