package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest<AlphabetIdHolder> {

    @Override
    public LangbookManager<AlphabetIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new AlphabetIdManager());
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getMaxConcept() + 1);
    }
}
