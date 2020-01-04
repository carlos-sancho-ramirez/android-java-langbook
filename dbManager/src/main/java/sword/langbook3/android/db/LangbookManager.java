package sword.langbook3.android.db;

public interface LangbookManager extends QuizzesManager, DefinitionsManager, SentencesManager, LangbookChecker {
    void updateSearchHistory(int dynamicAcceptation);
}
