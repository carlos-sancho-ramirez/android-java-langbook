package sword.langbook3.android.db;

import org.junit.Test;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    public void testAddFirstLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);
        assertNotEquals(langPair.left, langPair.right);

        assertEquals(langPair.left, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.left);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair.right, alphabetSet.valueAt(0));
    }

    @Test
    public void testAddSameLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);
        assertNull(manager.addLanguage(code));
        assertNotEquals(langPair.left, langPair.right);

        assertEquals(langPair.left, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.left);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair.right, alphabetSet.valueAt(0));
    }

    @Test
    public void testRemoveUniqueLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        assertTrue(manager.removeLanguage(langPair.left));
        assertNull(manager.findLanguageByCode(code));
    }

    @Test
    public void testRemoveFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code1 = "es";
        final String code2 = "en";
        final ImmutableIntPair langPair1 = manager.addLanguage(code1);
        final ImmutableIntPair langPair2 = manager.addLanguage(code2);

        assertTrue(manager.removeLanguage(langPair1.left));
        assertNull(manager.findLanguageByCode(code1));
        assertEquals(langPair2.left, manager.findLanguageByCode(code2).intValue());

        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair2.left);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair2.right, alphabetSet.valueAt(0));
    }

    @Test
    public void testRemoveLastAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code1 = "es";
        final String code2 = "en";
        final ImmutableIntPair langPair1 = manager.addLanguage(code1);
        final ImmutableIntPair langPair2 = manager.addLanguage(code2);

        assertTrue(manager.removeLanguage(langPair2.left));
        assertNull(manager.findLanguageByCode(code2));
        assertEquals(langPair1.left, manager.findLanguageByCode(code1).intValue());

        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair1.left);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair1.right, alphabetSet.valueAt(0));
    }

    @Test
    public void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;
        final int secondAlphabet = manager.addAlphabetCopyingFromOther(mainAlphabet);
        assertNotEquals(language, secondAlphabet);
        assertNotEquals(mainAlphabet, secondAlphabet);

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.left);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));
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
    public void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "ja";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        final int acceptation = addSimpleAcceptation(manager, mainAlphabet, language, "日本語");

        final int secondAlphabet = manager.addAlphabetCopyingFromOther(mainAlphabet);
        assertNotEquals(language, secondAlphabet);
        assertNotEquals(mainAlphabet, secondAlphabet);

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.left);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final ImmutableIntKeyMap<String> acceptationTexts = manager.getAcceptationTexts(acceptation);
        assertEquals(2, acceptationTexts.size());
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
        assertTrue(alphabetSet.equalSet(acceptationTexts.keySet()));
    }

    @Test
    public void testRemoveLanguageAfterAddingAlphabetCopyingFromOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "ja";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        addSimpleAcceptation(manager, mainAlphabet, language, "日本語");
        manager.addAlphabetCopyingFromOther(mainAlphabet);

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode(code));
    }

    @Test
    public void testAddAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;
        final int secondAlphabet = mainAlphabet + 1;
        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.left);
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
    public void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        final String text = "casa";
        final int concept = manager.getMaxConcept() + 1;
        final int secondAlphabet = concept + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, text);

        final Conversion conversion = new Conversion(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode(code).intValue());
        final ImmutableIntSet alphabetSet = manager.findAlphabetsByLanguage(langPair.left);
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
    public void testRemoveLanguageAfterAddingAlphabetAsConversionTarget() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String code = "es";
        final ImmutableIntPair langPair = manager.addLanguage(code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

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
    public void testRemoveLanguageButLeaveOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String esCode = "es";
        final ImmutableIntPair esLangPair = manager.addLanguage(esCode);
        final int esLanguage = esLangPair.left;
        final int esMainAlphabet = esLangPair.right;

        final String enCode = "en";
        final int enMainAlphabet = manager.addLanguage(enCode).right;

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
    public void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final int alphabet = manager.addLanguage("es").right;
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
    public void testAddJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final int kanji = manager.addLanguage("ja").right;
        final int kana = manager.addAlphabetCopyingFromOther(kanji);
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
    public void testAddJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final int kanji = manager.addLanguage("ja").right;
        final int kana = manager.addAlphabetCopyingFromOther(kanji);
        final int roumaji = kana + 1;

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
    public void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();
        final AcceptationsManager manager1 = new LangbookDatabaseManager(db1);

        final String text = "cantar";
        final int alphabet1 = manager1.addLanguage("es").right;
        final int concept1 = manager1.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager1, alphabet1, concept1, text);

        final MemoryDatabase db2 = new MemoryDatabase();
        final AcceptationsManager manager2 = new LangbookDatabaseManager(db2);

        final int alphabet2 = manager2.addLanguage("es").right;
        final int concept2 = manager2.getMaxConcept() + 1;
        assertEquals(acceptationId, addSimpleAcceptation(manager2, alphabet2, concept2, text));
        assertEquals(db1, db2);

        updateAcceptationSimpleCorrelationArray(manager2, alphabet2, acceptationId, text);
        assertEquals(db1, db2);
    }

    @Test
    public void testReplaceConversionAfterAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String kanaText = "ねこ";

        final int kanaAlphabet = manager.addLanguage("ja").right;
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
    public void testReplaceConversionBeforeAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager manager = new LangbookDatabaseManager(db);

        final String kanaText = "ねこ";

        final int kanaAlphabet = manager.addLanguage("ja").right;
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
