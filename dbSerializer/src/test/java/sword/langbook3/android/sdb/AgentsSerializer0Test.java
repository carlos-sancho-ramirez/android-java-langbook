package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsChecker2;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.BunchesManager;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.IntSetter;
import sword.langbook3.android.db.LanguageIdInterface;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.SearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContains;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializer0Test.findAcceptationsMatchingText;

/**
 * Include all test related to all values that a BunchesSerializer should serialize.
 *
 * Values the the AcceptationsSerializer should serialize are limited to:
 * <li>Bunches</li>
 */
interface AgentsSerializer0Test<ConceptId, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends BunchesSerializer0Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> {

    static <T> ImmutableSet<T> setOf() {
        return ImmutableHashSet.empty();
    }

    static <T> ImmutableSet<T> setOf(T a) {
        return new ImmutableHashSet.Builder<T>().add(a).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).build();
    }

    static <AlphabetId> ImmutableCorrelationArray<AlphabetId> composeSingleElementArray(ImmutableCorrelation<AlphabetId> correlation) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>().append(correlation).build();
    }

    final class ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> {
        private final MemoryDatabase db;
        private final IntSetter<AcceptationId> acceptationIdSetter;
        private final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> checker;
        private final MutableHashSet<ConceptId> found = MutableHashSet.empty();

        ConceptFinder(MemoryDatabase db, IntSetter<AcceptationId> acceptationIdSetter, AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> checker) {
            this.db = db;
            this.acceptationIdSetter = acceptationIdSetter;
            this.checker = checker;
        }

        ConceptId find(String text) {
            final AcceptationId acceptation = getSingleValue(findAcceptationsMatchingText(db, acceptationIdSetter, text));
            final ConceptId concept = checker.conceptFromAcceptation(acceptation);
            assertTrue(found.add(concept));
            return concept;
        }

        void assertUnknown(ConceptId concept) {
            if (found.contains(concept)) {
                fail("Concept matching an already found bunch concept");
            }
        }
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> AcceptationId addSimpleAcceptation(
            AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept, String text) {
        final ImmutableCorrelation<AlphabetId> correlation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(alphabet, text)
                .build();

        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlation)
                .build();

        return manager.addAcceptation(concept, correlationArray);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> AgentId addSingleAlphabetAgent(
            AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, RuleId rule) {
        final ImmutableCorrelation<AlphabetId> startMatcher = (startMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, startMatcherText).build();

        final ImmutableCorrelationArray<AlphabetId> startAdder = (startAdderText == null)? ImmutableCorrelationArray.empty() :
                composeSingleElementArray(new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, startAdderText).build());

        final ImmutableCorrelation<AlphabetId> endMatcher = (endMatcherText == null)? ImmutableCorrelation.empty() :
                new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, endMatcherText).build();

        final ImmutableCorrelationArray<AlphabetId> endAdder = (endAdderText == null)? ImmutableCorrelationArray.empty() :
                composeSingleElementArray(new ImmutableCorrelation.Builder<AlphabetId>().put(alphabet, endAdderText).build());

        return manager.addAgent(targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> void assertSearchResult(
            AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AcceptationId acceptation, String str, String expectedMainStr, String expectedMainAccMainStr, ImmutableList<RuleId> expectedRules) {
        final ImmutableList<SearchResult<AcceptationId, RuleId>> list = manager.findAcceptationAndRulesFromText(str, DbQuery.RestrictionStringTypes.EXACT, new ImmutableIntRange(0, 19));
        assertSize(1, list);

        SearchResult<AcceptationId, RuleId> searchResult = list.valueAt(0);
        assertEquals(acceptation, searchResult.getId());
        assertEquals(str, searchResult.getStr());
        assertEquals(expectedRules, searchResult.getAppliedRules());
        assertEquals(expectedMainStr, searchResult.getMainStr());
        assertEquals(expectedMainAccMainStr, searchResult.getMainAccMainStr());
        assertNotEquals(expectedRules.isEmpty(), searchResult.isDynamic());
    }

    static <ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> void assertSearchResult(
            AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AcceptationId acceptation, String str, String expectedMainStr) {
        assertSearchResult(manager, acceptation, str, expectedMainStr, expectedMainStr, ImmutableList.empty());
    }

    @Override
    AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> createInManager(MemoryDatabase db);

    @Override
    AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> createOutChecker(MemoryDatabase db);
    RuleId conceptAsRuleId(ConceptId conceptId);

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> ConceptId obtainNewConcept(
            AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> manager, AlphabetId alphabet, String text) {
        final ConceptId newConcept = manager.getNextAvailableConceptId();
        assertNotNull(addSimpleAcceptation(manager, alphabet, newConcept, text));
        return newConcept;
    }

    default BunchId obtainNewBunch(BunchesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId> manager, AlphabetId alphabet, String text) {
        return conceptAsBunchId(obtainNewConcept(manager, alphabet, text));
    }

    default RuleId obtainNewRule(AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager, AlphabetId alphabet, String text) {
        return conceptAsRuleId(obtainNewConcept(manager, alphabet, text));
    }

    default ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> newConceptFinder(MemoryDatabase db, AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> checker) {
        return new ConceptFinder<>(db, getAcceptationIdManager(), checker);
    }

    final class DoubleAlphabetCorrelationComposer<AlphabetId> {
        final AlphabetId first;
        final AlphabetId second;

        DoubleAlphabetCorrelationComposer(AlphabetId first, AlphabetId second) {
            this.first = first;
            this.second = second;
        }

        ImmutableCorrelation<AlphabetId> compose(String text1, String text2) {
            return new ImmutableCorrelation.Builder<AlphabetId>()
                    .put(first, text1)
                    .put(second, text2)
                    .build();
        }
    }

    @Test
    default void testAddAcceptationIncludesMixtureOfAlphabetsWhenAddingJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

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
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> checker = createOutChecker(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("ja");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);

        final AlphabetId outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "注文"));
        assertSearchResult(checker, outAcceptation, "注文", "注文");
        assertSearchResult(checker, outAcceptation, "注もん", "注文");
        assertSearchResult(checker, outAcceptation, "ちゅう文", "注文");
        assertSearchResult(checker, outAcceptation, "ちゅうもん", "注文");
    }

    @Test
    default void testAddAcceptationIncludesMixtureOfAlphabetsWhenAddingJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

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
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> checker = createOutChecker(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("ja");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);

        final AlphabetId outKanjiAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outKanjiAlphabet, outAlphabets);
        final ImmutableMap<AlphabetId, AlphabetId> outConversionMap = checker.findConversions(outAlphabets);
        final AlphabetId outKanaAlphabet = getSingleValue(outConversionMap);
        final AlphabetId outRoumajiAlphabet = outConversionMap.keyAt(0);
        assertContainsOnly(outKanjiAlphabet, outKanaAlphabet, outRoumajiAlphabet, outAlphabets);

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "注文"));
        assertSearchResult(checker, outAcceptation, "注文", "注文");
        assertSearchResult(checker, outAcceptation, "注もん", "注文");
        assertSearchResult(checker, outAcceptation, "ちゅう文", "注文");
        assertSearchResult(checker, outAcceptation, "ちゅうもん", "注文");
    }

    @Test
    default void testIncludeMixtureOfAlphabetsForRuledAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        assertTrue(manager.addAlphabetCopyingFromOther(kana, kanji));

        final ImmutableCorrelation<AlphabetId> ruCorrelation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(kanji, "る")
                .put(kana, "る")
                .build();

        final ConceptId concept = manager.getNextAvailableConceptId();
        final ImmutableCorrelationArray<AlphabetId> correlationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .add(new ImmutableCorrelation.Builder<AlphabetId>()
                        .put(kanji, "食")
                        .put(kana, "た")
                        .build())
                .add(new ImmutableCorrelation.Builder<AlphabetId>()
                        .put(kanji, "べ")
                        .put(kana, "べ")
                        .build())
                .add(ruCorrelation)
                .build();

        final AcceptationId inEatAcceptation = manager.addAcceptation(concept, correlationArray);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;

        final BunchId inSourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(inSourceBunch, inEatAcceptation);

        final ImmutableCorrelation<AlphabetId> sugiruCorrelation = new ImmutableCorrelation.Builder<AlphabetId>()
                .put(kanji, "過ぎる")
                .put(kana, "すぎる")
                .build();
        final ImmutableCorrelationArray<AlphabetId> sugiruCorrelationArray = composeSingleElementArray(sugiruCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final RuleId exceedRule = obtainNewRule(manager, enAlphabet, "exceed");
        manager.addAgent(setOf(), setOf(inSourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, sugiruCorrelationArray, exceedRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> checker = createOutChecker(outDb);

        final LanguageId outLanguage = checker.findLanguageByCode("ja");
        final ImmutableSet<AlphabetId> outAlphabets = checker.findAlphabetsByLanguage(outLanguage);
        assertSize(2, outAlphabets);

        final AlphabetId outMainAlphabet = checker.findMainAlphabetForLanguage(outLanguage);
        assertContains(outMainAlphabet, outAlphabets);

        final AcceptationId outExceedAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "exceed"));
        final RuleId outExceedRule = conceptAsRuleId(manager.conceptFromAcceptation(outExceedAcceptation));

        final AcceptationId outAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べ過ぎる"));
        final ImmutableList<RuleId> expectedRules = ImmutableList.<RuleId>empty().append(outExceedRule);
        assertSearchResult(checker, outAcceptation, "食べ過ぎる", "食べ過ぎる", "食べる", expectedRules);
        assertSearchResult(checker, outAcceptation, "たべ過ぎる", "食べ過ぎる", "食べる", expectedRules);
        assertSearchResult(checker, outAcceptation, "食べすぎる", "食べ過ぎる", "食べる", expectedRules);
        assertSearchResult(checker, outAcceptation, "たべすぎる", "食べ過ぎる", "食べる", expectedRules);
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptId outGerundConcept = newConceptFinder(outDb, outManager).find("gerundio");
        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();

        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);
        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptationWithEmptyDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptationWithEmptyDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchNoMatchingAcceptationWithMatchingAcceptationInDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final ConceptId palate = inManager.getNextAvailableConceptId();
        final AcceptationId palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        inManager.addAcceptationInBunch(exceptionsBunch, palateAcc);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingAcceptationWithMatchingAcceptationInDiff() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId exceptionsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, exceptionsConcept, "excepciones");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final ConceptId palate = inManager.getNextAvailableConceptId();
        final AcceptationId palateAcc = addSimpleAcceptation(inManager, alphabet, palate, "paladar");

        final BunchId exceptionsBunch = conceptAsBunchId(exceptionsConcept);
        inManager.addAcceptationInBunch(exceptionsBunch, palateAcc);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(exceptionsBunch), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outExceptionsConcept = bunchFinder.find("excepciones");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertContainsOnly(conceptAsBunchId(outExceptionsConcept), outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));
        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertContainsOnly(conceptAsBunchId(outGerundBunchConcept), outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));
        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
        assertEmpty(outManager.getAcceptationsInBunch(conceptAsBunchId(outGerundBunchConcept)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithMultipleTargetsWithoutSourceBunchNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final ConceptId secondConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, secondConcept, "mis palabras");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final BunchId secondBunch = conceptAsBunchId(secondConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch, secondBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");
        final ConceptId outSecondBunchConcept = bunchFinder.find("mis palabras");

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);

        final BunchId outGerundBunch = conceptAsBunchId(outGerundBunchConcept);
        final BunchId outSecondBunch = conceptAsBunchId(outSecondBunchConcept);
        assertContainsOnly(outGerundBunch, outSecondBunch, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
        assertEmpty(outManager.getAcceptationsInBunch(outGerundBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outSecondBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithTargetWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);

        final BunchId outGerundBunch = conceptAsBunchId(outGerundBunchConcept);
        assertContainsOnly(outGerundBunch, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outGerundBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithMultipleTargetsWithoutSourceBunchMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId gerundConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerundConcept, "gerundios");

        final ConceptId secondConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, secondConcept, "mis palabras");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final BunchId gerundBunch = conceptAsBunchId(gerundConcept);
        final BunchId secondBunch = conceptAsBunchId(secondConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(gerundBunch, secondBunch), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outGerundBunchConcept = bunchFinder.find("gerundios");
        final ConceptId outSecondBunchConcept = bunchFinder.find("mis palabras");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outGerundBunchBunch = conceptAsBunchId(outGerundBunchConcept);
        final BunchId outSecondBunch = conceptAsBunchId(outSecondBunchConcept);
        assertContainsOnly(outGerundBunchBunch, outSecondBunch, outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outGerundBunchBunch));
        assertContainsOnly(processedMap.valueAt(0), outManager.getAcceptationsInBunch(outSecondBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithoutSourceBunchMatchingBothStaticAndDynamicAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptId repeat = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, repeat, "repetición");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final RuleId repeatRule = conceptAsRuleId(repeat);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "arar", repeatRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outRepeatConcept = bunchFinder.find("repetición");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "arar", outAgentDetails.endAdder.valueAt(0));

        final RuleId outRepeatRule = conceptAsRuleId(outRepeatConcept);
        assertEquals(outRepeatRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outRepeatRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithEmptySourceBunch() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");

        final ConceptId concept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
        assertEmpty(outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertContainsOnly(outVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertNull(outManager.findRuledConcept(outGerundRule, outSingConcept));
        assertNull(outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation));
    }

    @Test
    default void testSerializeAgentApplyingRuleNoMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId concept = inManager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAcceptationInBunch(verbBunch, acceptation);

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "er", "iendo", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
        assertContainsOnly(outVerbBunch, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertContainsOnly(outVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "er", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "iendo", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);
        assertEmpty(outManager.findRuledConceptsByRule(outGerundRule));
        assertNull(outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation));
    }

    @Test
    default void testSerializeAgentApplyingRuleMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId concept = inManager.getNextAvailableConceptId();
        final AcceptationId acceptation = addSimpleAcceptation(inManager, alphabet, concept, "cantar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAcceptationInBunch(verbBunch, acceptation);
        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(verbBunch), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
        assertContainsOnly(outVerbBunch, outManager.findBunchesWhereAcceptationIsIncluded(outSingAcceptation));

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertContainsOnly(outVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));
        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ConceptId outSingingConcept = outManager.findRuledConcept(outGerundRule, outSingConcept);
        final AcceptationId outSingingAcceptation = outManager.findRuledAcceptationByAgentAndBaseAcceptation(outAgentId, outSingAcceptation);
        assertNotEquals(outSingAcceptation, outSingingAcceptation);

        assertEquals(outSingingConcept, outManager.conceptFromAcceptation(outSingingAcceptation));
        assertSinglePair(outAlphabet, "cantando", outManager.getAcceptationTexts(outSingingAcceptation));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(), emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId transitiveVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, transitiveVerbConcept, "verbo transitivo");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId transitiveVerbBunch = conceptAsBunchId(transitiveVerbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        inManager.addAgent(setOf(verbBunch, transitiveVerbBunch), setOf(arVerbBunch), setOf(), emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outTransitiveVerbConcept = bunchFinder.find("verbo transitivo");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outTransitiveVerbBunch = conceptAsBunchId(outTransitiveVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outTransitiveVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outTransitiveVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        final ImmutableSet<BunchId> sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAgent(setOf(verbBunch), sourceBunches, noBunches, emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsAgentWithAMatchingAcceptation() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId secondConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, secondConcept, "mis palabras");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        final ImmutableSet<BunchId> sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId secondBunch = conceptAsBunchId(secondConcept);
        inManager.addAgent(setOf(verbBunch, secondBunch), sourceBunches, noBunches, emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outSecondConcept = bunchFinder.find("mis palabras");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outSecondBunch = conceptAsBunchId(outSecondConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        assertContainsOnly(outVerbBunch, outSecondBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outSecondBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        final ImmutableSet<BunchId> noBunches = ImmutableHashSet.empty();
        final ImmutableSet<BunchId> sourceBunches = noBunches.add(arVerbBunch);
        final ImmutableSet<BunchId> diffBunches = noBunches.add(arEndedNounsBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAgent(setOf(verbBunch), sourceBunches, diffBunches, emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsWithDiffBunchAgentWithoutMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId actionConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, actionConcept, "acción");

        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        final ImmutableSet<BunchId> targetBunches = setOf(verbBunch, actionBunch);
        final ImmutableSet<BunchId> sourceBunches = setOf(arVerbBunch);
        final ImmutableSet<BunchId> diffBunches = setOf(arEndedNounsBunch);
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        inManager.addAgent(targetBunches, sourceBunches, diffBunches, emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");
        final ConceptId outActionConcept = bunchFinder.find("acción");

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outActionBunch = conceptAsBunchId(outActionConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outActionBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outActionBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToMultipleTargetsWithDiffBunchAgentWithMatchingAcceptations() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId actionConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, actionConcept, "acción");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId actionBunch = conceptAsBunchId(actionConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAgent(setOf(verbBunch, actionBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNoundConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");
        final ConceptId outActionConcept = bunchFinder.find("acción");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outActionBunch = conceptAsBunchId(outActionConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNoundBunch = conceptAsBunchId(outArEndedNoundConcept);
        assertContainsOnly(outVerbBunch, outActionBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNoundBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outArEndedNoundBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outActionBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsInBothSourceAndDiffBunches() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAcceptationInBunch(arVerbBunch, inAcceptation);
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeCopyFromSingleSourceToTargetWithDiffBunchAgentWithMatchingAcceptationsOnlyInDiffBunch() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId inAlphabet = inManager.addLanguage("es").mainAlphabet;
        final ConceptId arVerbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arVerbConcept, "verbo de primera conjugación");

        final ConceptId arEndedNounsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, arEndedNounsConcept, "sustantivos acabados en ar");

        final ConceptId verbConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, inAlphabet, verbConcept, "verbo");

        final ConceptId inSingConcept = inManager.getNextAvailableConceptId();
        final AcceptationId inAcceptation = addSimpleAcceptation(inManager, inAlphabet, inSingConcept, "cantar");
        final BunchId arEndedNounsBunch = conceptAsBunchId(arEndedNounsConcept);
        inManager.addAcceptationInBunch(arEndedNounsBunch, inAcceptation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId verbBunch = conceptAsBunchId(verbConcept);
        final BunchId arVerbBunch = conceptAsBunchId(arVerbConcept);
        inManager.addAgent(setOf(verbBunch), setOf(arVerbBunch), setOf(arEndedNounsBunch),
                emptyCorrelation, emptyCorrelationArray, emptyCorrelation, emptyCorrelationArray, null);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outArVerbConcept = bunchFinder.find("verbo de primera conjugación");
        final ConceptId outArEndedNounConcept = bunchFinder.find("sustantivos acabados en ar");
        final ConceptId outVerbConcept = bunchFinder.find("verbo");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        final BunchId outVerbBunch = conceptAsBunchId(outVerbConcept);
        final BunchId outArVerbBunch = conceptAsBunchId(outArVerbConcept);
        final BunchId outArEndedNounBunch = conceptAsBunchId(outArEndedNounConcept);
        assertContainsOnly(outVerbBunch, outAgentDetails.targetBunches);
        assertContainsOnly(outArVerbBunch, outAgentDetails.sourceBunches);
        assertContainsOnly(outArEndedNounBunch, outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertEmpty(outAgentDetails.endMatcher);
        assertEmpty(outAgentDetails.endAdder);

        assertEmpty(outManager.getAcceptationsInBunch(outArVerbBunch));
        assertContainsOnly(outSingAcceptation, outManager.getAcceptationsInBunch(outArEndedNounBunch));
        assertEmpty(outManager.getAcceptationsInBunch(outVerbBunch));
    }

    @Test
    default void testSerializeAgentApplyingRuleWithConversionPresent() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> inManager = createInManager(inDb);

        final AlphabetId alphabet = inManager.addLanguage("es").mainAlphabet;
        final AlphabetId upperCaseAlphabet = getNextAvailableId(inManager);
        final Conversion<AlphabetId> conversion = new Conversion<>(alphabet, upperCaseAlphabet, AcceptationsSerializerTest.upperCaseConversion);
        inManager.addAlphabetAsConversionTarget(conversion);

        final ConceptId gerund = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, gerund, "gerundio");

        final ConceptId singConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, singConcept, "cantar");

        final RuleId gerundRule = conceptAsRuleId(gerund);
        addSingleAlphabetAgent(inManager, setOf(), setOf(), setOf(), alphabet, null, null, "ar", "ando", gerundRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = outManager.getConversionsMap();
        assertSize(1, conversionMap);
        final AlphabetId outAlphabet = conversionMap.valueAt(0);
        final AlphabetId outUpperCaseAlphabet = conversionMap.keyAt(0);

        final ConceptFinder<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> bunchFinder = newConceptFinder(outDb, outManager);
        final ConceptId outGerundConcept = bunchFinder.find("gerundio");

        final AcceptationId outSingAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "cantar"));
        final ConceptId outSingConcept = outManager.conceptFromAcceptation(outSingAcceptation);
        bunchFinder.assertUnknown(outSingConcept);

        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        final AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> outAgentDetails = outManager.getAgentDetails(outAgentId);
        assertEmpty(outAgentDetails.targetBunches);
        assertEmpty(outAgentDetails.sourceBunches);
        assertEmpty(outAgentDetails.diffBunches);
        assertEmpty(outAgentDetails.startMatcher);
        assertEmpty(outAgentDetails.startAdder);
        assertSinglePair(outAlphabet, "ar", outAgentDetails.endMatcher);
        assertSize(1, outAgentDetails.endAdder);
        assertSinglePair(outAlphabet, "ando", outAgentDetails.endAdder.valueAt(0));

        final RuleId outGerundRule = conceptAsRuleId(outGerundConcept);
        assertEquals(outGerundRule, outAgentDetails.rule);

        final ImmutableMap<ConceptId, ConceptId> outRuledConcepts = outManager.findRuledConceptsByRule(outGerundRule);
        assertContainsOnly(outSingConcept, outRuledConcepts);
        final ConceptId outSingRuledConcept = outRuledConcepts.keyAt(0);

        final ImmutableMap<AcceptationId, AcceptationId> processedMap = outManager.getAgentProcessedMap(outAgentId);
        assertContainsOnly(outSingAcceptation, processedMap.keySet());
        assertEquals(outSingRuledConcept, outManager.conceptFromAcceptation(processedMap.valueAt(0)));

        final ImmutableCorrelation<AlphabetId> texts = outManager.getAcceptationTexts(processedMap.valueAt(0));
        assertSize(2, texts);
        assertEquals("cantando", texts.get(outAlphabet));
        assertEquals("CANTANDO", texts.get(outUpperCaseAlphabet));
    }

    @Test
    default void testSerializeAgentWithJustEndAdderForAcceptationFromOtherLanguage() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(inDb);
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;
        final AlphabetId jaAlphabet = manager.addLanguage("ja").mainAlphabet;

        final ConceptId singConcept = manager.getNextAvailableConceptId();
        final AcceptationId singAcceptation = addSimpleAcceptation(manager, esAlphabet, singConcept, "cantar");

        final ConceptId myConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, myConcept, "palabras");
        final BunchId myBunch = conceptAsBunchId(myConcept);
        manager.addAcceptationInBunch(myBunch, singAcceptation);

        final ConceptId studyConcept = manager.getNextAvailableConceptId();
        final AcceptationId studyAcceptation = addSimpleAcceptation(manager, jaAlphabet, studyConcept, "べんきょう");
        manager.addAcceptationInBunch(myBunch, studyAcceptation);

        final ConceptId verbalitationConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, verbalitationConcept, "verbalización");

        final RuleId verbalitationRule = conceptAsRuleId(verbalitationConcept);
        addSingleAlphabetAgent(manager, setOf(), setOf(myBunch), setOf(), jaAlphabet, null, null, null, "する", verbalitationRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId outStudyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "べんきょう"));
        final AgentId outAgentId = getSingleValue(outManager.getAgentIds());
        assertContainsOnly(outStudyAcceptation, manager.getAgentProcessedMap(outAgentId).keySet());
    }

    @Test
    default void testSerializeChainedAgentsApplyingRules() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(inDb);
        final AlphabetId esAlphabet = manager.addLanguage("es").mainAlphabet;

        final ConceptId studentConcept = manager.getNextAvailableConceptId();
        final AcceptationId studentAcceptation = addSimpleAcceptation(manager, esAlphabet, studentConcept, "alumno");

        final ConceptId concept1 = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, concept1, "palabras masculinas");
        final BunchId bunch1 = conceptAsBunchId(concept1);
        manager.addAcceptationInBunch(bunch1, studentAcceptation);

        final ConceptId concept2 = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, concept2, "palabras femeninas");

        final ConceptId femenineConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, femenineConcept, "femenino");

        final ConceptId pluralConcept = manager.getNextAvailableConceptId();
        addSimpleAcceptation(manager, esAlphabet, pluralConcept, "plural");

        final BunchId bunch2 = conceptAsBunchId(concept2);
        final RuleId femenineRule = conceptAsRuleId(femenineConcept);
        addSingleAlphabetAgent(manager, setOf(bunch2), setOf(bunch1), setOf(), esAlphabet, null, null, "o", "a", femenineRule);

        final RuleId pluralRule = conceptAsRuleId(pluralConcept);
        addSingleAlphabetAgent(manager, setOf(), setOf(bunch1, bunch2), setOf(), esAlphabet, null, null, null, "s", pluralRule);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId outStudentAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumno"));
        final ConceptId outStudentConcept = outManager.conceptFromAcceptation(outStudentAcceptation);

        final AcceptationId outFemaleStudentAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumna"));
        final ConceptId outFemaleStudentConcept = outManager.conceptFromAcceptation(outFemaleStudentAcceptation);

        final AcceptationId outStudentsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumnos"));
        final ConceptId outStudentsConcept = outManager.conceptFromAcceptation(outStudentsAcceptation);

        final AcceptationId outFemaleStudentsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "alumnas"));
        final ConceptId outFemaleStudentsConcept = outManager.conceptFromAcceptation(outFemaleStudentsAcceptation);

        final AcceptationId outFemenineRuleAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "femenino"));
        final ConceptId outFemenineConcept = outManager.conceptFromAcceptation(outFemenineRuleAcceptation);

        final AcceptationId outPluralRuleAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "plural"));
        final ConceptId outPluralConcept = outManager.conceptFromAcceptation(outPluralRuleAcceptation);

        final RuleId outFemenineRule = conceptAsRuleId(outFemenineConcept);
        assertSinglePair(outFemaleStudentConcept, outStudentConcept, outManager.findRuledConceptsByRule(outFemenineRule));

        final RuleId outPluralRule = conceptAsRuleId(outPluralConcept);
        final ImmutableMap<ConceptId, ConceptId> pluralProcessedConcepts = outManager.findRuledConceptsByRule(outPluralRule);
        assertSize(2, pluralProcessedConcepts);
        assertEquals(outStudentConcept, pluralProcessedConcepts.get(outStudentsConcept));
        assertEquals(outFemaleStudentConcept, pluralProcessedConcepts.get(outFemaleStudentsConcept));

        final LanguageId outLanguage = outManager.findLanguageByCode("es");
        final AlphabetId outAlphabet = getSingleValue(outManager.findAlphabetsByLanguage(outLanguage));

        final ImmutableSet<MorphologyResult<AcceptationId, RuleId>> morphologies = outManager.readMorphologiesFromAcceptation(outStudentAcceptation, outAlphabet).morphologies.toSet();
        assertSize(3, morphologies);
        assertContainsOnly(outFemenineRule, morphologies.findFirst(result -> result.dynamicAcceptation.equals(outFemaleStudentAcceptation), null).rules);
        assertContainsOnly(outPluralRule, morphologies.findFirst(result -> result.dynamicAcceptation.equals(outStudentsAcceptation), null).rules);
        assertContainsOnly(outFemenineRule, outPluralRule, morphologies.findFirst(result -> result.dynamicAcceptation.equals(outFemaleStudentsAcceptation), null).rules);
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationForTaberu1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taberuCorrelation = correlationComposer.compose("食べる", "たべる");
        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taberuCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べたい"));
        final ImmutableCorrelation<AlphabetId> tabeCorrelation = correlationComposer.compose("食べ", "たべ");
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(2, correlationIds);
        assertEquals(tabeCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(taiCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(1)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationsForTaberu2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> beruCorrelation = correlationComposer.compose("べる", "べる");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べたい"));
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationsForTaberu3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べたい"));
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(3, correlationIds);
        assertEquals(taCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(taiCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(2)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationForTaberu1AndSugiru3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taberuCorrelation = correlationComposer.compose("食べる", "たべる");
        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taberuCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("過", "す");
        final ImmutableCorrelation<AlphabetId> giCorrelation = correlationComposer.compose("ぎ", "ぎ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelationArray<AlphabetId> sugiruCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(giCorrelation)
                .append(ruCorrelation)
                .build();

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, sugiruCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId eatTooMuchAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べ過ぎる"));
        final ImmutableCorrelation<AlphabetId> tabeCorrelation = correlationComposer.compose("食べ", "たべ");
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(eatTooMuchAcceptation);
        assertSize(4, correlationIds);
        assertEquals(tabeCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(suCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(giCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(2)));
        assertEquals(ruCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(3)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationsForTaberu2AndSugiru3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> beruCorrelation = correlationComposer.compose("べる", "べる");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("過", "す");
        final ImmutableCorrelation<AlphabetId> giCorrelation = correlationComposer.compose("ぎ", "ぎ");
        final ImmutableCorrelationArray<AlphabetId> sugiruCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(giCorrelation)
                .append(ruCorrelation)
                .build();

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, sugiruCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べ過ぎる"));
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(5, correlationIds);
        assertEquals(taCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(suCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(2)));
        assertEquals(giCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(3)));
        assertEquals(ruCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(4)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationsForTaberu3AndSugiru3() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("過", "す");
        final ImmutableCorrelation<AlphabetId> giCorrelation = correlationComposer.compose("ぎ", "ぎ");
        final ImmutableCorrelationArray<AlphabetId> sugiruCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(giCorrelation)
                .append(ruCorrelation)
                .build();

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, sugiruCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "食べ過ぎる"));
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(5, correlationIds);
        assertEquals(taCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
        assertEquals(beCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(1)));
        assertEquals(suCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(2)));
        assertEquals(giCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(3)));
        assertEquals(ruCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(4)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationWithMultipleCorrelationsForSuru1() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");
        final ImmutableCorrelationArray<AlphabetId> shitaiCorrelationArray = composeSingleElementArray(shitaiCorrelation);

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, suruCorrelation, shitaiCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "したい"));
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testInflationCreatesRuledAcceptationsWithMultipleCorrelationsForSuru2() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getNextAvailableId(manager);
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final ImmutableCorrelation<AlphabetId> suCorrelation = correlationComposer.compose("為", "す");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(suCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        final AcceptationId eatAcceptation = manager.addAcceptation(eatConcept, eatCorrelationArray);

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        manager.addAcceptationInBunch(sourceBunch, eatAcceptation);

        final ImmutableCorrelation<AlphabetId> suruCorrelation = correlationComposer.compose("為る", "する");
        final ImmutableCorrelation<AlphabetId> shitaiCorrelation = correlationComposer.compose("したい", "したい");
        final ImmutableCorrelationArray<AlphabetId> shitaiCorrelationArray = composeSingleElementArray(shitaiCorrelation);

        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");
        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, suruCorrelation, shitaiCorrelationArray, desireRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AgentsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> outManager = createOutChecker(outDb);

        final AcceptationId wannaEatAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "したい"));
        final ImmutableList<CorrelationId> correlationIds = outManager.getAcceptationCorrelationArray(wannaEatAcceptation);
        assertSize(1, correlationIds);
        assertEquals(shitaiCorrelation, outManager.getCorrelationWithText(correlationIds.valueAt(0)));
    }

    @Test
    default void testAgentsInflationIncludesRuledAcceptationsWithValidConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("し", "shi")
                .put("た", "ta")
                .put("て", "te")
                .put("ひ", "hi")
                .put("よ", "yo")
                .put("な", "na")
                .build();

        final AlphabetId roumaji = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        manager.addAlphabetAsConversionTarget(conversion);

        final ConceptId highConcept = manager.getNextAvailableConceptId();
        final AcceptationId highAcceptation = addSimpleAcceptation(manager, kana, highConcept, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        manager.addAcceptationInBunch(sourceBunch, highAcceptation);

        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        final AcceptationId notHighAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "たかくない"));
        assertContainsOnly(notHighAcceptation, findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "takakunai"));
    }

    @Test
    default void testAgentsInflationDoesNotIncludeRuledAcceptationsWithInvalidConversion() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> manager = createInManager(db);

        final AlphabetId kana = manager.addLanguage("ja").mainAlphabet;
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("い", "i")
                .put("う", "u")
                .put("か", "ka")
                .put("き", "ki")
                .put("け", "ke")
                .put("し", "shi")
                .put("た", "ta")
                .put("て", "te")
                .put("ひ", "hi")
                .put("よ", "yo")
                .build();

        final AlphabetId roumaji = getNextAvailableId(manager);
        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        manager.addAlphabetAsConversionTarget(conversion);

        final ConceptId highConcept = manager.getNextAvailableConceptId();
        final AcceptationId highAcceptation = addSimpleAcceptation(manager, kana, highConcept, "たかい");

        final BunchId sourceBunch = obtainNewBunch(manager, kana, "いけいようし");
        manager.addAcceptationInBunch(sourceBunch, highAcceptation);

        final RuleId negativeRule = obtainNewRule(manager, kana, "ひていてき");
        addSingleAlphabetAgent(manager, setOf(), setOf(sourceBunch), setOf(), kana, null, null, "い", "くない", negativeRule);

        final MemoryDatabase outDb = cloneBySerializing(db);
        assertEmpty(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "たかくない"));
        assertEmpty(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "takakunai"));
    }
}
