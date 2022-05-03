package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashMap;
import sword.collections.TraversableTestUtils;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.CharacterCompositionEditorModel;
import sword.langbook3.android.models.CharacterCompositionRepresentation;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.IdentifiableCharacterCompositionResult;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;

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
public interface AcceptationsManagerTest<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId extends CharacterCompositionTypeIdInterface<ConceptId>, CorrelationId, CorrelationArrayId, AcceptationId> {

    AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> createManager(MemoryDatabase db);
    AlphabetId getNextAvailableAlphabetId(ConceptsChecker<ConceptId> manager);
    CharacterCompositionTypeId conceptAsCharacterCompositionTypeId(ConceptId conceptId);

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

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> AcceptationId addSimpleAcceptation(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> boolean updateAcceptationSimpleCorrelationArray(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, AcceptationId acceptationId, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.updateAcceptationCorrelationArray(acceptationId, correlationArray);
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> ConceptId obtainNewConcept(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, String text) {
        final ConceptId newConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, newConcept, text));
        return newConcept;
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> AcceptationId obtainNewAcceptation(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, String text) {
        final ConceptId newConcept = manager.getNextAvailableConceptId();
        final AcceptationId newAcceptation = addSimpleAcceptation(manager, alphabet, newConcept, text);
        assertNotNull(newAcceptation);
        return newAcceptation;
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> void insertUpDownCharacterCompositionDefinition(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, CharacterCompositionTypeId typeId) {
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(manager.updateCharacterCompositionDefinition(typeId, register));
    }

    @Test
    default void testAddLanguageForFirstLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final ConceptId expectedLanguageConceptId = manager.getNextAvailableConceptId();
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");
        assertEquals(expectedLanguageConceptId, langPair.language.getConceptId());

        assertEquals(langPair.language, manager.findLanguageByCode("es"));
        assertContainsOnly(langPair.mainAlphabet, manager.findAlphabetsByLanguage(langPair.language));
    }

    @Test
    default void testAddLanguageWhenAddingTheSameLanguageTwice() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");
        assertNull(manager.addLanguage("es"));

        assertEquals(langPair.language, manager.findLanguageByCode("es"));
        assertContainsOnly(langPair.mainAlphabet, manager.findAlphabetsByLanguage(langPair.language));
    }

