package sword.langbook3.android;

import org.junit.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.IntResultFunction;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.MemoryDatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static sword.langbook3.android.LangbookDatabase.addAcceptation;
import static sword.langbook3.android.LangbookDatabase.addAcceptationInBunch;
import static sword.langbook3.android.LangbookDatabase.addAgent;
import static sword.langbook3.android.LangbookDatabase.deleteAgent;
import static sword.langbook3.android.LangbookDatabase.insertCorrelation;
import static sword.langbook3.android.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.selectSingleRow;

public final class LangbookDatabaseTest {

    @Test
    public void testAddSpanishAcceptation() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int concept = alphabet + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String text = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals(text, stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals(text, stringRow.get(3).toText());
    }

    @Test
    public void testAddJapaneseAcceptationWithoutConversion() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int kanji = language + 1;
        final int kana = kanji + 1;
        final int concept = kana + 1;

        LangbookDbInserter.insertLanguage(db, language, "ja", kanji);
        LangbookDbInserter.insertAlphabet(db, kanji, language);
        LangbookDbInserter.insertAlphabet(db, kana, language);

        final ImmutableList<ImmutableIntKeyMap<String>> correlations = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        final IntResultFunction<ImmutableIntKeyMap<String>> mapFunc = corr -> insertCorrelation(db, corr);
        final ImmutableIntList correlationIds = correlations.map(mapFunc);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationIds);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery kanjiQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kanji)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row kanjiRow = selectSingleRow(db, kanjiQuery);
        assertEquals(acceptation, kanjiRow.get(0).toInt());
        assertEquals("注文", kanjiRow.get(1).toText());
        assertEquals("注文", kanjiRow.get(2).toText());

        final DbQuery kanaQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kana)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row kanaRow = selectSingleRow(db, kanaQuery);
        assertEquals(acceptation, kanaRow.get(0).toInt());
        assertEquals("注文", kanaRow.get(1).toText());
        assertEquals("ちゅうもん", kanaRow.get(2).toText());
    }

    private static void insertConversion(Database db, int sourceAlphabet, int targetAlphabet,
            String sourceText, String targetText) {
        LangbookDbInserter.insertConversion(db, sourceAlphabet, targetAlphabet,
                obtainSymbolArray(db, sourceText), obtainSymbolArray(db, targetText));
    }

    @Test
    public void testAddJapaneseAcceptationWithConversion() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int kanji = language + 1;
        final int kana = kanji + 1;
        final int roumaji = kana + 1;
        final int concept = roumaji + 1;

        LangbookDbInserter.insertLanguage(db, language, "ja", kanji);
        LangbookDbInserter.insertAlphabet(db, kanji, language);
        LangbookDbInserter.insertAlphabet(db, kana, language);

        insertConversion(db, kana, roumaji, "あ", "a");
        insertConversion(db, kana, roumaji, "も", "mo");
        insertConversion(db, kana, roumaji, "ん", "n");
        insertConversion(db, kana, roumaji, "う", "u");
        insertConversion(db, kana, roumaji, "ちゅ", "chu");
        insertConversion(db, kana, roumaji, "ち", "chi");

        final ImmutableList<ImmutableIntKeyMap<String>> correlations = new ImmutableList.Builder<ImmutableIntKeyMap<String>>()
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "注")
                        .put(kana, "ちゅう")
                        .build())
                .add(new ImmutableIntKeyMap.Builder<String>()
                        .put(kanji, "文")
                        .put(kana, "もん")
                        .build())
                .build();

        final IntResultFunction<ImmutableIntKeyMap<String>> mapFunc = corr -> insertCorrelation(db, corr);
        final ImmutableIntList correlationIds = correlations.map(mapFunc);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationIds);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery kanjiQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kanji)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row kanjiRow = selectSingleRow(db, kanjiQuery);
        assertEquals(acceptation, kanjiRow.get(0).toInt());
        assertEquals("注文", kanjiRow.get(1).toText());
        assertEquals("注文", kanjiRow.get(2).toText());

        final DbQuery kanaQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kana)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row kanaRow = selectSingleRow(db, kanaQuery);
        assertEquals(acceptation, kanaRow.get(0).toInt());
        assertEquals("注文", kanaRow.get(1).toText());
        assertEquals("ちゅうもん", kanaRow.get(2).toText());

        final DbQuery roumajiQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), roumaji)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row roumajiRow = selectSingleRow(db, roumajiQuery);
        assertEquals(acceptation, roumajiRow.get(0).toInt());
        assertEquals("注文", roumajiRow.get(1).toText());
        assertEquals("chuumon", roumajiRow.get(2).toText());
    }

    @Test
    public void testAddAgentApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int gerund = alphabet + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String verbText = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbText)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, concept, correlationArrayId);
        LangbookDbInserter.insertBunchAcceptation(db, verbConcept, acceptation, 0);

        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();
        final int agentId = addAgent(db, 0, sourceBunches, diffBunches, matcher, adder, gerund, 0);

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery ruledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), gerund)
                .where(ruledConcepts.getConceptColumnIndex(), concept)
                .select(ruledConcepts.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, ruledConceptQuery).get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), agentId)
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex());
        final int ruledAcceptation = selectSingleRow(db, ruledAcceptationsQuery).get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), ruledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(ruledConcept, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantando", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantando", stringRow.get(3).toText());
    }

    @Test
    public void testAddAgentComposingBunch() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int arVerbConcept = alphabet + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int verbConcept = erVerbConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String singText = "cantar";
        final ImmutableIntKeyMap<String> singCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, singText)
                .build();
        final int singCorrelationId = insertCorrelation(db, singCorrelation);
        final int singCorrelationArrayId = LangbookDatabase.insertCorrelationArray(db, singCorrelationId);
        final int singAcceptation = addAcceptation(db, singConcept, singCorrelationArrayId);
        LangbookDbInserter.insertBunchAcceptation(db, verbConcept, singAcceptation, 0);

        final String coughtText = "toser";
        final ImmutableIntKeyMap<String> coughtCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, coughtText)
                .build();
        final int coughtCorrelationId = insertCorrelation(db, coughtCorrelation);
        final int coughtCorrelationArrayId = LangbookDatabase.insertCorrelationArray(db, coughtCorrelationId);
        final int coughtAcceptation = addAcceptation(db, coughtConcept, coughtCorrelationArrayId);
        LangbookDbInserter.insertBunchAcceptation(db, verbConcept, coughtAcceptation, 0);

        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();
        final int agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, arMatcher, arMatcher, 0, 0);

        final LangbookDbSchema.AgentSetsTable agentSets = LangbookDbSchema.Tables.agentSets;
        final DbQuery agentSetQuery = new DbQuery.Builder(agentSets)
                .where(agentSets.getAgentColumnIndex(), agentId)
                .select(agentSets.getSetIdColumnIndex());
        final int agentSetId = selectSingleRow(db, agentSetQuery).get(0).toInt();

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery verbBunchAccQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), verbConcept)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult bunchAccResult = db.select(verbBunchAccQuery)) {
            for (int i = 0; i < 2; i++) {
                builder.add(bunchAccResult.next().get(0).toInt());
            }
            assertFalse(bunchAccResult.hasNext());
        }
        assertEquals(new ImmutableIntSetBuilder().add(singAcceptation).add(coughtAcceptation).build(), builder.build());

        final DbQuery arVerbBunchAccQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), arVerbConcept)
                .where(bunchAcceptations.getAgentSetColumnIndex(), agentSetId)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertEquals(singAcceptation, selectSingleRow(db, arVerbBunchAccQuery).get(0).toInt());
    }

    private void checkAdd2ChainedAgents(boolean reversedAdditionOrder) {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int gerund = alphabet + 1;
        final int verbConcept = gerund + 1;
        final int arVerbConcept = verbConcept + 1;
        final int singConcept = arVerbConcept + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String verbText = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbText)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        final int acceptation = addAcceptation(db, singConcept, correlationArrayId);
        LangbookDbInserter.insertBunchAcceptation(db, verbConcept, acceptation, 0);

        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet arVerbBunchSet = new ImmutableIntSetBuilder().add(arVerbConcept).build();
        final ImmutableIntSet verbBunchSet = new ImmutableIntSetBuilder().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();

        final int agent2Id;
        if (reversedAdditionOrder) {
            agent2Id = addAgent(db, 0, arVerbBunchSet, diffBunches, matcher, adder, gerund, 0);
            addAgent(db, arVerbConcept, verbBunchSet, diffBunches, matcher, matcher, 0, 0);
        }
        else {
            addAgent(db, arVerbConcept, verbBunchSet, diffBunches, matcher, matcher, 0, 0);
            agent2Id = addAgent(db, 0, arVerbBunchSet, diffBunches, matcher, adder, gerund, 0);
        }

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery ruledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), gerund)
                .where(ruledConcepts.getConceptColumnIndex(), singConcept)
                .select(ruledConcepts.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, ruledConceptQuery).get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), agent2Id)
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex());
        final int ruledAcceptation = selectSingleRow(db, ruledAcceptationsQuery).get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), ruledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(ruledConcept, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        final DbResult.Row stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantando", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantando", stringRow.get(3).toText());
    }

    @Test
    public void testAdd2ChainedAgents() {
        checkAdd2ChainedAgents(false);
    }

    @Test
    public void testAdd2ChainedAgentsReversedAdditionOrder() {
        checkAdd2ChainedAgents(true);
    }

    private void checkAddAgentWithDiffBunch(boolean addAgentBeforeAcceptations) {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int arVerbConcept = alphabet + 1;
        final int arEndingNounConcept = arVerbConcept + 1;
        final int singConcept = arEndingNounConcept + 1;
        final int palateConcept = singConcept + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String singText = "cantar";
        final ImmutableIntKeyMap<String> singCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, singText)
                .build();
        final int singCorrelationId = insertCorrelation(db, singCorrelation);
        final int singCorrelationArrayId = LangbookDatabase.insertCorrelationArray(db, singCorrelationId);

        final String palateText = "paladar";
        final ImmutableIntKeyMap<String> palateCorrelation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, palateText)
                .build();
        final int palateCorrelationId = insertCorrelation(db, palateCorrelation);
        final int palateCorrelationArrayId = LangbookDatabase.insertCorrelationArray(db, palateCorrelationId);

        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().add(arEndingNounConcept).build();

        final int singAcceptation = addAcceptation(db, singConcept, singCorrelationArrayId);
        final int palateAcceptation;
        final int agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, arMatcher, arMatcher, 0, 0);
            palateAcceptation = addAcceptation(db, palateConcept, palateCorrelationArrayId);
            addAcceptationInBunch(db, arEndingNounConcept, palateAcceptation);
        }
        else {
            palateAcceptation = addAcceptation(db, palateConcept, palateCorrelationArrayId);
            addAcceptationInBunch(db, arEndingNounConcept, palateAcceptation);
            agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, arMatcher, arMatcher, 0, 0);
        }

        final LangbookDbSchema.AgentSetsTable agentSets = LangbookDbSchema.Tables.agentSets;
        final DbQuery agentSetQuery = new DbQuery.Builder(agentSets)
                .where(agentSets.getAgentColumnIndex(), agentId)
                .select(agentSets.getSetIdColumnIndex());
        final int agentSetId = selectSingleRow(db, agentSetQuery).get(0).toInt();

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery arVerbsQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), arVerbConcept)
                .select(bunchAcceptations.getAcceptationColumnIndex(), bunchAcceptations.getAgentSetColumnIndex());
        final DbResult.Row row = selectSingleRow(db, arVerbsQuery);
        assertEquals(singAcceptation, row.get(0).toInt());
        assertEquals(agentSetId, row.get(1).toInt());
    }

    @Test
    public void testAddAcceptationBeforeAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(false);
    }

    @Test
    public void testAddAcceptationAfterAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(true);
    }

    private static final class Add3ChainedAgentsResult {
        final int agent1Id;
        final int agent2Id;
        final int agent3Id;

        Add3ChainedAgentsResult(int agent1Id, int agent2Id, int agent3Id) {
            this.agent1Id = agent1Id;
            this.agent2Id = agent2Id;
            this.agent3Id = agent3Id;
        }
    }

    private static int addSpanishSingAcceptation(Database db, int alphabet, int concept) {
        final String verbText = "cantar";
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbText)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        return addAcceptation(db, concept, correlationArrayId);
    }

    private static Add3ChainedAgentsResult add3ChainedAgents(Database db,
            int alphabet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "o")
                .build();
        final ImmutableIntKeyMap<String> noMatcher = new ImmutableIntKeyMap.Builder<String>()
                .build();
        final ImmutableIntKeyMap<String> pluralAdder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "s")
                .build();

        final ImmutableIntSet arVerbBunchSet = new ImmutableIntSetBuilder().add(arVerbConcept).build();
        final ImmutableIntSet actionConceptBunchSet = new ImmutableIntSetBuilder().add(actionConcept).build();
        final ImmutableIntSet noBunches = new ImmutableIntSetBuilder().build();

        final int agent3Id = addAgent(db, 0, actionConceptBunchSet, noBunches, noMatcher, pluralAdder, pluralRule, 0);
        final int agent2Id = addAgent(db, actionConcept, arVerbBunchSet, noBunches, matcher, adder, nominalizationRule, 0);
        final int agent1Id = addAgent(db, arVerbConcept, noBunches, noBunches, matcher, matcher, 0, 0);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    @Test
    public void testAdd3ChainedAgents() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int arVerbConcept = alphabet + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(db, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery nounRuledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), nominalizationRule)
                .select(ruledConcepts.getIdColumnIndex(), ruledConcepts.getConceptColumnIndex());
        final DbResult.Row nounRuledConceptResult = selectSingleRow(db, nounRuledConceptQuery);
        assertEquals(singConcept, nounRuledConceptResult.get(1).toInt());
        final int nounRuledConcept = nounRuledConceptResult.get(0).toInt();

        final DbQuery pluralRuledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), pluralRule)
                .select(ruledConcepts.getIdColumnIndex(), ruledConcepts.getConceptColumnIndex());
        final DbResult.Row pluralRuledConceptResult = selectSingleRow(db, pluralRuledConceptQuery);
        assertEquals(nounRuledConcept, pluralRuledConceptResult.get(1).toInt());
        final int pluralRuledConcept = pluralRuledConceptResult.get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery nounRuledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), addAgentsResult.agent2Id)
                .select(ruledAcceptations.getIdColumnIndex(), ruledAcceptations.getAcceptationColumnIndex());
        final DbResult.Row nounRuledAcceptationResult = selectSingleRow(db, nounRuledAcceptationsQuery);
        assertEquals(acceptation, nounRuledAcceptationResult.get(1).toInt());
        final int nounRuledAcceptation = nounRuledAcceptationResult.get(0).toInt();

        final DbQuery pluralRuledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), addAgentsResult.agent3Id)
                .select(ruledAcceptations.getIdColumnIndex(), ruledAcceptations.getAcceptationColumnIndex());
        final DbResult.Row pluralRuledAcceptationResult = selectSingleRow(db, pluralRuledAcceptationsQuery);
        assertEquals(nounRuledAcceptation, pluralRuledAcceptationResult.get(1).toInt());
        final int pluralRuledAcceptation = pluralRuledAcceptationResult.get(0).toInt();

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery nounAcceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), nounRuledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(nounRuledConcept, selectSingleRow(db, nounAcceptationQuery).get(0).toInt());

        final DbQuery pluralAcceptationQuery = new DbQuery.Builder(acceptations)
                .where(acceptations.getIdColumnIndex(), pluralRuledAcceptation)
                .select(acceptations.getConceptColumnIndex());
        assertEquals(pluralRuledConcept, selectSingleRow(db, pluralAcceptationQuery).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        DbQuery stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), nounRuledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        DbResult.Row stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("canto", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("canto", stringRow.get(3).toText());

        stringQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), pluralRuledAcceptation)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex());
        stringRow = selectSingleRow(db, stringQuery);
        assertEquals(acceptation, stringRow.get(0).toInt());
        assertEquals("cantos", stringRow.get(1).toText());
        assertEquals(alphabet, stringRow.get(2).toInt());
        assertEquals("cantos", stringRow.get(3).toText());
    }

    @Test
    public void testDeleteChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int arVerbConcept = alphabet + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(db, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        deleteAgent(db, addAgentsResult.agent1Id);
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .select(ruledAcceptations.getIdColumnIndex());
        assertFalse(db.select(ruledAcceptationsQuery).hasNext());

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptation, selectSingleRow(db, acceptationQuery).get(0).toInt());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery bunchAcceptationQuery = new DbQuery.Builder(bunchAcceptations)
                .select(bunchAcceptations.getIdColumnIndex());
        assertFalse(db.select(bunchAcceptationQuery).hasNext());
    }
}
