package sword.langbook3.android.sqlite;

import sword.database.DbColumn;
import sword.database.DbValue;

public final class SqliteUtils {

    private SqliteUtils() {
    }

    public static String sqlType(DbColumn column) {
        if (column.isPrimaryKey()) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }
        else {
            final String typeName = column.isText()? "TEXT" : "INTEGER";
            return column.isUnique()? typeName + " UNIQUE ON CONFLICT IGNORE" : typeName;
        }
    }

    public static String sqlValue(DbValue value) {
        return value.isText()? "'" + value.toText() + '\'' : Integer.toString(value.toInt());
    }
}
