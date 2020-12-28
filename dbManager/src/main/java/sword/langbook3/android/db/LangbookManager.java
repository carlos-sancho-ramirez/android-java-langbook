package sword.langbook3.android.db;

public interface LangbookManager<AlphabetId> extends QuizzesManager<AlphabetId>, DefinitionsManager, SentencesManager<AlphabetId>, LangbookChecker<AlphabetId> {
    void updateSearchHistory(int dynamicAcceptation);
}
