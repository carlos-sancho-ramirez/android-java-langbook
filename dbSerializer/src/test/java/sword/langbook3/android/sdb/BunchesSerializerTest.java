package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.BunchesManager;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.TraversableTestUtils.assertContains;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.findAcceptationsMatchingText;

/**
 * Include all test related to all responsibilities of a BunchesManager.
 *
 * BunchesManager responsibilities include all responsibilities from AcceptationsManager, and include the following ones:
 * <li>Bunches</li>
 */
interface BunchesSerializerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId> extends AcceptationsSerializerTest<LanguageId, AlphabetId, CorrelationId, AcceptationId> {

    @Override
    BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> createManager(MemoryDatabase db);

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSimpleAcceptation(AcceptationsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, int concept, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSpanishSingAcceptation(AcceptationsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, int concept) {
        return addSimpleAcceptation(manager, alphabet, concept, "cantar");
    }

    @Test
    default void testSerializeBunchWithASingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final AcceptationId acceptation = addSpanishSingAcceptation(inManager, inAlphabet, inConcept);

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        inManager.addAcceptationInBunch(bunch, acceptation);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> outManager = createManager(outDb);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final AcceptationId outBunchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), bunchText));
        assertNotEquals(outBunchAcceptation, outAcceptation);

        assertContainsOnly(outAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outAcceptation)));
    }

    @Test
    default void testSerializeBunchWithMultipleSpanishAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inSingConcept = inManager.getMaxConcept() + 1;
        final AcceptationId inSingAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final int inDrinkConcept = inManager.getMaxConcept() + 1;
        final AcceptationId inDrinkAcceptation = addSimpleAcceptation(inManager, inAlphabet, inDrinkConcept, "saltar");

        final int bunch = inManager.getMaxConcept() + 1;
        final String bunchText = "verbo de primera conjugación";
        addSimpleAcceptation(inManager, inAlphabet, bunch, bunchText);
        inManager.addAcceptationInBunch(bunch, inSingAcceptation);
        inManager.addAcceptationInBunch(bunch, inDrinkAcceptation);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> outManager = createManager(outDb);

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final AcceptationId outDrinkAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "saltar"));
        assertNotEquals(outDrinkAcceptation, outSingAcceptation);

        final AcceptationId outBunchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), bunchText));
        assertNotEquals(outBunchAcceptation, outSingAcceptation);
        assertNotEquals(outBunchAcceptation, outDrinkAcceptation);

        final ImmutableSet<AcceptationId> acceptationsInBunch = outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outBunchAcceptation));
        assertEquals(2, acceptationsInBunch.size());
        assertContains(outSingAcceptation, acceptationsInBunch);
        assertContains(outDrinkAcceptation, acceptationsInBunch);

        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outDrinkAcceptation)));
    }

    @Test
    default void testSerializeChainedBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> inManager = createManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        final AcceptationId singAcceptation = addSimpleAcceptation(inManager, inAlphabet, inConcept, "cantar");

        final int arVerbBunch = inManager.getMaxConcept() + 1;
        final AcceptationId arVerbAcceptation = addSimpleAcceptation(inManager, inAlphabet, arVerbBunch, "verbo ar");
        inManager.addAcceptationInBunch(arVerbBunch, singAcceptation);

        final int verbBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, verbBunch, "verbo");
        inManager.addAcceptationInBunch(verbBunch, arVerbAcceptation);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> outManager = createManager(outDb);

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final AcceptationId outArVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "verbo ar"));
        assertNotEquals(outArVerbAcceptation, outSingAcceptation);

        final AcceptationId outVerbAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "verbo"));
        assertNotEquals(outVerbAcceptation, outSingAcceptation);
        assertNotEquals(outVerbAcceptation, outArVerbAcceptation);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outArVerbAcceptation)));
        assertContainsOnly(outArVerbAcceptation, outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outVerbAcceptation)));
        assertEmpty(outManager.getAcceptationsInBunch(outManager.conceptFromAcceptation(outSingAcceptation)));
    }
}
