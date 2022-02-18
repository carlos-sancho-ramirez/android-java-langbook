package sword.langbook3.android.db;

import sword.database.Database;

public final class LangbookDbManagerImpl extends LangbookDatabaseManager<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> implements LangbookDbManager {
    public LangbookDbManagerImpl(Database db) {
        super(db, new ConceptIdManager(), new LanguageIdManager(), new AlphabetIdManager(), new CharacterIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), new AcceptationIdManager(), new BunchIdManager(), new BunchSetIdManager(), new RuleIdManager(), new AgentIdManager(), new QuizIdManager(), new SentenceIdManager());
    }
}