    @Test
    default void testRemoveLanguageWhenUnique() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageId language = manager.addLanguage("es").language;
        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
    }

    @Test
    default void testRemoveLanguageWhenRemovingFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair1 = manager.addLanguage("es");
        final LanguageCreationResult<LanguageId, AlphabetId> langPair2 = manager.addLanguage("en");

        assertTrue(manager.removeLanguage(langPair1.language));
        assertNull(manager.findLanguageByCode("es"));
        assertEquals(langPair2.language, manager.findLanguageByCode("en"));
        assertContainsOnly(langPair2.mainAlphabet, manager.findAlphabetsByLanguage(langPair2.language));
    }

    @Test
    default void testRemoveLanguageWhenRemovingLastAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

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
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final AlphabetId secondAlphabet = getNextAvailableAlphabetId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertEquals(language, manager.findLanguageByCode("es"));
        assertContainsOnly(mainAlphabet, secondAlphabet, manager.findAlphabetsByLanguage(language));
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("ja");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final AcceptationId acceptation = addSimpleAcceptation(manager, mainAlphabet, language.getConceptId(), "日本語");

        final AlphabetId secondAlphabet = getNextAvailableAlphabetId(manager);
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
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("ja");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        addSimpleAcceptation(manager, mainAlphabet, language.getConceptId(), "日本語");
        final AlphabetId secondAlphabet = getNextAvailableAlphabetId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet));

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("ja"));
    }

    @Test
    default void testAddAcceptationWhenAddingAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final AlphabetId secondAlphabet = getNextAvailableAlphabetId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        assertEquals(language, manager.findLanguageByCode("es"));
        assertContainsOnly(mainAlphabet, secondAlphabet, manager.findAlphabetsByLanguage(language));

        final String convertedText = manager.getConversion(new ImmutablePair<>(mainAlphabet, secondAlphabet)).convert("casa");

        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("casa", texts.get(mainAlphabet));
        assertEquals(convertedText, texts.get(secondAlphabet));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final AlphabetId secondAlphabet = getNextAvailableAlphabetId(manager);

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
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final ConceptId concept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, mainAlphabet, concept, "casa");
        final AlphabetId secondAlphabet = getNextAvailableAlphabetId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        manager.addAlphabetAsConversionTarget(conversion);

        assertTrue(manager.removeLanguage(language));
        assertNull(manager.findLanguageByCode("es"));
        assertTrue(manager.getConversionsMap().isEmpty());
    }

    @Test
    default void testRemoveLanguageButLeaveOther() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> esLangPair = manager.addLanguage("es");
        final LanguageId esLanguage = esLangPair.language;
        final AlphabetId esMainAlphabet = esLangPair.mainAlphabet;

        final AlphabetId enMainAlphabet = manager.addLanguage("en").mainAlphabet;

        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId esAcc = addSimpleAcceptation(manager, esMainAlphabet, concept, "casa");
        final AcceptationId enAcc = addSimpleAcceptation(manager, enMainAlphabet, concept, "house");
        assertNotEquals(esAcc, enAcc);

        assertTrue(manager.removeLanguage(esLanguage));
        assertNull(manager.findLanguageByCode("es"));
        assertNotNull(manager.findLanguageByCode("en"));
        TraversableTestUtils.assertContainsOnly(enAcc, manager.findAcceptationsByConcept(concept));
    }

    @Test
    default void testAddAcceptationForSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId concept = manager.getNextAvailableConceptId();

        final AcceptationId acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");
        assertSinglePair(alphabet, "cantar", manager.getAcceptationTexts(acceptation));
    }

    @Test
    default void testAddAcceptationWhenAddingAJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableAlphabetId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));
        final ConceptId concept = manager.getNextAvailableConceptId();

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

        final AcceptationId acceptation = manager.addAcceptation(concept, correlationArray);
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptation);
        assertSize(2, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
    }

    @Test
    default void testAddAcceptationWhenAddingAJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableAlphabetId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));

        final AlphabetId roumaji = getNextAvailableAlphabetId(manager);
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
        final ConceptId concept = manager.getNextAvailableConceptId();

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

        final AcceptationId acceptation = manager.addAcceptation(concept, correlationArray);

        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptation);
        assertSize(3, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager1 = createManager(db1);

        final AlphabetId alphabet1 = manager1.addLanguage("es").mainAlphabet;
        final ConceptId concept1 = manager1.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager1, alphabet1, concept1, "cantar");

        final MemoryDatabase db2 = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager2 = createManager(db2);

        final AlphabetId alphabet2 = manager2.addLanguage("es").mainAlphabet;
        final ConceptId concept2 = manager2.getNextAvailableConceptId();
        assertEquals(acceptationId, addSimpleAcceptation(manager2, alphabet2, concept2, "cantar"));
        assertEquals(db1, db2);

        updateAcceptationSimpleCorrelationArray(manager2, alphabet2, acceptationId, "cantar");
        assertEquals(db1, db2);
    }

    @Test
    default void testReplaceConversionAfterAddingAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId kanaAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId roumajiAlphabet = getNextAvailableAlphabetId(manager);

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

        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, "ねこ");

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
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId kanaAlphabet = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId roumajiAlphabet = getNextAvailableAlphabetId(manager);

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

        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptationId = addSimpleAcceptation(manager, kanaAlphabet, concept, "ねこ");
        final ImmutableCorrelation<AlphabetId> texts = manager.getAcceptationTexts(acceptationId);
        assertSize(2, texts);
        assertEquals("ねこ", texts.get(kanaAlphabet));
        assertEquals("neko", texts.get(roumajiAlphabet));
    }

    @Test
    default void testShareConceptRemovesDuplicatedAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId guyConcept = manager.getNextAvailableConceptId();
        final AcceptationId guyAcc = addSimpleAcceptation(manager, alphabet, guyConcept, "persona");

        final ConceptId personConcept = manager.getNextAvailableConceptId();
        final AcceptationId personAcc = addSimpleAcceptation(manager, alphabet, personConcept, "persona");
        assertNotEquals(guyAcc, personAcc);

        assertTrue(manager.shareConcept(personAcc, guyConcept));
        assertEquals(personConcept, manager.conceptFromAcceptation(personAcc));
        assertSinglePair(alphabet, "persona", manager.getAcceptationTexts(personAcc));
        assertNull(manager.conceptFromAcceptation(guyAcc));
        assertEmpty(manager.getAcceptationTexts(guyAcc));
    }

    @Test
    default void testUpdateCharacterCompositionDefinitionIsIdempotent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(manager.updateCharacterCompositionDefinition(compositionTypeId, register));
        assertTrue(manager.updateCharacterCompositionDefinition(compositionTypeId, register));
    }

    @Test
    default void testUpdateCharacterCompositionDefinitionRejectsDuplicatedDefinition() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 2);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(manager.updateCharacterCompositionDefinition(compositionTypeId, register));

        final ConceptId compositionTypeConcept2 = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept2, "arriba-abajo2"));

        final CharacterCompositionTypeId compositionTypeId2 = conceptAsCharacterCompositionTypeId(compositionTypeConcept2);
        assertFalse(manager.updateCharacterCompositionDefinition(compositionTypeId2, register));
    }

    @Test
    default void testUpdateCharacterComposition() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));
        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(composed);
        assertEquals('´', model.first.representation.character);
        assertNull(model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testUpdateCharacterCompositionRejectsDuplicates() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final CharacterId sChar = manager.findCharacter('s');
        assertNotNull(sChar);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));
        assertFalse(manager.updateCharacterComposition(sChar, firstRepresentation, secondRepresentation, compositionTypeId));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(composed);
        assertEquals('´', model.first.representation.character);
        assertNull(model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> sCharModel = manager.getCharacterCompositionDetails(sChar);
        assertEquals('s', sCharModel.representation.character);
        assertNull(sCharModel.first);
        assertNull(sCharModel.second);
        assertNull(sCharModel.compositionType);
    }

    @Test
    default void testUpdateCharacterCompositionRejectsUnknownCompositionTypes() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final CharacterId sChar = manager.findCharacter('s');
        assertNotNull(sChar);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertFalse(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));
    }

    @Test
    default void testUpdateCharacterCompositionRejectsLoops() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));

        final CharacterId aChar = manager.findCharacter('a');
        assertNotNull(aChar);

        final CharacterCompositionRepresentation composedRepresentation = new CharacterCompositionRepresentation('á', null);

        final CharacterCompositionRepresentation fakeRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "fake");

        final ConceptId badCompositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, badCompositionTypeConcept, "izquierda-derecha"));

        final CharacterCompositionTypeId badCompositionTypeId = conceptAsCharacterCompositionTypeId(badCompositionTypeConcept);
        assertFalse(manager.updateCharacterComposition(aChar, composedRepresentation, fakeRepresentation, badCompositionTypeId));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(composed);
        assertEquals('´', model.first.representation.character);
        assertNull(model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> aCharModel = manager.getCharacterCompositionDetails(aChar);
        assertEquals('a', aCharModel.representation.character);
        assertNull(aCharModel.first);
        assertNull(aCharModel.second);
        assertNull(aCharModel.compositionType);
    }

    @Test
    default void testAssignUnicode() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));

        final CharacterId firstCharacterId = manager.getCharacterCompositionDetails(composed).first.id;
        assertTrue(manager.assignUnicode(firstCharacterId, '´'));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(composed);
        assertEquals('´', model.first.representation.character);
        assertNull(model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testAssignUnicodeRejectsDuplicatedUnicode() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));

        final CharacterId firstCharacterId = manager.getCharacterCompositionDetails(composed).first.id;
        assertFalse(manager.assignUnicode(firstCharacterId, 'm'));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(composed);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tilde", model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testMergeCharacters() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId songConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, songConcept, "canción"));

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, eatConcept, "comer"));

        final CharacterId oCharId = manager.findCharacter('o');
        assertNotNull(oCharId);

        final CharacterId oTildeCharId = manager.findCharacter('ó');
        assertNotNull(oTildeCharId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "oNoTilde");

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);
        assertTrue(manager.updateCharacterComposition(oTildeCharId, firstRepresentation, secondRepresentation, compositionTypeId));

        final CharacterId secondCharacterId = manager.getCharacterCompositionDetails(oTildeCharId).second.id;
        assertTrue(manager.mergeCharacters(oCharId, secondCharacterId));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> nullModel = manager.getCharacterCompositionDetails(secondCharacterId);
        assertNull(nullModel.first);
        assertNull(nullModel.second);
        assertNull(nullModel.compositionType);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(oTildeCharId);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tilde", model.first.representation.token);
        assertEquals(oCharId, model.second.id);
        assertEquals('o', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testMergeCharactersOpposite() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId songConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, songConcept, "canción"));

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, eatConcept, "comer"));

        final CharacterId oCharId = manager.findCharacter('o');
        assertNotNull(oCharId);

        final CharacterId oTildeCharId = manager.findCharacter('ó');
        assertNotNull(oTildeCharId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "oNoTilde");

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);
        assertTrue(manager.updateCharacterComposition(oTildeCharId, firstRepresentation, secondRepresentation, compositionTypeId));

        final CharacterId secondCharacterId = manager.getCharacterCompositionDetails(oTildeCharId).second.id;
        assertTrue(manager.mergeCharacters(secondCharacterId, oCharId));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> nullModel = manager.getCharacterCompositionDetails(oCharId);
        assertNull(nullModel.first);
        assertNull(nullModel.second);
        assertNull(nullModel.compositionType);

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(oTildeCharId);
        assertEquals(INVALID_CHARACTER, model.first.representation.character);
        assertEquals("tilde", model.first.representation.token);
        assertEquals(secondCharacterId, model.second.id);
        assertEquals('o', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionTypeId, model.compositionType);
    }

    @Test
    default void testMergeCharactersRejectsIfBothCharactersHasUnicode() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId songConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, songConcept, "canción"));

        final CharacterId cCharId = manager.findCharacter('c');
        assertNotNull(cCharId);

        final CharacterId aCharId = manager.findCharacter('a');
        assertNotNull(aCharId);

        assertFalse(manager.mergeCharacters(cCharId, aCharId));
    }

    @Test
    default void testMergeCharactersBothWithToken() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId songConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, songConcept, "canción"));

        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId aTildeCharId = manager.findCharacter('á');
        assertNotNull(aTildeCharId);

        final CharacterId oTildeCharId = manager.findCharacter('ó');
        assertNotNull(oTildeCharId);

        final CharacterCompositionRepresentation aFirstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation aSecondRepresentation = new CharacterCompositionRepresentation('a', null);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);
        assertTrue(manager.updateCharacterComposition(aTildeCharId, aFirstRepresentation, aSecondRepresentation, compositionTypeId));

        final CharacterCompositionRepresentation oFirstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "oTilde");
        final CharacterCompositionRepresentation oSecondRepresentation = new CharacterCompositionRepresentation('o', null);
        assertTrue(manager.updateCharacterComposition(oTildeCharId, oFirstRepresentation, oSecondRepresentation, compositionTypeId));

        final CharacterId aFirstCharacterId = manager.getCharacterCompositionDetails(aTildeCharId).first.id;
        final CharacterId oFirstCharacterId = manager.getCharacterCompositionDetails(oTildeCharId).first.id;
        assertTrue(manager.mergeCharacters(aFirstCharacterId, oFirstCharacterId));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> aModel = manager.getCharacterCompositionDetails(aTildeCharId);
        assertEquals(aFirstCharacterId, aModel.first.id);
        assertEquals(INVALID_CHARACTER, aModel.first.representation.character);
        assertEquals("tilde", aModel.first.representation.token);
        assertEquals('a', aModel.second.representation.character);
        assertNull(aModel.second.representation.token);
        assertEquals(compositionTypeId, aModel.compositionType);

        CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> oModel = manager.getCharacterCompositionDetails(oTildeCharId);
        assertEquals(aFirstCharacterId, oModel.first.id);
        assertEquals(INVALID_CHARACTER, oModel.first.representation.character);
        assertEquals("tilde", oModel.first.representation.token);
        assertEquals('o', oModel.second.representation.character);
        assertNull(oModel.second.representation.token);
        assertEquals(compositionTypeId, oModel.compositionType);

        assertNull(manager.getToken(oFirstCharacterId));
    }

    @Test
    default void testMergeCharactersBothWithTokenOpposite() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId songConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, songConcept, "canción"));

        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId aTildeCharId = manager.findCharacter('á');
        assertNotNull(aTildeCharId);

        final CharacterId oTildeCharId = manager.findCharacter('ó');
        assertNotNull(oTildeCharId);

        final CharacterCompositionRepresentation aFirstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "tilde");
        final CharacterCompositionRepresentation aSecondRepresentation = new CharacterCompositionRepresentation('a', null);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);
        assertTrue(manager.updateCharacterComposition(aTildeCharId, aFirstRepresentation, aSecondRepresentation, compositionTypeId));

        final CharacterCompositionRepresentation oFirstRepresentation = new CharacterCompositionRepresentation(INVALID_CHARACTER, "oTilde");
        final CharacterCompositionRepresentation oSecondRepresentation = new CharacterCompositionRepresentation('o', null);
        assertTrue(manager.updateCharacterComposition(oTildeCharId, oFirstRepresentation, oSecondRepresentation, compositionTypeId));

        final CharacterId aFirstCharacterId = manager.getCharacterCompositionDetails(aTildeCharId).first.id;
        final CharacterId oFirstCharacterId = manager.getCharacterCompositionDetails(oTildeCharId).first.id;
        assertTrue(manager.mergeCharacters(oFirstCharacterId, aFirstCharacterId));

        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> aModel = manager.getCharacterCompositionDetails(aTildeCharId);
        assertEquals(oFirstCharacterId, aModel.first.id);
        assertEquals(INVALID_CHARACTER, aModel.first.representation.character);
        assertEquals("oTilde", aModel.first.representation.token);
        assertEquals('a', aModel.second.representation.character);
        assertNull(aModel.second.representation.token);
        assertEquals(compositionTypeId, aModel.compositionType);

        CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> oModel = manager.getCharacterCompositionDetails(oTildeCharId);
        assertEquals(oFirstCharacterId, oModel.first.id);
        assertEquals(INVALID_CHARACTER, oModel.first.representation.character);
        assertEquals("oTilde", oModel.first.representation.token);
        assertEquals('o', oModel.second.representation.character);
        assertNull(oModel.second.representation.token);
        assertEquals(compositionTypeId, oModel.compositionType);

        assertNull(manager.getToken(aFirstCharacterId));
    }

    @Test
    default void testRemoveAcceptationRejectsWhenAcceptationIsTheOnlyOneWithAConceptUsedAsCharacterCompositionTypeId() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        final AcceptationId definitionAcceptation = addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo");

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));
        assertFalse(manager.removeAcceptation(definitionAcceptation));
        assertNotNull(manager.getCharacterCompositionDefinition(compositionTypeId));
    }

    @Test
    default void testRemoveAcceptationWhenAcceptationIsNotTheOnlyOneWithAConceptUsedAsCharacterCompositionTypeId() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        final AcceptationId definitionAcceptation = addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo");

        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "up-down"));
        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));
        assertTrue(manager.removeAcceptation(definitionAcceptation));
        assertNull(manager.getCharacterCompositionDefinition(compositionTypeId));
    }

    @Test
    default void testShareConceptRejectsWhenBothConceptsAreCharacterCompositionDefinitions() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        final AcceptationId compositionTypeAcceptation = addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo-50-50");
        assertNotNull(compositionTypeAcceptation);

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final ConceptId compositionType2Concept = manager.getNextAvailableConceptId();
        final AcceptationId compositionType2Acceptation = addSimpleAcceptation(manager, alphabet, compositionType2Concept, "arriba-abajo-25-75");

        final CharacterCompositionTypeId compositionType2Id = conceptAsCharacterCompositionTypeId(compositionType2Concept);
        final CharacterCompositionDefinitionArea first = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 4);
        final CharacterCompositionDefinitionArea second = new CharacterCompositionDefinitionArea(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT / 4, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT * 3 / 4);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(first, second);
        assertTrue(manager.updateCharacterCompositionDefinition(compositionType2Id, register));

        assertFalse(manager.shareConcept(compositionTypeAcceptation, compositionType2Concept));
        assertEquals(compositionTypeConcept, manager.conceptFromAcceptation(compositionTypeAcceptation));
        assertEquals(compositionType2Concept, manager.conceptFromAcceptation(compositionType2Acceptation));
    }

    @Test
    default void testShareConceptWhenOldConceptIsDefinedAsCharacterCompositionDefinition() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId moreConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, moreConcept, "más"));

        final CharacterId composed = manager.findCharacter('á');
        assertNotNull(composed);

        final ConceptId compositionTypeConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, compositionTypeConcept, "arriba-abajo"));

        final CharacterCompositionTypeId compositionTypeId = conceptAsCharacterCompositionTypeId(compositionTypeConcept);
        insertUpDownCharacterCompositionDefinition(manager, compositionTypeId);

        final ConceptId compositionType2Concept = manager.getNextAvailableConceptId();
        final AcceptationId compositionType2Acceptation = addSimpleAcceptation(manager, alphabet, compositionType2Concept, "arriba-abajo2");

        final CharacterCompositionRepresentation firstRepresentation = new CharacterCompositionRepresentation('´', null);
        final CharacterCompositionRepresentation secondRepresentation = new CharacterCompositionRepresentation('a', null);
        assertTrue(manager.updateCharacterComposition(composed, firstRepresentation, secondRepresentation, compositionTypeId));
        assertTrue(manager.shareConcept(compositionType2Acceptation, compositionTypeConcept));

        final CharacterCompositionTypeId compositionType2Id = conceptAsCharacterCompositionTypeId(compositionType2Concept);
        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = manager.getCharacterCompositionDetails(composed);
        assertEquals('´', model.first.representation.character);
        assertNull(model.first.representation.token);
        assertEquals('a', model.second.representation.character);
        assertNull(model.second.representation.token);
        assertEquals(compositionType2Id, model.compositionType);

        final ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> result = manager.getCharacterCompositionTypes(alphabet);
        assertSize(1, result);
        assertEquals(compositionType2Id, result.valueAt(0).id);
    }
}
