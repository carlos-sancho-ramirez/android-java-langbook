package sword.langbook3.android;

import org.junit.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.IntList;
import sword.collections.List;
import sword.collections.MutableHashMap;
import sword.database.Database;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.database.MemoryDatabase;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookReadableDatabase.Conversion;
import sword.langbook3.android.LangbookReadableDatabase.QuestionFieldDetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.LangbookDatabase.addAcceptation;
import static sword.langbook3.android.LangbookDatabase.addAcceptationInBunch;
import static sword.langbook3.android.LangbookDatabase.addAgent;
import static sword.langbook3.android.LangbookDatabase.addAlphabetAsConversionTarget;
import static sword.langbook3.android.LangbookDatabase.addAlphabetCopyingFromOther;
import static sword.langbook3.android.LangbookDatabase.addLanguage;
import static sword.langbook3.android.LangbookDatabase.obtainCorrelation;
import static sword.langbook3.android.LangbookDatabase.obtainCorrelationArray;
import static sword.langbook3.android.LangbookDatabase.obtainQuiz;
import static sword.langbook3.android.LangbookDatabase.obtainSimpleCorrelationArray;
import static sword.langbook3.android.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.LangbookDatabase.removeAcceptation;
import static sword.langbook3.android.LangbookDatabase.removeAcceptationFromBunch;
import static sword.langbook3.android.LangbookDatabase.removeAgent;
import static sword.langbook3.android.LangbookDatabase.replaceConversion;
import static sword.langbook3.android.LangbookDatabase.updateAcceptationCorrelationArray;
import static sword.langbook3.android.LangbookDbInserter.insertSearchHistoryEntry;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookReadableDatabase.findLanguageByCode;
import static sword.langbook3.android.LangbookReadableDatabase.getAcceptationTexts;
import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.getConversionsMap;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConcept;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.getSearchHistory;
import static sword.langbook3.android.LangbookReadableDatabase.readAllMatchingBunches;
import static sword.langbook3.android.LangbookReadableDatabase.readCorrelationArrayTexts;
import static sword.langbook3.android.LangbookReadableDatabase.selectSingleRow;

public final class LangbookDatabaseTest {

