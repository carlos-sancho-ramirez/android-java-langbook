package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.MutableIntList;
import sword.collections.Sizable;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.BunchesManager;
import sword.langbook3.android.db.LangbookDbSchema;

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
abstract class BunchesSerializerTest {

    abstract BunchesManager createManager(MemoryDatabase db);

    void assertEmpty(Sizable sizable) {
        final int size = sizable.size();
        if (size != 0) {
            fail("Expected empty, but had size " + size);
        }
    }

    private static final class AssertStream extends InputStream {
        private final ImmutableIntList data;
        private final int dataSizeInBytes;
        private int byteIndex;

        private AssertStream(ImmutableIntList data, int dataSizeInBytes) {
            if (((dataSizeInBytes + 3) >>> 2) != data.size()) {
                throw new IllegalArgumentException();
            }

            this.data = data;
            this.dataSizeInBytes = dataSizeInBytes;
        }

        @Override
        public int read() {
            if (byteIndex >= dataSizeInBytes) {
                throw new AssertionError("End of the stream already reached");
            }

            final int wordIndex = byteIndex >>> 2;
            final int wordByteIndex = byteIndex & 3;
            ++byteIndex;
            return (data.valueAt(wordIndex) >>> (wordByteIndex * 8)) & 0xFF;
        }

        boolean allBytesRead() {
            return byteIndex == dataSizeInBytes;
        }
    }

    private static final class TestStream extends OutputStream {
        private final MutableIntList data = MutableIntList.empty();
        private int currentWord;
        private int byteCount;

        @Override
        public void write(int b) {
            final int wordByte = byteCount & 3;
            currentWord |= (b & 0xFF) << (wordByte * 8);

            ++byteCount;
            if ((byteCount & 3) == 0) {
                data.append(currentWord);
                currentWord = 0;
            }
        }

        AssertStream toInputStream() {
            final ImmutableIntList inData = ((byteCount & 3) == 0)? data.toImmutable() : data.toImmutable().append(currentWord);
            return new AssertStream(inData, byteCount);
        }
    }

    static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static ImmutableIntSet findAcceptationsMatchingText(DbExporter.Database db, String text) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringColumnIndex(), text)
                .select(strings.getDynamicAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    int getSingleInt(ImmutableIntSet set) {
        final int setSize = set.size();
        if (setSize != 1) {
            fail("Expected set size was 1, but it was " + setSize);
        }

        return set.valueAt(0);
    }

    void assertSingleInt(int expectedValue, ImmutableIntSet set) {
        final int actualValue = getSingleInt(set);
        if (expectedValue != actualValue) {
            fail("Value within the set was expected to be " + expectedValue + " but it was " + actualValue);
        }
    }

    static MemoryDatabase cloneBySerializing(MemoryDatabase inDb) {
        final TestStream outStream = new TestStream();
        try {
            new StreamedDatabaseWriter(inDb, outStream, null).write();
            final AssertStream inStream = outStream.toInputStream();

            final MemoryDatabase newDb = new MemoryDatabase();
            new DatabaseInflater(newDb, inStream, null).read();
            assertTrue(inStream.allBytesRead());
            return newDb;
        }
        catch (IOException e) {
            throw new AssertionError("IOException thrown");
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
