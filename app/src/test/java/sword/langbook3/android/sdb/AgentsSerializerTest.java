package sword.langbook3.android.sdb;

import org.junit.Test;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.BunchesManager;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.models.AgentDetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;

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

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int arVerbBunch = inManager.getMaxConcept() + 1;
        final String arVerbBunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, arVerbBunchText);

        final int verbBunch = inManager.getMaxConcept() + 1;
        final String verbBunchText = "verbo";
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, verbBunchText);

        final ImmutableIntSet noBunches = ImmutableIntArraySet.empty();
        final ImmutableIntSet sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableIntKeyMap<String> emptyCorrelation = ImmutableIntKeyMap.empty();
        assertNotNull(inManager.addAgent(verbBunch, sourceBunches, noBunches, emptyCorrelation, emptyCorrelation, emptyCorrelation, emptyCorrelation, 0));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsManager outManager = new LangbookDatabaseManager(outDb);

        final ImmutableIntSet outAgentIds = outManager.getAgentIds();
        assertEquals(1, outAgentIds.size());
        final int outAgentId = outAgentIds.valueAt(0);

        final ImmutableIntSet outArVerbAcceptations = findAcceptationsMatchingText(outDb, arVerbBunchText);
        assertEquals(1, outArVerbAcceptations.size());
        final int outArVerbAcceptation = outArVerbAcceptations.valueAt(0);
        final int outArVerbConcept = outManager.conceptFromAcceptation(outArVerbAcceptation);

        final ImmutableIntSet outVerbAcceptations = findAcceptationsMatchingText(outDb, verbBunchText);
        assertEquals(1, outVerbAcceptations.size());
        final int outVerbAcceptation = outVerbAcceptations.valueAt(0);
        final int outVerbConcept = outManager.conceptFromAcceptation(outVerbAcceptation);
        assertNotEquals(outArVerbConcept, outVerbConcept);

        final AgentDetails outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEquals(outVerbConcept, outAgentDetails.targetBunch);
        assertEquals(1, outAgentDetails.sourceBunches.size());
        assertEquals(outArVerbConcept, outAgentDetails.sourceBunches.valueAt(0));
        assertTrue(outAgentDetails.diffBunches.isEmpty());
        assertTrue(outAgentDetails.startMatcher.isEmpty());
        assertTrue(outAgentDetails.startAdder.isEmpty());
        assertTrue(outAgentDetails.endMatcher.isEmpty());
        assertTrue(outAgentDetails.endAdder.isEmpty());

        assertTrue(outManager.getAcceptationsInBunch(outArVerbConcept).isEmpty());
        assertTrue(outManager.getAcceptationsInBunch(outVerbConcept).isEmpty());
    }
}
