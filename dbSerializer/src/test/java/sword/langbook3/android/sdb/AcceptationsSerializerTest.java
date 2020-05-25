package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.MutableHashMap;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.db.AcceptationsChecker;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.IntSetTestUtils.assertEqualSet;
import static sword.collections.IntTraversableTestUtils.assertContains;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;
import static sword.collections.IntTraversableTestUtils.assertNotContains;
import static sword.collections.IntTraversableTestUtils.getSingleValue;
import static sword.collections.SizableTestUtils.assertSize;

/**
 * Include all test related to all responsibilities of an AcceptationsManager.
 *
 * AcceptationsManager responsibilities are limited to the management of:
 * <li>Languages</li>
 * <li>Alphabets</li>
 * <li>Symbol arrays</li>
 * <li>Correlations</li>
 * <li>Correlation arrays</li>
 * <li>Conversions</li>
 * <li>Acceptations</li>
 */
public interface AcceptationsSerializerTest {

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

    static ImmutableIntSet findAcceptationsMatchingText(DbExporter.Database db, String text) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringColumnIndex(), text)
                .select(strings.getDynamicAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    AcceptationsManager createManager(MemoryDatabase db);

    ImmutableHashMap<String, String> upperCaseConversion = new ImmutableHashMap.Builder<String, String>()
            .put("a", "A")
            .put("b", "B")
            .put("c", "C")
            .put("d", "D")
            .put("e", "E")
            .put("f", "F")
            .put("g", "G")
            .put("h", "H")
            .put("i", "I")
            .put("j", "J")
            .put("k", "K")
            .put("l", "L")
            .put("m", "M")
            .put("n", "N")
            .put("o", "O")
            .put("p", "P")
            .put("q", "Q")
            .put("r", "R")
            .put("s", "S")
            .put("t", "T")
            .put("u", "U")
            .put("v", "V")
            .put("w", "W")
            .put("x", "X")
            .put("y", "Y")
            .put("z", "Z")
            .build();

    static int addSimpleAcceptation(
            AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    @Test
    default void testForEmptyDatabase() {
        cloneBySerializing(new MemoryDatabase());
    }

    @Test
    default void testAddFirstLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        manager.addLanguage("es");
        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("es").intValue();
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(outLanguage);
        assertSize(1, outAlphabets);
        assertNotEquals(outLanguage, outAlphabets.valueAt(0));
    }

    @Test
    default void testRemoveFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        int inEsLanguage = manager.addLanguage("es").language;
        manager.addLanguage("en");
        manager.removeLanguage(inEsLanguage);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager outManager = createManager(outDb);

        assertNull(outManager.findLanguageByCode("es"));
        final int outEnLanguage = outManager.findLanguageByCode("en");
        final ImmutableIntSet outEnAlphabets = outManager.findAlphabetsByLanguage(outEnLanguage);
        assertSize(1, outEnAlphabets);
        assertNotEquals(outEnLanguage, outEnAlphabets.valueAt(0));
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);
        final LanguageCreationResult langPair = manager.addLanguage("es");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;
        final int secondAlphabet = Math.max(language, mainAlphabet) + 1;
        manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager outManager = createManager(outDb);

        final int outEsLanguage = outManager.findLanguageByCode("es");
        final ImmutableIntSet outEsAlphabets = outManager.findAlphabetsByLanguage(outEsLanguage);
        assertSize(2, outEsAlphabets);
        assertNotContains(outEsLanguage, outEsAlphabets);
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);
        final LanguageCreationResult langPair = manager.addLanguage("ja");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;
        addSimpleAcceptation(manager, mainAlphabet, language, "日本語");
        manager.addAlphabetCopyingFromOther(manager.getMaxConcept() + 1, mainAlphabet);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager outManager = createManager(outDb);

        final int outLanguage = outManager.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = outManager.findAlphabetsByLanguage(language);
        assertSize(2, outAlphabets);
        assertNotContains(outLanguage, outAlphabets);

        final int outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "日本語"));
        final ImmutableIntKeyMap<String> acceptationTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEqualSet(outAlphabets, acceptationTexts.keySet());
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;
        final int secondAlphabet = mainAlphabet + 1;
        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker checker = createManager(outDb);

        final int outLanguage = checker.findLanguageByCode("es");
        final ImmutableIntSet outAlphabets = checker.findAlphabetsByLanguage(language);
        assertSize(2, outAlphabets);
        assertNotContains(outLanguage, outAlphabets);

        final int outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);
        final int outSecondAlphabet = outAlphabets.remove(outMainAlphabet).valueAt(0);

        assertEquals("CASA", checker.getConversion(new ImmutableIntPair(outMainAlphabet, outSecondAlphabet)).convert("casa"));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, mainAlphabet, concept, "casa");

        final int secondAlphabet = manager.getMaxConcept() + 1;
        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker checker = createManager(outDb);

        final int outLanguage = checker.findLanguageByCode("es");
        final ImmutableIntSet outAlphabets = checker.findAlphabetsByLanguage(language);
        assertSize(2, outAlphabets);
        assertNotContains(outLanguage, outAlphabets);

        final int outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);
        final int outSecondAlphabet = outAlphabets.remove(outMainAlphabet).valueAt(0);

        final int outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "casa"));
        final ImmutableIntKeyMap<String> outTexts = checker.getAcceptationTexts(outAcceptation);
        assertEqualSet(outAlphabets, outTexts.keySet());
        assertEquals("casa", outTexts.get(outMainAlphabet));
        assertEquals("CASA", outTexts.get(outSecondAlphabet));
    }

    @Test
    default void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final MemoryDatabase outDb = cloneBySerializing(db);
        assertSize(1, findAcceptationsMatchingText(outDb, "cantar"));
    }

    @Test
    default void testAddJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int kanji = manager.addLanguage("ja").mainAlphabet;
        final int kana = manager.getMaxConcept() + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));
        final int concept = manager.getMaxConcept() + 1;

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArrays = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        manager.addAcceptation(concept, correlationArrays);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker checker = createManager(outDb);

        final int outLanguage = checker.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);
        assertNotContains(outLanguage, outAlphabets);

        final int outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);
        final int outSecondAlphabet = outAlphabets.remove(outMainAlphabet).valueAt(0);

        final int outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "注文"));
        final ImmutableIntList correlationIds = checker.getAcceptationCorrelationArray(outAcceptation);
        assertSize(2, correlationIds);

        final ImmutableIntKeyMap<String> outFirstCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(0));
        assertSize(2, outFirstCorrelation);
        assertEquals("注", outFirstCorrelation.get(outMainAlphabet));
        assertEquals("ちゅう", outFirstCorrelation.get(outSecondAlphabet));

        final ImmutableIntKeyMap<String> outSecondCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(1));
        assertSize(2, outSecondCorrelation);
        assertEquals("文", outSecondCorrelation.get(outMainAlphabet));
        assertEquals("もん", outSecondCorrelation.get(outSecondAlphabet));

        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(outAcceptation);
        assertSize(2, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
    }

    @Test
    default void testAddJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int kanji = manager.addLanguage("ja").mainAlphabet;
        final int kana = manager.getMaxConcept() + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));

        final int roumaji = manager.getMaxConcept() + 1;
        final MutableHashMap<String, String> convMap = new MutableHashMap.Builder<String, String>()
                .put("あ", "a")
                .put("も", "mo")
                .put("ん", "n")
                .put("う", "u")
                .put("ちゅ", "chu")
                .put("ち", "chi")
                .build();
        final Conversion conversion = new Conversion(kana, roumaji, convMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));
        final int concept = manager.getMaxConcept() + 1;

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        manager.addAcceptation(concept, correlationArray);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker checker = createManager(outDb);

        final int outLanguage = checker.findLanguageByCode("ja");
        final ImmutableIntSet outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertNotContains(outLanguage, outAlphabets);

        final int outKanjiAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outKanjiAlphabet, outAlphabets);
        final ImmutableIntPairMap outConversionMap = checker.findConversions(outAlphabets);
        final int outKanaAlphabet = getSingleValue(outConversionMap);
        final int outRoumajiAlphabet = outConversionMap.keyAt(0);
        assertContainsOnly(outKanjiAlphabet, outKanaAlphabet, outRoumajiAlphabet, outAlphabets);

        final int outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "注文"));
        final ImmutableIntList correlationIds = checker.getAcceptationCorrelationArray(outAcceptation);
        assertSize(2, correlationIds);

        final ImmutableIntKeyMap<String> outFirstCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(0));
        assertSize(2, outFirstCorrelation);
        assertEquals("注", outFirstCorrelation.get(outKanjiAlphabet));
        assertEquals("ちゅう", outFirstCorrelation.get(outKanaAlphabet));

        final ImmutableIntKeyMap<String> outSecondCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(1));
        assertSize(2, outSecondCorrelation);
        assertEquals("文", outSecondCorrelation.get(outKanjiAlphabet));
        assertEquals("もん", outSecondCorrelation.get(outKanaAlphabet));

        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(outAcceptation);
        assertSize(3, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }
}
