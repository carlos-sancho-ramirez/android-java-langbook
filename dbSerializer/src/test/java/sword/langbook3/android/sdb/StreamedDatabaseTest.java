package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;
import sword.database.DbExporter;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptsChecker;
import sword.langbook3.android.db.IntSetter;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sword.collections.StringTestUtils.rangeOf;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;
import static sword.langbook3.android.sdb.AgentsSerializerTest.setOf;

final class StreamedDatabaseTest implements AgentsSerializerTest<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder> {

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSimpleAcceptation(AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, ConceptId concept, String text) {
        return BunchesSerializerTest.addSimpleAcceptation(manager, alphabet, concept, text);
    }

    static <ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId> int addSingleAlphabetAgent(
            AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, RuleId rule) {
        return AgentsSerializerTest.addSingleAlphabetAgent(manager, targetBunches, sourceBunches, diffBunches, alphabet, startMatcherText, startAdderText, endMatcherText, endAdderText, rule);
    }

    static <AcceptationId> SentenceSpan<AcceptationId> newSpan(String text, String segment, AcceptationId acceptation) {
        return new SentenceSpan<>(rangeOf(text, segment), acceptation);
    }

    private final AlphabetIdManager alphabetIdManager = new AlphabetIdManager();
    private final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
    private final BunchIdManager bunchIdManager = new BunchIdManager();
    private final RuleIdManager ruleIdManager = new RuleIdManager();

    @Override
    public LangbookManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new ConceptIdManager(), new LanguageIdManager(), alphabetIdManager, new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), acceptationIdManager, bunchIdManager, new BunchSetIdManager(), ruleIdManager);
    }

    @Override
    public RuleIdHolder conceptAsRuleId(ConceptIdHolder conceptId) {
        return ruleIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public BunchIdHolder conceptAsBunchId(ConceptIdHolder conceptId) {
        return bunchIdManager.getKeyFromConceptId(conceptId);
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker<ConceptIdHolder> manager) {
        return alphabetIdManager.getKeyFromConceptId(manager.getNextAvailableConceptId());
    }

    @Override
    public IntSetter<AcceptationIdHolder> getAcceptationIdManager() {
        return acceptationIdManager;
    }

    static <AcceptationId> ImmutableSet<AcceptationId> findAcceptationsMatchingText(DbExporter.Database db, IntSetter<AcceptationId> acceptationIdManager, String text) {
        return AcceptationsSerializerTest.findAcceptationsMatchingText(db, acceptationIdManager, text);
    }

    @Test
    void testSerializeSentenceWithDynamicAcceptationAsSpan() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final LangbookManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder> inManager = createManager(inDb);
        final AlphabetIdHolder alphabet = inManager.addLanguage("es").mainAlphabet;

        final ConceptIdHolder feminableWordsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, feminableWordsConcept, "feminizable");

        final ConceptIdHolder pluralableWordsConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, pluralableWordsConcept, "pluralizable");

        final ConceptIdHolder boyConcept = inManager.getNextAvailableConceptId();
        final AcceptationIdHolder boyAcc = addSimpleAcceptation(inManager, alphabet, boyConcept, "chico");
        final BunchIdHolder feminableWordsBunch = conceptAsBunchId(feminableWordsConcept);
        inManager.addAcceptationInBunch(feminableWordsBunch, boyAcc);

        final ConceptIdHolder femenineConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, femenineConcept, "femenino");

        final ConceptIdHolder pluralConcept = inManager.getNextAvailableConceptId();
        addSimpleAcceptation(inManager, alphabet, pluralConcept, "plural");

        final BunchIdHolder pluralableWordsBunch = conceptAsBunchId(pluralableWordsConcept);
        final RuleIdHolder femenineRule = conceptAsRuleId(femenineConcept);
        final int agent1 = addSingleAlphabetAgent(inManager, setOf(pluralableWordsBunch), setOf(feminableWordsBunch), setOf(), alphabet, null, null, "o", "a", femenineRule);

        final RuleIdHolder pluralRule = conceptAsRuleId(pluralConcept);
        final int agent2 = addSingleAlphabetAgent(inManager, setOf(), setOf(pluralableWordsBunch), setOf(), alphabet, null, null, null, "s", pluralRule);

        final AcceptationIdHolder girlAcc = inManager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, boyAcc);
        final AcceptationIdHolder girlsAcc = inManager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, girlAcc);

        final String sentenceText = "las chicas salieron juntas";
        final ImmutableSet<SentenceSpan<AcceptationIdHolder>> spans = new ImmutableHashSet.Builder<SentenceSpan<AcceptationIdHolder>>()
                .add(newSpan(sentenceText, "chicas", girlsAcc))
                .build();

        final ConceptIdHolder sentenceConcept = inManager.getNextAvailableConceptId();
        inManager.addSentence(sentenceConcept, sentenceText, spans);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final LangbookManager<ConceptIdHolder, LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, BunchSetIdHolder, RuleIdHolder> outManager = createManager(outDb);

        final AcceptationIdHolder outBoyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "chico"));
        final ImmutableIntKeyMap<String> outSentences = outManager.getSampleSentences(outBoyAcceptation);
        assertContainsOnly(sentenceText, outSentences);

        final AcceptationIdHolder outGirlsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "chicas"));
        final SentenceSpan<AcceptationIdHolder> outSpan = getSingleValue(outManager.getSentenceSpans(outSentences.keyAt(0)));
        assertEquals(rangeOf(sentenceText, "chicas"), outSpan.range);
        assertEquals(outGirlsAcceptation, outSpan.acceptation);
    }
}
