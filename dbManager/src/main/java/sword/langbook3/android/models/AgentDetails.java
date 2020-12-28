package sword.langbook3.android.models;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.langbook3.android.db.ImmutableCorrelation;

public final class AgentDetails<AlphabetId> {
    public final ImmutableIntSet targetBunches;
    public final ImmutableIntSet sourceBunches;
    public final ImmutableIntSet diffBunches;
    public final ImmutableCorrelation<AlphabetId> startMatcher;
    public final ImmutableCorrelation<AlphabetId> startAdder;
    public final ImmutableCorrelation<AlphabetId> endMatcher;
    public final ImmutableCorrelation<AlphabetId> endAdder;
    public final int rule;

    public AgentDetails(ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> startAdder, ImmutableCorrelation<AlphabetId> endMatcher,
            ImmutableCorrelation<AlphabetId> endAdder, int rule) {

        if (startMatcher == null) {
            startMatcher = ImmutableCorrelation.empty();
        }

        if (startAdder == null) {
            startAdder = ImmutableCorrelation.empty();
        }

        if (endMatcher == null) {
            endMatcher = ImmutableCorrelation.empty();
        }

        if (endAdder == null) {
            endAdder = ImmutableCorrelation.empty();
        }

        if (startMatcher.equalCorrelation(startAdder) && endMatcher.equalCorrelation(endAdder)) {
            if (targetBunches.isEmpty()) {
                throw new IllegalArgumentException();
            }
            rule = 0;
        }
        else if (rule == 0) {
            throw new IllegalArgumentException();
        }

        if (sourceBunches == null) {
            sourceBunches = new ImmutableIntSetCreator().build();
        }

        if (diffBunches == null) {
            diffBunches = new ImmutableIntSetCreator().build();
        }

        if (!sourceBunches.filter(diffBunches::contains).isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (sourceBunches.contains(0)) {
            throw new IllegalArgumentException();
        }

        if (diffBunches.contains(0)) {
            throw new IllegalArgumentException();
        }

        this.targetBunches = targetBunches;
        this.sourceBunches = sourceBunches;
        this.diffBunches = diffBunches;
        this.startMatcher = startMatcher;
        this.startAdder = startAdder;
        this.endMatcher = endMatcher;
        this.endAdder = endAdder;
        this.rule = rule;
    }

    public boolean modifyCorrelations() {
        return !startMatcher.equals(startAdder) || !endMatcher.equals(endAdder);
    }
}
