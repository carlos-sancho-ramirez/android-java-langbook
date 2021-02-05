package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableMap;

public final class DerivedAcceptationsReaderResult<AcceptationId> {
    public final ImmutableMap<AcceptationId, DerivedAcceptationResult> acceptations;
    public final ImmutableIntPairMap agentRules;
    public final ImmutableIntKeyMap<String> ruleTexts;

    public DerivedAcceptationsReaderResult(ImmutableMap<AcceptationId, DerivedAcceptationResult> acceptations, ImmutableIntKeyMap<String> ruleTexts, ImmutableIntPairMap agentRules) {
        this.acceptations = acceptations;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
