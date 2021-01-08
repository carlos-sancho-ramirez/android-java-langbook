package sword.langbook3.android.db;

public interface LangbookManager<LanguageId, AlphabetId> extends QuizzesManager<LanguageId, AlphabetId>, DefinitionsManager, SentencesManager<LanguageId, AlphabetId>, LangbookChecker<LanguageId, AlphabetId> {
    void updateSearchHistory(int dynamicAcceptation);
}
