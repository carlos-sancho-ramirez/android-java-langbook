package sword.langbook3.sqlite2xml;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import sword.collections.AbstractTransformer;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.IntFunction;
import sword.collections.List;
import sword.database.DbColumn;
import sword.database.DbIntValue;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbValue;

final class SQLiteDbResult extends AbstractTransformer<List<DbValue>> implements DbResult {
    private final Statement _statement;
    private final ImmutableList<DbColumn> _columns;
    private final ResultSet _resultSet;

    private boolean _hasNext;

    SQLiteDbResult(Statement statement, ImmutableList<DbColumn> columns, ResultSet resultSet) {
        _statement = statement;
        _columns = columns;
        _resultSet = resultSet;
        try {
            _hasNext = resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            _resultSet.close();
            _statement.close();
        } catch (SQLException e) {
            // Nothing to be done
        }
    }

    @Override
    public int getRemainingRows() {
        throw new UnsupportedOperationException("Unsupported for ResultSet");
    }

    @Override
    public boolean hasNext() {
        return _hasNext;
    }

    @Override
    public ImmutableList<DbValue> next() {
        if (!hasNext()) {
            throw new UnsupportedOperationException("End already reached");
        }

        final ImmutableList<DbValue> row = _columns.map(column -> {
            // Assuming for now that it is an integer column
            try {
                return new DbIntValue(_resultSet.getInt(column.name()));
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
        _hasNext = false;

        try {
            _hasNext = _resultSet.next();
        } catch (SQLException e) {
            // Nothing to be done
        }

        return row;
    }
}
