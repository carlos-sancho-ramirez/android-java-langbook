package sword.langbook3.android.db;

public interface AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends BunchesChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId>, AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {

    /**
     * Check if there is at least one bunch used as source bunch by an agent
     * whose matcher may match the given texts.
     *
     * Required conditions are:
     * <li>The agent's matchers must match the given string</li>
     * <li>Start or end matcher must not be empty</li>
     * <li>There must be an adder different from the matcher</li>
     *
     * @param texts Map containing the word to be matched. Keys in the map are alphabets and values are the text on those alphabets.
     * @return Whether if there is at least one bunch matching the condition.
     */
    boolean hasMatchingBunches(ImmutableCorrelation<AlphabetId> texts);
}
