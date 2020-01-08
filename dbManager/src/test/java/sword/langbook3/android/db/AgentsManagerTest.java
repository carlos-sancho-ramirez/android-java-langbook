package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.List;
import sword.database.Database;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.DbValue;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.MorphologyResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sword.langbook3.android.db.AcceptationsManagerTest.addSimpleAcceptation;
import static sword.langbook3.android.db.AcceptationsManagerTest.updateAcceptationSimpleCorrelationArray;
import static sword.langbook3.android.db.BunchesManagerTest.addSpanishSingAcceptation;
import static sword.langbook3.android.db.IntKeyMapTestUtils.assertSinglePair;
import static sword.langbook3.android.db.IntSetTestUtils.assertEqualSet;
import static sword.langbook3.android.db.IntSetTestUtils.intSetOf;
import static sword.langbook3.android.db.IntTraversableTestUtils.assertContainsOnly;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationTexts;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationsInBunchByBunchAndAgent;
import static sword.langbook3.android.db.LangbookReadableDatabase.readMorphologiesFromAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.selectSingleRow;
import static sword.langbook3.android.db.SizableTestUtils.assertEmpty;
import static sword.langbook3.android.db.TraversableTestUtils.getSingleValue;

/**
 * Include all test related to all responsibilities of a AgentsManager.
 *
 * AgentsManager responsibilities include all responsibilities from BunchesManager, and include the following ones:
 * <li>Bunch sets</li>
 * <li>Rules</li>
 * <li>Ruled concepts</li>
 * <li>Ruled acceptations</li>
 * <li>Agents</li>
 */
interface AgentsManagerTest extends BunchesManagerTest {

    @Override
    AgentsManager createManager(MemoryDatabase db);

