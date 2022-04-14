package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashMap;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsChecker;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.ConceptsChecker;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.IntSetter;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContains;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;

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
public interface AcceptationsSerializer0Test<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> {

    MemoryDatabase cloneBySerializing(MemoryDatabase inDb);
    AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> createManager(MemoryDatabase db);
    AlphabetId getNextAvailableId(ConceptsChecker<ConceptId> manager);
    IntSetter<AcceptationId> getAcceptationIdManager();

    static <AcceptationId> ImmutableSet<AcceptationId> findAcceptationsMatchingText(DbExporter.Database db, IntSetter<AcceptationId> acceptationIdSetter, String text) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringColumnIndex(), text)
                .select(strings.getDynamicAcceptationColumnIndex());
        return db.select(query).map(row -> acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

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

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> AcceptationId addSimpleAcceptation(
            AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
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
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        manager.addLanguage("es");
        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final ImmutableSet<AlphabetId> outAlphabets = outManager.findAlphabetsByLanguage(outLanguage);
        assertSize(1, outAlphabets);
        assertNotEquals(outLanguage, outAlphabets.valueAt(0));
    }

    @Test
    default void testRemoveFirstAddedLanguage() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        LanguageId inEsLanguage = manager.addLanguage("es").language;
        manager.addLanguage("en");
        manager.removeLanguage(inEsLanguage);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createManager(outDb);

        assertNull(outManager.findLanguageByCode("es"));
        final LanguageId outEnLanguage = outManager.findLanguageByCode("en");
        final ImmutableSet<AlphabetId> outEnAlphabets = outManager.findAlphabetsByLanguage(outEnLanguage);
        assertSize(1, outEnAlphabets);
        assertNotEquals(outEnLanguage, outEnAlphabets.valueAt(0));
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(secondAlphabet, mainAlphabet);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createManager(outDb);

        final LanguageId outEsLanguage = outManager.findLanguageByCode("es");
        final ImmutableSet<AlphabetId> outEsAlphabets = outManager.findAlphabetsByLanguage(outEsLanguage);
        assertSize(2, outEsAlphabets);
    }

    @Test
    default void testAddAlphabetCopyingFromOtherWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("ja");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        addSimpleAcceptation(manager, mainAlphabet, language.getConceptId(), "日本語");
        manager.addAlphabetCopyingFromOther(getNextAvailableId(manager), mainAlphabet);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> outManager = createManager(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("ja");
        final ImmutableSet<AlphabetId> outAlphabets = outManager.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "日本語"));
        final ImmutableMap<AlphabetId, String> acceptationTexts = outManager.getAcceptationTexts(outAcceptation);
        assertEqualSet(outAlphabets, acceptationTexts.keySet());
        assertEquals(acceptationTexts.valueAt(0), acceptationTexts.valueAt(1));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithoutCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> checker = createManager(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("es");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);

        final AlphabetId outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);
        final AlphabetId outSecondAlphabet = outAlphabets.remove(outMainAlphabet).valueAt(0);

        assertEquals("CASA", checker.getConversion(new ImmutablePair<>(outMainAlphabet, outSecondAlphabet)).convert("casa"));
    }

    @Test
    default void testAddAlphabetAsConversionTargetWithCorrelations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage("es");

        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;

        final ConceptId concept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, mainAlphabet, concept, "casa");

        final AlphabetId secondAlphabet = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(mainAlphabet, secondAlphabet, upperCaseConversion);
        assertTrue(manager.addAlphabetAsConversionTarget(conversion));

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> checker = createManager(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("es");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);

        final AlphabetId outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);
        final AlphabetId outSecondAlphabet = outAlphabets.remove(outMainAlphabet).valueAt(0);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "casa"));
        final ImmutableMap<AlphabetId, String> outTexts = checker.getAcceptationTexts(outAcceptation);
        assertEqualSet(outAlphabets, outTexts.keySet());
        assertEquals("casa", outTexts.get(outMainAlphabet));
        assertEquals("CASA", outTexts.get(outSecondAlphabet));
    }

    @Test
    default void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId alphabet = manager.addLanguage("es").mainAlphabet;
        final ConceptId concept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, alphabet, concept, "cantar");

        final MemoryDatabase outDb = cloneBySerializing(db);
        assertSize(1, findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
    }

    @Test
    default void testAddJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
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

        manager.addAcceptation(concept, correlationArray);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> checker = createManager(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("ja");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);

        final AlphabetId outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);
        final AlphabetId outSecondAlphabet = outAlphabets.remove(outMainAlphabet).valueAt(0);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "注文"));
        final ImmutableList<CorrelationId> correlationIds = checker.getAcceptationCorrelationArray(outAcceptation);
        assertSize(2, correlationIds);

        final ImmutableMap<AlphabetId, String> outFirstCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(0));
        assertSize(2, outFirstCorrelation);
        assertEquals("注", outFirstCorrelation.get(outMainAlphabet));
        assertEquals("ちゅう", outFirstCorrelation.get(outSecondAlphabet));

        final ImmutableMap<AlphabetId, String> outSecondCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(1));
        assertSize(2, outSecondCorrelation);
        assertEquals("文", outSecondCorrelation.get(outMainAlphabet));
        assertEquals("もん", outSecondCorrelation.get(outSecondAlphabet));

        final ImmutableMap<AlphabetId, String> texts = manager.getAcceptationTexts(outAcceptation);
        assertSize(2, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
    }

    @Test
    default void testAddJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> manager = createManager(db);

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

        manager.addAcceptation(concept, correlationArray);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> checker = createManager(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("ja");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);

        final AlphabetId outKanjiAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outKanjiAlphabet, outAlphabets);
        final ImmutableMap<AlphabetId, AlphabetId> outConversionMap = checker.findConversions(outAlphabets);
        final AlphabetId outKanaAlphabet = getSingleValue(outConversionMap);
        final AlphabetId outRoumajiAlphabet = outConversionMap.keyAt(0);
        assertContainsOnly(outKanjiAlphabet, outKanaAlphabet, outRoumajiAlphabet, outAlphabets);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "注文"));
        final ImmutableList<CorrelationId> correlationIds = checker.getAcceptationCorrelationArray(outAcceptation);
        assertSize(2, correlationIds);

        final ImmutableMap<AlphabetId, String> outFirstCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(0));
        assertSize(2, outFirstCorrelation);
        assertEquals("注", outFirstCorrelation.get(outKanjiAlphabet));
        assertEquals("ちゅう", outFirstCorrelation.get(outKanaAlphabet));

        final ImmutableMap<AlphabetId, String> outSecondCorrelation = checker.getCorrelationWithText(correlationIds.valueAt(1));
        assertSize(2, outSecondCorrelation);
        assertEquals("文", outSecondCorrelation.get(outKanjiAlphabet));
        assertEquals("もん", outSecondCorrelation.get(outKanaAlphabet));

        final ImmutableMap<AlphabetId, String> texts = manager.getAcceptationTexts(outAcceptation);
        assertSize(3, texts);
        assertEquals("注文", texts.get(kanji));
        assertEquals("ちゅうもん", texts.get(kana));
        assertEquals("chuumon", texts.get(roumaji));
    }
}
