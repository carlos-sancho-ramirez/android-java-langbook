package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntTraversableTestUtils;
import sword.collections.MutableHashMap;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;

/**
 * Include all test related to all responsibilities of an AcceptationsManager<LanguageId, AlphabetId>.
 *
 * AcceptationsManager<LanguageId, AlphabetId> responsibilities are limited to the management of:
 * <li>Languages</li>
 * <li>Alphabets</li>
 * <li>Symbol arrays</li>
 * <li>Correlations</li>
 * <li>Correlation arrays</li>
 * <li>Conversions</li>
 * <li>Acceptations</li>
 */
public interface AcceptationsManagerTest<LanguageId, AlphabetId, CorrelationId> {

    AcceptationsManager<LanguageId, AlphabetId, CorrelationId> createManager(MemoryDatabase db);
    AlphabetId getNextAvailableId(ConceptsChecker manager);
    int getLanguageConceptId(LanguageId language);

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

    static <LanguageId, AlphabetId, CorrelationId> int addSimpleAcceptation(AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager, AlphabetId alphabet, int concept, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static <LanguageId, AlphabetId, CorrelationId> boolean updateAcceptationSimpleCorrelationArray(AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager, AlphabetId alphabet, int acceptationId, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.updateAcceptationCorrelationArray(acceptationId, correlationArray);
    }

    @Test
    default void testAddFirstLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        assertEquals(langPair.language, manager.findLanguageByCode("es"));
        assertContainsOnly(langPair.mainAlphabet, manager.findAlphabetsByLanguage(langPair.language));
    }

    @Test
    default void testAddSameLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");
        assertNull(manager.addLanguage("es"));

