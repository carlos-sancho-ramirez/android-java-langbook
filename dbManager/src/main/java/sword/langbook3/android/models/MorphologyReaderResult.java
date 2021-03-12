package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;

public final class MorphologyReaderResult<AcceptationId, RuleId> {

    public final ImmutableList<MorphologyResult<AcceptationId, RuleId>> morphologies;
    public final ImmutableMap<RuleId, String> ruleTexts;
    public final ImmutableIntKeyMap<RuleId> agentRules;

    public MorphologyReaderResult(ImmutableList<MorphologyResult<AcceptationId, RuleId>> morphologies, ImmutableMap<RuleId, String> ruleTexts, ImmutableIntKeyMap<RuleId> agentRules) {
        this.morphologies = morphologies;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
