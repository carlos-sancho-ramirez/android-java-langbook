package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;
import sword.database.DbExporter;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsManager;
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

final class StreamedDatabaseTest implements AgentsSerializerTest<LanguageIdHolder, AlphabetIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, RuleIdHolder> {

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId> AcceptationId addSimpleAcceptation(AcceptationsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> manager, AlphabetId alphabet, int concept, String text) {
        return BunchesSerializerTest.addSimpleAcceptation(manager, alphabet, concept, text);
    }

    static <LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> int addSingleAlphabetAgent(
            AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> manager, ImmutableSet<BunchId> targetBunches, ImmutableSet<BunchId> sourceBunches,
            ImmutableSet<BunchId> diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, RuleId rule) {
        return AgentsSerializerTest.addSingleAlphabetAgent(manager, targetBunches, sourceBunches, diffBunches, alphabet, startMatcherText, startAdderText, endMatcherText, endAdderText, rule);
    }

    static <AcceptationId> SentenceSpan<AcceptationId> newSpan(String text, String segment, AcceptationId acceptation) {
        return new SentenceSpan<>(rangeOf(text, segment), acceptation);
    }

    private final AcceptationIdManager acceptationIdManager = new AcceptationIdManager();
    private final RuleIdManager ruleIdManager = new RuleIdManager();

    @Override
    public LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, RuleIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager(), new CorrelationIdManager(), new CorrelationArrayIdManager(), acceptationIdManager, new BunchIdManager(), ruleIdManager);
    }

    @Override
    public RuleIdHolder conceptAsRuleId(int conceptId) {
        return ruleIdManager.getKeyFromInt(conceptId);
    }

    @Override
    public BunchIdHolder conceptAsBunchId(int conceptId) {
        return new BunchIdHolder(conceptId);
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getMaxConcept() + 1);
    }

    @Override
    public int getLanguageConceptId(LanguageIdHolder language) {
        return language.key;
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
        final LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, RuleIdHolder> inManager = createManager(inDb);
        final AlphabetIdHolder alphabet = inManager.addLanguage("es").mainAlphabet;

        final int feminableWordsConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, feminableWordsConcept, "feminizable");

        final int pluralableWordsConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, pluralableWordsConcept, "pluralizable");

        final int boyConcept = inManager.getMaxConcept() + 1;
        final AcceptationIdHolder boyAcc = addSimpleAcceptation(inManager, alphabet, boyConcept, "chico");
        final BunchIdHolder feminableWordsBunch = conceptAsBunchId(feminableWordsConcept);
        inManager.addAcceptationInBunch(feminableWordsBunch, boyAcc);

        final int femenineConcept = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, femenineConcept, "femenino");

        final int pluralConcept = inManager.getMaxConcept() + 1;
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

        final int sentenceConcept = inManager.getMaxConcept() + 1;
        inManager.addSentence(sentenceConcept, sentenceText, spans);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder, CorrelationIdHolder, AcceptationIdHolder, BunchIdHolder, RuleIdHolder> outManager = createManager(outDb);

        final AcceptationIdHolder outBoyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "chico"));
        final ImmutableIntKeyMap<String> outSentences = outManager.getSampleSentences(outBoyAcceptation);
        assertContainsOnly(sentenceText, outSentences);

        final AcceptationIdHolder outGirlsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, getAcceptationIdManager(), "chicas"));
        final SentenceSpan<AcceptationIdHolder> outSpan = getSingleValue(outManager.getSentenceSpans(outSentences.keyAt(0)));
        assertEquals(rangeOf(sentenceText, "chicas"), outSpan.range);
        assertEquals(outGirlsAcceptation, outSpan.acceptation);
    }
}
