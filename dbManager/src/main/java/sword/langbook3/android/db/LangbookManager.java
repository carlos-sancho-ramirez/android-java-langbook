package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId> extends QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
