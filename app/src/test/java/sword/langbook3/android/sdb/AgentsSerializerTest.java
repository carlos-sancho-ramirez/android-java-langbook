package sword.langbook3.android.sdb;

import org.junit.Test;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.models.AgentDetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
public abstract class AgentsSerializerTest extends BunchesSerializerTest {

    @Override
    abstract AgentsManager createManager(MemoryDatabase db);

    @Test
    public void testSerializeCopyFromSingleSourceToTargetAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        assertNotNull(inManager.addAgent(verbBunch, sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = new LangbookDatabaseManager(outDb);

        final int outAgentId = getSingleInt(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outVerbAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertEquals(1, outAgentDetails.sourceBunches.size());
        assertEquals(outArVerbConcept, outAgentDetails.sourceBunches.valueAt(0));
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }
}
