package sword.langbook3.android.sdb;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.Map;
import sword.collections.MutableIntList;
import sword.collections.Sizable;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.DatabaseInflater;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.Conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
public abstract class AcceptationsSerializerTest {

    abstract AcceptationsManager createManager(MemoryDatabase db);

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

    private <E> void assertSingleValue(int expectedKey, E expectedValue, ImmutableIntKeyMap<E> map) {
        assertEquals(1, map.size());
        assertEquals(expectedValue, map.get(expectedKey));
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

    private static final String singFirstKanjiText = "歌";
    private static final String singFirstKanaText = "うた";
    private static final String singSecondText = "う";

    private static final String singKanjiText = singFirstKanjiText + singSecondText;
    private static final String singKanaText = singFirstKanaText + singSecondText;

    private static void addJapaneseSingAcceptation(AcceptationsManager manager, int kanjiAlphabet, int kanaAlphabet, int concept) {
        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanjiAlphabet, singFirstKanjiText)
                        .put(kanaAlphabet, singFirstKanaText)
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanjiAlphabet, singSecondText)
                        .put(kanaAlphabet, singSecondText)
                        .build())
                .build();

        manager.addAcceptation(concept, correlationArray);
    }

    private static final String eatFirstKanjiText = "食べ";
    private static final String eatFirstKanaText = "たべ";
    private static final String eatSecondText = "る";

    private static final String eatKanjiText = eatFirstKanjiText + eatSecondText;
    private static final String eatKanaText = eatFirstKanaText + eatSecondText;

    private static final String eatSynonymFirstKanjiText = "食";
    private static final String eatSynonymFirstKanaText = "く";
    private static final String eatSynonymSecondText = "う";

    private static final String eatSynonymKanjiText = eatSynonymFirstKanjiText + eatSynonymSecondText;
    private static final String eatSynonymKanaText = eatSynonymFirstKanaText + eatSynonymSecondText;

    private static void addJapaneseEatAcceptation(AcceptationsManager manager, int kanjiAlphabet, int kanaAlphabet, int concept) {
        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanjiAlphabet, eatFirstKanjiText)
                        .put(kanaAlphabet, eatFirstKanaText)
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanjiAlphabet, eatSecondText)
                        .put(kanaAlphabet, eatSecondText)
                        .build())
                .build();

        manager.addAcceptation(concept, correlationArray);
    }

    private static void addJapaneseEatSynonymAcceptation(AcceptationsManager manager, int kanjiAlphabet, int kanaAlphabet, int concept) {
        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanjiAlphabet, eatSynonymFirstKanjiText)
                        .put(kanaAlphabet, eatSynonymFirstKanaText)
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanjiAlphabet, eatSynonymSecondText)
                        .put(kanaAlphabet, eatSynonymSecondText)
                        .build())
                .build();

        manager.addAcceptation(concept, correlationArray);
    }

    @Test
    public void testSerializeSpanishLanguage() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);
        inManager.addLanguage("es");

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("es");
        assertEquals(1, outManager.findAlphabetsByLanguage(outLang).size());
    }

    @Test
    public void testSerializeJapaneseLanguageWithoutConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;
        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(2, outAlphabets.size());
    }

    @Test
    public void testSerializeSingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final int inConcept = inManager.getMaxConcept() + 1;

        addSimpleAcceptation(inManager, inAlphabet, inConcept, "cantar");

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleInt(outManager.findAlphabetsByLanguage(outLang));
        final int outAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));

        assertSingleValue(outAlphabet, "cantar", outManager.getAcceptationTexts(outAcceptation));
    }

    @Test
    public void testSerializeMultipleSpanishAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept1 = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, inConcept1, "cantar");

        final int inConcept2 = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, inConcept2, "toser");

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleInt(outManager.findAlphabetsByLanguage(outLang));
        final int outAcceptation1 = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));
        assertSingleValue(outAlphabet, "cantar", outManager.getAcceptationTexts(outAcceptation1));

        final int outAcceptation2 = getSingleInt(findAcceptationsMatchingText(outDb, "toser"));
        assertNotEquals(outAcceptation1, outAcceptation2);
        assertNotEquals(outManager.conceptFromAcceptation(outAcceptation1), outManager.conceptFromAcceptation(outAcceptation2));
        assertSingleValue(outAlphabet, "toser", outManager.getAcceptationTexts(outAcceptation2));
    }

    @Test
    public void testSerializeSingleJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;
        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final int inConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(2, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);
        assertTrue(outAlphabets.contains(outKanjiAlphabet));
        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).valueAt(0);
        final int outAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, singKanjiText));

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEquals(2, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
    }

    @Test
    public void testSerializeMultipleJapaneseAcceptationsWithoutConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final int inSingConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inSingConcept);

        final int inEatConcept = inManager.getMaxConcept() + 1;
        addJapaneseEatAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inEatConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(2, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);
        assertTrue(outAlphabets.contains(outKanjiAlphabet));
        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).valueAt(0);
        final int outSingAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, singKanjiText));

        final ImmutableIntKeyMap<String> outSingTexts = outManager.getAcceptationTexts(outSingAcceptation);
        assertEquals(2, outSingTexts.size());
        assertEquals(singKanjiText, outSingTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outSingTexts.get(outKanaAlphabet));

        final int outEatAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, eatKanjiText));
        assertNotEquals(outSingAcceptation, outEatAcceptation);
        assertNotEquals(outManager.conceptFromAcceptation(outSingAcceptation), outManager.conceptFromAcceptation(outEatAcceptation));

        final ImmutableIntKeyMap<String> outEatTexts = outManager.getAcceptationTexts(outEatAcceptation);
        assertEquals(2, outEatTexts.size());
        assertEquals(eatKanjiText, outEatTexts.get(outKanjiAlphabet));
        assertEquals(eatKanaText, outEatTexts.get(outKanaAlphabet));
    }

    @Test
    public void testSerializeJustRoumajiConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final Map<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("う", "u")
                .put("た", "ta")
                .put("べ", "be")
                .put("る", "ru")
                .build();

        final int inRoumajiAlphabet = inManager.getMaxConcept() + 1;
        final Conversion conversion = new Conversion(inKanaAlphabet, inRoumajiAlphabet, conversionMap);
        assertTrue(inManager.addAlphabetAsConversionTarget(conversion));

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(3, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);

        final ImmutableIntPairMap conversionPairs = outManager.getConversionsMap();
        final int outRoumajiAlphabet = getSingleInt(outAlphabets.filter(conversionPairs.keySet()::contains));
        assertNotEquals(outKanjiAlphabet, outRoumajiAlphabet);

        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).remove(outRoumajiAlphabet).valueAt(0);
        assertEquals(outKanaAlphabet, conversionPairs.get(outRoumajiAlphabet));

        final int concept = outManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(outManager, outKanjiAlphabet, outKanaAlphabet, concept);

        final int outAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "utau"));

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEquals(3, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
        assertEquals("utau", outTexts.get(outRoumajiAlphabet));
    }

    @Test
    public void testSerializeSingleJapaneseAcceptationWithConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final Map<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("う", "u")
                .put("た", "ta")
                .put("べ", "be")
                .put("る", "ru")
                .build();

        final int inRoumajiAlphabet = inManager.getMaxConcept() + 1;
        final Conversion conversion = new Conversion(inKanaAlphabet, inRoumajiAlphabet, conversionMap);
        assertTrue(inManager.addAlphabetAsConversionTarget(conversion));

        final int inConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(3, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);

        final ImmutableIntPairMap conversionPairs = outManager.getConversionsMap();
        final int outRoumajiAlphabet = getSingleInt(outAlphabets.filter(conversionPairs.keySet()::contains));
        assertNotEquals(outKanjiAlphabet, outRoumajiAlphabet);

        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).remove(outRoumajiAlphabet).valueAt(0);
        assertEquals(outKanaAlphabet, conversionPairs.get(outRoumajiAlphabet));
        final int outAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "utau"));

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEquals(3, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
        assertEquals("utau", outTexts.get(outRoumajiAlphabet));
    }

    @Test
    public void testSerializeMultipleJapaneseAcceptationsWithConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final String languageCode = "ja";
        final int inKanjiAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final Map<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("う", "u")
                .put("た", "ta")
                .put("べ", "be")
                .put("る", "ru")
                .build();

        final int inRoumajiAlphabet = inManager.getMaxConcept() + 1;
        final Conversion conversion = new Conversion(inKanaAlphabet, inRoumajiAlphabet, conversionMap);
        assertTrue(inManager.addAlphabetAsConversionTarget(conversion));

        final int inSingConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inSingConcept);

        final int inEatConcept = inManager.getMaxConcept() + 1;
        addJapaneseEatAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inEatConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(3, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);

        final ImmutableIntPairMap conversionPairs = outManager.getConversionsMap();
        final int outRoumajiAlphabet = getSingleInt(outAlphabets.filter(conversionPairs.keySet()::contains));
        assertNotEquals(outKanjiAlphabet, outRoumajiAlphabet);

        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).remove(outRoumajiAlphabet).valueAt(0);
        assertEquals(outKanaAlphabet, conversionPairs.get(outRoumajiAlphabet));
        final int outSingAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "utau"));

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outSingAcceptation);
        assertEquals(3, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
        assertEquals("utau", outTexts.get(outRoumajiAlphabet));

        final int outEatAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "taberu"));
        assertNotEquals(outManager.conceptFromAcceptation(outSingAcceptation), outManager.conceptFromAcceptation(outEatAcceptation));

        final ImmutableIntKeyMap<String> outEatTexts = outManager.getAcceptationTexts(outEatAcceptation);
        assertEquals(3, outEatTexts.size());
        assertEquals(eatKanjiText, outEatTexts.get(outKanjiAlphabet));
        assertEquals(eatKanaText, outEatTexts.get(outKanaAlphabet));
        assertEquals("taberu", outEatTexts.get(outRoumajiAlphabet));
    }

    @Test
    public void testSerializeSpanishSynonyms() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, inConcept, "saltar");
        addSimpleAcceptation(inManager, inAlphabet, inConcept, "brincar");

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("es");
        final int outAlphabet = getSingleInt(outManager.findAlphabetsByLanguage(outLang));
        final int outAcceptation1 = getSingleInt(findAcceptationsMatchingText(outDb, "saltar"));
        assertSingleValue(outAlphabet, "saltar", outManager.getAcceptationTexts(outAcceptation1));

        final int outAcceptation2 = getSingleInt(findAcceptationsMatchingText(outDb, "brincar"));
        assertNotEquals(outAcceptation1, outAcceptation2);
        assertEquals(outManager.conceptFromAcceptation(outAcceptation1), outManager.conceptFromAcceptation(outAcceptation2));
        assertSingleValue(outAlphabet, "brincar", outManager.getAcceptationTexts(outAcceptation2));
    }

    @Test
    public void testSerializeJapaneseSynonyms() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final int inConcept = inManager.getMaxConcept() + 1;
        addJapaneseEatAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);
        addJapaneseEatSynonymAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLang = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(2, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);
        assertTrue(outAlphabets.contains(outKanjiAlphabet));
        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).valueAt(0);

        final int outEatAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, eatKanjiText));

        final ImmutableIntKeyMap<String> outEatTexts = outManager.getAcceptationTexts(outEatAcceptation);
        assertEquals(2, outEatTexts.size());
        assertEquals(eatKanjiText, outEatTexts.get(outKanjiAlphabet));
        assertEquals(eatKanaText, outEatTexts.get(outKanaAlphabet));

        final int outEatSynonymAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, eatSynonymKanjiText));
        assertNotEquals(outEatAcceptation, outEatSynonymAcceptation);
        assertEquals(outManager.conceptFromAcceptation(outEatAcceptation), outManager.conceptFromAcceptation(outEatSynonymAcceptation));

        final ImmutableIntKeyMap<String> outEatSynonymTexts = outManager.getAcceptationTexts(outEatSynonymAcceptation);
        assertEquals(2, outEatSynonymTexts.size());
        assertEquals(eatSynonymKanjiText, outEatSynonymTexts.get(outKanjiAlphabet));
        assertEquals(eatSynonymKanaText, outEatSynonymTexts.get(outKanaAlphabet));
    }

    @Test
    public void testSerializeAPairOfTranslations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = createManager(inDb);

        final int inSpanishAlphabet = inManager.addLanguage("es").mainAlphabet;

        final int inKanjiAlphabet = inManager.addLanguage("ja").mainAlphabet;
        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final int inConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inSpanishAlphabet, inConcept, "cantar");
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = createManager(outDb);

        final int outSpanish = outManager.findLanguageByCode("es");
        final int outSpanishAlphabet = getSingleInt(outManager.findAlphabetsByLanguage(outSpanish));

        final int outJapanese = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outJapaneseAlphabets = outManager.findAlphabetsByLanguage(outJapanese);
        assertEquals(2, outJapaneseAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outJapanese);
        final int outKanaAlphabet = outJapaneseAlphabets.remove(outKanjiAlphabet).valueAt(0);
        final int outSpanishAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, "cantar"));
        assertSingleValue(outSpanishAlphabet, "cantar", outManager.getAcceptationTexts(outSpanishAcceptation));

        final int outJapaneseAcceptation = getSingleInt(findAcceptationsMatchingText(outDb, singKanjiText));

        assertNotEquals(outSpanishAcceptation, outJapaneseAcceptation);
        final int outConcept = outManager.conceptFromAcceptation(outSpanishAcceptation);
        assertEquals(outConcept, outManager.conceptFromAcceptation(outJapaneseAcceptation));

        final ImmutableIntKeyMap<String> outJapaneseTexts = outManager.getAcceptationTexts(outJapaneseAcceptation);
        assertEquals(2, outJapaneseTexts.size());
        assertEquals(singKanjiText, outJapaneseTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outJapaneseTexts.get(outKanaAlphabet));
    }
}
