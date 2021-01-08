package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest<LanguageIdHolder, AlphabetIdHolder> {

    @Override
    public LangbookManager<LanguageIdHolder, AlphabetIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new LanguageIdManager(), new AlphabetIdManager());
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getMaxConcept() + 1);
    }

    @Override
    public int getLanguageConceptId(LanguageIdHolder language) {
        return language.key;
    }
}
