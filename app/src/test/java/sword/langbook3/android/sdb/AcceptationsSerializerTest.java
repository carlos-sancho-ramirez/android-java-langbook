package sword.langbook3.android.sdb;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.MutableIntList;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.DatabaseInflater;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookDbSchema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;

/**
 * Include all test related to all values that an AcceptationsSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Languages</li>
 * <li>Alphabets</li>
 * <li>Symbol arrays</li>
 * <li>Correlations</li>
 * <li>Correlation arrays</li>
 * <li>Conversions</li>
 * <li>Acceptations</li>
 */
public final class AcceptationsSerializerTest {

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

        public boolean allBytesRead() {
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

        public AssertStream toInputStream() {
            final ImmutableIntList inData = ((byteCount & 3) == 0)? data.toImmutable() : data.toImmutable().append(currentWord);
            return new AssertStream(inData, byteCount);
        }
    }

    private static ImmutableIntSet findAcceptationsWithSimpleCorrelationArray(DbExporter.Database db, int alphabet, String text) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .where(strings.getStringColumnIndex(), text)
                .where(strings.getMainStringColumnIndex(), text)
                .select(strings.getDynamicAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
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

    @Test
    public void testSerializeSingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;
        final int concept = inManager.getMaxConcept() + 1;

        final String text = "cantar";
        addSimpleAcceptation(inManager, inAlphabet, concept, text);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(1, outAlphabets.size());
        final int outAlphabet = outAlphabets.valueAt(0);

        final ImmutableIntSet outAcceptations = findAcceptationsWithSimpleCorrelationArray(outDb, outAlphabet, text);
        assertEquals(1, outAcceptations.size());
    }
}
