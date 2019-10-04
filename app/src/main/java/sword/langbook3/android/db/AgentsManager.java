package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;

public interface AgentsManager extends BunchesManager, AgentsChecker {

    int obtainRuledConcept(int rule, int concept);

    Integer addAgent(int targetBunch, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> startMatcher,
            ImmutableIntKeyMap<String> startAdder, ImmutableIntKeyMap<String> endMatcher,
            ImmutableIntKeyMap<String> endAdder, int rule);

    void removeAgent(int agentId);
}
