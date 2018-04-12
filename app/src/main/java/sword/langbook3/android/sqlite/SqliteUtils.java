package sword.langbook3.android.sqlite;

import sword.langbook3.android.db.DbColumn;
import sword.langbook3.android.db.DbValue;

public final class SqliteUtils {

    private SqliteUtils() {
    }

    public static String sqlType(DbColumn column) {
        if (column.isPrimaryKey()) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }
        else if (column.isUnique()) {
            return "TEXT UNIQUE ON CONFLICT IGNORE";
        }
        else if (column.isText()) {
            return "TEXT";
        }
        else {
            return "INTEGER";
        }
    }

    public static String sqlValue(DbValue value) {
        return value.isText()? "'" + value.toText() + '\'' : Integer.toString(value.toInt());
    }
}
