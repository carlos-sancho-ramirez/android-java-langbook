package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId> extends QuizzesManager<LanguageId, AlphabetId, CorrelationId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId> {
    void updateSearchHistory(int dynamicAcceptation);
}