        assertEquals(langPair.language, manager.findLanguageByCode("es"));
        assertContainsOnly(langPair.mainAlphabet, manager.findAlphabetsByLanguage(langPair.language));
    }

    @Test
    default void testRemoveUniqueLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageId language = manager.addLanguage("es").language;
        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
    }

    @Test
    default void testRemoveFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair1 = manager.addLanguage("es");
        final LanguageCreationResult<LanguageId, AlphabetId> langPair2 = manager.addLanguage("en");

        assertTrue(manager.removeLanguage(langPair1.language));
        assertNull(manager.findLanguageByCode("es"));
        assertEquals(langPair2.language, manager.findLanguageByCode("en"));
        assertContainsOnly(langPair2.mainAlphabet, manager.findAlphabetsByLanguage(langPair2.language));
    }

    @Test
    default void testRemoveLastAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair1 = manager.addLanguage("es");
        final LanguageCreationResult<LanguageId, AlphabetId> langPair2 = manager.addLanguage("en");

        assertTrue(manager.removeLanguage(langPair2.language));
        assertNull(manager.findLanguageByCode("en"));
        assertEquals(langPair1.language, manager.findLanguageByCode("es"));
        assertContainsOnly(langPair1.mainAlphabet, manager.findAlphabetsByLanguage(langPair1.language));
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode("es"));
        assertContainsOnly(mainAlphabet, secondAlphabet, manager.findAlphabetsByLanguage(language));
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("ja");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final int acceptation = addSimpleAcceptation(manager, mainAlphabet, getLanguageConceptId(language), "日本語");

        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode("ja"));
        final ImmutableSet<AlphabetId> alphabetSet = manager.findAlphabetsByLanguage(language);
        assertContainsOnly(mainAlphabet, secondAlphabet, alphabetSet);

        final ImmutableCorrelation<AlphabetId> acceptationTexts = manager.getAcceptationTexts(acceptation);
        assertSize(2, acceptationTexts);
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
        assertEqualSet(alphabetSet, acceptationTexts.keySet());
    }

    @Test
    default void testRemoveLanguageAfterAddingAlphabetCopyingFromOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("ja");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        addSimpleAcceptation(manager, mainAlphabet, getLanguageConceptId(language), "日本語");
        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("ja"));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode("es"));
        assertContainsOnly(mainAlphabet, secondAlphabet, manager.findAlphabetsByLanguage(language));

        final String convertedText = manager.getConversion(new ImmutablePair<>(mainAlphabet, secondAlphabet)).convert("casa");

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("casa", texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final AlphabetId secondAlphabet = getNextAvailableId(manager);

        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode("es"));
        assertContainsOnly(mainAlphabet, secondAlphabet, manager.findAlphabetsByLanguage(language));

        final String convertedText = manager.getConversion(new ImmutablePair<>(mainAlphabet, secondAlphabet)).convert("casa");

        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("casa", texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    default void testRemoveLanguageAfterAddingAlphabetAsConversionTarget() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        manager.addAlphabetAsConversionTarget(conversion);

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
        assertTrue(manager.getConversionsMap().isEmpty());
    }

    @Test
    default void testRemoveLanguageButLeaveOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> esLangPair = manager.addLanguage("es");
        final LanguageId esLanguage = esLangPair.language;
        final AlphabetId esMainAlphabet = esLangPair.mainAlphabet;

        final AlphabetId enMainAlphabet = manager.addLanguage("en").mainAlphabet;

        final int concept = manager.getMaxConcept() + 1;
        final int esAcc = addSimpleAcceptation(manager, esMainAlphabet, concept, "casa");
        final int enAcc = addSimpleAcceptation(manager, enMainAlphabet, concept, "house");
        assertNotEquals(esAcc, enAcc);

        assertTrue(manager.removeLanguage(esLanguage));
        assertNull(manager.findLanguageByCode("es"));
        assertNotNull(manager.findLanguageByCode("en"));
        IntTraversableTestUtils.assertContainsOnly(enAcc, manager.findAcceptationsByConcept(concept));
    }

    @Test
    default void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");
        assertSinglePair(alphabet, "cantar", manager.getAcceptationTexts(acceptation));
    }

    @Test
    default void testAddJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));
        final int concept = manager.getMaxConcept() + 1;

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .add(new ImmutableCorrelation.Builder<AlphabetId>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableCorrelation.Builder<AlphabetId>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        final int acceptation = manager.addAcceptation(concept, correlationArray);
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptation);
        assertSize(2, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
    }

    @Test
    default void testAddJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));

        final AlphabetId roumaji = getNextAvailableId(manager);
        final MutableHashMap<String, String> convMap = new MutableHashMap.Builder<String, String>()
                .put("あ", "a")
                .put("も", "mo")
                .put("ん", "n")
                .put("う", "u")
                .put("ちゅ", "chu")
                .put("ち", "chi")
                .build();
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, convMap);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));
        final int concept = manager.getMaxConcept() + 1;

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .add(new ImmutableCorrelation.Builder<AlphabetId>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableCorrelation.Builder<AlphabetId>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        final int acceptation = manager.addAcceptation(concept, correlationArray);

        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptation);
        assertSize(3, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager1 = createManager(db1);

        final AlphabetId alphabet1 = manager1.addLanguage("es").mainAlphabet;
        final int concept1 = manager1.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager1, alphabet1, concept1, "cantar");

        final MemoryDatabase db2 = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager2 = createManager(db2);

        final AlphabetId alphabet2 = manager2.addLanguage("es").mainAlphabet;
        final int concept2 = manager2.getMaxConcept() + 1;
        assertEquals(acceptationId, addSimpleAcceptation(manager2, alphabet2, concept2, "cantar"));
        assertEquals(db1, db2);

        updateAcceptationSimpleCorrelationArray(manager2, alphabet2, acceptationId, "cantar");
        assertEquals(db1, db2);
    }

    @Test
    default void testReplaceConversionAfterAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final AlphabetId kanaAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId roumajiAlphabet = getNextAvailableId(manager);

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

        final Conversion<AlphabetId> conversion1 = new Conversion<>(kanaAlphabet, roumajiAlphabet, convMap);
        manager.addAlphabetAsConversionTarget(conversion1);

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, "ねこ");

        final ImmutableCorrelation<AlphabetId> texts1 = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts1);
        assertEquals("ねこ", texts1.get(kanaAlphabet));
        assertEquals("neo", texts1.get(roumajiAlphabet));

        convMap.put("こ", "ko");
        final Conversion<AlphabetId> conversion2 = new Conversion<>(kanaAlphabet, roumajiAlphabet, convMap);
        assertTrue(manager.replaceConversion(conversion2));

        final ImmutableCorrelation<AlphabetId> texts2 = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts2);
        assertEquals("ねこ", texts2.get(kanaAlphabet));
        assertEquals("neko", texts2.get(roumajiAlphabet));
    }

    @Test
    default void testReplaceConversionBeforeAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final AlphabetId kanaAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId roumajiAlphabet = getNextAvailableId(manager);

        final Conversion<AlphabetId> emptyConversion = new Conversion<>(kanaAlphabet, roumajiAlphabet, ImmutableHashMap.empty());
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
        final Conversion<AlphabetId> conversion = new Conversion<>(kanaAlphabet, roumajiAlphabet, convMap);
        assertTrue(manager.replaceConversion(conversion));

        final int concept = manager.getMaxConcept() + 1;
        final int acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, "ねこ");
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("ねこ", texts.get(kanaAlphabet));
        assertEquals("neko", texts.get(roumajiAlphabet));
    }

    @Test
    default void testShareConceptRemovesDuplicatedAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<LanguageId, AlphabetId, CorrelationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final int guyConcept = manager.getMaxConcept() + 1;
        final int guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "persona");

        final int personConcept = manager.getMaxConcept() + 1;
        final int personAcc = addSimpleAcceptation(manager, alphabet, personConcept, "persona");
        assertNotEquals(guyAcc, personAcc);

        assertTrue(manager.shareConcept(personAcc, guyConcept));
        assertEquals(personConcept, manager.conceptFromAcceptation(personAcc));
        assertSinglePair(alphabet, "persona", manager.getAcceptationTexts(personAcc));
        assertEquals(0, manager.conceptFromAcceptation(guyAcc));
        assertEmpty(manager.getAcceptationTexts(guyAcc));
    }
}
