package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> extends QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, RuleId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
