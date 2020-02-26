package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sword.collections.ImmutableIntArraySet;
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
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.AgentDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
abstract class AgentsSerializerTest {

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

    private static MemoryDatabase cloneBySerializing(MemoryDatabase inDb) {
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

    private static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    private static ImmutableIntSet findAcceptationsMatchingText(DbExporter.Database db, String text) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringColumnIndex(), text)
                .select(strings.getDynamicAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    abstract AgentsManager createManager(MemoryDatabase db);

    private void assertEmpty(Sizable sizable) {
        final int size = sizable.size();
        if (size != 0) {
            fail("Expected empty, but had size " + size);
        }
    }

    private int getSingleInt(ImmutableIntSet set) {
        final int setSize = set.size();
        if (setSize != 1) {
            fail("Expected set size was 1, but it was " + setSize);
        }

        return set.valueAt(0);
    }

    private void assertSingleInt(int expectedValue, ImmutableIntSet set) {
        final int actualValue = getSingleInt(set);
        if (expectedValue != actualValue) {
            fail("Value within the set was expected to be " + expectedValue + " but it was " + actualValue);
        }
    }

    @Test
    void testSerializeCopyFromSingleSourceToTargetAgentWithoutMatchingAcceptations() {
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

    @Test
    void testSerializeCopyFromSingleSourceToTargetAgentWithAMatchingAcceptation() {
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

        final int outSingAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));
        assertSingleInt(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbConcept));
        assertSingleInt(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbConcept));
    }
}
