package sword.langbook3.android.db;

import sword.database.Database;

public final class LangbookDbManagerImpl extends LangbookDatabaseManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> implements LangbookDbManager {
    public LangbookDbManagerImpl(Database db) {
        super(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), new AcceptationIdManager());
    }
}
