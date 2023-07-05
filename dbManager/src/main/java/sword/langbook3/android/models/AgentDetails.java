package sword.langbook3.android.models;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

public final class AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> {
    public final ImmutableSet<BunchId> targetBunches;
    public final ImmutableSet<BunchId> sourceBunches;
    public final ImmutableSet<BunchId> diffBunches;
    public final ImmutableCorrelation<AlphabetId> startMatcher;
    public final ImmutableCorrelationArray<AlphabetId> startAdder;
    public final ImmutableList<CorrelationId> startAdderCorrelationIds;
    public final ImmutableCorrelation<AlphabetId> endMatcher;
    public final ImmutableCorrelationArray<AlphabetId> endAdder;
    public final ImmutableList<CorrelationId> endAdderCorrelationIds;
    public final RuleId rule;

    public AgentDetails(ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches, ImmutableSet<BunchId> diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelationArray<AlphabetId> startAdder, ImmutableList<CorrelationId> startAdderCorrelationIds,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelationArray<AlphabetId> endAdder, ImmutableList<CorrelationId> endAdderCorrelationIds, RuleId rule) {

        if (startMatcher == null) {
            startMatcher = ImmutableCorrelation.empty();
        }

        if (startAdder == null) {
            startAdder = ImmutableCorrelationArray.empty();
        }

        if (startAdderCorrelationIds == null) {
            startAdderCorrelationIds = ImmutableList.empty();
        }

        if (startAdder.size() != startAdderCorrelationIds.size()) {
            throw new IllegalArgumentException();
        }

        if (endMatcher == null) {
            endMatcher = ImmutableCorrelation.empty();
        }

        if (endAdder == null) {
            endAdder = ImmutableCorrelationArray.empty();
        }

        if (endAdderCorrelationIds == null) {
            endAdderCorrelationIds = ImmutableList.empty();
        }

        if (endAdder.size() != endAdderCorrelationIds.size()) {
            throw new IllegalArgumentException();
        }

        if (startMatcher.equalCorrelation(startAdder.concatenateTexts()) && endMatcher.equalCorrelation(endAdder.concatenateTexts())) {
            if (rule == null && targetBunches.isEmpty()) {
                throw new IllegalArgumentException();
            }
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
        this.startAdderCorrelationIds = startAdderCorrelationIds;
        this.endMatcher = endMatcher;
        this.endAdder = endAdder;
        this.endAdderCorrelationIds = endAdderCorrelationIds;
        this.rule = rule;
    }

    public boolean modifyCorrelations() {
        return rule != null;
    }
}
