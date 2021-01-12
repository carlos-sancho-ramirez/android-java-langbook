package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId, SymbolArrayId> extends QuizzesManager<LanguageId, AlphabetId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId, SymbolArrayId>, LangbookChecker<LanguageId, AlphabetId, SymbolArrayId> {
    void updateSearchHistory(int dynamicAcceptation);
}
