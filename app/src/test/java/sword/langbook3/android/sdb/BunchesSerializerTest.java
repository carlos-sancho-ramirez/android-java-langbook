package sword.langbook3.android.sdb;

import org.junit.Test;

import sword.collections.ImmutableIntSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.BunchesManager;
import sword.langbook3.android.db.LangbookDatabaseManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
public final class BunchesSerializerTest {

    @Test
    public void testSerializeBunchWithASingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final BunchesManager inManager = new LangbookDatabaseManager(inDb);

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
        final BunchesManager outManager = new LangbookDatabaseManager(outDb);

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
    }
}
