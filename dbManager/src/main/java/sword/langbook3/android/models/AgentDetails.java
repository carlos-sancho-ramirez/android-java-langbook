package sword.langbook3.android.models;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableMap;
import sword.langbook3.android.db.AlphabetId;

public final class AgentDetails {
    public final ImmutableIntSet targetBunches;
    public final ImmutableIntSet sourceBunches;
    public final ImmutableIntSet diffBunches;
    public final ImmutableMap<AlphabetId, String> startMatcher;
    public final ImmutableMap<AlphabetId, String> startAdder;
    public final ImmutableMap<AlphabetId, String> endMatcher;
    public final ImmutableMap<AlphabetId, String> endAdder;
    public final int rule;

    public AgentDetails(ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableMap<AlphabetId, String> startMatcher,
            ImmutableMap<AlphabetId, String> startAdder, ImmutableMap<AlphabetId, String> endMatcher,
            ImmutableMap<AlphabetId, String> endAdder, int rule) {

        if (startMatcher == null) {
            startMatcher = ImmutableHashMap.empty();
        }

        if (startAdder == null) {
            startAdder = ImmutableHashMap.empty();
        }

        if (endMatcher == null) {
            endMatcher = ImmutableHashMap.empty();
        }

        if (endAdder == null) {
            endAdder = ImmutableHashMap.empty();
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
