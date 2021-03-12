package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder> {

    private final AcceptationIdManager _acceptationIdManager = new AcceptationIdManager();

    @Override
    public LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), _acceptationIdManager, new BunchIdManager());
    }

    @Override
    public BunchIdHolder conceptAsBunchId(int conceptId) {
        return new BunchIdHolder(conceptId);
    }

    @Override
    public IntSetter<AcceptationIdHolder> getAcceptationIdManager() {
        return _acceptationIdManager;
    }

    @Override
    public AlphabetIdHolder getNextAvailableAlphabetId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getMaxConcept() + 1);
    }

    @Override
    public int getLanguageConceptId(LanguageIdHolder language) {
        return language.key;
    }
}
