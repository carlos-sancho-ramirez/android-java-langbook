package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.LangbookDatabaseManager;

final class StreamedDatabaseTest implements AgentsSerializerTest {
    @Override
    public AgentsManager createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager(db);
    }
}
