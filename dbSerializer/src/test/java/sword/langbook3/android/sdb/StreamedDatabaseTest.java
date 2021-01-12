package sword.langbook3.android.sdb;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableSet;
import sword.database.DbExporter;
import sword.database.MemoryDatabase;
import sword.langbook3.android.db.AcceptationsManager;
import sword.langbook3.android.db.AgentsManager;
import sword.langbook3.android.db.ConceptsChecker;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.SentenceSpan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sword.collections.IntSetTestUtils.intSetOf;
import static sword.collections.IntTraversableTestUtils.getSingleValue;
import static sword.collections.StringTestUtils.rangeOf;
import static sword.collections.TraversableTestUtils.assertContainsOnly;
import static sword.collections.TraversableTestUtils.getSingleValue;
import static sword.langbook3.android.sdb.AcceptationsSerializerTest.cloneBySerializing;

final class StreamedDatabaseTest implements AgentsSerializerTest<LanguageIdHolder, AlphabetIdHolder> {

    static <LanguageId, AlphabetId> int addSimpleAcceptation(AcceptationsManager<LanguageId, AlphabetId> manager, AlphabetId alphabet, int concept, String text) {
        return BunchesSerializerTest.addSimpleAcceptation(manager, alphabet, concept, text);
    }

    static <LanguageId, AlphabetId> int addSingleAlphabetAgent(
            AgentsManager<LanguageId, AlphabetId> manager, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, AlphabetId alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
        return AgentsSerializerTest.addSingleAlphabetAgent(manager, targetBunches, sourceBunches, diffBunches, alphabet, startMatcherText, startAdderText, endMatcherText, endAdderText, rule);
    }

    static SentenceSpan newSpan(String text, String segment, int acceptation) {
        return new SentenceSpan(rangeOf(text, segment), acceptation);
    }

    @Override
    public LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder> createManager(MemoryDatabase db) {
        return new LangbookDatabaseManager<>(db, new LanguageIdManager(), new AlphabetIdManager(), new SymbolArrayIdManager());
    }

    @Override
    public AlphabetIdHolder getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetIdHolder(manager.getMaxConcept() + 1);
    }

    @Override
    public int getLanguageConceptId(LanguageIdHolder language) {
        return language.key;
    }

    static ImmutableIntSet findAcceptationsMatchingText(DbExporter.Database db, String text) {
        return AcceptationsSerializerTest.findAcceptationsMatchingText(db, text);
    }

    @Test
    void testSerializeSentenceWithDynamicAcceptationAsSpan() {
        final MemoryDatabase inDb = new MemoryDatabase();
        final LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder> inManager = createManager(inDb);
        final AlphabetIdHolder alphabet = inManager.addLanguage("es").mainAlphabet;

        final int feminableWordsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, feminableWordsBunch, "feminizable");

        final int pluralableWordsBunch = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, pluralableWordsBunch, "pluralizable");

        final int boyConcept = inManager.getMaxConcept() + 1;
        final int boyAcc = addSimpleAcceptation(inManager, alphabet, boyConcept, "chico");
        inManager.addAcceptationInBunch(feminableWordsBunch, boyAcc);

        final int femenineRule = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, femenineRule, "femenino");

        final int pluralRule = inManager.getMaxConcept() + 1;
        addSimpleAcceptation(inManager, alphabet, femenineRule, "plural");

        final int agent1 = addSingleAlphabetAgent(inManager, intSetOf(pluralableWordsBunch), intSetOf(feminableWordsBunch), intSetOf(), alphabet, null, null, "o", "a", femenineRule);

        final int agent2 = addSingleAlphabetAgent(inManager, intSetOf(), intSetOf(pluralableWordsBunch), intSetOf(), alphabet, null, null, null, "s", pluralRule);

        final int girlAcc = inManager.findRuledAcceptationByAgentAndBaseAcceptation(agent1, boyAcc);
        final int girlsAcc = inManager.findRuledAcceptationByAgentAndBaseAcceptation(agent2, girlAcc);

        final String sentenceText = "las chicas salieron juntas";
        final ImmutableSet<SentenceSpan> spans = new ImmutableHashSet.Builder<SentenceSpan>()
                .add(newSpan(sentenceText, "chicas", girlsAcc))
                .build();

        final int sentenceConcept = inManager.getMaxConcept() + 1;
        inManager.addSentence(sentenceConcept, sentenceText, spans);

        final MemoryDatabase outDb = cloneBySerializing(inDb);
        final LangbookManager<LanguageIdHolder, AlphabetIdHolder, SymbolArrayIdHolder> outManager = createManager(outDb);

        final int outBoyAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "chico"));
        final ImmutableIntKeyMap<String> outSentences = outManager.getSampleSentences(outBoyAcceptation);
        assertContainsOnly(sentenceText, outSentences);

        final int outGirlsAcceptation = getSingleValue(findAcceptationsMatchingText(outDb, "chicas"));
        final SentenceSpan outSpan = getSingleValue(outManager.getSentenceSpans(outSentences.keyAt(0)));
        assertEquals(rangeOf(sentenceText, "chicas"), outSpan.range);
        assertEquals(outGirlsAcceptation, outSpan.acceptation);
    }
}
