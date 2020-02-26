package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.BunchesManager;
import sword.langbook3.android.db.LangbookDatabaseManager;

final class StreamedDatabaseTest implements BunchesSerializerTest {
    @Override
    public BunchesManager createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager(db);
    }
}
