package sword.langbook3.android.db;

public interface LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, QuizId, SentenceId> extends QuizzesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId>, DefinitionsManager<ConceptId>, SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, SentenceId>, LangbookChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
