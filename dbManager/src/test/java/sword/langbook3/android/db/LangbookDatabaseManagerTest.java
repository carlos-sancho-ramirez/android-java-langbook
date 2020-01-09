package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest extends LangbookManagerTest {

    @Override
    public LangbookManager createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager(db);
    }
}
