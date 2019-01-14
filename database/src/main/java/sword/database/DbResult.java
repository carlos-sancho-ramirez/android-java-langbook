package sword.database;

import java.io.Closeable;

import sword.collections.List;
import sword.collections.Transformer;
import sword.collections.Traverser;

public interface DbResult extends Transformer<List<DbValue>>, Closeable {

    @Override
    void close();
    int getRemainingRows();
}
