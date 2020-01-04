package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntSet;
import sword.collections.Sizable;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.BunchesManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
abstract class BunchesSerializerTest extends AcceptationsSerializerTest {

    abstract BunchesManager createManager(MemoryDatabase db);

    void assertEmpty(Sizable sizable) {
        final int size = sizable.size();
        if (size != 0) {
            fail("Expected empty, but had size " + size);
        }
    }

    @Test
    void testSerializeBunchWithASingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final int acceptation = addSimpleAcceptation(inManager, inAlphabet, inConcept, "cantar");

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        assertTrue(inManager.addAcceptationInBunch(bunch, acceptation));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final int outAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));
        final int outBunchAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, bunchText));
        assertNotEquals(outBunchAcceptation, outAcceptation);

        assertSingleInt(outAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation)));
        assertTrue(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outAcceptation)).isEmpty());
    }

    @Test
    void testSerializeBunchWithMultipleSpanishAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inSingAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final int inDrinkConcept = inManager.getMaxConcept() + 1;
        final int inDrinkAcceptation = addSimpleAcceptation(inManager, inAlphabet, inDrinkConcept, "beber");

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        assertTrue(inManager.addAcceptationInBunch(bunch, inSingAcceptation));
        assertTrue(inManager.addAcceptationInBunch(bunch, inDrinkAcceptation));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final int outSingAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));
        final int outDrinkAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "beber"));
        assertNotEquals(outDrinkAcceptation, outSingAcceptation);

        final int outBunchAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, bunchText));
        assertNotEquals(outBunchAcceptation, outSingAcceptation);
        assertNotEquals(outBunchAcceptation, outDrinkAcceptation);

        final ImmutableIntSet acceptationsInBunch = outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation));
        assertEquals(2, acceptationsInBunch.size());
        assertTrue(acceptationsInBunch.contains(outSingAcceptation));
        assertTrue(acceptationsInBunch.contains(outDrinkAcceptation));

        assertTrue(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)).isEmpty());
        assertTrue(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outDrinkAcceptation)).isEmpty());
    }

    @Test
    void testSerializeChainedBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(inManager, inAlphabet, inConcept, "cantar");

        final int arVerbBunch = inManager.getMaxConcept() + 1;
        final int arVerbAcceptation = addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo ar");
        assertTrue(inManager.addAcceptationInBunch(arVerbBunch, singAcceptation));

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");
        assertTrue(inManager.addAcceptationInBunch(verbBunch, arVerbAcceptation));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final int outSingAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));
        final int outArVerbAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "verbo ar"));
        assertNotEquals(outArVerbAcceptation, outSingAcceptation);

        final int outVerbAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "verbo"));
        assertNotEquals(outVerbAcceptation, outSingAcceptation);
        assertNotEquals(outVerbAcceptation, outArVerbAcceptation);

        assertSingleInt(outSingAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outArVerbAcceptation)));
        assertSingleInt(outArVerbAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outVerbAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)));
    }
}
