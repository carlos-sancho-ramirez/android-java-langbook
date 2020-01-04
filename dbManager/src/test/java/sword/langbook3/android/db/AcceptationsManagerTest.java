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
import static sword.langbook3.android.db.LangbookReadableDatabase.selectSingleRow;

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
public final class AcceptationsManagerTest {

    static final ImmutableHashMap<String, String> upperCaseConversion = new ImmutableHashMap.Builder<String, String>()
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

    @Test
    void testAddFirstLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        assertEquals(langPair.language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.language);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair.mainAlphabet, alphabetSet.valueAt(0));
    }

    @Test
    void testAddSameLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final LanguageCreationResult langPair = manager.addLanguage(code);
        assertNull(manager.addLanguage(code));

        assertEquals(langPair.language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.language);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair.mainAlphabet, alphabetSet.valueAt(0));
    }

    @Test
    void testRemoveUniqueLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final int language = manager.addLanguage(code).language;
        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode(code));
    }

    @Test
    void testRemoveFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code1 = "es";
        final String code2 = "en";
        final LanguageCreationResult langPair1 = manager.addLanguage(code1);
        final LanguageCreationResult langPair2 = manager.addLanguage(code2);

        assertTrue(manager.removeLanguage(langPair1.language));
        assertNull(manager.findLanguageByCode(code1));
        assertEquals(langPair2.language, manager.findLanguageByCode(code2).intValue());

        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair2.language);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair2.mainAlphabet, alphabetSet.valueAt(0));
    }

    @Test
    void testRemoveLastAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code1 = "es";
        final String code2 = "en";
        final LanguageCreationResult langPair1 = manager.addLanguage(code1);
        final LanguageCreationResult langPair2 = manager.addLanguage(code2);

        assertTrue(manager.removeLanguage(langPair2.language));
        assertNull(manager.findLanguageByCode(code2));
        assertEquals(langPair1.language, manager.findLanguageByCode(code1).intValue());

        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair1.language);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair1.mainAlphabet, alphabetSet.valueAt(0));
    }

    @Test
    void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;
        final int secondAlphabet = mainAlphabet + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));
    }

    public static int addSimpleAcceptation(AcceptationsManager manager, int alphabet, int concept, String text) {
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
    void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "ja";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final int acceptation = addSimpleAcceptation(manager, mainAlphabet, language, "日本語");

        final int secondAlphabet = manager.getMaxConcept() + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final ImmutableIntKeyMap<String> acceptationTexts = manager.getAcceptationTexts(acceptation);
        assertEquals(2, acceptationTexts.size());
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
        assertTrue(alphabetSet.equalSet(acceptationTexts.keySet()));
    }

    @Test
    void testRemoveLanguageAfterAddingAlphabetCopyingFromOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "ja";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        addSimpleAcceptation(manager, mainAlphabet, language, "日本語");
        final int secondAlphabet = manager.getMaxConcept() + 1;
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode(code));
    }

    @Test
    void testAddAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;
        final int secondAlphabet = mainAlphabet + 1;
        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final String text = "casa";
        final String convertedText = manager.getConversion(new ImmutableIntPair(mainAlphabet, secondAlphabet)).convert(text);

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, text);
        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptationId);
        assertEquals(2, texts.size());
        assertEquals(text, texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final String text = "casa";
        final int concept = manager.getMaxConcept() + 1;
        final int secondAlphabet = concept + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, text);

        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(language);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final String convertedText = manager.getConversion(new ImmutableIntPair(mainAlphabet, secondAlphabet)).convert(text);

        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptationId);
        assertEquals(2, texts.size());
        assertEquals(text, texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    void testRemoveLanguageAfterAddingAlphabetAsConversionTarget() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final LanguageCreationResult langPair = manager.addLanguage(code);

        final int language = langPair.language;
        final int mainAlphabet = langPair.mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        final int secondAlphabet = concept + 1;
        addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        manager.addAlphabetAsConversionTarget(conversion);

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode(code));
        assertTrue(manager.getConversionsMap().isEmpty());
    }

    @Test
    void testRemoveLanguageButLeaveOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String esCode = "es";
        final LanguageCreationResult esLangPair = manager.addLanguage(esCode);
        final int esLanguage = esLangPair.language;
        final int esMainAlphabet = esLangPair.mainAlphabet;

        final String enCode = "en";
        final int enMainAlphabet = manager.addLanguage(enCode).mainAlphabet;

        final String esText = "casa";
        final String enText = "house";

        final int concept = manager.getMaxConcept() + 1;
        final int esAcc = addSimpleAcceptation(manager, esMainAlphabet, concept, esText);
        final int enAcc = addSimpleAcceptation(manager, enMainAlphabet, concept, enText);
        assertNotEquals(esAcc, enAcc);

        assertTrue(manager.removeLanguage(esLanguage));
        assertNull(manager.findLanguageByCode(esCode));
        assertNotNull(manager.findLanguageByCode(enCode));
        final ImmutableIntSet acceptations = manager.findAcceptationsByConcept(concept);
        assertEquals(1, acceptations.size());
        assertEquals(enAcc, acceptations.valueAt(0));
    }

    @Test
    void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;

        final String text = "cantar";
        final int acceptation = addSimpleAcceptation(manager, alphabet, concept, text);

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals(text, stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals(text, stringRow.get(3).toText());
    }

    @Test
    void testAddJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

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
    void testAddJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

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
        assertEquals(3, texts.size());
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }

    @Test
    void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();
        final AcceptationsManager manager1 = new LangbookDatabaseManager(db1);

        final String text = "cantar";
        final int alphabet1 = manager1.addLanguage("es").mainAlphabet;
        final int concept1 = manager1.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager1, alphabet1, concept1, text);

        final MemoryDatabase db2 = new MemoryDatabase();
        final AcceptationsManager manager2 = new LangbookDatabaseManager(db2);

        final int alphabet2 = manager2.addLanguage("es").mainAlphabet;
        final int concept2 = manager2.getMaxConcept() + 1;
        assertEquals(acceptationId, addSimpleAcceptation(manager2, alphabet2, concept2, text));
        assertEquals(db1, db2);

        updateAcceptationSimpleCorrelationArray(manager2, alphabet2, acceptationId, text);
        assertEquals(db1, db2);
    }

    @Test
    void testReplaceConversionAfterAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String kanaText = "ねこ";

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

        final int acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, kanaText);

        final ImmutableIntKeyMap<String> texts1 = manager.getAcceptationTexts(acceptationId);
        assertEquals(2, texts1.size());
        assertEquals(kanaText, texts1.get(kanaAlphabet));
        assertEquals("neo", texts1.get(roumajiAlphabet));

        convMap.put("こ", "ko");
        final Conversion conversion2 = new Conversion(kanaAlphabet, roumajiAlphabet, convMap);
        assertTrue(manager.replaceConversion(conversion2));

        final ImmutableIntKeyMap<String> texts2 = manager.getAcceptationTexts(acceptationId);
        assertEquals(2, texts2.size());
        assertEquals(kanaText, texts2.get(kanaAlphabet));
        assertEquals("neko", texts2.get(roumajiAlphabet));
    }

    @Test
    void testReplaceConversionBeforeAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String kanaText = "ねこ";

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

        final int acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, kanaText);
        final ImmutableIntKeyMap<String> texts = manager.getAcceptationTexts(acceptationId);
        assertEquals(2, texts.size());
        assertEquals(kanaText, texts.get(kanaAlphabet));
        assertEquals("neko", texts.get(roumajiAlphabet));
    }
}
