package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder> {

    @Override
    public LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager());
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
