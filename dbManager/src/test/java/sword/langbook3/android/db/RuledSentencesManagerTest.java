package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableMap;
import sword.collections.Set;
import sword.database.DbQuery;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.collections.MapTestUtils.assertSinglePair;
import static sword.collections.SizableTestUtils.assertEmpty;
import static sword.collections.SizableTestUtils.assertSize;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.AgentsManagerTest.composeSingleElementArray;
import static sword.langbook3.android.db.AgentsManagerTest.setOf;

interface RuledSentencesManagerTest<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface, SentenceId extends SentenceIdInterface> extends AgentsManagerTest<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId>, SentencesManagerTest<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> {

    @Override
    RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> createManager(MemoryDatabase db);

    static <ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface, SentenceId extends SentenceIdInterface> AcceptationId addTaberuAcceptation(RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager, DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer) {
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

    static <ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId extends AcceptationIdInterface, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId extends AgentIdInterface, SentenceId extends SentenceIdInterface> AgentId addDesireAgent(RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager, DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer, BunchId sourceBunch, RuleId desireRule) {
        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelation<AlphabetId> taiCorrelation = correlationComposer.compose("たい", "たい");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = composeSingleElementArray(taiCorrelation);

        final ImmutableCorrelation<AlphabetId> emptyCorrelation = ImmutableCorrelation.empty();
        final ImmutableCorrelationArray<AlphabetId> emptyCorrelationArray = ImmutableCorrelationArray.empty();
        return manager.addAgent(setOf(), setOf(sourceBunch), setOf(), emptyCorrelation, emptyCorrelationArray, ruCorrelation, taiCorrelationArray, desireRule);
    }

    @Test
    default void testAddSentenceContainingOneRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());
    }

    @Test
    default void testRemoveSentenceContainingOneRuledAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());
    }

    @Test
    default void testUpdateSentenceAddingRuledAcceptationSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());
    }

    @Test
    default void testUpdateSentenceRemovingRuledAcceptationSpan() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());
    }

    @Test
    default void testUpdateSentenceRemovingOneRuledAcceptationSpanButLeavingOtherSharingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());
    }

    @Test
    default void testRemoveAgentWhenThereIsOneSentenceContainingOneRuledAcceptationFromThatAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());

        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsTwoSentencesContainingTwoDifferentRuledAcceptationsForTheSameRuleButDifferentAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), eatSentence)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());
        assertEmpty(manager.getSentenceSpans(eatSentence));

        query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), buySentence)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());
        assertSize(1, manager.getSentenceSpans(buySentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsOneSentenceContainingTwoRuledAcceptationFromTwoDifferentAgentsApplyingTheSameRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());

        assertContainsOnly(wannaDrinkSpan, manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsOneSentenceContainingOneRuledAcceptationFromAChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());

        assertEmpty(manager.getSentenceSpans(sentence));
    }

    @Test
    default void testRemoveAgentWhenThereIsTwoSentencesAndOneIsContainingOneRuledAcceptationFromAChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final RuledSentencesManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> manager = createManager(db);

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

        final LangbookDbSchema.RuleSentenceMatchesTable table = LangbookDbSchema.Tables.ruleSentenceMatches;
        DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence1)
                .select(table.getRuleColumnIndex());
        assertEquals(0, db.select(query).size());

        query = new DbQueryBuilder(table)
                .where(table.getSentenceColumnIndex(), sentence2)
                .select(table.getRuleColumnIndex());
        assertEquals(1, db.select(query).size());
    }
}