package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableMap;
import sword.collections.Set;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.AgentsManager2Test.composeSingleElementArray;
import static sword.langbook3.android.db.AgentsManager2Test.setOf;

interface RuledSentencesManager2Test<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, CharacterId, CharacterCompositionTypeId extends CharacterCompositionTypeIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId extends RuleIdInterface<ConceptId>, AgentId extends AgentIdInterface, SentenceId extends SentenceIdInterface> extends AgentsManager2Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, SentencesManager2Test<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> {

    @Override
    RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> createManager(MemoryDatabase db);

    static <ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface, SentenceId extends SentenceIdInterface> AcceptationId addTaberuAcceptation(
            RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager, DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer) {
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("食", "た");
        final ImmutableCorrelation<AlphabetId> beCorrelation = correlationComposer.compose("べ", "べ");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");

        final ImmutableCorrelationArray<AlphabetId> eatCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(beCorrelation)
                .append(ruCorrelation)
                .build();

        final ConceptId eatConcept = manager.getNextAvailableConceptId();
        return manager.addAcceptation(eatConcept, eatCorrelationArray);
    }

    static <ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface, SentenceId extends SentenceIdInterface> AgentId addDesireAgent(
            RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager, DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer, BunchId sourceBunch, RuleId desireRule) {
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        return manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);
    }

    static void assertNoRuleSentenceFoundForSentence(DbExporter.Database db, SentenceIdInterface sentence) {
        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());
    }

    static void assertJustOneRuleSentenceFoundForSentence(DbExporter.Database db, SentenceIdInterface sentence) {
        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());
    }

    static <ConceptId> void assertSingleRuleSentenceMatchFoundForSentence(DbExporter.Database db, RuleIdInterface<ConceptId> expectedRule, SentenceIdInterface sentence) {
        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());

        try (DbResult dbResult = db.select(query)) {
            assertTrue(dbResult.hasNext());
            assertTrue(expectedRule.sameValue(dbResult.next().get(0)));
            assertFalse(dbResult.hasNext());
        }
    }

    @Test
    default void testAddSentenceContainingOneRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);

        final ImmutableMap<SentenceId, String> sampleSentences = manager.getSampleSentencesApplyingRule(desireRule);
        assertSinglePair(sentence, "ケーキを食べたい", sampleSentences);
    }

    @Test
    default void testAddSentenceContainingTwoRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .add(new SentenceSpan<>(new ImmutableIntRange(13, 16), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたいが、ピザも食べたい", sentenceSpans);

        final ImmutableMap<SentenceId, String> sampleSentences = manager.getSampleSentencesApplyingRule(desireRule);
        assertSinglePair(sentence, "ケーキを食べたいが、ピザも食べたい", sampleSentences);
        assertJustOneRuleSentenceFoundForSentence(db, sentence);
    }

    @Test
    default void testRemoveSentenceContainingOneRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);

        assertTrue(manager.removeSentence(sentence));
        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
    }

    @Test
    default void testUpdateSentenceAddingRuledAcceptationSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", ImmutableHashSet.empty());

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        assertTrue(manager.updateSentenceTextAndSpans(sentence, "ケーキを食べたい", sentenceSpans));

        final ImmutableMap<SentenceId, String> sampleSentences = manager.getSampleSentencesApplyingRule(desireRule);
        assertSinglePair(sentence, "ケーキを食べたい", sampleSentences);
    }

    @Test
    default void testUpdateSentenceAddingTwoRuledAcceptationSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたいが、ピザも食べたい", ImmutableHashSet.empty());

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .add(new SentenceSpan<>(new ImmutableIntRange(13, 16), wannaEatAcceptation))
                .build();

        assertTrue(manager.updateSentenceTextAndSpans(sentence, "ケーキを食べたいが、ピザも食べたい", sentenceSpans));

        final ImmutableMap<SentenceId, String> sampleSentences = manager.getSampleSentencesApplyingRule(desireRule);
        assertSinglePair(sentence, "ケーキを食べたいが、ピザも食べたい", sampleSentences);
        assertJustOneRuleSentenceFoundForSentence(db, sentence);
    }

    @Test
    default void testUpdateSentenceRemovingRuledAcceptationSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);

        assertTrue(manager.updateSentenceTextAndSpans(sentence, "ケーキを食べたい", ImmutableHashSet.empty()));
        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
    }

    @Test
    default void testUpdateSentenceRemovingOneRuledAcceptationSpanButLeavingOtherSharingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .add(new SentenceSpan<>(new ImmutableIntRange(13, 16), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたいが、ピザも食べたい", sentenceSpans);

        final Set<SentenceSpan<AcceptationId>> newSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        assertTrue(manager.updateSentenceTextAndSpans(sentence, "ケーキを食べたいが、ピザも食べたい", newSpans));
        final ImmutableMap<SentenceId, String> sampleSentences = manager.getSampleSentencesApplyingRule(desireRule);
        assertSinglePair(sentence, "ケーキを食べたいが、ピザも食べたい", sampleSentences);
        assertJustOneRuleSentenceFoundForSentence(db, sentence);
    }

    @Test
    default void testRemoveAgentWhenThereIsOneSentenceContainingOneRuledAcceptationFromThatAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);
        manager.removeAgent(agent);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsTwoSentencesContainingTwoDifferentRuledAcceptationsForTheSameRuleButDifferentAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId ruAgent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final BunchId uSourceBunch = obtainNewBunch(manager, enAlphabet, "my u words");
        final ImmutableCorrelation<AlphabetId> uCorrelation = correlationComposer.compose("う", "う");
        final ImmutableCorrelation<AlphabetId> itaiCorrelation = correlationComposer.compose("いたい", "いたい");
        final ImmutableCorrelationArray<AlphabetId> itaiCorrelationArray = composeSingleElementArray(itaiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final AgentId uAgent = manager.addAgent(setOf(), setOf(uSourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, uCorrelation, itaiCorrelationArray, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(ruAgent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> kaCorrelation = correlationComposer.compose("買", "か");
        final ImmutableCorrelationArray<AlphabetId> buyCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(kaCorrelation)
                .append(uCorrelation)
                .build();

        final ConceptId buyConcept = manager.getNextAvailableConceptId();
        final AcceptationId buyAcceptation = manager.addAcceptation(buyConcept, buyCorrelationArray);

        assertTrue(manager.addAcceptationInBunch(uSourceBunch, buyAcceptation));
        final AcceptationId wannaBuyAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(uAgent, buyAcceptation);
        assertNotNull(wannaBuyAcceptation);

        final Set<SentenceSpan<AcceptationId>> eatSentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final Set<SentenceSpan<AcceptationId>> buySentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(2, 5), wannaBuyAcceptation))
                .build();

        final ConceptId eatSentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId eatSentence = manager.addSentence(eatSentenceConcept, "ケーキを食べたい", eatSentenceSpans);

        final ConceptId buySentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId buySentence = manager.addSentence(buySentenceConcept, "本は買いたい", buySentenceSpans);
        manager.removeAgent(ruAgent);

        assertSinglePair(buySentence, "本は買いたい", manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, eatSentence);
        assertEmpty(manager.getSentenceSpans(eatSentence));
        assertJustOneRuleSentenceFoundForSentence(db, buySentence);
        assertSize(1, manager.getSentenceSpans(buySentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsOneSentenceContainingTwoRuledAcceptationFromTwoDifferentAgentsApplyingTheSameRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId ruSourceBunch = obtainNewBunch(manager, enAlphabet, "my ru words");
        final AgentId ruAgent = addDesireAgent(manager, correlationComposer, ruSourceBunch, desireRule);

        final BunchId muSourceBunch = obtainNewBunch(manager, enAlphabet, "my mu words");

        final ImmutableCorrelation<AlphabetId> muCorrelation = correlationComposer.compose("む", "む");
        final ImmutableCorrelation<AlphabetId> mitaiCorrelation = correlationComposer.compose("みたい", "みたい");
        final ImmutableCorrelationArray<AlphabetId> mitaiCorrelationArray = composeSingleElementArray(mitaiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final AgentId muAgent = manager.addAgent(setOf(), setOf(muSourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, muCorrelation, mitaiCorrelationArray, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(ruSourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(ruAgent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> noCorrelation = correlationComposer.compose("飲", "の");

        final ImmutableCorrelationArray<AlphabetId> drinkCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(noCorrelation)
                .append(muCorrelation)
                .build();

        final ConceptId drinkConcept = manager.getNextAvailableConceptId();
        final AcceptationId drinkAcceptation = manager.addAcceptation(drinkConcept, drinkCorrelationArray);
        assertTrue(manager.addAcceptationInBunch(muSourceBunch, drinkAcceptation));
        final AcceptationId wannaDrinkAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(muAgent, drinkAcceptation);
        assertNotNull(wannaDrinkAcceptation);

        final SentenceSpan<AcceptationId> wannaDrinkSpan = new SentenceSpan<>(new ImmutableIntRange(12, 15), wannaDrinkAcceptation);
        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .add(wannaDrinkSpan)
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキが食べたい。お酒が飲みたい", sentenceSpans);
        manager.removeAgent(ruAgent);

        assertSinglePair(sentence, "ケーキが食べたい。お酒が飲みたい", manager.getSampleSentencesApplyingRule(desireRule));
        assertJustOneRuleSentenceFoundForSentence(db, sentence);
        assertContainsOnly(wannaDrinkSpan, manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsOneSentenceContainingOneRuledAcceptationFromAChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("た", "た");
        final ImmutableCorrelation<AlphabetId> iCorrelation = correlationComposer.compose("い", "い");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(iCorrelation)
                .build();

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId middleBunch = obtainNewBunch(manager, enAlphabet, "words for desire");
        final AgentId desireAgent = manager.addAgent(setOf(middleBunch), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final ImmutableCorrelation<AlphabetId> kuCorrelation = correlationComposer.compose("く", "く");
        final ImmutableCorrelation<AlphabetId> naCorrelation = correlationComposer.compose("な", "な");
        final ImmutableCorrelationArray<AlphabetId> kunaiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(kuCorrelation)
                .append(naCorrelation)
                .append(iCorrelation)
                .build();

        final RuleId negativeRule = obtainNewRule(manager, enAlphabet, "negative");
        final AgentId negativeAgent = manager.addAgent(setOf(), setOf(middleBunch), setOf(), emptyCorrelation, emptyCorrelationArray, iCorrelation, kunaiCorrelationArray, negativeRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(desireAgent, eatAcceptation);
        final AcceptationId noWannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(negativeAgent, wannaEatAcceptation);
        assertNotNull(noWannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 9), noWannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたくない", sentenceSpans);
        manager.removeAgent(desireAgent);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertEmpty(manager.getSampleSentencesApplyingRule(negativeRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsTwoSentencesAndOneIsContainingOneRuledAcceptationFromAChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taCorrelation = correlationComposer.compose("た", "た");
        final ImmutableCorrelation<AlphabetId> iCorrelation = correlationComposer.compose("い", "い");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(taCorrelation)
                .append(iCorrelation)
                .build();

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId middleBunch = obtainNewBunch(manager, enAlphabet, "words for desire");
        final AgentId desireAgent = manager.addAgent(setOf(middleBunch), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final ImmutableCorrelation<AlphabetId> kuCorrelation = correlationComposer.compose("く", "く");
        final ImmutableCorrelation<AlphabetId> naCorrelation = correlationComposer.compose("な", "な");
        final ImmutableCorrelationArray<AlphabetId> kunaiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(kuCorrelation)
                .append(naCorrelation)
                .append(iCorrelation)
                .build();

        final RuleId negativeRule = obtainNewRule(manager, enAlphabet, "negative");
        final AgentId negativeAgent = manager.addAgent(setOf(), setOf(middleBunch), setOf(), emptyCorrelation, emptyCorrelationArray, iCorrelation, kunaiCorrelationArray, negativeRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(desireAgent, eatAcceptation);
        final AcceptationId noWannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(negativeAgent, wannaEatAcceptation);
        assertNotNull(noWannaEatAcceptation);

        final ImmutableCorrelation<AlphabetId> akaCorrelation = correlationComposer.compose("赤", "あか");

        final ImmutableCorrelationArray<AlphabetId> akaiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(akaCorrelation)
                .append(iCorrelation)
                .build();

        final ConceptId redConcept = manager.getNextAvailableConceptId();
        final AcceptationId redAcceptation = manager.addAcceptation(redConcept, akaiCorrelationArray);
        assertTrue(manager.addAcceptationInBunch(middleBunch, redAcceptation));
        final AcceptationId noRedAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(negativeAgent, redAcceptation);
        assertNotNull(noRedAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentence1Spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 9), noWannaEatAcceptation))
                .build();

        final ConceptId sentence1Concept = manager.getNextAvailableConceptId();
        final SentenceId sentence1 = manager.addSentence(sentence1Concept, "ケーキを食べたくない", sentence1Spans);

        final Set<SentenceSpan<AcceptationId>> sentence2Spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(2, 5), noRedAcceptation))
                .build();

        final ConceptId sentence2Concept = manager.getNextAvailableConceptId();
        final SentenceId sentence2 = manager.addSentence(sentence2Concept, "車は赤くない", sentence2Spans);

        manager.removeAgent(desireAgent);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertSinglePair(sentence2, "車は赤くない", manager.getSampleSentencesApplyingRule(negativeRule));
        assertNoRuleSentenceFoundForSentence(db, sentence1);
        assertJustOneRuleSentenceFoundForSentence(db, sentence2);
    }

    @Test
    default void testRemoveAcceptationWhenAnAgentIsTransformingItAndTheResultIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);
        manager.removeAcceptation(eatAcceptation);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAcceptationFromBunchWhenAnAgentIsTransformingAcceptationsOfThatBunchAndTheResultIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);
        manager.removeAcceptationFromBunch(sourceBunch, eatAcceptation);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testUpdateAcceptationCorrelationArrayWhenAnAgentIsTransformingItAndTheResultIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final AgentId agent = addDesireAgent(manager, correlationComposer, sourceBunch, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation))
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);
        final ImmutableCorrelationArray<AlphabetId> newCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose("投", "な"))
                .append(correlationComposer.compose("げ", "げ"))
                .append(correlationComposer.compose("る", "る"))
                .build();
        manager.updateAcceptationCorrelationArray(eatAcceptation, newCorrelationArray);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertNoRuleSentenceFoundForSentence(db, sentence);
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testUpdateAgentRuleWhenThereIsOneSentenceContainingOneRuledAcceptationFromThatAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final SentenceSpan<AcceptationId> sentenceSpan = new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation);
        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(sentenceSpan)
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);

        final RuleId desireRule2 = obtainNewRule(manager, enAlphabet, "desire2");
        manager.updateAgent(agent, setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule2);

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertSinglePair(sentence, "ケーキを食べたい", manager.getSampleSentencesApplyingRule(desireRule2));
        assertSingleRuleSentenceMatchFoundForSentence(db, desireRule2, sentence);
        assertContainsOnly(sentenceSpan, manager.getSentenceSpans(sentence));
    }

    @Test
    default void testUpdateAgentRemovingSourceBunchContainingAnAcceptationWhenThereIsOneSentenceContainingItsRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final BunchId emptyBunch = obtainNewBunch(manager, enAlphabet, "no words");
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch, emptyBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final SentenceSpan<AcceptationId> sentenceSpan = new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation);
        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(sentenceSpan)
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);

        assertTrue(manager.updateAgent(agent, setOf(), setOf(emptyBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule));

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testUpdateAgentChangingSourceBunchContainingAnAcceptationForAnEmptyOneWhenThereIsOneSentenceContainingTheRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId desireRule = obtainNewRule(manager, enAlphabet, "desire");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "my words");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final AgentId agent = manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId wannaEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent, eatAcceptation);
        assertNotNull(wannaEatAcceptation);

        final SentenceSpan<AcceptationId> sentenceSpan = new SentenceSpan<>(new ImmutableIntRange(4, 7), wannaEatAcceptation);
        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(sentenceSpan)
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "ケーキを食べたい", sentenceSpans);

        final BunchId emptyBunch = obtainNewBunch(manager, enAlphabet, "no words");
        assertTrue(manager.updateAgent(agent, setOf(), setOf(emptyBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule));

        assertEmpty(manager.getSampleSentencesApplyingRule(desireRule));
        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testUpdateAgentChangingRuleForTheFirstAgentInAChainWhereTheFinalResultingAcceptationIsIncludedInASentenceSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId negativeRule = obtainNewRule(manager, enAlphabet, "negative");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final BunchId intermediateBunch = obtainNewBunch(manager, enAlphabet, "intermediate");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> naCorrelation = correlationComposer.compose("な", "な");
        final ImmutableCorrelation<AlphabetId> iCorrelation = correlationComposer.compose("い", "い");
        final ImmutableCorrelationArray<AlphabetId> naiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(naCorrelation)
                .append(iCorrelation)
                .build();

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final AgentId agent1 = manager.addAgent(setOf(intermediateBunch), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, naiCorrelationArray, negativeRule);

        final ImmutableCorrelation<AlphabetId> kattaCorrelation = correlationComposer.compose("かった", "かった");
        final ImmutableCorrelationArray<AlphabetId> kattaCorrelationArray = composeSingleElementArray(kattaCorrelation);

        final RuleId pastRule = obtainNewRule(manager, enAlphabet, "past");
        final AgentId agent2 = manager.addAgent(setOf(), setOf(intermediateBunch), setOf(), emptyCorrelation, emptyCorrelationArray, iCorrelation, kattaCorrelationArray, pastRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId noEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, eatAcceptation);
        final AcceptationId noAteAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, noEatAcceptation);

        final SentenceSpan<AcceptationId> sentenceSpan = new SentenceSpan<>(new ImmutableIntRange(2, 7), noAteAcceptation);
        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(sentenceSpan)
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "何も食べなかった", sentenceSpans);

        final RuleId noRule = obtainNewRule(manager, enAlphabet, "no!");
        assertTrue(manager.updateAgent(agent1, setOf(intermediateBunch), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, naiCorrelationArray, noRule));

        assertEmpty(manager.getSampleSentencesApplyingRule(negativeRule));
        assertSinglePair(sentence, "何も食べなかった", manager.getSampleSentencesApplyingRule(pastRule));
        assertSinglePair(sentence, "何も食べなかった", manager.getSampleSentencesApplyingRule(noRule));
    }

    @Test
    default void testUpdateAgentChangingRuleForTheFirstAgentInAChainWhereTheFinalResultingAcceptationIsIncludedInASentenceSpanAndTheRuleIsStillUsedByAnotherAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

        final AlphabetId enAlphabet = manager.addLanguage("en").mainAlphabet;
        final AlphabetId kanji = manager.addLanguage("ja").mainAlphabet;
        final AlphabetId kana = getAlphabetIdManager().getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final RuleId negativeRule = obtainNewRule(manager, enAlphabet, "negative");

        final BunchId sourceBunch = obtainNewBunch(manager, enAlphabet, "source");
        final BunchId intermediateBunch = obtainNewBunch(manager, enAlphabet, "intermediate");
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> naCorrelation = correlationComposer.compose("な", "な");
        final ImmutableCorrelation<AlphabetId> iCorrelation = correlationComposer.compose("い", "い");
        final ImmutableCorrelationArray<AlphabetId> naiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(naCorrelation)
                .append(iCorrelation)
                .build();

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        final AgentId agent1 = manager.addAgent(setOf(intermediateBunch), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, naiCorrelationArray, negativeRule);

        final ImmutableCorrelation<AlphabetId> kattaCorrelation = correlationComposer.compose("かった", "かった");
        final ImmutableCorrelationArray<AlphabetId> kattaCorrelationArray = composeSingleElementArray(kattaCorrelation);

        final RuleId pastRule = obtainNewRule(manager, enAlphabet, "past");
        final AgentId agent2 = manager.addAgent(setOf(), setOf(intermediateBunch), setOf(), emptyCorrelation, emptyCorrelationArray, iCorrelation, kattaCorrelationArray, pastRule);

        final ImmutableCorrelation<AlphabetId> kuCorrelation = correlationComposer.compose("く", "く");
        final ImmutableCorrelationArray<AlphabetId> kunaiCorrelationArray = new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(kuCorrelation)
                .append(naCorrelation)
                .append(iCorrelation)
                .build();

        final BunchId adjectivesBunch = obtainNewBunch(manager, enAlphabet, "adjectives");
        final AgentId agent3 = manager.addAgent(setOf(), setOf(adjectivesBunch), setOf(), emptyCorrelation, emptyCorrelationArray, iCorrelation, kunaiCorrelationArray, negativeRule);

        final AcceptationId eatAcceptation = addTaberuAcceptation(manager, correlationComposer);
        assertTrue(manager.addAcceptationInBunch(sourceBunch, eatAcceptation));
        final AcceptationId noEatAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, eatAcceptation);
        final AcceptationId noAteAcceptation = manager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, noEatAcceptation);

        final SentenceSpan<AcceptationId> sentenceSpan = new SentenceSpan<>(new ImmutableIntRange(2, 7), noAteAcceptation);
        final Set<SentenceSpan<AcceptationId>> sentenceSpans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationId>>()
                .add(sentenceSpan)
                .build();

        final ConceptId sentenceConcept = manager.getNextAvailableConceptId();
        final SentenceId sentence = manager.addSentence(sentenceConcept, "何も食べなかった", sentenceSpans);

        final RuleId noRule = obtainNewRule(manager, enAlphabet, "no!");
        assertTrue(manager.updateAgent(agent1, setOf(intermediateBunch), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, naiCorrelationArray, noRule));

        assertEmpty(manager.getSampleSentencesApplyingRule(negativeRule));
        assertSinglePair(sentence, "何も食べなかった", manager.getSampleSentencesApplyingRule(pastRule));
        assertSinglePair(sentence, "何も食べなかった", manager.getSampleSentencesApplyingRule(noRule));
    }
}
