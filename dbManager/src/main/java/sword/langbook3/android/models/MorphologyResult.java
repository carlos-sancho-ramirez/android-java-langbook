package sword.langbook3.android.models;

import sword.collections.ImmutableList;

public final class MorphologyResult<AcceptationId, RuleId> {
    public final AcceptationId dynamicAcceptation;
    public final ImmutableList<RuleId> rules;
    public final String text;

    public MorphologyResult(AcceptationId dynamicAcceptation, ImmutableList<RuleId> rules, String text) {
        this.dynamicAcceptation = dynamicAcceptation;
        this.rules = rules;
        this.text = text;
    }
}
