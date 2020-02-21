package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.LangbookDatabaseManager;

final class StreamedDatabaseTest implements AcceptationsSerializerTest {
    @Override
    public AcceptationsManager createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager(db);
    }
}
