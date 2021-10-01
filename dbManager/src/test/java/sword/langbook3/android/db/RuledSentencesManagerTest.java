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
}
