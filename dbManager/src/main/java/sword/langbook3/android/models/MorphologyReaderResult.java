package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableList;

public final class MorphologyReaderResult<AcceptationId> {

    public final ImmutableList<MorphologyResult<AcceptationId>> morphologies;
    public final ImmutableIntKeyMap<String> ruleTexts;
    public final ImmutableIntPairMap agentRules;

    public MorphologyReaderResult(ImmutableList<MorphologyResult<AcceptationId>> morphologies, ImmutableIntKeyMap<String> ruleTexts, ImmutableIntPairMap agentRules) {
        this.morphologies = morphologies;
        this.ruleTexts = ruleTexts;
        this.agentRules = agentRules;
    }
}
