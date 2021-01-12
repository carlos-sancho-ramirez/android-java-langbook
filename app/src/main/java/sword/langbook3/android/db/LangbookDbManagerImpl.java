package sword.langbook3.android.db;

import sword.database.Database;

public final class LangbookDbManagerImpl extends LangbookDatabaseManager<LanguageId, AlphabetId, SymbolArrayId> implements LangbookDbManager {
    public LangbookDbManagerImpl(Database db) {
        super(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager());
    }
}
