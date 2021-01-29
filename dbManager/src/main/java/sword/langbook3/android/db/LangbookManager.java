package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> extends QuizzesManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> {
    void updateSearchHistory(int dynamicAcceptation);
}
