package sword.langbook3.android.db;

import sword.collections.ImmutableMap;

public interface RuledSentencesChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, SentenceId> extends AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, SentencesChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> {

    /**
     * Return a map for all sentences that has at least one span with a dynamic
     * acceptation where the given rule has been applied.
     *
     * @param appliedRule Applied rule the is looked up.
     * @return Keys for the returned map are the sentence identifiers, values are the text representation of that sentence.
     */
    ImmutableMap<SentenceId, String> getSampleSentencesApplyingRule(RuleId appliedRule);
}
