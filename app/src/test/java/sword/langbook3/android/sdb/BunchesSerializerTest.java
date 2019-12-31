package sword.langbook3.android.sdb;

import org.junit.Test;

import sword.collections.ImmutableIntSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.BunchesManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
public abstract class BunchesSerializerTest extends AcceptationsSerializerTest {

    abstract BunchesManager createManager(MemoryDatabase db);

    @Test
    public void testSerializeBunchWithASingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final String text = "cantar";
        final int acceptation = addSimpleAcceptation(inManager, inAlphabet, inConcept, text);

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        assertTrue(inManager.addAcceptationInBunch(bunch, acceptation));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final ImmutableIntSet outAcceptations = findAcceptationsMatchingText(outDb, text);
        assertEquals(1, outAcceptations.size());
        final int outAcceptation = outAcceptations.valueAt(0);

        final ImmutableIntSet outBunchAcceptations = findAcceptationsMatchingText(outDb, bunchText);
        assertEquals(1, outBunchAcceptations.size());
        final int outBunchAcceptation = outBunchAcceptations.valueAt(0);
        assertNotEquals(outBunchAcceptation, outAcceptation);

        final ImmutableIntSet acceptationsInBunch = outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation));
        assertEquals(1, acceptationsInBunch.size());
        assertEquals(outAcceptation, acceptationsInBunch.valueAt(0));

        assertTrue(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outAcceptation)).isEmpty());
    }

    @Test
    public void testSerializeBunchWithMultipleSpanishAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final String singText = "cantar";
        final int inSingAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, singText);

        final int inDrinkConcept = inManager.getMaxConcept() + 1;
        final String drinkText = "beber";
        final int inDrinkAcceptation = addSimpleAcceptation(inManager, inAlphabet, inDrinkConcept, drinkText);

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        assertTrue(inManager.addAcceptationInBunch(bunch, inSingAcceptation));
        assertTrue(inManager.addAcceptationInBunch(bunch, inDrinkAcceptation));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final ImmutableIntSet outSingAcceptations = findAcceptationsMatchingText(outDb, singText);
        assertEquals(1, outSingAcceptations.size());
        final int outSingAcceptation = outSingAcceptations.valueAt(0);

        final ImmutableIntSet outDrinkAcceptations = findAcceptationsMatchingText(outDb, drinkText);
        assertEquals(1, outDrinkAcceptations.size());
        final int outDrinkAcceptation = outDrinkAcceptations.valueAt(0);
        assertNotEquals(outDrinkAcceptation, outSingAcceptation);

        final ImmutableIntSet outBunchAcceptations = findAcceptationsMatchingText(outDb, bunchText);
        assertEquals(1, outBunchAcceptations.size());
        final int outBunchAcceptation = outBunchAcceptations.valueAt(0);
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
    public void testSerializeChainedBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final String singText = "cantar";
        final int singAcceptation = addSimpleAcceptation(inManager, inAlphabet, inConcept, singText);

        final int arVerbBunch = inManager.getMaxConcept() + 1;
        final String arVerbBunchText = "verbo de primera conjugación";
        final int arVerbAcceptation = addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, arVerbBunchText);
        assertTrue(inManager.addAcceptationInBunch(arVerbBunch, singAcceptation));

        final int verbBunch = inManager.getMaxConcept() + 1;
        final String verbBunchText = "verbo";
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, verbBunchText);
        assertTrue(inManager.addAcceptationInBunch(verbBunch, arVerbAcceptation));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final ImmutableIntSet outSingAcceptations = findAcceptationsMatchingText(outDb, singText);
        assertEquals(1, outSingAcceptations.size());
        final int outSingAcceptation = outSingAcceptations.valueAt(0);

        final ImmutableIntSet outArVerbAcceptations = findAcceptationsMatchingText(outDb, arVerbBunchText);
        assertEquals(1, outArVerbAcceptations.size());
        final int outArVerbAcceptation = outArVerbAcceptations.valueAt(0);
        assertNotEquals(outArVerbAcceptation, outSingAcceptation);

        final ImmutableIntSet outVerbAcceptations = findAcceptationsMatchingText(outDb, verbBunchText);
        assertEquals(1, outVerbAcceptations.size());
        final int outVerbAcceptation = outVerbAcceptations.valueAt(0);
        assertNotEquals(outVerbAcceptation, outSingAcceptation);
        assertNotEquals(outVerbAcceptation, outArVerbAcceptation);

        final ImmutableIntSet acceptationsInArVerbBunch = outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outArVerbAcceptation));
        assertEquals(1, acceptationsInArVerbBunch.size());
        assertEquals(outSingAcceptation, acceptationsInArVerbBunch.valueAt(0));

        final ImmutableIntSet acceptationsInVerbBunch = outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outVerbAcceptation));
        assertEquals(1, acceptationsInVerbBunch.size());
        assertEquals(outArVerbAcceptation, acceptationsInVerbBunch.valueAt(0));

        assertTrue(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)).isEmpty());
    }
}
