package sword.langbook3.android;

import sword.langbook3.android.db.DbQuery;

final class SQLiteDbQuery {

    private final DbQuery _query;

    SQLiteDbQuery(DbQuery query) {
        _query = query;
    }

    private String getSqlColumnName(int index) {
        return "J" + _query.getTableIndexFromColumnIndex(index) + '.' + _query.getJoinColumn(index).getName();
    }

    private String getSqlSelectedColumnName(int index) {
        return getSqlColumnName(_query.getSelectedColumnIndex(index));
    }

    private String getSqlSelectedColumnNames() {
        final int selectedColumnCount = _query.getSelectedColumnCount();
        final StringBuilder sb = new StringBuilder(getSqlSelectedColumnName(0));
        for (int i = 1; i < selectedColumnCount; i++) {
            sb.append(',').append(getSqlSelectedColumnName(i));
        }

        return sb.toString();
    }

    private String getSqlFromClause() {
        final int tableCount = _query.getTableCount();
        final StringBuilder sb = new StringBuilder(" FROM ");
        sb.append(_query.getTable(0).getName()).append(" AS J0");

        for (int i = 1; i < tableCount; i++) {
            final DbQuery.JoinColumnPair pair = _query.getJoinPair(i - 1);
            sb.append(" JOIN ").append(_query.getTable(i).getName()).append(" AS J").append(i);
            sb.append(" ON J").append(_query.getTableIndexFromColumnIndex(pair.getLeft())).append('.');
            sb.append(_query.getJoinColumn(pair.getLeft()).getName()).append("=J").append(i);
            sb.append('.').append(_query.getJoinColumn(pair.getRight()).getName());
        }

        return sb.toString();
    }

    private String getSqlWhereClause() {
        final int restrictionCount = _query.getRestrictionCount();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < restrictionCount; i++) {
            if (i == 0) {
                sb.append(" WHERE ");
            }
            else {
                sb.append(" AND ");
            }
            sb.append(getSqlColumnName(_query.getRestrictedColumnIndex(i)))
                    .append('=').append(_query.getRestriction(i).toSql());
        }

        return sb.toString();
    }

    public String toSql() {
        return "SELECT " + getSqlSelectedColumnNames() + getSqlFromClause() + getSqlWhereClause();
    }
}
