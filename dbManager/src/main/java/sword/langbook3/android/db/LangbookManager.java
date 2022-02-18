package sword.langbook3.android.db;

public interface LangbookManager<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, QuizId, SentenceId> extends QuizzesManager<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId>, DefinitionsManager<ConceptId>, RuledSentencesManager<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId>, LangbookChecker<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
