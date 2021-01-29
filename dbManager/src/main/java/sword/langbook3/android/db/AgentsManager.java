package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;

public interface AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> extends BunchesManager<LanguageId, AlphabetId, CorrelationId>, AgentsChecker<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> {

    /**
     * Include a new agent into the database and returns its identifier if succeeded,
     * or null if it was not possible to be included.
     *
     * This method will ensure that all affected acceptations will be refreshed before the method returns.
     *
     * @param targetBunches Set of bunches where all processed acceptations should be included,
     *                      or 0 if the processed acceptation should not be included in any bunch.
     * @param sourceBunches Set of bunches from where acceptations to be processed will be picked.
     *                      Any acceptation included in at least one of the bunches within the set
     *                      will be processed, except if the same acceptation is included in any of the <code>diffBunches</code>.
     *                      If this bunch is empty, it is understood that all static acceptations within the database will be used as source.
     *                      This set must not contain any of the bunches given in diffBunches. It is considered an error and the agent will not be added.
     * @param diffBunches Set of bunches whose included acceptations will be used to filter acceptations coming from the <code>sourceBunches</code>.
     *                    In other words, any acceptation included in any of the source bunches,
     *                    that is not included in any of the diff bunches will be processed.
     *                    If this is empty, all acceptations in source will be processed.
     * @param startMatcher Correlation used to filter any acceptation from source.
     *                     Any acceptation that has at least one text that does not begin with this correlation, will be excluded from the process.
     * @param startAdder Correlation used to replace acceptation texts matched by the <code>startMatcher</code>.
     * @param endMatcher Correlation used to filter any acceptation from source.
     *                   Any acceptation that has at least one text that does not finish with this correlation, will be excluded from the process.
     * @param endAdder Correlation used to replace acceptation texts matched by the <code>endMatcher</code>.
     * @param rule Concept for the rule that this agent is applying.
     *             This is required only if the resulting acceptation text is different from the source acceptation text.
     *             This will happen if <code>startMatcher</code> does not match <code>startAdder</code>,
     *             or <code>endMatcher</code> does not match <code>endAdder</code>.
     *             If the resulting text matches the source text, then 0 is expected here.
     * @return An identifier for the new agent if all OK, or null if not possible to add the agent.
     */
    Integer addAgent(ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> startAdder, ImmutableCorrelation<AlphabetId> endMatcher,
            ImmutableCorrelation<AlphabetId> endAdder, int rule);

    boolean updateAgent(int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> startAdder, ImmutableCorrelation<AlphabetId> endMatcher,
            ImmutableCorrelation<AlphabetId> endAdder, int rule);

    void removeAgent(int agentId);
}
