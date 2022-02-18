package sword.langbook3.android.db;

public interface RuledSentencesManager<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, SentenceId> extends AgentsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, SentencesManager<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId>, RuledSentencesChecker<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> {
}