    private final ImmutableHashMap<String, String> upperCaseConversion = new ImmutableHashMap.Builder<String, String>()
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
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);
        assertNotEquals(langPair.left, langPair.right);

        assertEquals(langPair.left, LangbookReadableDatabase.findLanguageByCode(db, code).intValue());
        final ImmutableIntSet alphabetSet = LangbookReadableDatabase.findAlphabetsByLanguage(db, langPair.left);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair.right, alphabetSet.valueAt(0));
    }

    @Test
    public void testAddSameLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);
        assertNull(LangbookDatabase.addLanguage(db, code));
        assertNotEquals(langPair.left, langPair.right);

        assertEquals(langPair.left, LangbookReadableDatabase.findLanguageByCode(db, code).intValue());
        final ImmutableIntSet alphabetSet = LangbookReadableDatabase.findAlphabetsByLanguage(db, langPair.left);
        assertEquals(1, alphabetSet.size());
        assertEquals(langPair.right, alphabetSet.valueAt(0));
    }

    @Test
    public void testRemoveLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        assertTrue(LangbookDatabase.removeLanguage(db, langPair.left));
        assertNull(findLanguageByCode(db, code));
    }

    @Test
    public void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;
        final int secondAlphabet = addAlphabetCopyingFromOther(db, mainAlphabet);
        assertNotEquals(language, secondAlphabet);
        assertNotEquals(mainAlphabet, secondAlphabet);

        assertEquals(language, LangbookReadableDatabase.findLanguageByCode(db, code).intValue());
        final ImmutableIntSet alphabetSet = LangbookReadableDatabase.findAlphabetsByLanguage(db, langPair.left);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));
    }

    @Test
    public void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "ja";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        final ImmutableIntKeyMap<String> langCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(mainAlphabet, "日本語")
                .build();

        final IntList langCorrelations = new ImmutableIntList.Builder()
                .append(obtainCorrelation(db, langCorrelation))
                .build();

        final int acceptation = addAcceptation(db, language, obtainCorrelationArray(db, langCorrelations));

        final int secondAlphabet = addAlphabetCopyingFromOther(db, mainAlphabet);
        assertNotEquals(language, secondAlphabet);
        assertNotEquals(mainAlphabet, secondAlphabet);

        assertEquals(language, LangbookReadableDatabase.findLanguageByCode(db, code).intValue());
        final ImmutableIntSet alphabetSet = LangbookReadableDatabase.findAlphabetsByLanguage(db, langPair.left);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final ImmutableIntKeyMap<String> acceptationTexts = LangbookReadableDatabase.getAcceptationTexts(db, acceptation);
        assertEquals(2, acceptationTexts.size());
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
        assertTrue(alphabetSet.equalSet(acceptationTexts.keySet()));
    }

    @Test
    public void testRemoveLanguageAfterAddingAlphabetCopyingFromOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "ja";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        final ImmutableIntKeyMap<String> langCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(mainAlphabet, "日本語")
                .build();

        final IntList langCorrelations = new ImmutableIntList.Builder()
                .append(obtainCorrelation(db, langCorrelation))
                .build();

        addAcceptation(db, language, obtainCorrelationArray(db, langCorrelations));
        addAlphabetCopyingFromOther(db, mainAlphabet);

        assertTrue(LangbookDatabase.removeLanguage(db, language));
        assertNull(findLanguageByCode(db, code));
    }

    @Test
    public void testAddAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;
        final int secondAlphabet = addAlphabetAsConversionTarget(db, mainAlphabet, upperCaseConversion);
        assertNotEquals(language, secondAlphabet);
        assertNotEquals(mainAlphabet, secondAlphabet);

        assertEquals(language, LangbookReadableDatabase.findLanguageByCode(db, code).intValue());
        final ImmutableIntSet alphabetSet = LangbookReadableDatabase.findAlphabetsByLanguage(db, langPair.left);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final String text = "casa";
        final String convertedText = getConversion(db, new ImmutableIntPair(mainAlphabet, secondAlphabet)).convert(text);

        final int correlationId = obtainCorrelation(db, new ImmutableIntKeyMap.Builder<String>().put(mainAlphabet, text).build());
        final int correlationArrayId = obtainCorrelationArray(db, new ImmutableIntList.Builder().append(correlationId).build());

        final int concept = getMaxConcept(db) + 1;
        final int acceptationId = addAcceptation(db, concept, correlationArrayId);
        final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, acceptationId);
        assertEquals(2, texts.size());
        assertEquals(text, texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    public void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        final String text = "casa";
        final int correlationId = obtainCorrelation(db, new ImmutableIntKeyMap.Builder<String>().put(mainAlphabet, text).build());
        final int correlationArrayId = obtainCorrelationArray(db, new ImmutableIntList.Builder().append(correlationId).build());

        final int concept = getMaxConcept(db) + 1;
        final int acceptationId = addAcceptation(db, concept, correlationArrayId);

        final int secondAlphabet = addAlphabetAsConversionTarget(db, mainAlphabet, upperCaseConversion);
        assertNotEquals(language, secondAlphabet);
        assertNotEquals(mainAlphabet, secondAlphabet);
        assertNotEquals(concept, secondAlphabet);

        assertEquals(language, LangbookReadableDatabase.findLanguageByCode(db, code).intValue());
        final ImmutableIntSet alphabetSet = LangbookReadableDatabase.findAlphabetsByLanguage(db, langPair.left);
        assertEquals(2, alphabetSet.size());
        assertTrue(alphabetSet.contains(mainAlphabet));
        assertTrue(alphabetSet.contains(secondAlphabet));

        final String convertedText = getConversion(db, new ImmutableIntPair(mainAlphabet, secondAlphabet)).convert(text);

        final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, acceptationId);
        assertEquals(2, texts.size());
        assertEquals(text, texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    public void testRemoveLanguageAfterAddingAlphabetAsConversionTarget() {
        final MemoryDatabase db = new MemoryDatabase();
        final String code = "es";
        final ImmutableIntPair langPair = LangbookDatabase.addLanguage(db, code);

        final int language = langPair.left;
        final int mainAlphabet = langPair.right;

        final String text = "casa";
        final int correlationId = obtainCorrelation(db, new ImmutableIntKeyMap.Builder<String>().put(mainAlphabet, text).build());
        final int correlationArrayId = obtainCorrelationArray(db, new ImmutableIntList.Builder().append(correlationId).build());

        final int concept = getMaxConcept(db) + 1;
        addAcceptation(db, concept, correlationArrayId);
        addAlphabetAsConversionTarget(db, mainAlphabet, upperCaseConversion);

        assertTrue(LangbookDatabase.removeLanguage(db, language));
        assertNull(findLanguageByCode(db, code));
        assertTrue(getConversionsMap(db).isEmpty());
    }

    @Test
    public void testObtainCorrelationForSingleAlphabetLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final int alphabet = LangbookDatabase.addLanguage(db, "es").right;

        final ImmutableIntPairMap correlation = new ImmutableIntPairMap.Builder()
                .put(alphabet, obtainSymbolArray(db, "casa"))
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final ImmutableIntPairMap given = getCorrelation(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationForSingleAlphabetLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final int alphabet = LangbookDatabase.addLanguage(db, "es").right;

        final ImmutableIntPairMap correlation = new ImmutableIntPairMap.Builder()
                .put(alphabet, obtainSymbolArray(db, "casa"))
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        assertEquals(correlationId, obtainCorrelation(db, correlation).intValue());

        final ImmutableIntPairMap given = getCorrelation(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationForMultipleAlphabetLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final int kanji = LangbookDatabase.addLanguage(db, "ja").right;
        final int kana = addAlphabetCopyingFromOther(db, kanji);

        final ImmutableIntPairMap correlation = new ImmutableIntPairMap.Builder()
                .put(kanji, obtainSymbolArray(db, "心"))
                .put(kana, obtainSymbolArray(db, "こころ"))
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final ImmutableIntPairMap given = getCorrelation(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationForMultipleAlphabetLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final int kanji = LangbookDatabase.addLanguage(db, "ja").right;
        final int kana = addAlphabetCopyingFromOther(db, kanji);

        final ImmutableIntPairMap correlation = new ImmutableIntPairMap.Builder()
                .put(kanji, obtainSymbolArray(db, "心"))
                .put(kana, obtainSymbolArray(db, "こころ"))
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        assertEquals(correlationId, obtainCorrelation(db, correlation).intValue());

        final ImmutableIntPairMap given = getCorrelation(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationWithTextForSingleAlphabetLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final int alphabet = LangbookDatabase.addLanguage(db, "es").right;

        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "casa")
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final ImmutableIntKeyMap<String> given = getCorrelationWithText(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationWithTextForSingleAlphabetLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final int alphabet = LangbookDatabase.addLanguage(db, "es").right;

        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "casa")
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        assertEquals(correlationId, obtainCorrelation(db, correlation).intValue());

        final ImmutableIntKeyMap<String> given = getCorrelationWithText(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationWithTextForMultipleAlphabetLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final int kanji = LangbookDatabase.addLanguage(db, "ja").right;
        final int kana = addAlphabetCopyingFromOther(db, kanji);

        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(kanji, "心")
                .put(kana, "こころ")
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final ImmutableIntKeyMap<String> given = getCorrelationWithText(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testObtainCorrelationWithTextForMultipleAlphabetLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final int kanji = LangbookDatabase.addLanguage(db, "ja").right;
        final int kana = addAlphabetCopyingFromOther(db, kanji);

        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(kanji, "心")
                .put(kana, "こころ")
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        assertEquals(correlationId, obtainCorrelation(db, correlation).intValue());

        final ImmutableIntKeyMap<String> given = getCorrelationWithText(db, correlationId);
        assertTrue(correlation.equalMap(given));
    }

    @Test
    public void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();

        final ImmutableIntPair langPair = addLanguage(db, "es");
        final int alphabet = langPair.right;
        final int concept = getMaxConcept(db) + 1;

        final String text = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

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

        final int kanji = addLanguage(db, "ja").right;
        final int kana = addAlphabetCopyingFromOther(db, kanji);
        final int concept = getMaxConcept(db) + 1;

        final ImmutableList<ImmutableIntKeyMap<String>> correlations = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        final ImmutableIntList correlationIds = correlations.mapToInt(corr -> obtainCorrelation(db, corr));
        final int correlationArrayId = obtainCorrelationArray(db, correlationIds);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

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

        final int kanji = addLanguage(db, "ja").right;
        final int kana = addAlphabetCopyingFromOther(db, kanji);

        final MutableHashMap<String, String> convMap = new MutableHashMap.Builder<String, String>()
                .put("あ", "a")
                .put("も", "mo")
                .put("ん", "n")
                .put("う", "u")
                .put("ちゅ", "chu")
                .put("ち", "chi")
                .build();
        final int roumaji = addAlphabetAsConversionTarget(db, kana, convMap);
        final int concept = getMaxConcept(db) + 1;

        final ImmutableList<ImmutableIntKeyMap<String>> correlations = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        final ImmutableIntList correlationIds = correlations.mapToInt(corr -> obtainCorrelation(db, corr));
        final int correlationArrayId = obtainCorrelationArray(db, correlationIds);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

        final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, acceptation);
        assertEquals(3, texts.size());
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }

    @Test
    public void testAddAgentApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int gerund = getMaxConcept(db) + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        final String verbText = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbText)
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);
        assertTrue(addAcceptationInBunch(db, verbConcept, acceptation));

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetCreator().build();
        final int agentId = addAgent(db, 0, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery ruledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), gerund)
                .where(ruledConcepts.getConceptColumnIndex(), concept)
                .select(ruledConcepts.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, ruledConceptQuery).get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), agentId)
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex());
        final int ruledAcceptation = selectSingleRow(db, ruledAcceptationsQuery).get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), ruledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(ruledConcept, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantando", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantando", stringRow.get(3).toText());
    }

    @Test
    public void testAddAgentComposingBunch() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int arVerbConcept = getMaxConceptInAcceptations(db) + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int verbConcept = erVerbConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        final String singText = "cantar";
        final ImmutableIntKeyMap<String> singCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, singText)
                .build();
        final int singCorrelationId = obtainCorrelation(db, singCorrelation);
        final int singCorrelationArrayId = obtainSimpleCorrelationArray(db, singCorrelationId);
        final int singAcceptation = addAcceptation(db, singConcept, singCorrelationArrayId);
        assertTrue(addAcceptationInBunch(db, verbConcept, singAcceptation));

        final String coughtText = "toser";
        final ImmutableIntKeyMap<String> coughtCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, coughtText)
                .build();
        final int coughtCorrelationId = obtainCorrelation(db, coughtCorrelation);
        final int coughtCorrelationArrayId = obtainSimpleCorrelationArray(db, coughtCorrelationId);
        final int coughtAcceptation = addAcceptation(db, coughtConcept, coughtCorrelationArrayId);
        assertTrue(addAcceptationInBunch(db, verbConcept, coughtAcceptation));

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetCreator().build();
        final int agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arMatcher, 0);

        final LangbookDbSchema.AgentSetsTable agentSets = LangbookDbSchema.Tables.agentSets;
        final DbQuery agentSetQuery = new DbQuery.Builder(agentSets)
                .where(agentSets.getAgentColumnIndex(), agentId)
                .select(agentSets.getSetIdColumnIndex());
        final int agentSetId = selectSingleRow(db, agentSetQuery).get(0).toInt();

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery verbBunchAccQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), verbConcept)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        try (DbResult bunchAccResult = db.select(verbBunchAccQuery)) {
            for (int i = 0; i < 2; i++) {
                builder.add(bunchAccResult.next().get(0).toInt());
            }
            assertFalse(bunchAccResult.hasNext());
        }
        assertEquals(new ImmutableIntSetCreator().add(singAcceptation).add(coughtAcceptation).build(), builder.build());

        final DbQuery arVerbBunchAccQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), arVerbConcept)
                .where(bunchAcceptations.getAgentSetColumnIndex(), agentSetId)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertEquals(singAcceptation, selectSingleRow(db, arVerbBunchAccQuery).get(0).toInt());
    }

    private void checkAdd2ChainedAgents(boolean reversedAdditionOrder) {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int gerund = getMaxConceptInAcceptations(db) + 1;
        final int verbConcept = gerund + 1;
        final int arVerbConcept = verbConcept + 1;
        final int singConcept = arVerbConcept + 1;

        final String verbText = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbText)
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, singConcept, correlationArrayId);
        assertTrue(addAcceptationInBunch(db, verbConcept, acceptation));

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet arVerbBunchSet = new ImmutableIntSetCreator().add(arVerbConcept).build();
        final ImmutableIntSet verbBunchSet = new ImmutableIntSetCreator().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetCreator().build();

        final int agent2Id;
        if (reversedAdditionOrder) {
            agent2Id = addAgent(db, 0, arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
            addAgent(db, arVerbConcept, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
        }
        else {
            addAgent(db, arVerbConcept, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
            agent2Id = addAgent(db, 0, arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
        }

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery ruledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), gerund)
                .where(ruledConcepts.getConceptColumnIndex(), singConcept)
                .select(ruledConcepts.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, ruledConceptQuery).get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), agent2Id)
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex());
        final int ruledAcceptation = selectSingleRow(db, ruledAcceptationsQuery).get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), ruledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(ruledConcept, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantando", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantando", stringRow.get(3).toText());
    }

    @Test
    public void testAdd2ChainedAgents() {
        checkAdd2ChainedAgents(false);
    }

    @Test
    public void testAdd2ChainedAgentsReversedAdditionOrder() {
        checkAdd2ChainedAgents(true);
    }

    private void checkAddAgentWithDiffBunch(boolean addAgentBeforeAcceptations) {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int arVerbConcept = getMaxConceptInAcceptations(db) + 1;
        final int arEndingNounConcept = arVerbConcept + 1;
        final int singConcept = arEndingNounConcept + 1;
        final int palateConcept = singConcept + 1;

        final String singText = "cantar";
        final ImmutableIntKeyMap<String> singCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, singText)
                .build();
        final int singCorrelationId = obtainCorrelation(db, singCorrelation);
        final int singCorrelationArrayId = obtainSimpleCorrelationArray(db, singCorrelationId);

        final String palateText = "paladar";
        final ImmutableIntKeyMap<String> palateCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, palateText)
                .build();
        final int palateCorrelationId = obtainCorrelation(db, palateCorrelation);
        final int palateCorrelationArrayId = obtainSimpleCorrelationArray(db, palateCorrelationId);

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetCreator().add(arEndingNounConcept).build();

        final int singAcceptation = addAcceptation(db, singConcept, singCorrelationArrayId);
        final int palateAcceptation;
        final int agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arMatcher, 0);
            palateAcceptation = addAcceptation(db, palateConcept, palateCorrelationArrayId);
            addAcceptationInBunch(db, arEndingNounConcept, palateAcceptation);
        }
        else {
            palateAcceptation = addAcceptation(db, palateConcept, palateCorrelationArrayId);
            addAcceptationInBunch(db, arEndingNounConcept, palateAcceptation);
            agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arMatcher, 0);
        }

        final LangbookDbSchema.AgentSetsTable agentSets = LangbookDbSchema.Tables.agentSets;
        final DbQuery agentSetQuery = new DbQuery.Builder(agentSets)
                .where(agentSets.getAgentColumnIndex(), agentId)
                .select(agentSets.getSetIdColumnIndex());
        final int agentSetId = selectSingleRow(db, agentSetQuery).get(0).toInt();

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery arVerbsQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), arVerbConcept)
                .select(bunchAcceptations.getAcceptationColumnIndex(), bunchAcceptations.getAgentSetColumnIndex());
        final List<DbValue> row = selectSingleRow(db, arVerbsQuery);
        assertEquals(singAcceptation, row.get(0).toInt());
        assertEquals(agentSetId, row.get(1).toInt());
    }

    @Test
    public void testAddAcceptationBeforeAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(false);
    }

    @Test
    public void testAddAcceptationAfterAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(true);
    }

    private static final class Add3ChainedAgentsResult {
        final int agent1Id;
        final int agent2Id;
        final int agent3Id;

        Add3ChainedAgentsResult(int agent1Id, int agent2Id, int agent3Id) {
            this.agent1Id = agent1Id;
            this.agent2Id = agent2Id;
            this.agent3Id = agent3Id;
        }
    }

    private static int addSimpleCorrelationArray(Database db, int alphabet, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        return obtainSimpleCorrelationArray(db, correlationId);
    }

    private static int addSpanishAcceptation(Database db, int alphabet, int concept, String text) {
        return addAcceptation(db, concept, addSimpleCorrelationArray(db, alphabet, text));
    }

    private static int addSpanishSingAcceptation(Database db, int alphabet, int concept) {
        return addSpanishAcceptation(db, alphabet, concept, "cantar");
    }

    private static int addSpanishRunAcceptation(Database db, int alphabet, int concept) {
        return addSpanishAcceptation(db, alphabet, concept, "correr");
    }

    private static int addJapaneseSingAcceptation(Database db, int kanjiAlphabet, int kanaAlphabet, int concept) {
        final ImmutableIntKeyMap<String> correlation1 = new ImmutableIntKeyMap.Builder<String>()
                .put(kanjiAlphabet, "歌")
                .put(kanaAlphabet, "うた")
                .build();
        final ImmutableIntKeyMap<String> correlation2 = new ImmutableIntKeyMap.Builder<String>()
                .put(kanjiAlphabet, "う")
                .put(kanaAlphabet, "う")
                .build();
        final int correlation1Id = obtainCorrelation(db, correlation1);
        final int correlation2Id = obtainCorrelation(db, correlation2);
        final ImmutableIntList array = new ImmutableIntList.Builder().append(correlation1Id).append(correlation2Id).build();
        final int correlationArrayId = obtainCorrelationArray(db, array);
        return addAcceptation(db, concept, correlationArrayId);
    }

    private static Add3ChainedAgentsResult add3ChainedAgents(Database db,
            int alphabet, ImmutableIntSet sourceBunchSet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "o")
                .build();
        final ImmutableIntKeyMap<String> noMatcher = new ImmutableIntKeyMap.Builder<String>()
                .build();
        final ImmutableIntKeyMap<String> pluralAdder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "s")
                .build();

        final ImmutableIntSet arVerbBunchSet = new ImmutableIntSetCreator().add(arVerbConcept).build();
        final ImmutableIntSet actionConceptBunchSet = new ImmutableIntSetCreator().add(actionConcept).build();
        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();

        final int agent3Id = addAgent(db, 0, actionConceptBunchSet, noBunches, noMatcher, noMatcher, noMatcher, pluralAdder, pluralRule);
        final int agent2Id = addAgent(db, actionConcept, arVerbBunchSet, noBunches, noMatcher, noMatcher, matcher, adder, nominalizationRule);
        final int agent1Id = addAgent(db, arVerbConcept, sourceBunchSet, noBunches, noMatcher, noMatcher, matcher, matcher, 0);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    private static Add3ChainedAgentsResult add3ChainedAgents(Database db,
            int alphabet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        return add3ChainedAgents(db, alphabet, noBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);
    }

    @Test
    public void testAdd3ChainedAgents() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int arVerbConcept = getMaxConcept(db) + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(db, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery nounRuledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), nominalizationRule)
                .select(ruledConcepts.getIdColumnIndex(), ruledConcepts.getConceptColumnIndex());
        final List<DbValue> nounRuledConceptResult = selectSingleRow(db, nounRuledConceptQuery);
        assertEquals(singConcept, nounRuledConceptResult.get(1).toInt());
        final int nounRuledConcept = nounRuledConceptResult.get(0).toInt();

        final DbQuery pluralRuledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), pluralRule)
                .select(ruledConcepts.getIdColumnIndex(), ruledConcepts.getConceptColumnIndex());
        final List<DbValue> pluralRuledConceptResult = selectSingleRow(db, pluralRuledConceptQuery);
        assertEquals(nounRuledConcept, pluralRuledConceptResult.get(1).toInt());
        final int pluralRuledConcept = pluralRuledConceptResult.get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery nounRuledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), addAgentsResult.agent2Id)
                .select(ruledAcceptations.getIdColumnIndex(), ruledAcceptations.getAcceptationColumnIndex());
        final List<DbValue> nounRuledAcceptationResult = selectSingleRow(db, nounRuledAcceptationsQuery);
        assertEquals(acceptation, nounRuledAcceptationResult.get(1).toInt());
        final int nounRuledAcceptation = nounRuledAcceptationResult.get(0).toInt();

        final DbQuery pluralRuledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), addAgentsResult.agent3Id)
                .select(ruledAcceptations.getIdColumnIndex(), ruledAcceptations.getAcceptationColumnIndex());
        final List<DbValue> pluralRuledAcceptationResult = selectSingleRow(db, pluralRuledAcceptationsQuery);
        assertEquals(nounRuledAcceptation, pluralRuledAcceptationResult.get(1).toInt());
        final int pluralRuledAcceptation = pluralRuledAcceptationResult.get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery nounAcceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), nounRuledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(nounRuledConcept, selectSingleRow(db, nounAcceptationQuery).get(0).toInt());

        final DbQuery pluralAcceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), pluralRuledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(pluralRuledConcept, selectSingleRow(db, pluralAcceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), nounRuledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("canto", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("canto", stringRow.get(3).toText());

        stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), pluralRuledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantos", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantos", stringRow.get(3).toText());
    }

    @Test
    public void testRemoveChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int arVerbConcept = getMaxConcept(db) + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(db, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        removeAgent(db, addAgentsResult.agent1Id);
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .select(ruledAcceptations.getIdColumnIndex());
        assertFalse(db.select(ruledAcceptationsQuery).hasNext());

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptation, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery bunchAcceptationQuery = new DbQuery.Builder(bunchAcceptations)
                .select(bunchAcceptations.getIdColumnIndex());
        assertFalse(db.select(bunchAcceptationQuery).hasNext());
    }

    @Test
    public void testRemoveAcceptationWithChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int arVerbConcept = getMaxConcept(db) + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        add3ChainedAgents(db, alphabet, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        removeAcceptation(db, acceptation);
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .select(ruledAcceptations.getIdColumnIndex());
        assertFalse(db.select(ruledAcceptationsQuery).hasNext());

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .select(acceptations.getIdColumnIndex());
        assertFalse(db.select(acceptationQuery).hasNext());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery bunchAcceptationQuery = new DbQuery.Builder(bunchAcceptations)
                .select(bunchAcceptations.getIdColumnIndex());
        assertFalse(db.select(bunchAcceptationQuery).hasNext());
    }

    @Test
    public void testRemoveAcceptationWithBunchChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int verbConcept = getMaxConcept(db) + 1;
        final int arVerbConcept = verbConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        addAcceptationInBunch(db, verbConcept, acceptation);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(verbConcept).build();
        add3ChainedAgents(db, alphabet, sourceBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        removeAcceptationFromBunch(db, verbConcept, acceptation);
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .select(ruledAcceptations.getIdColumnIndex());
        assertFalse(db.select(ruledAcceptationsQuery).hasNext());

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptation, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery bunchAcceptationQuery = new DbQuery.Builder(bunchAcceptations)
                .select(bunchAcceptations.getIdColumnIndex());
        assertFalse(db.select(bunchAcceptationQuery).hasNext());
    }

    @Test
    public void testAddAcceptationInBunchAndQuiz() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;

        final int kanjiAlphabet = addLanguage(db, "ja").right;
        final int kanaAlphabet = addAlphabetCopyingFromOther(db, kanjiAlphabet);

        final int myVocabularyConcept = getMaxConcept(db) + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);

        final int jaAcceptation = addJapaneseSingAcceptation(db, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(kanjiAlphabet, 0, QuestionFieldFlags.IS_ANSWER | QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = obtainQuiz(db, myVocabularyConcept, fields);

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        final DbQuery query = new DbQuery.Builder(knowledge)
                .select(knowledge.getAcceptationColumnIndex(), knowledge.getQuizDefinitionColumnIndex());
        final List<DbValue> row = selectSingleRow(db, query);
        assertEquals(esAcceptation, row.get(0).toInt());
        assertEquals(quizId, row.get(1).toInt());
    }

    @Test
    public void testAddQuizAndAcceptationInBunch() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int kanjiAlphabet = addLanguage(db, "ja").right;
        final int kanaAlphabet = addAlphabetCopyingFromOther(db, kanjiAlphabet);

        final int myVocabularyConcept = getMaxConcept(db) + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        final int jaAcceptation = addJapaneseSingAcceptation(db, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(kanjiAlphabet, 0, QuestionFieldFlags.IS_ANSWER | QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = obtainQuiz(db, myVocabularyConcept, fields);
        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        final DbQuery knowledgeQuery = new DbQuery.Builder(knowledge)
                .select(knowledge.getAcceptationColumnIndex(), knowledge.getQuizDefinitionColumnIndex());
        assertFalse(db.select(knowledgeQuery).hasNext());

        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);

        final List<DbValue> row = selectSingleRow(db, knowledgeQuery);
        assertEquals(esAcceptation, row.get(0).toInt());
        assertEquals(quizId, row.get(1).toInt());

        removeAcceptationFromBunch(db, myVocabularyConcept, esAcceptation);
        assertFalse(db.select(knowledgeQuery).hasNext());
    }

    @Test
    public void testCallingTwiceAddAcceptationInBunchDoesNotDuplicate() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int kanjiAlphabet = getMaxConcept(db) + 1;
        final int kanaAlphabet = kanjiAlphabet + 1;
        final int myVocabularyConcept = kanaAlphabet + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int esAcceptation = addSpanishSingAcceptation(db, alphabet, singConcept);

        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);
        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);

        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), myVocabularyConcept)
                .select(table.getAcceptationColumnIndex());
        assertEquals(esAcceptation, selectSingleRow(db, query).get(0).toInt());
    }

    @Test
    public void testSearchHistory() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int concept = getMaxConcept(db) + 1;

        final String text = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();
        final int correlationId = obtainCorrelation(db, correlation);
        final int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);
        assertTrue(getSearchHistory(db).isEmpty());

        insertSearchHistoryEntry(db, acceptation);
        final ImmutableList<SearchResult> history = getSearchHistory(db);
        assertEquals(1, history.size());

        final SearchResult expectedEntry = new SearchResult(text, text, SearchResult.Types.ACCEPTATION, acceptation, acceptation);
        assertEquals(expectedEntry, history.get(0));

        removeAcceptation(db, acceptation);

        final LangbookDbSchema.SearchHistoryTable table = LangbookDbSchema.Tables.searchHistory;
        assertFalse(db.select(new DbQuery.Builder(table).select(table.getIdColumnIndex())).hasNext());
    }

    @Test
    public void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();

        final int alphabet = addLanguage(db, "es").right;
        final int gerund = getMaxConcept(db) + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;

        final String verbArText = "verbo de primera conjugación";
        ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbArText)
                .build();
        int correlationId = obtainCorrelation(db, correlation);
        int correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
        addAcceptation(db, verbArConcept, correlationArrayId);

        final String verbErText = "verbo de segunda conjugación";
        correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbErText)
                .build();
        correlationId = obtainCorrelation(db, correlation);
        correlationArrayId = obtainSimpleCorrelationArray(db, correlationId);
        addAcceptation(db, verbErConcept, correlationArrayId);

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> arAdder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet arSourceBunches = new ImmutableIntSetCreator().add(verbArConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetCreator().build();
        addAgent(db, 0, arSourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arAdder, gerund);

        final ImmutableIntKeyMap<String> erMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "er")
                .build();
        final ImmutableIntKeyMap<String> erAdder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "iendo")
                .build();

        final ImmutableIntSet erSourceBunches = new ImmutableIntSetCreator().add(verbErConcept).build();
        addAgent(db, 0, erSourceBunches, diffBunches, nullCorrelation, nullCorrelation, erMatcher, erAdder, gerund);

        ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "jugar").build();
        ImmutableIntKeyMap<String> result = readAllMatchingBunches(db, texts, alphabet);
        assertEquals(1, result.size());
        assertEquals(verbArConcept, result.keyAt(0));
        assertEquals(verbArText, result.valueAt(0));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        result = readAllMatchingBunches(db, texts, alphabet);
        assertEquals(1, result.size());
        assertEquals(verbErConcept, result.keyAt(0));
        assertEquals(verbErText, result.valueAt(0));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "dormir").build();
        result = readAllMatchingBunches(db, texts, alphabet);
        assertEquals(0, result.size());
    }

    @Test
    public void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();

        final String text = "cantar";
        final int alphabet1 = addLanguage(db1, "es").right;
        final int concept1 = getMaxConcept(db1) + 1;

        final int correlationArrayId = addSimpleCorrelationArray(db1, alphabet1, text);
        final int acceptationId = addAcceptation(db1, concept1, correlationArrayId);

        final MemoryDatabase db2 = new MemoryDatabase();
        final int alphabet2 = addLanguage(db2, "es").right;
        final int concept2 = getMaxConcept(db2) + 1;
        assertEquals(correlationArrayId, addSimpleCorrelationArray(db2, alphabet2, text));
        assertEquals(acceptationId, addAcceptation(db2, concept2, correlationArrayId).intValue());
        assertEquals(db1, db2);

        updateAcceptationCorrelationArray(db2, acceptationId, correlationArrayId);
        assertEquals(db1, db2);
    }

    private void assertNoAcceptationWithCorrelationArray(Database db, int correlationArrayId) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId)
                .select(acceptations.getIdColumnIndex());
        assertFalse(db.select(query).hasNext());
    }

    @Test
    public void testUpdateAcceptationCorrelationArray() {
        final MemoryDatabase db = new MemoryDatabase();

        final String text1 = "cantar";
        final String text2 = "beber";
        final String text2UpperCase = "BEBER";

        final ImmutableIntPair langPair = addLanguage(db, "es");
        final int alphabet = langPair.right;
        final int upperCaseAlphabet = addAlphabetAsConversionTarget(db, alphabet, upperCaseConversion);
        final int concept = getMaxConcept(db) + 1;
        final int secondConjugationVerbBunch = concept + 1;

        final int correlationArrayId1 = addSimpleCorrelationArray(db, alphabet, text1);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId1);

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "er")
                .build();

        addAgent(db, secondConjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(upperCaseAlphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC | QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = obtainQuiz(db, secondConjugationVerbBunch, quizFields);

        final int correlationArrayId2 = addSimpleCorrelationArray(db, alphabet, text2);
        updateAcceptationCorrelationArray(db, acceptationId, correlationArrayId2);

        assertNoAcceptationWithCorrelationArray(db, correlationArrayId1);

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId2)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2, row.get(1).toText());

        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), upperCaseAlphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2UpperCase, row.get(1).toText());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), secondConjugationVerbBunch)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getAcceptationColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());
    }

    @Test
    public void testUpdateAcceptationCorrelationArrayFromMatching() {
        final MemoryDatabase db = new MemoryDatabase();

        final String text1 = "cantar";
        final String text2 = "beber";
        final String text2UpperCase = "BEBER";

        final ImmutableIntPair langPair = addLanguage(db, "es");
        final int alphabet = langPair.right;
        final int upperCaseAlphabet = addAlphabetAsConversionTarget(db, alphabet, upperCaseConversion);
        final int concept = getMaxConcept(db) + 1;
        final int firstConjugationVerbBunch = concept + 1;

        final int correlationArrayId1 = addSimpleCorrelationArray(db, alphabet, text1);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId1);

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        addAgent(db, firstConjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(upperCaseAlphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC | QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = obtainQuiz(db, firstConjugationVerbBunch, quizFields);

        final int correlationArrayId2 = addSimpleCorrelationArray(db, alphabet, text2);
        updateAcceptationCorrelationArray(db, acceptationId, correlationArrayId2);

        assertNoAcceptationWithCorrelationArray(db, correlationArrayId1);

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId2)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2, row.get(1).toText());

        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), upperCaseAlphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2UpperCase, row.get(1).toText());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), firstConjugationVerbBunch)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertFalse(db.select(query).hasNext());

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getAcceptationColumnIndex());
        assertFalse(db.select(query).hasNext());
    }

    @Test
    public void testUpdateCorrelationArrayForAcceptationWithRuleAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final String wrongText = "contar";
        final String rightText = "cantar";
        final String rightGerundText = "cantando";

        final int alphabet = addLanguage(db, "es").right;
        final int concept = getMaxConceptInAcceptations(db) + 1;
        final int gerundRule = concept + 1;
        final int firstConjugationVerbBunch = gerundRule + 1;

        final int wrongCorrelationArrayId = addSimpleCorrelationArray(db, alphabet, wrongText);
        final int acceptationId = addAcceptation(db, concept, wrongCorrelationArrayId);
        addAcceptationInBunch(db, firstConjugationVerbBunch, acceptationId);

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        final ImmutableIntSet firstConjugationVerbBunchSet = new ImmutableIntSetCreator().add(firstConjugationVerbBunch).build();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        addAgent(db, NO_BUNCH, firstConjugationVerbBunchSet, noBunches, nullCorrelation, nullCorrelation, matcher, adder, gerundRule);

        final int rightCorrelationArrayId = addSimpleCorrelationArray(db, alphabet, rightText);
        updateAcceptationCorrelationArray(db, acceptationId, rightCorrelationArrayId);

        final LangbookDbSchema.RuledConceptsTable ruledConceptsTable = LangbookDbSchema.Tables.ruledConcepts;
        DbQuery query = new DbQuery.Builder(ruledConceptsTable)
                .where(ruledConceptsTable.getConceptColumnIndex(), concept)
                .where(ruledConceptsTable.getRuleColumnIndex(), gerundRule)
                .select(ruledConceptsTable.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, query).get(0).toInt();
        assertNotEquals(concept, ruledConcept);

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        query = new DbQuery.Builder(acceptations)
                .where(acceptations.getConceptColumnIndex(), ruledConcept)
                .select(acceptations.getIdColumnIndex(), acceptations.getCorrelationArrayColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        final int ruledAcceptation = row.get(0).toInt();
        final int rightGerundCorrelationArray = row.get(1).toInt();

        final ImmutableIntKeyMap<String> rightGerundTexts = readCorrelationArrayTexts(db, rightGerundCorrelationArray).toImmutable();
        assertEquals(1, rightGerundTexts.size());
        assertEquals(alphabet, rightGerundTexts.keyAt(0));
        assertEquals(rightGerundText, rightGerundTexts.valueAt(0));

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(rightGerundText, row.get(1).toText());
    }

    @Test
    public void testReplaceConversionAfterAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();

        final String kanaText = "ねこ";

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int kanaAlphabet = language + 1;
        final int roumajiAlphabet = kanaAlphabet + 1;
        final int concept = roumajiAlphabet + 1;

        LangbookDbInserter.insertLanguage(db, language, "ja", kanaAlphabet);
        LangbookDbInserter.insertAlphabet(db, kanaAlphabet, language);
        LangbookDbInserter.insertAlphabet(db, roumajiAlphabet, language);

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
        assertTrue(replaceConversion(db, conversion1));

        final int correlationArrayId = addSimpleCorrelationArray(db, kanaAlphabet, kanaText);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId);

        final ImmutableIntKeyMap<String> texts1 = getAcceptationTexts(db, acceptationId);
        assertEquals(2, texts1.size());
        assertEquals(kanaText, texts1.get(kanaAlphabet));
        assertEquals("neo", texts1.get(roumajiAlphabet));

        convMap.put("こ", "ko");
        final Conversion conversion2 = new Conversion(kanaAlphabet, roumajiAlphabet, convMap);
        assertTrue(replaceConversion(db, conversion2));

        final ImmutableIntKeyMap<String> texts2 = getAcceptationTexts(db, acceptationId);
        assertEquals(2, texts2.size());
        assertEquals(kanaText, texts2.get(kanaAlphabet));
        assertEquals("neko", texts2.get(roumajiAlphabet));
    }

    @Test
    public void testReplaceConversionBeforeAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();

        final String kanaText = "ねこ";

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int kanaAlphabet = language + 1;
        final int roumajiAlphabet = kanaAlphabet + 1;
        final int concept = roumajiAlphabet + 1;

        LangbookDbInserter.insertLanguage(db, language, "ja", kanaAlphabet);
        LangbookDbInserter.insertAlphabet(db, kanaAlphabet, language);
        LangbookDbInserter.insertAlphabet(db, roumajiAlphabet, language);

        final int correlationArrayId = addSimpleCorrelationArray(db, kanaAlphabet, kanaText);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId);

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
        assertTrue(replaceConversion(db, conversion));

        final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, acceptationId);
        assertEquals(2, texts.size());
        assertEquals(kanaText, texts.get(kanaAlphabet));
        assertEquals("neko", texts.get(roumajiAlphabet));
    }

    @Test
    public void testReplaceConversionToRemoveConversion() {
        final MemoryDatabase db = new MemoryDatabase();

        final String kanaText = "ねこ";

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int kanaAlphabet = language + 1;
        final int roumajiAlphabet = kanaAlphabet + 1;
        final int concept = roumajiAlphabet + 1;

        LangbookDbInserter.insertLanguage(db, language, "ja", kanaAlphabet);
        LangbookDbInserter.insertAlphabet(db, kanaAlphabet, language);
        LangbookDbInserter.insertAlphabet(db, roumajiAlphabet, language);

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
        assertTrue(replaceConversion(db, conversion));

        final int correlationArrayId = addSimpleCorrelationArray(db, kanaAlphabet, kanaText);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId);

        assertTrue(replaceConversion(db, new Conversion(kanaAlphabet, roumajiAlphabet, MutableHashMap.empty())));

        final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, acceptationId);
        assertEquals(1, texts.size());
        assertEquals(kanaText, texts.get(kanaAlphabet));
    }
}
