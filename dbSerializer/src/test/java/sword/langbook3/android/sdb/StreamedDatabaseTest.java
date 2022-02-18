package sword.langbook3.android.sdb;

import sword.database.MemoryDatabase;
import sword.langbook3.android.db.ConceptsChecker;
import sword.langbook3.android.db.IntSetter;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookManager;

final class StreamedDatabaseTest implements RuledSentencesSerializerTest<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CharacterIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, SentenceIdHolder> {

    private final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
    private final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
    private final BunchIdManager bunchIdManager = new BunchIdManager();
    private final RuleIdManager ruleIdManager = new RuleIdManager();

    @Override
    public LangbookManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CharacterIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, CorrelationArrayIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder, AgentIdHolder, QuizIdHolder, SentenceIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new ConceptIdManager(), new LanguageIdManager(), alphabetIdManager, new CharacterIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), acceptationIdManager, bunchIdManager, new BunchSetIdManager(), ruleIdManager, new AgentIdManager(), new QuizIdManager(), new SentenceIdManager());
    }

    @Override
    public RuleIdHolder conceptAsRuleId(ConceptIdHolder conceptId) {
        return ruleIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public BunchIdHolder conceptAsBunchId(ConceptIdHolder conceptId) {
        return bunchIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker<ConceptIdHolder> manager) {
        return alphabetIdManager.getKeyFromConceptId(manager.getNextAvailableConceptId());
    }

    @Override
    public IntSetter<AcceptationIdHolder> getAcceptationIdManager() {
        return acceptationIdManager;
    }
}
