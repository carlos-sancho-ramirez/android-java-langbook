package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.models.AgentDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.SizableTestUtils.assertEmpty;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializerTest extends BunchesSerializerTest {

    static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    AgentsManager createManager(MemoryDatabase db);

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithoutMatchingAcceptations() {
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
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbConcept));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbConcept));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int arVerbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo de primera conjugación");

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        assertTrue(inManager.addAcceptationInBunch(arVerbBunch, inAcceptation));

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        assertNotNull(inManager.addAgent(verbBunch, sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = createManager(outDb);

        final int outAgentId = getSingleValue(outManager.getAgentIds());

        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo de primera conjugación"));
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertContainsOnly(outArVerbConcept, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
    }
}