    static ImmutableIntSet findBunchesWhereAcceptationIsIncluded(Database db, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .select(table.getBunchColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(Database db, int bunch, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getAgentColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static int findDynamicAcceptation(Database db, int baseAcceptation, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .where(table.getAcceptationColumnIndex(), baseAcceptation)
                .select(table.getIdColumnIndex());
        return selectSingleRow(db, query).get(0).toInt();
    }

    static Integer addSingleAlphabetAgent(AgentsManager manager, int targetBunch, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, int alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
        final ImmutableIntKeyMap<String> startMatcher = (startMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startMatcherText).build();

        final ImmutableIntKeyMap<String> startAdder = (startAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startAdderText).build();

        final ImmutableIntKeyMap<String> endMatcher = (endMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endMatcherText).build();

        final ImmutableIntKeyMap<String> endAdder = (endAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endAdderText).build();

        return manager.addAgent(targetBunch, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static boolean updateSingleAlphabetAgent(AgentsManager manager, int agentId, int targetBunch, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, int alphabet, String startMatcherText, String startAdderText, String endMatcherText,
            String endAdderText, int rule) {
        final ImmutableIntKeyMap<String> startMatcher = (startMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startMatcherText).build();

        final ImmutableIntKeyMap<String> startAdder = (startAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, startAdderText).build();

        final ImmutableIntKeyMap<String> endMatcher = (endMatcherText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endMatcherText).build();

        final ImmutableIntKeyMap<String> endAdder = (endAdderText == null)? ImmutableIntKeyMap.empty() :
                new ImmutableIntKeyMap.Builder<String>().put(alphabet, endAdderText).build();

        return manager.updateAgent(agentId, targetBunch, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    static void assertOnlyOneMorphology(DbExporter.Database db, int staticAcceptation, int preferredAlphabet, String expectedText, int expectedRule) {
        final MorphologyResult morphology = getSingleValue(readMorphologiesFromAcceptation(db, staticAcceptation, preferredAlphabet).morphologies);
        assertEquals(expectedText, morphology.text);
        assertContainsOnly(expectedRule, morphology.rules);
    }

    @Test
    default void testAddAgentApplyingRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int concept = verbConcept + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, concept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, acceptation));

        final int agentId = addSingleAlphabetAgent(manager, 0, intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ando", gerund);

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
    default void testAddAgentComposingBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int erVerbConcept = arVerbConcept + 1;
        final int verbConcept = erVerbConcept + 1;
        final int singConcept = erVerbConcept + 1;
        final int coughtConcept = singConcept + 1;

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcceptation));

        final int coughtAcceptation = addSimpleAcceptation(manager, alphabet, coughtConcept, "toser");
        assertTrue(manager.addAcceptationInBunch(verbConcept, coughtAcceptation));

        final int agentId = addSingleAlphabetAgent(manager, arVerbConcept, intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(singAcceptation, coughtAcceptation, manager.getAcceptationsInBunch(verbConcept));
        assertContainsOnly(singAcceptation, getAcceptationsInBunchByBunchAndAgent(db, arVerbConcept, agentId));
    }

    default void checkAdd2ChainedAgents(boolean reversedAdditionOrder) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbConcept = gerund + 1;
        final int arVerbConcept = verbConcept + 1;
        final int singConcept = arVerbConcept + 1;

        final int acceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        assertTrue(manager.addAcceptationInBunch(verbConcept, acceptation));

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ar")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ando")
                .build();

        final ImmutableIntSet arVerbBunchSet = intSetOf(arVerbConcept);
        final ImmutableIntSet verbBunchSet = intSetOf(verbConcept);
        final ImmutableIntSet diffBunches = intSetOf();

        final int agent2Id;
        if (reversedAdditionOrder) {
            agent2Id = manager.addAgent(0, arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
            manager.addAgent(arVerbConcept, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
        }
        else {
            manager.addAgent(arVerbConcept, verbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, matcher, 0);
            agent2Id = manager.addAgent(0, arVerbBunchSet, diffBunches, nullCorrelation, nullCorrelation, matcher, adder, gerund);
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
    default void testAdd2ChainedAgents() {
        checkAdd2ChainedAgents(false);
    }

    @Test
    default void testAdd2ChainedAgentsReversedAdditionOrder() {
        checkAdd2ChainedAgents(true);
    }

    default void checkAdd2ChainedAgentsFirstWithoutSource(boolean reversedAdditionOrder, boolean acceptationBeforeAgents) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int bunchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, bunchConcept, "pluralizable sustituyendo ón por ones");

        int songConcept = 0;
        int acceptation = 0;
        if (acceptationBeforeAgents) {
            songConcept = manager.getMaxConcept() + 1;
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        final ImmutableIntKeyMap<String> nullCorrelation = new ImmutableIntKeyMap.Builder<String>().build();
        final ImmutableIntKeyMap<String> matcher = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ón")
                .build();
        final ImmutableIntKeyMap<String> adder = new ImmutableIntKeyMap.Builder<String>()
                .put(alphabet, "ones")
                .build();

        final ImmutableIntSet middleBunchSet = intSetOf(bunchConcept);
        final ImmutableIntSet noBunchSet = intSetOf();

        final int pluralConcept;
        if (reversedAdditionOrder) {
            pluralConcept = manager.getMaxConcept() + 1;
            assertNotNull(manager.addAgent(0, middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralConcept));
            assertNotNull(manager.addAgent(bunchConcept, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, 0));
        }
        else {
            assertNotNull(manager.addAgent(bunchConcept, noBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, matcher, 0));
            pluralConcept = manager.getMaxConcept() + 1;
            assertNotNull(manager.addAgent(0, middleBunchSet, noBunchSet, nullCorrelation, nullCorrelation, matcher, adder, pluralConcept));
        }

        if (!acceptationBeforeAgents) {
            songConcept = manager.getMaxConcept() + 1;
            acceptation = addSimpleAcceptation(manager, alphabet, songConcept, "canción");
        }

        final MorphologyResult morphology = getSingleValue(readMorphologiesFromAcceptation(db, acceptation, alphabet).morphologies);
        assertEquals("canciones", morphology.text);
        assertContainsOnly(pluralConcept, morphology.rules);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceBeforeMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(false, false);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceReversedAdditionOrderBeforeMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(true, false);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceAfterMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(false, true);
    }

    @Test
    default void testAdd2ChainedAgentsFirstWithoutSourceReversedAdditionOrderAfterMatchingAcceptation() {
        checkAdd2ChainedAgentsFirstWithoutSource(true, true);
    }

    default void checkAddAgentWithDiffBunch(boolean addAgentBeforeAcceptations) {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int arEndingNounConcept = arVerbConcept + 1;
        final int singConcept = arEndingNounConcept + 1;
        final int palateConcept = singConcept + 1;

        final ImmutableIntSet sourceBunches = intSetOf();
        final ImmutableIntSet diffBunches = intSetOf(arEndingNounConcept);

        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");
        final int palateAcceptation;
        final int agentId;
        if (addAgentBeforeAcceptations) {
            agentId = addSingleAlphabetAgent(manager, arVerbConcept, sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", 0);
            palateAcceptation = addSimpleAcceptation(manager, alphabet, palateConcept, "paladar");
            manager.addAcceptationInBunch(arEndingNounConcept, palateAcceptation);
        }
        else {
            palateAcceptation = addSimpleAcceptation(manager, alphabet, palateConcept, "paladar");
            manager.addAcceptationInBunch(arEndingNounConcept, palateAcceptation);
            agentId = addSingleAlphabetAgent(manager, arVerbConcept, sourceBunches, diffBunches, alphabet, null, null, "ar", "ar", 0);
        }

        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery arVerbsQuery = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), arVerbConcept)
                .select(bunchAcceptations.getAcceptationColumnIndex(), bunchAcceptations.getAgentColumnIndex());
        final List<DbValue> row = selectSingleRow(db, arVerbsQuery);
        assertEquals(singAcceptation, row.get(0).toInt());
        assertEquals(agentId, row.get(1).toInt());
    }

    @Test
    default void testAddAcceptationBeforeAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(false);
    }

    @Test
    default void testAddAcceptationAfterAgentWithDiffBunch() {
        checkAddAgentWithDiffBunch(true);
    }

    class Add3ChainedAgentsResult {
        final int agent1Id;
        final int agent2Id;
        final int agent3Id;

        Add3ChainedAgentsResult(int agent1Id, int agent2Id, int agent3Id) {
            this.agent1Id = agent1Id;
            this.agent2Id = agent2Id;
            this.agent3Id = agent3Id;
        }
    }

    static Add3ChainedAgentsResult add3ChainedAgents(
            AgentsManager manager,
            int alphabet, ImmutableIntSet sourceBunchSet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = intSetOf();
        final int agent3Id = addSingleAlphabetAgent(manager, 0, intSetOf(actionConcept), noBunches, alphabet, null, null, null, "s", pluralRule);
        final int agent2Id = addSingleAlphabetAgent(manager, actionConcept, intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "o", nominalizationRule);
        final int agent1Id = addSingleAlphabetAgent(manager, arVerbConcept, sourceBunchSet, noBunches, alphabet, null, null, "ar", "ar", 0);

        return new Add3ChainedAgentsResult(agent1Id, agent2Id, agent3Id);
    }

    static Add3ChainedAgentsResult add3ChainedAgents(AgentsManager manager,
            int alphabet, int arVerbConcept, int actionConcept,
            int nominalizationRule, int pluralRule) {

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        return add3ChainedAgents(manager, alphabet, noBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);
    }

    @Test
    default void testAdd3ChainedAgents() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet,
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
    default void testRemoveChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        final Add3ChainedAgentsResult addAgentsResult = add3ChainedAgents(manager, alphabet,
                arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        manager.removeAgent(addAgentsResult.agent1Id);
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
    default void testRemoveAcceptationWithChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int arVerbConcept = manager.getMaxConcept() + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        add3ChainedAgents(manager, alphabet, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        manager.removeAcceptation(acceptation);
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
    default void testRemoveAcceptationWithBunchChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int arVerbConcept = verbConcept + 1;
        final int actionConcept = arVerbConcept + 1;
        final int nominalizationRule = actionConcept + 1;
        final int pluralRule = nominalizationRule + 1;
        final int singConcept = pluralRule + 1;

        final int acceptation = addSpanishSingAcceptation(manager, alphabet, singConcept);
        manager.addAcceptationInBunch(verbConcept, acceptation);

        final ImmutableIntSet sourceBunches = new ImmutableIntSetCreator().add(verbConcept).build();
        add3ChainedAgents(manager, alphabet, sourceBunches, arVerbConcept, actionConcept, nominalizationRule, pluralRule);

        manager.removeAcceptationFromBunch(verbConcept, acceptation);
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
    default void testReadAllMatchingBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int gerund = manager.getMaxConcept() + 1;
        final int verbArConcept = gerund + 1;
        final int verbErConcept = verbArConcept + 1;

        addSimpleAcceptation(manager, alphabet, verbArConcept, "verbo ar");
        addSimpleAcceptation(manager, alphabet, verbErConcept, "verbo er");

        final ImmutableIntSet diffBunches = intSetOf();
        addSingleAlphabetAgent(manager, 0, intSetOf(verbArConcept), diffBunches, alphabet, null, null, "ar", "ando", gerund);

        addSingleAlphabetAgent(manager, 0, intSetOf(verbErConcept), diffBunches, alphabet, null, null, "er", "iendo", gerund);

        ImmutableIntKeyMap<String> texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "jugar").build();
        assertSinglePair(verbArConcept, "verbo ar", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "comer").build();
        assertSinglePair(verbErConcept, "verbo er", manager.readAllMatchingBunches(texts, alphabet));

        texts = new ImmutableIntKeyMap.Builder<String>().put(alphabet, "dormir").build();
        assertEmpty(manager.readAllMatchingBunches(texts, alphabet));
    }

    @Test
    default void testUpdateCorrelationArrayForAcceptationWithRuleAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int concept = manager.getMaxConcept() + 1;
        final int gerundRule = concept + 1;
        final int firstConjugationVerbBunch = gerundRule + 1;

        final int acceptationId = addSimpleAcceptation(manager, alphabet, concept, "contar");
        manager.addAcceptationInBunch(firstConjugationVerbBunch, acceptationId);

        addSingleAlphabetAgent(manager, NO_BUNCH, intSetOf(firstConjugationVerbBunch), intSetOf(), alphabet, null, null, "ar", "ando", gerundRule);

        updateAcceptationSimpleCorrelationArray(manager, alphabet, acceptationId, "cantar");

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

        assertSinglePair(alphabet, "cantando", manager.readCorrelationArrayTexts(rightGerundCorrelationArray).toImmutable());

        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), ruledAcceptation)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getMainAcceptationColumnIndex(),
                        strings.getStringColumnIndex());
        row = selectSingleRow(db, query);
        assertEquals(acceptationId, row.get(0).toInt());
        assertEquals("cantando", row.get(1).toText());
    }

    @Test
    default void testUnabletoRemoveAcceptationsWhenTheyAreUniqueAgentSourceOrTargetBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;
        final int verbConcept = manager.getMaxConcept() + 1;
        final int firstConjugationVerbConcept = verbConcept + 1;
        final int singConcept = firstConjugationVerbConcept + 1;

        final int verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");
        final int firstConjugationVerbAcc = addSimpleAcceptation(manager, alphabet, firstConjugationVerbConcept, "verbo ar");
        final int singAcc = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        assertTrue(manager.addAcceptationInBunch(verbConcept, singAcc));

        assertNotEquals(null, addSingleAlphabetAgent(manager, firstConjugationVerbConcept, intSetOf(verbConcept), intSetOf(), alphabet, null, null, "ar", "ar", 0));

        assertFalse(manager.removeAcceptation(verbAcc));
        assertFalse(manager.removeAcceptation(firstConjugationVerbAcc));

        assertEquals("verbo", LangbookReadableDatabase.getAcceptationTexts(db, verbAcc).get(alphabet));
        assertEquals("verbo ar", LangbookReadableDatabase.getAcceptationTexts(db, firstConjugationVerbAcc).get(alphabet));
    }

    @Test
    default void testMultipleAgentsTargetingSameBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int verbConcept = manager.getMaxConcept() + 1;
        final int verbAcc = addSimpleAcceptation(manager, alphabet, verbConcept, "desconfiar");

        final int myBunch = manager.getMaxConcept() + 1;
        final String myBunchText = "palabaras raras";
        addSimpleAcceptation(manager, alphabet, myBunch, myBunchText);

        final ImmutableIntSet emptyBunchSet = intSetOf();
        final int desAgent = addSingleAlphabetAgent(manager, myBunch, emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, 0);
        final int arAgent = addSingleAlphabetAgent(manager, myBunch, emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(desAgent, arAgent, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, verbAcc));
    }

    @Test
    default void testAcceptationAddedInBunchBeforeAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int bedConcept = manager.getMaxConcept() + 1;
        final int bedAcc = addSimpleAcceptation(manager, alphabet, bedConcept, "cama");

        final int verbConcept1 = manager.getMaxConcept() + 1;
        final int verbAcc1 = addSimpleAcceptation(manager, alphabet, verbConcept1, "confiar");

        final int verbConcept2 = manager.getMaxConcept() + 1;
        final int verbAcc2 = addSimpleAcceptation(manager, alphabet, verbConcept2, "desconfiar");

        final int myBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myBunch, "palabras raras");

        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        final ImmutableIntSet emptyBunchSet = intSetOf();
        final int desAgent = addSingleAlphabetAgent(manager, myBunch, emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, 0);
        final int arAgent = addSingleAlphabetAgent(manager, myBunch, emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", 0);

        assertContainsOnly(0, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, bedAcc));
        assertContainsOnly(0, arAgent, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, verbAcc1));
        assertContainsOnly(0, desAgent, arAgent, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, verbAcc2));
    }

    @Test
    default void testAcceptationAddedInBunchAfterAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);

        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int bedConcept = manager.getMaxConcept() + 1;
        final int bedAcc = addSimpleAcceptation(manager, alphabet, bedConcept, "cama");

        final int verbConcept1 = manager.getMaxConcept() + 1;
        final int verbAcc1 = addSimpleAcceptation(manager, alphabet, verbConcept1, "confiar");

        final int verbConcept2 = manager.getMaxConcept() + 1;
        final int verbAcc2 = addSimpleAcceptation(manager, alphabet, verbConcept2, "desconfiar");

        final int myBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myBunch, "palabras raras");

        final ImmutableIntSet emptyBunchSet = intSetOf();
        final int desAgent = addSingleAlphabetAgent(manager, myBunch, emptyBunchSet, emptyBunchSet, alphabet, "des", "des", null, null, 0);
        final int arAgent = addSingleAlphabetAgent(manager, myBunch, emptyBunchSet, emptyBunchSet, alphabet, null, null, "ar", "ar", 0);

        assertTrue(manager.addAcceptationInBunch(myBunch, bedAcc));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc1));
        assertTrue(manager.addAcceptationInBunch(myBunch, verbAcc2));

        assertContainsOnly(0, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, bedAcc));
        assertContainsOnly(0, arAgent, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, verbAcc1));
        assertContainsOnly(0, desAgent, arAgent, findAllAgentsThatIncludedAcceptationInBunch(db, myBunch, verbAcc2));
    }

    @Test
    default void testUpdateAgentTargetForNoChainedAgentWithoutRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, erVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, arVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(arVerbConcept, findBunchesWhereAcceptationIsIncluded(db, singAcceptation));
    }

    @Test
    default void testUpdateAgentTargetForChainedAgentWithoutRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int erVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, erVerbConcept, "verbo de segunda conjugación");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableIntSet noBunches = intSetOf();
        final int agent1Id = addSingleAlphabetAgent(manager, erVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        final int agent2Id = addSingleAlphabetAgent(manager, 0, intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent1Id, arVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        final int dynamicAcceptation = findDynamicAcceptation(db, singAcceptation, agent2Id);
        assertSinglePair(alphabet, "cantando", getAcceptationTexts(db, dynamicAcceptation));
    }

    @Test
    default void testRemoveAgentTargetFromSecondChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int recentWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, recentWordsConcept, "palabras recientes");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableIntSet noBunches = intSetOf();
        addSingleAlphabetAgent(manager, arVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        final int agent2Id = addSingleAlphabetAgent(manager, recentWordsConcept, intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final int dynamicAcceptation = findDynamicAcceptation(db, singAcceptation, agent2Id);
        assertSinglePair(alphabet, "cantando", getAcceptationTexts(db, dynamicAcceptation));
        assertEmpty(manager.getAcceptationsInBunch(recentWordsConcept));
    }

    @Test
    default void testIncludeAgentTargetToSecondChainedAgent() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final int recentWordsConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, recentWordsConcept, "palabras recientes");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerundio");

        final ImmutableIntSet noBunches = intSetOf();
        addSingleAlphabetAgent(manager, arVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        final int agent2Id = addSingleAlphabetAgent(manager, 0, intSetOf(arVerbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept);
        assertTrue(updateSingleAlphabetAgent(manager, agent2Id, recentWordsConcept, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final int dynamicAcceptation = findDynamicAcceptation(db, singAcceptation, agent2Id);
        assertSinglePair(alphabet, "cantando", getAcceptationTexts(db, dynamicAcceptation));
        assertContainsOnly(dynamicAcceptation, manager.getAcceptationsInBunch(recentWordsConcept));
    }

    @Test
    default void testIncludeAgentSourceBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(singAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testRemoveAgentSourceBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertContainsOnly(singAcceptation, touchAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testChangeAgentSourceBunches() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, intSetOf(chapter2), noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testIncludeExtraSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int passConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, passConcept, "pasar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, intSetOf(chapter1), noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, intSetOf(chapter1, chapter2), noBunches, alphabet, null, null, "ar", "ar", 0));

        final ImmutableIntSet expectedAcceptations = new ImmutableIntSetCreator().add(singAcceptation).add(touchAcceptation).build();
        assertEqualSet(expectedAcceptations, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testRemoveOneSourceBunch() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int passConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, passConcept, "pasar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, intSetOf(chapter1, chapter2), noBunches, alphabet, null, null, "ar", "ar", 0);

        final ImmutableIntSet chapter1Only = new ImmutableIntSetCreator().add(chapter1).build();
        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, chapter1Only, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(singAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testIncludeAgentDiffBunchMatchingSource() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, chapter2Only, intSetOf(chapter1), alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testRemoveAgentDiffBunchMatchingSource() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, singAcceptation));
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, chapter2Only, intSetOf(chapter1), alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0));

        final ImmutableIntSet expectedAcceptations = new ImmutableIntSetCreator().add(singAcceptation).add(touchAcceptation).build();
        assertEqualSet(expectedAcceptations, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testIncludeAgentDiffBunchNoMatchingSource() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, chapter2Only, intSetOf(chapter1), alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testRemoveAgentDiffBunchNoMatchingSource() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int touchConcept = manager.getMaxConcept() + 1;
        final int touchAcceptation = addSimpleAcceptation(manager, alphabet, touchConcept, "tocar");

        final int chapter1 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter1, "vocabulario del capítulo 1");
        assertTrue(manager.addAcceptationInBunch(chapter1, singAcceptation));

        final int chapter2 = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, chapter2, "vocabulario del capítulo 2");
        assertTrue(manager.addAcceptationInBunch(chapter2, touchAcceptation));

        final int allVocabulary = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, allVocabulary, "vocabulario a repasar");

        final ImmutableIntSet noBunches = intSetOf();
        final ImmutableIntSet chapter1Only = intSetOf(chapter1);
        final ImmutableIntSet chapter2Only = intSetOf(chapter2);
        final int agentId = addSingleAlphabetAgent(manager, allVocabulary, chapter2Only, chapter1Only, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, allVocabulary, chapter2Only, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(touchAcceptation, getAcceptationsInBunchByBunchAndAgent(db, allVocabulary, agentId));
    }

    @Test
    default void testChangeAgentEndMatcherAndAdder() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int eatConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, eatConcept, "comer");

        final int arVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, arVerbConcept, "verbo de primera conjugación");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, arVerbConcept, noBunches, noBunches, alphabet, null, null, "er", "er", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, arVerbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));
        assertContainsOnly(singAcceptation, getAcceptationsInBunchByBunchAndAgent(db, arVerbConcept, agentId));
    }

    @Test
    default void testChangeAgentStartMatcherAndAdder() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int trustConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, trustConcept, "confiar");

        final int untrustConcept = manager.getMaxConcept() + 1;
        final int untrustAcceptation = addSimpleAcceptation(manager, alphabet, untrustConcept, "desconfiar");

        final int unVerbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, unVerbConcept, "verbo que comienza por des");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, unVerbConcept, noBunches, noBunches, alphabet, "con", "con", null, null, 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, unVerbConcept, noBunches, noBunches, alphabet, "des", "des", null, null, 0));
        assertContainsOnly(untrustAcceptation, getAcceptationsInBunchByBunchAndAgent(db, unVerbConcept, agentId));
    }

    @Test
    default void testChangeRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pastConcept, "pasado");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", pastConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));
        assertOnlyOneMorphology(db, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testChangeAdder() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, 0, noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));
        assertOnlyOneMorphology(db, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testChangeAdderForMultipleAcceptations() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int cryConcept = manager.getMaxConcept() + 1;
        final int cryAcceptation = addSimpleAcceptation(manager, alphabet, cryConcept, "llorar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = new ImmutableIntSetCreator().build();
        final int agentId = addSingleAlphabetAgent(manager, 0, noBunches, noBunches, alphabet, null, null, "ar", "aba", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertOnlyOneMorphology(db, singAcceptation, alphabet, "cantando", gerundConcept);
        assertOnlyOneMorphology(db, cryAcceptation, alphabet, "llorando", gerundConcept);
    }

    @Test
    default void testChangeAdderAndRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int pastConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, pastConcept, "pasado");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, 0, noBunches, noBunches, alphabet, null, null, "ar", "aba", pastConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertOnlyOneMorphology(db, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testAddAdderAndRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, myTargetBunch, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, myTargetBunch, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        final MorphologyResult morphology = getSingleValue(readMorphologiesFromAcceptation(db, singAcceptation, alphabet).morphologies);
        assertEquals("cantando", morphology.text);
        assertContainsOnly(gerundConcept, morphology.rules);
        assertContainsOnly(morphology.dynamicAcceptation, getAcceptationsInBunchByBunchAndAgent(db, myTargetBunch, agentId));
    }

    @Test
    default void testRemoveAdderAndRule() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int myTargetBunch = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, myTargetBunch, "mi lista");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        final int agentId = addSingleAlphabetAgent(manager, myTargetBunch, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept);

        assertTrue(updateSingleAlphabetAgent(manager, agentId, myTargetBunch, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertEmpty(readMorphologiesFromAcceptation(db, singAcceptation, alphabet).morphologies);
        assertContainsOnly(singAcceptation, getAcceptationsInBunchByBunchAndAgent(db, myTargetBunch, agentId));
    }

    @Test
    default void testUpdateCorrelationArrayMatchingAgentBefore() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(readMorphologiesFromAcceptation(db, singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingAgentAfter() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar (sin instrumentos)");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, 0, noBunches, noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(db, singAcceptation, alphabet, "cantando", gerundConcept);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingChainedAgentBefore() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, verbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertNotNull(addSingleAlphabetAgent(manager, 0, intSetOf(verbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar (sin instrumentos)"));
        assertEmpty(readMorphologiesFromAcceptation(db, singAcceptation, alphabet).morphologies);
    }

    @Test
    default void testUpdateCorrelationArrayMatchingChainedAgentAfter() {
        final MemoryDatabase db = new MemoryDatabase();
        final AgentsManager manager = createManager(db);
        final int alphabet = manager.addLanguage("es").mainAlphabet;

        final int singConcept = manager.getMaxConcept() + 1;
        final int singAcceptation = addSimpleAcceptation(manager, alphabet, singConcept, "cantar (sin instrumentos)");

        final int gerundConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, gerundConcept, "gerund");

        final int verbConcept = manager.getMaxConcept() + 1;
        addSimpleAcceptation(manager, alphabet, verbConcept, "verbo");

        final ImmutableIntSet noBunches = intSetOf();
        assertNotNull(addSingleAlphabetAgent(manager, verbConcept, noBunches, noBunches, alphabet, null, null, "ar", "ar", 0));

        assertNotNull(addSingleAlphabetAgent(manager, 0, intSetOf(verbConcept), noBunches, alphabet, null, null, "ar", "ando", gerundConcept));

        assertTrue(updateAcceptationSimpleCorrelationArray(manager, alphabet, singAcceptation, "cantar"));
        assertOnlyOneMorphology(db, singAcceptation, alphabet, "cantando", gerundConcept);
    }
}
