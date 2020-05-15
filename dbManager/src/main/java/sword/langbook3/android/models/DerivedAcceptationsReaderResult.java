package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;

public final class DerivedAcceptationsReaderResult {
    public final ImmutableIntKeyMap<DerivedAcceptationResult> acceptations;
    public final ImmutableIntPairMap agentRules;
    public final ImmutableIntKeyMap<String> ruleTexts;

    public DerivedAcceptationsReaderResult(ImmutableIntKeyMap<DerivedAcceptationResult> acceptations, ImmutableIntKeyMap<String> ruleTexts, ImmutableIntPairMap agentRules) {
        this.acceptations = acceptations;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
