package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableMap;

public final class DerivedAcceptationsReaderResult<AcceptationId, RuleId> {
    public final ImmutableMap<AcceptationId, DerivedAcceptationResult> acceptations;
    public final ImmutableIntKeyMap<RuleId> agentRules;
    public final ImmutableMap<RuleId, String> ruleTexts;

    public DerivedAcceptationsReaderResult(ImmutableMap<AcceptationId, DerivedAcceptationResult> acceptations, ImmutableMap<RuleId, String> ruleTexts, ImmutableIntKeyMap<RuleId> agentRules) {
        this.acceptations = acceptations;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
