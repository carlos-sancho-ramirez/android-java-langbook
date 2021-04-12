package sword.langbook3.android.models;

import sword.collections.ImmutableMap;

public final class DerivedAcceptationsReaderResult<AcceptationId, RuleId, AgentId> {
    public final ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> acceptations;
    public final ImmutableMap<AgentId, RuleId> agentRules;
    public final ImmutableMap<RuleId, String> ruleTexts;

    public DerivedAcceptationsReaderResult(ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> acceptations, ImmutableMap<RuleId, String> ruleTexts, ImmutableMap<AgentId, RuleId> agentRules) {
        this.acceptations = acceptations;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
