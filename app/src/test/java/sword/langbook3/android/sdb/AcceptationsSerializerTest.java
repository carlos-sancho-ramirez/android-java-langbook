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
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.DatabaseInflater;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.Conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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

    private static ImmutableIntSet findAcceptationsMatchingText(DbExporter.Database db, String text) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringColumnIndex(), text)
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

    @Test
    public void testSerializeSingleSpanishAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;
        final int inConcept = inManager.getMaxConcept() + 1;

        final String text = "cantar";
        addSimpleAcceptation(inManager, inAlphabet, inConcept, text);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(1, outAlphabets.size());
        final int outAlphabet = outAlphabets.valueAt(0);

        final ImmutableIntSet outAcceptations = findAcceptationsMatchingText(outDb, text);
        assertEquals(1, outAcceptations.size());
        final int outAcceptation = outAcceptations.valueAt(0);

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEquals(1, outTexts.size());
        assertEquals(text, outTexts.get(outAlphabet));
    }

    @Test
    public void testSerializeMultipleSpanishAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final String text1 = "cantar";
        final int inConcept1 = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, inConcept1, text1);

        final String text2 = "toser";
        final int inConcept2 = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, inConcept2, text2);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(1, outAlphabets.size());
        final int outAlphabet = outAlphabets.valueAt(0);

        final ImmutableIntSet outAcceptations1 = findAcceptationsMatchingText(outDb, text1);
        assertEquals(1, outAcceptations1.size());
        final int outAcceptation1 = outAcceptations1.valueAt(0);

        final ImmutableIntKeyMap<String> outTexts1 = outManager.getAcceptationTexts(outAcceptations1.valueAt(0));
        assertEquals(1, outTexts1.size());
        assertEquals(text1, outTexts1.get(outAlphabet));

        final ImmutableIntSet outAcceptations2 = findAcceptationsMatchingText(outDb, text2);
        assertEquals(1, outAcceptations1.size());
        final int outAcceptation2 = outAcceptations2.valueAt(0);
        assertNotEquals(outAcceptation1, outAcceptation2);
        assertNotEquals(outManager.conceptFromAcceptation(outAcceptation1), outManager.conceptFromAcceptation(outAcceptation2));

        final ImmutableIntKeyMap<String> outTexts2 = outManager.getAcceptationTexts(outAcceptations2.valueAt(0));
        assertEquals(1, outTexts2.size());
        assertEquals(text2, outTexts2.get(outAlphabet));
    }

    @Test
    public void testSerializeSingleJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String languageCode = "ja";
        final int inKanjiAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final int inConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(2, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);
        assertTrue(outAlphabets.contains(outKanjiAlphabet));
        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).valueAt(0);

        final ImmutableIntSet outAcceptations = findAcceptationsMatchingText(outDb, singKanjiText);
        assertEquals(1, outAcceptations.size());
        final int outAcceptation = outAcceptations.valueAt(0);

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEquals(2, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
    }

    @Test
    public void testSerializeMultipleJapaneseAcceptationsWithoutConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String languageCode = "ja";
        final int inKanjiAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final int inSingConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inSingConcept);

        final int inEatConcept = inManager.getMaxConcept() + 1;
        addJapaneseEatAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inEatConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(2, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);
        assertTrue(outAlphabets.contains(outKanjiAlphabet));
        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).valueAt(0);

        final ImmutableIntSet outSingAcceptations = findAcceptationsMatchingText(outDb, singKanjiText);
        assertEquals(1, outSingAcceptations.size());
        final int outSingAcceptation = outSingAcceptations.valueAt(0);

        final ImmutableIntKeyMap<String> outSingTexts = outManager.getAcceptationTexts(outSingAcceptation);
        assertEquals(2, outSingTexts.size());
        assertEquals(singKanjiText, outSingTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outSingTexts.get(outKanaAlphabet));

        final ImmutableIntSet outEatAcceptations = findAcceptationsMatchingText(outDb, eatKanjiText);
        assertEquals(1, outEatAcceptations.size());
        final int outEatAcceptation = outEatAcceptations.valueAt(0);
        assertNotEquals(outSingAcceptation, outEatAcceptation);
        assertNotEquals(outManager.conceptFromAcceptation(outSingAcceptation), outManager.conceptFromAcceptation(outEatAcceptation));

        final ImmutableIntKeyMap<String> outEatTexts = outManager.getAcceptationTexts(outEatAcceptation);
        assertEquals(2, outEatTexts.size());
        assertEquals(eatKanjiText, outEatTexts.get(outKanjiAlphabet));
        assertEquals(eatKanaText, outEatTexts.get(outKanaAlphabet));
    }

    @Test
    public void testSerializeSingleJapaneseAcceptationWithConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

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

        final int inConcept = inManager.getMaxConcept() + 1;
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(3, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);

        final ImmutableIntPairMap conversionPairs = outManager.getConversionsMap();
        final ImmutableIntSet outSourceConversionAlphabets = outAlphabets.filter(conversionPairs.keySet()::contains);
        assertEquals(1, outSourceConversionAlphabets.size());
        final int outRoumajiAlphabet = outSourceConversionAlphabets.valueAt(0);
        assertNotEquals(outKanjiAlphabet, outRoumajiAlphabet);

        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).remove(outRoumajiAlphabet).valueAt(0);
        assertEquals(outKanaAlphabet, conversionPairs.get(outRoumajiAlphabet));

        final String singRoumajiText = "utau";
        final ImmutableIntSet outAcceptations = findAcceptationsMatchingText(outDb, singRoumajiText);
        assertEquals(1, outAcceptations.size());
        final int outAcceptation = outAcceptations.valueAt(0);

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEquals(3, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
        assertEquals(singRoumajiText, outTexts.get(outRoumajiAlphabet));
    }

    @Test
    public void testSerializeMultipleJapaneseAcceptationsWithConversion() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

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
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(3, outAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outLang);

        final ImmutableIntPairMap conversionPairs = outManager.getConversionsMap();
        final ImmutableIntSet outSourceConversionAlphabets = outAlphabets.filter(conversionPairs.keySet()::contains);
        assertEquals(1, outSourceConversionAlphabets.size());
        final int outRoumajiAlphabet = outSourceConversionAlphabets.valueAt(0);
        assertNotEquals(outKanjiAlphabet, outRoumajiAlphabet);

        final int outKanaAlphabet = outAlphabets.remove(outKanjiAlphabet).remove(outRoumajiAlphabet).valueAt(0);
        assertEquals(outKanaAlphabet, conversionPairs.get(outRoumajiAlphabet));

        final String singRoumajiText = "utau";
        final ImmutableIntSet outSingAcceptations = findAcceptationsMatchingText(outDb, singRoumajiText);
        assertEquals(1, outSingAcceptations.size());
        final int outSingAcceptation = outSingAcceptations.valueAt(0);

        final ImmutableIntKeyMap<String> outTexts = outManager.getAcceptationTexts(outSingAcceptation);
        assertEquals(3, outTexts.size());
        assertEquals(singKanjiText, outTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outTexts.get(outKanaAlphabet));
        assertEquals(singRoumajiText, outTexts.get(outRoumajiAlphabet));

        final String eatRoumajiText = "taberu";
        final ImmutableIntSet outEatAcceptations = findAcceptationsMatchingText(outDb, eatRoumajiText);
        assertEquals(1, outEatAcceptations.size());
        final int outEatAcceptation = outEatAcceptations.valueAt(0);
        assertNotEquals(outManager.conceptFromAcceptation(outSingAcceptation), outManager.conceptFromAcceptation(outEatAcceptation));

        final ImmutableIntKeyMap<String> outEatTexts = outManager.getAcceptationTexts(outEatAcceptation);
        assertEquals(3, outEatTexts.size());
        assertEquals(eatKanjiText, outEatTexts.get(outKanjiAlphabet));
        assertEquals(eatKanaText, outEatTexts.get(outKanaAlphabet));
        assertEquals(eatRoumajiText, outEatTexts.get(outRoumajiAlphabet));
    }

    @Test
    public void testSerializeSpanishSynonyms() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String languageCode = "es";
        final int inAlphabet = inManager.addLanguage(languageCode).mainAlphabet;

        final String text1 = "saltar";
        final int inConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inAlphabet, inConcept, text1);

        final String text2 = "bricar";
        addSimpleAcceptation(inManager, inAlphabet, inConcept, text2);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outLang = outManager.findLanguageByCode(languageCode);
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLang);
        assertEquals(1, outAlphabets.size());
        final int outAlphabet = outAlphabets.valueAt(0);

        final ImmutableIntSet outAcceptations1 = findAcceptationsMatchingText(outDb, text1);
        assertEquals(1, outAcceptations1.size());
        final int outAcceptation1 = outAcceptations1.valueAt(0);

        final ImmutableIntKeyMap<String> outTexts1 = outManager.getAcceptationTexts(outAcceptations1.valueAt(0));
        assertEquals(1, outTexts1.size());
        assertEquals(text1, outTexts1.get(outAlphabet));

        final ImmutableIntSet outAcceptations2 = findAcceptationsMatchingText(outDb, text2);
        assertEquals(1, outAcceptations1.size());
        final int outAcceptation2 = outAcceptations2.valueAt(0);

        assertNotEquals(outAcceptation1, outAcceptation2);
        assertEquals(outManager.conceptFromAcceptation(outAcceptation1), outManager.conceptFromAcceptation(outAcceptation2));

        final ImmutableIntKeyMap<String> outTexts2 = outManager.getAcceptationTexts(outAcceptations2.valueAt(0));
        assertEquals(1, outTexts2.size());
        assertEquals(text2, outTexts2.get(outAlphabet));
    }

    @Test
    public void testSerializeAPairOfTranslations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AcceptationsManager inManager = new LangbookDatabaseManager(inDb);

        final String spanishLangCode = "es";
        final int inSpanishAlphabet = inManager.addLanguage(spanishLangCode).mainAlphabet;

        final String japaneseLangCode = "ja";
        final int inKanjiAlphabet = inManager.addLanguage(japaneseLangCode).mainAlphabet;
        final int inKanaAlphabet = inManager.getMaxConcept() + 1;
        assertTrue(inManager.addAlphabetCopyingFromOther(inKanaAlphabet, inKanjiAlphabet));

        final String singSpanishText = "cantar";
        final int inConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, inSpanishAlphabet, inConcept, singSpanishText);
        addJapaneseSingAcceptation(inManager, inKanjiAlphabet, inKanaAlphabet, inConcept);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AcceptationsManager outManager = new LangbookDatabaseManager(outDb);

        final int outSpanish = outManager.findLanguageByCode(spanishLangCode);
        final ImmutableIntSet outSpanishAlphabets = outManager.findAlphabetsByLanguage(outSpanish);
        assertEquals(1, outSpanishAlphabets.size());
        final int outSpanishAlphabet = outSpanishAlphabets.valueAt(0);

        final int outJapanese = outManager.findLanguageByCode(japaneseLangCode);
        final ImmutableIntSet outJapaneseAlphabets = outManager.findAlphabetsByLanguage(outJapanese);
        assertEquals(2, outJapaneseAlphabets.size());
        final int outKanjiAlphabet = outManager.findMainAlphabetForLanguage(outJapanese);
        final int outKanaAlphabet = outJapaneseAlphabets.remove(outKanjiAlphabet).valueAt(0);

        final ImmutableIntSet outSpanishAcceptations = findAcceptationsMatchingText(outDb, singSpanishText);
        assertEquals(1, outSpanishAcceptations.size());
        final int outSpanishAcceptation = outSpanishAcceptations.valueAt(0);

        final ImmutableIntKeyMap<String> outSpanishTexts = outManager.getAcceptationTexts(outSpanishAcceptation);
        assertEquals(1, outSpanishTexts.size());
        assertEquals(singSpanishText, outSpanishTexts.get(outSpanishAlphabet));

        final ImmutableIntSet outJapaneseAcceptations = findAcceptationsMatchingText(outDb, singKanjiText);
        assertEquals(1, outJapaneseAcceptations.size());
        final int outJapaneseAcceptation = outJapaneseAcceptations.valueAt(0);

        assertNotEquals(outSpanishAcceptation, outJapaneseAcceptation);
        final int outConcept = outManager.conceptFromAcceptation(outSpanishAcceptation);
        assertEquals(outConcept, outManager.conceptFromAcceptation(outJapaneseAcceptation));

        final ImmutableIntKeyMap<String> outJapaneseTexts = outManager.getAcceptationTexts(outJapaneseAcceptation);
        assertEquals(2, outJapaneseTexts.size());
        assertEquals(singKanjiText, outJapaneseTexts.get(outKanjiAlphabet));
        assertEquals(singKanaText, outJapaneseTexts.get(outKanaAlphabet));
    }
}
