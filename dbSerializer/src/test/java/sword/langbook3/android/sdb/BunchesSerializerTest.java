package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.BunchesManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.assertContains;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.sdb.IntTraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.SizableTestUtils.assertEmpty;

/**
 * Include all test related to all responsibilities of a BunchesManager.
 *
 * BunchesManager responsibilities include all responsibilities from AcceptationsManager, and include the following ones:
 * <li>Bunches</li>
 */
interface BunchesSerializerTest extends AcceptationsSerializerTest {

    @Override
    BunchesManager createManager(MemoryDatabase db);

    static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static int addSpanishSingAcceptation(AcceptationsManager manager, int alphabet, int concept) {
        return addSimpleAcceptation(manager, alphabet, concept, "cantar");
    }

    @Test
    default void testSerializeBunchWithASingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final int acceptation = addSpanishSingAcceptation(inManager, inAlphabet, inConcept);

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        inManager.addAcceptationInBunch(bunch, acceptation);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final int outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outBunchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, bunchText));
        assertNotEquals(outBunchAcceptation, outAcceptation);

        assertContainsOnly(outAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outAcceptation)));
    }

    @Test
    default void testSerializeBunchWithMultipleSpanishAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final int inSingAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final int inDrinkConcept = inManager.getMaxConcept() + 1;
        final int inDrinkAcceptation = addSimpleAcceptation(inManager, inAlphabet, inDrinkConcept, "saltar");

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        inManager.addAcceptationInBunch(bunch, inSingAcceptation);
        inManager.addAcceptationInBunch(bunch, inDrinkAcceptation);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outDrinkAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "saltar"));
        assertNotEquals(outDrinkAcceptation, outSingAcceptation);

        final int outBunchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, bunchText));
        assertNotEquals(outBunchAcceptation, outSingAcceptation);
        assertNotEquals(outBunchAcceptation, outDrinkAcceptation);

        final ImmutableIntSet acceptationsInBunch = outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation));
        assertEquals(2, acceptationsInBunch.size());
        assertContains(outSingAcceptation, acceptationsInBunch);
        assertContains(outDrinkAcceptation, acceptationsInBunch);

        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outDrinkAcceptation)));
    }

    @Test
    default void testSerializeChainedBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(inManager, inAlphabet, inConcept, "cantar");

        final int arVerbBunch = inManager.getMaxConcept() + 1;
        final int arVerbAcceptation = addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo ar");
        inManager.addAcceptationInBunch(arVerbBunch, singAcceptation);

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");
        inManager.addAcceptationInBunch(verbBunch, arVerbAcceptation);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager outManager = createManager(outDb);

        final int outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "cantar"));
        final int outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo ar"));
        assertNotEquals(outArVerbAcceptation, outSingAcceptation);

        final int outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "verbo"));
        assertNotEquals(outVerbAcceptation, outSingAcceptation);
        assertNotEquals(outVerbAcceptation, outArVerbAcceptation);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outArVerbAcceptation)));
        assertContainsOnly(outArVerbAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outVerbAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)));
    }
}
