package sword.langbook3.android.db;

import java.io.Closeable;
import java.util.Iterator;

public interface DbResult extends Iterator<DbResult.Row>, Closeable {

    @Override
    void close();

    interface Row {
        DbValue get(int index);
    }
}
