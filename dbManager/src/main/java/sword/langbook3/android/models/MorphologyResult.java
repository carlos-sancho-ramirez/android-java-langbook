package sword.langbook3.android.models;

import sword.collections.ImmutableIntList;

public final class MorphologyResult {
    public final int dynamicAcceptation;
    public final ImmutableIntList rules;
    public final String text;

    public MorphologyResult(int dynamicAcceptation, ImmutableIntList rules, String text) {
        this.dynamicAcceptation = dynamicAcceptation;
        this.rules = rules;
        this.text = text;
    }
}
