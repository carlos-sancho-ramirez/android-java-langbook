package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest {

    @Override
    public LangbookManager createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager(db);
    }
}
