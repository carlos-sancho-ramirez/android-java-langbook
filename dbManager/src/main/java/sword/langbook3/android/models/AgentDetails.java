package sword.langbook3.android.models;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.ImmutableCorrelation;

public final class AgentDetails<AlphabetId, BunchId, RuleId> {
    public final ImmutableSet<BunchId> targetBunches;
    public final ImmutableSet<BunchId> sourceBunches;
    public final ImmutableSet<BunchId> diffBunches;
    public final ImmutableCorrelation<AlphabetId> startMatcher;
    public final ImmutableCorrelation<AlphabetId> startAdder;
    public final ImmutableCorrelation<AlphabetId> endMatcher;
    public final ImmutableCorrelation<AlphabetId> endAdder;
    public final RuleId rule;

    public AgentDetails(ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, ImmutableCorrelation<AlphabetId> startMatcher,
            ImmutableCorrelation<AlphabetId> startAdder, ImmutableCorrelation<AlphabetId> endMatcher,
            ImmutableCorrelation<AlphabetId> endAdder, RuleId rule) {

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
            rule = null;
        }
        else if (rule == null) {
            throw new IllegalArgumentException();
        }

        if (sourceBunches == null) {
            sourceBunches = ImmutableHashSet.empty();
        }

        if (diffBunches == null) {
            diffBunches = ImmutableHashSet.empty();
        }

        if (!sourceBunches.filter(diffBunches::contains).isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (sourceBunches.contains(null)) {
            throw new IllegalArgumentException();
        }

        if (diffBunches.contains(null)) {
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
