package sword.langbook3.android.sdb;

import java.io.IOException;

import sword.collections.ImmutableIntSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableSet;
import sword.collections.Traversable;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;

public interface StreamedDatabaseReaderInterface {
    Result read() throws IOException;

    final class AgentBunches {
        private final ImmutableIntSet _targets;
        private final ImmutableIntSet _sources;
        private final ImmutableIntSet _diff;

        AgentBunches(ImmutableIntSet targets, ImmutableIntSet sources, ImmutableIntSet diff) {
            _targets = targets;
            _sources = sources;
            _diff = diff;
        }

        boolean dependsOn(AgentBunches agent) {
            return agent._targets.anyMatch(target -> _sources.contains(target) || _diff.contains(target));
        }
    }

    final class AgentAcceptationPair {
        public final int agent;
        public final int acceptation;

        AgentAcceptationPair(int agent, int acceptation) {
            this.agent = agent;
            this.acceptation = acceptation;
        }
    }

    final class SentenceSpan {
        public final int sentenceId;
        public final int symbolArray;
        public final int start;
        public final int length;
        public final int acceptationFileIndex;

        SentenceSpan(int sentenceId, int symbolArray, int start, int length, int acceptationFileIndex) {
            this.sentenceId = sentenceId;
            this.symbolArray = symbolArray;
            this.start = start;
            this.length = length;
            this.acceptationFileIndex = acceptationFileIndex;
        }

        @Override
        public int hashCode() {
            return symbolArray * 41 + start;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SentenceSpan)) {
                return false;
            }

            final SentenceSpan that = (SentenceSpan) other;
            return symbolArray == that.symbolArray && start == that.start && length == that.length && acceptationFileIndex == that.acceptationFileIndex;
        }
    }

    final class Result {
        public final Conversion[] conversions;
        public final IntKeyMap<AgentBunches> agents;
        public final IntPairMap agentRules;
        public final int[] accIdMap;
        public final AgentAcceptationPair[] agentAcceptationPairs;
        public final SentenceSpan[] spans;
        public final int numberOfCorrelations;
        public final int numberOfCorrelationArrays;

        Result(Conversion[] conversions, IntKeyMap<AgentBunches> agents, IntPairMap agentRules, int[] accIdMap, AgentAcceptationPair[] agentAcceptationPairs, SentenceSpan[] spans, int numberOfCorrelations, int numberOfCorrelationArrays) {
            this.conversions = conversions;
            this.agents = agents;
            this.agentRules = agentRules;
            this.accIdMap = accIdMap;
            this.agentAcceptationPairs = agentAcceptationPairs;
            this.spans = spans;
            this.numberOfCorrelations = numberOfCorrelations;
            this.numberOfCorrelationArrays = numberOfCorrelationArrays;
        }

        public IntKeyMap<? extends Traversable<Conversion>> composeConversionMap() {
            final MutableIntKeyMap<MutableSet<Conversion>> result = MutableIntKeyMap.empty();
            final SyncCacheIntKeyNonNullValueMap<MutableSet<Conversion>> conversionsCacheMap = new SyncCacheIntKeyNonNullValueMap<>(result, alphabet -> MutableHashSet.empty());
            for (Conversion conversion : conversions) {
                conversionsCacheMap.get(conversion.getSourceAlphabet()).add(conversion);
            }

            return result;
        }
    }
}
