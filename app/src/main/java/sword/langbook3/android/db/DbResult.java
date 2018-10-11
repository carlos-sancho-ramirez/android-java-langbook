package sword.langbook3.android.db;

import java.io.Closeable;

import sword.collections.List;
import sword.collections.Traverser;

public interface DbResult extends Traverser<List<DbValue>>, Closeable {

    @Override
    void close();
    int getRemainingRows();
}
