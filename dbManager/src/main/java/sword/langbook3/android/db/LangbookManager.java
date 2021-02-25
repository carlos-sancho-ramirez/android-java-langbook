package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId> extends QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
