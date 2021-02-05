package sword.langbook3.android.models;

import sword.collections.ImmutableIntList;

public final class MorphologyResult<AcceptationId> {
    public final AcceptationId dynamicAcceptation;
    public final ImmutableIntList rules;
    public final String text;

    public MorphologyResult(AcceptationId dynamicAcceptation, ImmutableIntList rules, String text) {
        this.dynamicAcceptation = dynamicAcceptation;
        this.rules = rules;
        this.text = text;
    }
}
