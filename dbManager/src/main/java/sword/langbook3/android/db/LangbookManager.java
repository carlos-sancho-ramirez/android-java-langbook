package sword.langbook3.android.db;

public interface LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends QuizzesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, DefinitionsManager<ConceptId>, SentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId>, LangbookChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {
    void updateSearchHistory(AcceptationId dynamicAcceptation);
}
