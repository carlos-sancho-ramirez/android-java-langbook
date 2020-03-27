package sword.langbook3.android.models;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;

public final class AgentDetails {
    public final ImmutableIntSet targetBunches;
    public final ImmutableIntSet sourceBunches;
    public final ImmutableIntSet diffBunches;
    public final ImmutableIntKeyMap<String> startMatcher;
    public final ImmutableIntKeyMap<String> startAdder;
    public final ImmutableIntKeyMap<String> endMatcher;
    public final ImmutableIntKeyMap<String> endAdder;
    public final int rule;

    public AgentDetails(ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> startMatcher,
            ImmutableIntKeyMap<String> startAdder, ImmutableIntKeyMap<String> endMatcher,
            ImmutableIntKeyMap<String> endAdder, int rule) {

        if (startMatcher == null) {
            startMatcher = ImmutableIntKeyMap.empty();
        }

        if (startAdder == null) {
            startAdder = ImmutableIntKeyMap.empty();
        }

        if (endMatcher == null) {
            endMatcher = ImmutableIntKeyMap.empty();
        }

        if (endAdder == null) {
            endAdder = ImmutableIntKeyMap.empty();
        }

        if (startMatcher.equals(startAdder) && endMatcher.equals(endAdder)) {
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
