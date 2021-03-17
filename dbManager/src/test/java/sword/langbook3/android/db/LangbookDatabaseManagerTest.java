package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, RuleIdHolder> {

    private final AcceptationIdManager _acceptationIdManager = new AcceptationIdManager();
    private final RuleIdManager _ruleIdManager = new RuleIdManager();

    @Override
    public LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, RuleIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), _acceptationIdManager, new BunchIdManager(), _ruleIdManager);
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
    public RuleIdHolder conceptAsRuleId(int conceptId) {
        return _ruleIdManager.getKeyFromInt(conceptId);
    }

    @Override
    public AlphabetIdHolder getNextAvailableAlphabetId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getNextAvailableConceptId());
    }

    @Override
    public int getLanguageConceptId(LanguageIdHolder language) {
        return language.key;
    }
}
