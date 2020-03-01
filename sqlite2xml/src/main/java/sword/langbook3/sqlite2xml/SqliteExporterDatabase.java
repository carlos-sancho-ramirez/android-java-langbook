package sword.langbook3.sqlite2xml;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.DbResult;

final class SqliteExporterDatabase implements DbExporter.Database, Closeable {
    private String _path;
    private Connection _connection;

    SqliteExporterDatabase(String path) {
        _path = path;
    }

    @Override
    public DbResult select(DbQuery query) {
        final String sqlQuery = new SQLiteDbQuery(query).toSql();
        try {
            if (_connection == null) {
                _connection = DriverManager.getConnection("jdbc:sqlite:" + _path);
            }

            final Statement statement = _connection.createStatement();
            statement.setQueryTimeout(60);
            return new SQLiteDbResult(statement, query.columns(), statement.executeQuery(sqlQuery));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void close() {
        try {
            _connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        _connection = null;
    }
}
