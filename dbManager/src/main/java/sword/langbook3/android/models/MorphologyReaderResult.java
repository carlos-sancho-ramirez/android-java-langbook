package sword.langbook3.android.models;

import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;

public final class MorphologyReaderResult<AcceptationId, RuleId, AgentId> {

    public final ImmutableList<MorphologyResult<AcceptationId, RuleId>> morphologies;
    public final ImmutableMap<RuleId, String> ruleTexts;
    public final ImmutableMap<AgentId, RuleId> agentRules;

    public MorphologyReaderResult(ImmutableList<MorphologyResult<AcceptationId, RuleId>> morphologies, ImmutableMap<RuleId, String> ruleTexts, ImmutableMap<AgentId, RuleId> agentRules) {
        this.morphologies = morphologies;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
