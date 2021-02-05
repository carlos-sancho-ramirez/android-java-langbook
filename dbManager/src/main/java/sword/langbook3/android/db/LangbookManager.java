package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> extends QuizzesManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
