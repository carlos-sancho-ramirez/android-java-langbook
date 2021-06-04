package sword.langbook3.android.db;

import sword.database.MemoryDatabase;

final class LangbookDatabaseManagerTest implements LangbookManagerTest<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, QuizIdHolder, SentenceIdHolder> {

    private final ConceptIdManager _conceptIdManager = new ConceptIdManager();
    private final AlphabetIdManager _alphabetIdManager = new AlphabetIdManager();
    private final AcceptationIdManager _acceptationIdManager = new AcceptationIdManager();
    private final BunchIdManager _bunchIdManager = new BunchIdManager();
    private final RuleIdManager _ruleIdManager = new RuleIdManager();

    @Override
    public LangbookManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, QuizIdHolder, SentenceIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, _conceptIdManager, new LanguageIdManager(), _alphabetIdManager, new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), _acceptationIdManager, _bunchIdManager, new BunchSetIdManager(), _ruleIdManager, new AgentIdManager(), new QuizIdManager(), new SentenceIdManager());
    }

    @Override
    public ConceptSetter<ConceptIdHolder> getConceptIdManager() {
        return _conceptIdManager;
    }

    @Override
    public ConceptualizableSetter<ConceptIdHolder, AlphabetIdHolder> getAlphabetIdManager() {
        return _alphabetIdManager;
    }

    @Override
    public BunchIdHolder conceptAsBunchId(ConceptIdHolder conceptId) {
        return _bunchIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public IntSetter<AcceptationIdHolder> getAcceptationIdManager() {
        return _acceptationIdManager;
    }

    @Override
    public RuleIdHolder conceptAsRuleId(ConceptIdHolder conceptId) {
        return _ruleIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public AlphabetIdHolder getNextAvailableAlphabetId(ConceptsChecker<ConceptIdHolder> manager) {
        return _alphabetIdManager.getKeyFromConceptId(manager.getNextAvailableConceptId());
    }
}
