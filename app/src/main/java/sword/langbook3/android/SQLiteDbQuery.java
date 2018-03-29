package sword.langbook3.android;

import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.db.DbView;

final class SQLiteDbQuery {

    private final DbQuery _query;

    SQLiteDbQuery(DbQuery query) {
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
            sb.append(" ON J").append(_query.getTableIndexFromColumnIndex(pair.getLeft())).append('.');
            sb.append(_query.getJoinColumn(pair.getLeft()).name()).append("=J").append(i);
            sb.append('.').append(_query.getJoinColumn(pair.getRight()).name());
        }

        return sb.toString();
    }

    private String getSqlWhereClause(int subQueryIndex) {
        final int restrictionCount = _query.getRestrictionCount();
        final StringBuilder sb = new StringBuilder();
        boolean prefixAdded = false;
        for (int i = 0; i < restrictionCount; i++) {
            if (!prefixAdded) {
                sb.append(" WHERE ");
                prefixAdded = true;
            }
            else {
                sb.append(" AND ");
            }
            sb.append(getSqlColumnName(subQueryIndex, _query.getRestrictedColumnIndex(i)))
                    .append('=').append(_query.getRestriction(i).toSql());
        }

        final int columnValueMatchiPairCount = _query.getColumnValueMatchPairCount();
        for (int i = 0; i < columnValueMatchiPairCount; i++) {
            if (!prefixAdded) {
                sb.append(" WHERE ");
                prefixAdded = true;
            }
            else {
                sb.append(" AND ");
            }

            final DbQuery.JoinColumnPair pair = _query.getColumnValueMatchPair(i);
            sb.append(getSqlColumnName(subQueryIndex, pair.getLeft()))
                    .append('=').append(getSqlColumnName(subQueryIndex, pair.getRight()));
        }

        return sb.toString();
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
        final int count = _query.getOrderingCount();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                sb.append(" ORDER BY ");
            }
            else {
                sb.append(", ");
            }
            sb.append(getSqlColumnName(subQueryIndex, _query.getOrdering(i)));
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
