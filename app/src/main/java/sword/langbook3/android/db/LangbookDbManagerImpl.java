package sword.langbook3.android.db;

import sword.database.Database;

public final class LangbookDbManagerImpl extends LangbookDatabaseManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId> implements LangbookDbManager {
    public LangbookDbManagerImpl(Database db) {
        super(db, new ConceptIdManager(), new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), new AcceptationIdManager(), new BunchIdManager(), new BunchSetIdManager(), new RuleIdManager());
    }
}
