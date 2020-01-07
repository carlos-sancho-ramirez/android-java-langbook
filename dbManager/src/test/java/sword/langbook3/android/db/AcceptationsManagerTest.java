package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.List;
import sword.collections.MutableHashMap;
import sword.database.DbQuery;
import sword.database.DbValue;
import sword.database.MemoryDatabase;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.IntSetTestUtils.assertEqualSet;
import static sword.langbook3.android.db.IntTraversableTestUtils.assertContains;
import static sword.langbook3.android.db.IntTraversableTestUtils.assertSingleValue;
import static sword.langbook3.android.db.LangbookReadableDatabase.selectSingleRow;
import static sword.langbook3.android.db.SizableTestUtils.assertSize;

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
public interface AcceptationsManagerTest {

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

    static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static boolean updateAcceptationSimpleCorrelationArray(AcceptationsManager manager, int alphabet, int acceptationId, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();

        final ImmutableList<ImmutableIntKeyMap<String>> correlationArray = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .append(correlation)
                .build();

        return manager.updateAcceptationCorrelationArray(acceptationId, correlationArray);
    }

    @Test
    default void testAddFirstLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");

        assertEquals(langPair.language, manager.findLanguageByCode("es").intValue());
        assertSingleValue(langPair.mainAlphabet, manager.findAlphabetsByLanguage(langPair.language));
    }

    @Test
    default void testAddSameLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");
        assertNull(manager.addLanguage("es"));

        assertEquals(langPair.language, manager.findLanguageByCode("es").intValue());
        assertSingleValue(langPair.mainAlphabet, manager.findAlphabetsByLanguage(langPair.language));
    }

    @Test
    default void testRemoveUniqueLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int language = manager.addLanguage("es").language;
        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
    }

    @Test
    default void testRemoveFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair1 = manager.addLanguage("es");
        final LanguageCreationResult langPair2 = manager.addLanguage("en");

        assertTrue(manager.removeLanguage(langPair1.language));
        assertNull(manager.findLanguageByCode("es"));
        assertEquals(langPair2.language, manager.findLanguageByCode("en").intValue());
        assertSingleValue(langPair2.mainAlphabet, manager.findAlphabetsByLanguage(langPair2.language));
    }

    @Test
    default void testRemoveLastAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair1 = manager.addLanguage("es");
        final LanguageCreationResult langPair2 = manager.addLanguage("en");

        assertTrue(manager.removeLanguage(langPair2.language));
        assertNull(manager.findLanguageByCode("en"));
        assertEquals(langPair1.language, manager.findLanguageByCode("es").intValue());
        assertSingleValue(langPair1.mainAlphabet, manager.findAlphabetsByLanguage(langPair1.language));
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);
        final LanguageCreationResult langPair = manager.addLanguage("es");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;
        final int secondAlphabet = mainAlphabet + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode("es").intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertSize(2, alphabetSet);
        assertContains(mainAlphabet, alphabetSet);
        assertContains(secondAlphabet, alphabetSet);
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);
        final LanguageCreationResult langPair = manager.addLanguage("ja");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final int acceptation = addSimpleAcceptation(manager, mainAlphabet, language, "日本語");

        final int secondAlphabet = manager.getMaxConcept() + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode("ja").intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertSize(2, alphabetSet);
        assertContains(mainAlphabet, alphabetSet);
        assertContains(secondAlphabet, alphabetSet);

        final ImmutableIntKeyMap<String> acceptationTexts = manager.getAcceptationTexts(acceptation);
        assertSize(2, acceptationTexts);
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
        assertEqualSet(alphabetSet, acceptationTexts.keySet());
    }

    @Test
    default void testRemoveLanguageAfterAddingAlphabetCopyingFromOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("ja");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        addSimpleAcceptation(manager, mainAlphabet, language, "日本語");
        final int secondAlphabet = manager.getMaxConcept() + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("ja"));
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

        assertEquals(language, manager.findLanguageByCode("es").intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertSize(2, alphabetSet);
        assertContains(mainAlphabet, alphabetSet);
        assertContains(secondAlphabet, alphabetSet);

        final String convertedText = manager.getConversion(new ImmutableIntPair(mainAlphabet, secondAlphabet)).convert("casa");

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("casa", texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        final int secondAlphabet = concept + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, "casa");

        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode("es").intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertSize(2, alphabetSet);
        assertContains(mainAlphabet, alphabetSet);
        assertContains(secondAlphabet, alphabetSet);

        final String convertedText = manager.getConversion(new ImmutableIntPair(mainAlphabet, secondAlphabet)).convert("casa");

        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("casa", texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    default void testRemoveLanguageAfterAddingAlphabetAsConversionTarget() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult langPair = manager.addLanguage("es");

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        final int secondAlphabet = concept + 1;
        addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        manager.addAlphabetAsConversionTarget(conversion);

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
        assertTrue(manager.getConversionsMap().isEmpty());
    }

    @Test
    default void testRemoveLanguageButLeaveOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final LanguageCreationResult esLangPair = manager.addLanguage("es");
        final int esLanguage = esLangPair.language;
        final int esMainAlphabet = esLangPair.mainAlphabet;

        final int enMainAlphabet = manager.addLanguage("en").mainAlphabet;

        final String esText = "casa";
        final String enText = "house";

        final int concept = manager.getMaxConcept() + 1;
        final int esAcc = addSimpleAcceptation(manager, esMainAlphabet, concept, esText);
        final int enAcc = addSimpleAcceptation(manager, enMainAlphabet, concept, enText);
        assertNotEquals(esAcc, enAcc);

        assertTrue(manager.removeLanguage(esLanguage));
        assertNull(manager.findLanguageByCode("es"));
        assertNotNull(manager.findLanguageByCode("en"));
        assertSingleValue(enAcc, manager.findAcceptationsByConcept(concept));
    }

    @Test
    default void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());

        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantar", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantar", stringRow.get(3).toText());
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

        final int acceptation = manager.addAcceptation(concept, correlationArrays);

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery kanjiQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kanji)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> kanjiRow = selectSingleRow(db, kanjiQuery);
        assertEquals(acceptation, kanjiRow.get(0).toInt());
        assertEquals("注文", kanjiRow.get(1).toText());
        assertEquals("注文", kanjiRow.get(2).toText());

        final DbQuery kanaQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kana)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> kanaRow = selectSingleRow(db, kanaQuery);
        assertEquals(acceptation, kanaRow.get(0).toInt());
        assertEquals("注文", kanaRow.get(1).toText());
        assertEquals("ちゅうもん", kanaRow.get(2).toText());
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

        final int acceptation = manager.addAcceptation(concept, correlationArray);

        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptation);
        assertSize(3, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();
        final AcceptationsManager manager1 = createManager(db1);

        final int alphabet1 = manager1.addLanguage("es").mainAlphabet;
        final int concept1 = manager1.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager1, alphabet1, concept1, "cantar");

        final MemoryDatabase db2 = new MemoryDatabase();
        final AcceptationsManager manager2 = createManager(db2);

        final int alphabet2 = manager2.addLanguage("es").mainAlphabet;
        final int concept2 = manager2.getMaxConcept() + 1;
        assertEquals(acceptationId, addSimpleAcceptation(manager2, alphabet2, concept2, "cantar"));
        assertEquals(db1, db2);

        updateAcceptationSimpleCorrelationArray(manager2, alphabet2, acceptationId, "cantar");
        assertEquals(db1, db2);
    }

    @Test
    default void testReplaceConversionAfterAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int kanaAlphabet = manager.addLanguage("ja").mainAlphabet;
        final int roumajiAlphabet = manager.getMaxConcept() + 1;
        final int concept = roumajiAlphabet + 1;

        final MutableHashMap<String, String> convMap = new MutableHashMap.Builder<String, String>()
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("こ", "o")
                .put("な", "na")
                .put("に", "ni")
                .put("ぬ", "nu")
                .put("ね", "ne")
                .put("の", "no")
                .build();

        final Conversion conversion1 = new Conversion(kanaAlphabet, roumajiAlphabet, convMap);
        manager.addAlphabetAsConversionTarget(conversion1);

        final int acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, "ねこ");

        final ImmutableIntKeyMap<String> texts1 = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts1);
        assertEquals("ねこ", texts1.get(kanaAlphabet));
        assertEquals("neo", texts1.get(roumajiAlphabet));

        convMap.put("こ", "ko");
        final Conversion conversion2 = new Conversion(kanaAlphabet, roumajiAlphabet, convMap);
        assertTrue(manager.replaceConversion(conversion2));

        final ImmutableIntKeyMap<String> texts2 = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts2);
        assertEquals("ねこ", texts2.get(kanaAlphabet));
        assertEquals("neko", texts2.get(roumajiAlphabet));
    }

    @Test
    default void testReplaceConversionBeforeAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = createManager(db);

        final int kanaAlphabet = manager.addLanguage("ja").mainAlphabet;
        final int roumajiAlphabet = manager.getMaxConcept() + 1;
        final int concept = roumajiAlphabet + 1;

        final Conversion emptyConversion = new Conversion(kanaAlphabet, roumajiAlphabet, ImmutableHashMap.empty());
        assertTrue(manager.addAlphabetAsConversionTarget(emptyConversion));

        final MutableHashMap<String, String> convMap = new MutableHashMap.Builder<String, String>()
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("こ", "ko")
                .put("な", "na")
                .put("に", "ni")
                .put("ぬ", "nu")
                .put("ね", "ne")
                .put("の", "no")
                .build();
        final Conversion conversion = new Conversion(kanaAlphabet, roumajiAlphabet, convMap);
        assertTrue(manager.replaceConversion(conversion));

        final int acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, "ねこ");
        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("ねこ", texts.get(kanaAlphabet));
        assertEquals("neko", texts.get(roumajiAlphabet));
    }
}
