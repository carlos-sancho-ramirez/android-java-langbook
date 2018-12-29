package sword.langbook3.android;

import org.junit.Test;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.IntResultFunction;
import sword.collections.List;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookReadableDatabase.QuestionFieldDetails;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbValue;
import sword.langbook3.android.db.MemoryDatabase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static sword.langbook3.android.LangbookDatabase.addAcceptation;
import static sword.langbook3.android.LangbookDatabase.addAcceptationInBunch;
import static sword.langbook3.android.LangbookDatabase.addAgent;
import static sword.langbook3.android.LangbookDatabase.insertCorrelation;
import static sword.langbook3.android.LangbookDatabase.obtainQuiz;
import static sword.langbook3.android.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.LangbookDatabase.removeAcceptation;
import static sword.langbook3.android.LangbookDatabase.removeAcceptationFromBunch;
import static sword.langbook3.android.LangbookDatabase.removeAgent;
import static sword.langbook3.android.LangbookDatabase.updateAcceptationCorrelationArray;
import static sword.langbook3.android.LangbookDbInserter.insertSearchHistoryEntry;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.getSearchHistory;
import static sword.langbook3.android.LangbookReadableDatabase.readAllMatchingBunches;
import static sword.langbook3.android.LangbookReadableDatabase.readCorrelationArrayTexts;
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
        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
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
        final List<DbValue> kanjiRow = selectSingleRow(db, kanjiQuery);
        assertEquals(acceptation, kanjiRow.get(0).toInt());
        assertEquals("注文", kanjiRow.get(1).toText());
        assertEquals("注文", kanjiRow.get(2).toText());

        final DbQuery kanaQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kana)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> kanaRow = selectSingleRow(db, kanaQuery);
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
        final List<DbValue> kanjiRow = selectSingleRow(db, kanjiQuery);
        assertEquals(acceptation, kanjiRow.get(0).toInt());
        assertEquals("注文", kanjiRow.get(1).toText());
        assertEquals("注文", kanjiRow.get(2).toText());

        final DbQuery kanaQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), kana)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> kanaRow = selectSingleRow(db, kanaQuery);
        assertEquals(acceptation, kanaRow.get(0).toInt());
        assertEquals("注文", kanaRow.get(1).toText());
        assertEquals("ちゅうもん", kanaRow.get(2).toText());

        final DbQuery roumajiQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), roumaji)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getMainStringColumnIndex(),
                        strings.getStringColumnIndex());
        final List<DbValue> roumajiRow = selectSingleRow(db, roumajiQuery);
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

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();
        final int agentId = addAgent(db, 0, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);

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
        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
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

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().add(verbConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();
        final int agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arMatcher, 0);

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

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
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
            agent2Id = addAgent(db, 0, arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
            addAgent(db, arVerbConcept, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
        }
        else {
            addAgent(db, arVerbConcept, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
            agent2Id = addAgent(db, 0, arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
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
        final List<DbValue> stringRow = selectSingleRow(db, stringQuery);
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

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().add(arEndingNounConcept).build();

        final int singAcceptation = addAcceptation(db, singConcept, singCorrelationArrayId);
        final int palateAcceptation;
        final int agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arMatcher, 0);
            palateAcceptation = addAcceptation(db, palateConcept, palateCorrelationArrayId);
            addAcceptationInBunch(db, arEndingNounConcept, palateAcceptation);
        }
        else {
            palateAcceptation = addAcceptation(db, palateConcept, palateCorrelationArrayId);
            addAcceptationInBunch(db, arEndingNounConcept, palateAcceptation);
            agentId = addAgent(db, arVerbConcept, sourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arMatcher, 0);
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
        final List<DbValue> row = selectSingleRow(db, arVerbsQuery);
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

    private static int addSimpleCorrelationArray(Database db, int alphabet, String text) {
        final ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, text)
                .build();
        final int correlationId = insertCorrelation(db, correlation);
        return LangbookDatabase.insertCorrelationArray(db, correlationId);
    }

    private static int addSpanishAcceptation(Database db, int alphabet, int concept, String text) {
        return addAcceptation(db, concept, addSimpleCorrelationArray(db, alphabet, text));
    }

    private static int addSpanishSingAcceptation(Database db, int alphabet, int concept) {
        return addSpanishAcceptation(db, alphabet, concept, "cantar");
    }

    private static int addSpanishRunAcceptation(Database db, int alphabet, int concept) {
        return addSpanishAcceptation(db, alphabet, concept, "correr");
    }

    private static int addJapaneseSingAcceptation(Database db, int kanjiAlphabet, int kanaAlphabet, int concept) {
        final ImmutableIntKeyMap<String> correlation1 = new ImmutableIntKeyMap.Builder<String>()
                .put(kanjiAlphabet, "歌")
                .put(kanaAlphabet, "うた")
                .build();
        final ImmutableIntKeyMap<String> correlation2 = new ImmutableIntKeyMap.Builder<String>()
                .put(kanjiAlphabet, "う")
                .put(kanaAlphabet, "う")
                .build();
        final int correlation1Id = insertCorrelation(db, correlation1);
        final int correlation2Id = insertCorrelation(db, correlation2);
        final int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlation1Id, correlation2Id);
        return addAcceptation(db, concept, correlationArrayId);
    }

    private static Add3ChainedAgentsResult add3ChainedAgents(Database db,
            int alphabet, ImmutableIntSet sourceBunchSet, int arVerbConcept, int actionConcept,
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

        final int agent3Id = addAgent(db, 0, actionConceptBunchSet, noBunches, noMatcher, noMatcher, noMatcher, pluralAdder, pluralRule);
        final int agent2Id = addAgent(db, actionConcept, arVerbBunchSet, noBunches, noMatcher, noMatcher, matcher, adder, nominalizationRule);
        final int agent1Id = addAgent(db, arVerbConcept, sourceBunchSet, noBunches, noMatcher, noMatcher, matcher, matcher, 0);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    private static Add3ChainedAgentsResult add3ChainedAgents(Database db,
            int alphabet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = new ImmutableIntSetBuilder().build();
        return add3ChainedAgents(db, alphabet, noBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);
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
        final List<DbValue> nounRuledConceptResult = selectSingleRow(db, nounRuledConceptQuery);
        assertEquals(singConcept, nounRuledConceptResult.get(1).toInt());
        final int nounRuledConcept = nounRuledConceptResult.get(0).toInt();

        final DbQuery pluralRuledConceptQuery = new DbQuery.Builder(ruledConcepts)
                .where(ruledConcepts.getRuleColumnIndex(), pluralRule)
                .select(ruledConcepts.getIdColumnIndex(), ruledConcepts.getConceptColumnIndex());
        final List<DbValue> pluralRuledConceptResult = selectSingleRow(db, pluralRuledConceptQuery);
        assertEquals(nounRuledConcept, pluralRuledConceptResult.get(1).toInt());
        final int pluralRuledConcept = pluralRuledConceptResult.get(0).toInt();

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery nounRuledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), addAgentsResult.agent2Id)
                .select(ruledAcceptations.getIdColumnIndex(), ruledAcceptations.getAcceptationColumnIndex());
        final List<DbValue> nounRuledAcceptationResult = selectSingleRow(db, nounRuledAcceptationsQuery);
        assertEquals(acceptation, nounRuledAcceptationResult.get(1).toInt());
        final int nounRuledAcceptation = nounRuledAcceptationResult.get(0).toInt();

        final DbQuery pluralRuledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .where(ruledAcceptations.getAgentColumnIndex(), addAgentsResult.agent3Id)
                .select(ruledAcceptations.getIdColumnIndex(), ruledAcceptations.getAcceptationColumnIndex());
        final List<DbValue> pluralRuledAcceptationResult = selectSingleRow(db, pluralRuledAcceptationsQuery);
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
        List<DbValue> stringRow = selectSingleRow(db, stringQuery);
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
    public void testRemoveChainedAgent() {
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

        removeAgent(db, addAgentsResult.agent1Id);
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

    @Test
    public void testRemoveAcceptationWithChainedAgent() {
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
        add3ChainedAgents(db, alphabet, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        removeAcceptation(db, acceptation);
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery ruledAcceptationsQuery = new DbQuery.Builder(ruledAcceptations)
                .select(ruledAcceptations.getIdColumnIndex());
        assertFalse(db.select(ruledAcceptationsQuery).hasNext());

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery acceptationQuery = new DbQuery.Builder(acceptations)
                .select(acceptations.getIdColumnIndex());
        assertFalse(db.select(acceptationQuery).hasNext());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery bunchAcceptationQuery = new DbQuery.Builder(bunchAcceptations)
                .select(bunchAcceptations.getIdColumnIndex());
        assertFalse(db.select(bunchAcceptationQuery).hasNext());
    }

    @Test
    public void testRemoveAcceptationWithBunchChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int verbConcept = alphabet + 1;
        final int arVerbConcept = verbConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int acceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        addAcceptationInBunch(db, verbConcept, acceptation);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetBuilder().add(verbConcept).build();
        add3ChainedAgents(db, alphabet, sourceBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        removeAcceptationFromBunch(db, verbConcept, acceptation);
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

    @Test
    public void testAddAcceptationInBunchAndQuiz() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int kanjiAlphabet = alphabet + 1;
        final int kanaAlphabet = kanjiAlphabet + 1;
        final int myVocabularyConcept = kanaAlphabet + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int esAcceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);

        final int jaAcceptation = addJapaneseSingAcceptation(db, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(kanjiAlphabet, 0, QuestionFieldFlags.IS_ANSWER | QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = obtainQuiz(db, myVocabularyConcept, fields);

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        final DbQuery query = new DbQuery.Builder(knowledge)
                .select(knowledge.getAcceptationColumnIndex(), knowledge.getQuizDefinitionColumnIndex());
        final List<DbValue> row = selectSingleRow(db, query);
        assertEquals(esAcceptation, row.get(0).toInt());
        assertEquals(quizId, row.get(1).toInt());
    }

    @Test
    public void testAddQuizAndAcceptationInBunch() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int kanjiAlphabet = alphabet + 1;
        final int kanaAlphabet = kanjiAlphabet + 1;
        final int myVocabularyConcept = kanaAlphabet + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int esAcceptation = addSpanishSingAcceptation(db, alphabet, singConcept);
        final int jaAcceptation = addJapaneseSingAcceptation(db, kanjiAlphabet, kanaAlphabet, singConcept);

        final ImmutableList<QuestionFieldDetails> fields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(kanjiAlphabet, 0, QuestionFieldFlags.IS_ANSWER | QuestionFieldFlags.TYPE_SAME_CONCEPT))
                .build();

        final int quizId = obtainQuiz(db, myVocabularyConcept, fields);
        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        final DbQuery knowledgeQuery = new DbQuery.Builder(knowledge)
                .select(knowledge.getAcceptationColumnIndex(), knowledge.getQuizDefinitionColumnIndex());
        assertFalse(db.select(knowledgeQuery).hasNext());

        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);

        final List<DbValue> row = selectSingleRow(db, knowledgeQuery);
        assertEquals(esAcceptation, row.get(0).toInt());
        assertEquals(quizId, row.get(1).toInt());

        removeAcceptationFromBunch(db, myVocabularyConcept, esAcceptation);
        assertFalse(db.select(knowledgeQuery).hasNext());
    }

    @Test
    public void testCallingTwiceAddAcceptationInBunchDoesNotDuplicate() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int kanjiAlphabet = alphabet + 1;
        final int kanaAlphabet = kanjiAlphabet + 1;
        final int myVocabularyConcept = kanaAlphabet + 1;
        final int arVerbConcept = myVocabularyConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int esAcceptation = addSpanishSingAcceptation(db, alphabet, singConcept);

        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);
        addAcceptationInBunch(db, myVocabularyConcept, esAcceptation);

        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), myVocabularyConcept)
                .select(table.getAcceptationColumnIndex());
        assertEquals(esAcceptation, selectSingleRow(db, query).get(0).toInt());
    }

    @Test
    public void testSearchHistory() {
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
        assertTrue(getSearchHistory(db).isEmpty());

        insertSearchHistoryEntry(db, acceptation);
        final ImmutableList<SearchResult> history = getSearchHistory(db);
        assertEquals(1, history.size());

        final SearchResult expectedEntry = new SearchResult(text, text, SearchResult.Types.ACCEPTATION, acceptation, acceptation);
        assertEquals(expectedEntry, history.get(0));

        removeAcceptation(db, acceptation);

        final LangbookDbSchema.SearchHistoryTable table = LangbookDbSchema.Tables.searchHistory;
        assertFalse(db.select(new DbQuery.Builder(table).select(table.getIdColumnIndex())).hasNext());
    }

    @Test
    public void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int gerund = alphabet + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final String verbArText = "verbo de primera conjugación";
        ImmutableIntKeyMap<String> correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbArText)
                .build();
        int correlationId = insertCorrelation(db, correlation);
        int correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        addAcceptation(db, verbArConcept, correlationArrayId);

        final String verbErText = "verbo de segunda conjugación";
        correlation = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, verbErText)
                .build();
        correlationId = insertCorrelation(db, correlation);
        correlationArrayId = LangbookDatabase.insertCorrelationArray(db, correlationId);
        addAcceptation(db, verbErConcept, correlationArrayId);

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> arMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> arAdder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet arSourceBunches = new ImmutableIntSetBuilder().add(verbArConcept).build();
        final ImmutableIntSet diffBunches = new ImmutableIntSetBuilder().build();
        addAgent(db, 0, arSourceBunches, diffBunches, nullCorrelation, nullCorrelation, arMatcher, arAdder, gerund);

        final ImmutableIntKeyMap<String> erMatcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "er")
                .build();
        final ImmutableIntKeyMap<String> erAdder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "iendo")
                .build();

        final ImmutableIntSet erSourceBunches = new ImmutableIntSetBuilder().add(verbErConcept).build();
        addAgent(db, 0, erSourceBunches, diffBunches, nullCorrelation, nullCorrelation, erMatcher, erAdder, gerund);

        ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "jugar").build();
        ImmutableIntKeyMap<String> result = readAllMatchingBunches(db, texts, alphabet);
        assertEquals(1, result.size());
        assertEquals(verbArConcept, result.keyAt(0));
        assertEquals(verbArText, result.valueAt(0));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        result = readAllMatchingBunches(db, texts, alphabet);
        assertEquals(1, result.size());
        assertEquals(verbErConcept, result.keyAt(0));
        assertEquals(verbErText, result.valueAt(0));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "dormir").build();
        result = readAllMatchingBunches(db, texts, alphabet);
        assertEquals(0, result.size());
    }

    @Test
    public void testUpdateAcceptationCorrelationArrayForSame() {
        final MemoryDatabase db1 = new MemoryDatabase();

        final String text = "cantar";
        final int language = getMaxConceptInAcceptations(db1) + 1;
        final int alphabet = language + 1;
        final int concept = alphabet + 1;

        final int correlationArrayId = addSimpleCorrelationArray(db1, alphabet, text);
        final int acceptationId = addAcceptation(db1, concept, correlationArrayId);

        final MemoryDatabase db2 = new MemoryDatabase();
        assertEquals(correlationArrayId, addSimpleCorrelationArray(db2, alphabet, text));
        assertEquals(acceptationId, addAcceptation(db2, concept, correlationArrayId).intValue());
        assertEquals(db1, db2);

        updateAcceptationCorrelationArray(db2, acceptationId, correlationArrayId);
        assertEquals(db1, db2);
    }

    private void assertNoAcceptationWithCorrelationArray(Database db, int correlationArrayId) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId)
                .select(acceptations.getIdColumnIndex());
        assertFalse(db.select(query).hasNext());
    }

    private final ImmutableHashMap<String, String> upperCaseConversion = new ImmutableHashMap.Builder<String, String>()
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

    private void addUpperCaseConversion(Database db, int sourceAlphabet, int destAlphabet) {
        for (sword.collections.Map.Entry<String, String> entry : upperCaseConversion.entries()) {
            insertConversion(db, sourceAlphabet, destAlphabet, entry.key(), entry.value());
        }
    }

    @Test
    public void testUpdateAcceptationCorrelationArray() {
        final MemoryDatabase db = new MemoryDatabase();

        final String text1 = "cantar";
        final String text2 = "beber";
        final String text2UpperCase = "BEBER";

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int upperCaseAlphabet = alphabet + 1;
        final int concept = upperCaseAlphabet + 1;
        final int secondConjugationVerbBunch = concept + 1;

        addUpperCaseConversion(db, alphabet, upperCaseAlphabet);
        final int correlationArrayId1 = addSimpleCorrelationArray(db, alphabet, text1);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId1);

        final ImmutableIntSet noBunches = new ImmutableIntSetBuilder().build();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "er")
                .build();

        addAgent(db, secondConjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(upperCaseAlphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC | QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = obtainQuiz(db, secondConjugationVerbBunch, quizFields);

        final int correlationArrayId2 = addSimpleCorrelationArray(db, alphabet, text2);
        updateAcceptationCorrelationArray(db, acceptationId, correlationArrayId2);

        assertNoAcceptationWithCorrelationArray(db, correlationArrayId1);

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId2)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2, row.get(1).toText());

        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), upperCaseAlphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2UpperCase, row.get(1).toText());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), secondConjugationVerbBunch)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getAcceptationColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());
    }

    @Test
    public void testUpdateAcceptationCorrelationArrayFromMatching() {
        final MemoryDatabase db = new MemoryDatabase();

        final String text1 = "cantar";
        final String text2 = "beber";
        final String text2UpperCase = "BEBER";

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int upperCaseAlphabet = alphabet + 1;
        final int concept = upperCaseAlphabet + 1;
        final int firstConjugationVerbBunch = concept + 1;

        addUpperCaseConversion(db, alphabet, upperCaseAlphabet);
        final int correlationArrayId1 = addSimpleCorrelationArray(db, alphabet, text1);
        final int acceptationId = addAcceptation(db, concept, correlationArrayId1);

        final ImmutableIntSet noBunches = new ImmutableIntSetBuilder().build();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();

        addAgent(db, firstConjugationVerbBunch, noBunches, noBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);

        final ImmutableList<QuestionFieldDetails> quizFields = new ImmutableList.Builder<QuestionFieldDetails>()
                .add(new QuestionFieldDetails(alphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC))
                .add(new QuestionFieldDetails(upperCaseAlphabet, 0, QuestionFieldFlags.TYPE_SAME_ACC | QuestionFieldFlags.IS_ANSWER))
                .build();
        final int quizId = obtainQuiz(db, firstConjugationVerbBunch, quizFields);

        final int correlationArrayId2 = addSimpleCorrelationArray(db, alphabet, text2);
        updateAcceptationCorrelationArray(db, acceptationId, correlationArrayId2);

        assertNoAcceptationWithCorrelationArray(db, correlationArrayId1);

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId2)
                .select(acceptations.getIdColumnIndex());
        assertEquals(acceptationId, selectSingleRow(db, query).get(0).toInt());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2, row.get(1).toText());

        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptationId)
                .where(strings.getStringAlphabetColumnIndex(), upperCaseAlphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(text2UpperCase, row.get(1).toText());

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), firstConjugationVerbBunch)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        assertFalse(db.select(query).hasNext());

        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;
        query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getAcceptationColumnIndex());
        assertFalse(db.select(query).hasNext());
    }

    @Test
    public void testUpdateCorrelationArrayForAcceptationWithRuleAgent() {
        final MemoryDatabase db = new MemoryDatabase();

        final String wrongText = "contar";
        final String rightText = "cantar";
        final String rightGerundText = "cantando";

        final int language = getMaxConceptInAcceptations(db) + 1;
        final int alphabet = language + 1;
        final int concept = alphabet + 1;
        final int gerundRule = concept + 1;
        final int firstConjugationVerbBunch = gerundRule + 1;

        LangbookDbInserter.insertLanguage(db, language, "es", alphabet);
        LangbookDbInserter.insertAlphabet(db, alphabet, language);

        final int wrongCorrelationArrayId = addSimpleCorrelationArray(db, alphabet, wrongText);
        final int acceptationId = addAcceptation(db, concept, wrongCorrelationArrayId);
        addAcceptationInBunch(db, firstConjugationVerbBunch, acceptationId);

        final ImmutableIntSet noBunches = new ImmutableIntSetBuilder().build();
        final ImmutableIntSet firstConjugationVerbBunchSet = new ImmutableIntSetBuilder().add(firstConjugationVerbBunch).build();
        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        addAgent(db, NO_BUNCH, firstConjugationVerbBunchSet, noBunches, nullCorrelation, nullCorrelation, matcher, adder, gerundRule);

        final int rightCorrelationArrayId = addSimpleCorrelationArray(db, alphabet, rightText);
        updateAcceptationCorrelationArray(db, acceptationId, rightCorrelationArrayId);

        final LangbookDbSchema.RuledConceptsTable ruledConceptsTable = LangbookDbSchema.Tables.ruledConcepts;
        DbQuery query = new DbQuery.Builder(ruledConceptsTable)
                .where(ruledConceptsTable.getConceptColumnIndex(), concept)
                .where(ruledConceptsTable.getRuleColumnIndex(), gerundRule)
                .select(ruledConceptsTable.getIdColumnIndex());
        final int ruledConcept = selectSingleRow(db, query).get(0).toInt();
        assertNotEquals(concept, ruledConcept);

        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        query = new DbQuery.Builder(acceptations)
                .where(acceptations.getConceptColumnIndex(), ruledConcept)
                .select(acceptations.getIdColumnIndex(), acceptations.getCorrelationArrayColumnIndex());
        List<DbValue> row = selectSingleRow(db, query);
        final int ruledAcceptation = row.get(0).toInt();
        final int rightGerundCorrelationArray = row.get(1).toInt();

        final ImmutableIntKeyMap<String> rightGerundTexts = readCorrelationArrayTexts(db, rightGerundCorrelationArray).toImmutable();
        assertEquals(1, rightGerundTexts.size());
        assertEquals(alphabet, rightGerundTexts.keyAt(0));
        assertEquals(rightGerundText, rightGerundTexts.valueAt(0));

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals(rightGerundText, row.get(1).toText());
    }
}
