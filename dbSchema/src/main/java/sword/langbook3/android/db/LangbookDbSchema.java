package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.database.DbIndex;
import sword.database.DbIntColumn;
import sword.database.DbSchema;
import sword.database.DbSchemaDifference;
import sword.database.DbSchemaUtils;
import sword.database.DbTable;
import sword.database.DbTextColumn;
import sword.database.DbUniqueTextColumn;
import sword.database.MutableSchemaDatabase;
import sword.langbook3.android.collections.TraversableUtils;

public final class LangbookDbSchema {

    private static final int FIRST_VERSION_CODE = 5;

    /**
     * Bunch identifier used within a quiz definition to denote that no bunch should be used.
     * When this identifier is used in a quiz definition, questions are not coming from acceptations within an
     * specific bunch, but it can be any acceptation within the database that matches the field restrictions.
     */
    public static final int NO_BUNCH = 0;

    /**
     * Reserved bunch set identifier that should never be included as set identifier in the BunchSets table.
     * This way, it is guaranteed that this bunch set is always empty, even if the database is not queried.
     */
    public static final int EMPTY_BUNCH_SET_ID = 0;

    /**
     * Used in agents' rule field to indicate that there is no rule assigned.
     */
    public static final int NO_RULE = 0;

    /**
     * Used in Knowledge table to indicate that there is no score because the question has never been asked.
     */
    public static final int NO_SCORE = 0;

    /**
     * Minimum value expected in Knowledge table score field for questions already asked.
     */
    public static final int MIN_ALLOWED_SCORE = 1;

    /**
     * Maximum value expected in Knowledge table score field for questions already asked.
     */
    public static final int MAX_ALLOWED_SCORE = 20;

    /**
     * Reserved for empty correlations
     */
    public static final int EMPTY_CORRELATION_ID = 0;

    /**
     * Reserved for empty correlation arrays
     */
    public static final int EMPTY_CORRELATION_ARRAY_ID = 0;

    public static final class AcceptationsTable extends DbTable {

        private AcceptationsTable() {
            super("Acceptations", new DbIntColumn("concept"), new DbIntColumn("correlationArray"));
        }

        public int getConceptColumnIndex() {
            return 1;
        }

        public int getCorrelationArrayColumnIndex() {
            return 2;
        }
    }

    public static final class AgentsTable extends DbTable {

        private AgentsTable() {
            super("Agents", new DbIntColumn("targetSet"), new DbIntColumn("sourceSet"), new DbIntColumn("diffSet"),
                    new DbIntColumn("startMatcher"), new DbIntColumn("startAdder"),
                    new DbIntColumn("endMatcher"), new DbIntColumn("endAdder"),
                    new DbIntColumn("rule"));
        }

        /**
         * Bunch set where all results coming from the agent should be stored.
         *
         * This may be 0, reserved for empty bunch sets, to indicate that the
         * result should not be stored in any bunch.
         */
        public int getTargetBunchSetColumnIndex() {
            return 1;
        }

        public int getSourceBunchSetColumnIndex() {
            return 2;
        }

        public int getDiffBunchSetColumnIndex() {
            return 3;
        }

        public int getStartMatcherColumnIndex() {
            return 4;
        }

        public int getStartAdderArrayColumnIndex() {
            return 5;
        }

        public int getEndMatcherColumnIndex() {
            return 6;
        }

        public int getEndAdderArrayColumnIndex() {
            return 7;
        }

        /**
         * Rule applied by this agent, if any.
         *
         * This value will match {@link #NO_RULE} if the agent is not applying any rule.
         * And this can only happen if the matchers and adders of this agent matches and
         * then the resulting correlation array is exactly the same as the original one.
         *
         * This value will never match {@link #NO_RULE} if the agent is altering the
         * correlation array in the produced acceptations.
         */
        public int getRuleColumnIndex() {
            return 8;
        }
    }

    public static final class AlphabetsTable extends DbTable {

        private AlphabetsTable() {
            super("Alphabets", new DbIntColumn("language"));
        }

        public int getLanguageColumnIndex() {
            return 1;
        }
    }

    /**
     * List which acceptations are included in which bunches, and by who.
     *
     * It is possible to have multiple rows with the same value for 2 of the three columns,
     * but it is considered an error to have multiple rows where bunch, acceptation and agent are matching.
     */
    public static final class BunchAcceptationsTable extends DbTable {

        private BunchAcceptationsTable() {
            super("BunchAcceptations", new DbIntColumn("bunch"), new DbIntColumn("acceptation"), new DbIntColumn("agentSet"));
        }

        /**
         * Bunch where the acceptation is included. This is a concept.
         */
        public int getBunchColumnIndex() {
            return 1;
        }

        /**
         * Identifier for the acceptation that is included.
         */
        public int getAcceptationColumnIndex() {
            return 2;
        }

        /**
         * Identifier for the agent that included the acceptation in this bunch because it was it's target.
         * Or 0 if the user included the acceptation in the bunch manually.
         */
        public int getAgentColumnIndex() {
            return 3;
        }
    }

    /**
     * Define semantics creating new concepts, from another base concept and a complement.
     *
     * Leaving the complement aside, if we call the complemented concept 'A', and the base concept 'B'.
     * Semantically we could say that 'A' is 'B', but it may not be true that 'B' is 'A'.
     *
     * As an example, 'dog' could be a complemented concept, being its base 'animal'.
     * In this example, it is true that a 'dog' is an 'animal', but it is not true that any 'animal' is a 'dog', as there is a lot of animals.
     *
     * Complemented concepts can be chained. So, if there is a complemented concept 'A' whose base is 'B',
     * and 'B' is another complemented concept (written in this table as well but in another row)
     * whose base is 'C' , then 'A' is both 'B' and 'C'.
     *
     * Following with the example. 'A' could be 'dog', 'B' could be 'animal' and 'C' could be 'living creature'.
     * So, it is true that an 'animal' is a 'living creature', and a 'dog' is both, an animal and a living creature.
     *
     * On the other side, complements qualifies the base concept to create a new complemented concept.
     *
     * So, as en example, we can say that a 'dog', as complemented concept, is an 'animal', as base concept,
     * that is 'domestic' as a possible complement.
     *
     * New complemented concepts and the base concept are substantives,
     * while complements can be understood as adjectives.
     *
     * Restrictions within the table (serialization is optimized based on this):
     * <ul>
     *   <li>It is not possible to have 2 rows with the same complemented concept.</li>
     *   <li>Complemented concept will never match in value with the base concept nor the complement in the same row.</li>
     *   <li>Base concept and complement will never match in value in the same row.</li>
     *   <li>Complement can be 0 to determine that no suitable complement</li>
     * </ul>
     */
    public static final class ComplementedConceptsTable extends DbTable {

        private ComplementedConceptsTable() {
            super("ComplementedConcepts", new DbIntColumn("base"), new DbIntColumn("complement"));
        }

        public int getBaseColumnIndex() {
            return 1;
        }

        public int getComplementColumnIndex() {
            return 2;
        }
    }

    /**
     * Define semantics for concepts by linking 2 or more concepts that act as qualifiers to it.
     *
     * It is expected to found at least 2 rows with the same composed concept, but among the rows
     * where the composed concepts is the same, it is not expected to find duplicated items.
     *
     * In terms of semantics, the concept can be understood as the addition to all of the items.
     * This table can be combined with {@link ComplementedConceptsTable}, where the composed concept
     * her can be used as the complement in the other table, creating a definition for a concept.
     *
     * So, for example, a 'dog' is an 'animal' that is 'domestic' and 'has the ability to bark',
     * among lot of other adjectives that we could find. In this example, 'domestic' and 'has the ability to bark'
     * are 2 item concepts within this table, composing a new concept that includes all its items
     * (the composed concept), this composed concept can be used as complement in the {@link ComplementedConceptsTable},
     * while 'animal' would be the base, and 'dog' the complemented concept within that table.
     */
    public static final class ConceptCompositionsTable extends DbTable {

        private ConceptCompositionsTable() {
            super("ConceptCompositions", new DbIntColumn("composed"), new DbIntColumn("item"));
        }

        public int getComposedColumnIndex() {
            return 1;
        }

        public int getItemColumnIndex() {
            return 2;
        }
    }

    public static final class BunchSetsTable extends DbTable {

        private BunchSetsTable() {
            super("BunchSets", new DbIntColumn("setId"), new DbIntColumn("bunch"));
        }

        public int getSetIdColumnIndex() {
            return 1;
        }

        public int getBunchColumnIndex() {
            return 2;
        }

        public int nullReference() {
            return EMPTY_BUNCH_SET_ID;
        }
    }

    /**
     * Determines how a character is visually composed from the visual representation of 2 more simple characters.
     *
     * Some examples would be 'â', where characters '^' and 'a' are drawn one below the other.
     * or maybe '明' where characters '日' and '月' are drawn side by side.
     *
     * All characters listed in this table expects are expected to be composed using exactly 2 simple characters.
     * There are other characters more complex characters that they may require 3 or 4.
     * In such case, the expectation is to chain compositions, joining 2 compositions for 3 simple characters,
     * or 3 compositions for 4, and so on. Because of that, it is perfectly OK if a resulting composition does
     * not exist in reality or there is no unicode assigned for it, while that result is used as a simple
     * character for a new composition whose target already exists.
     *
     * The compositionType within this table is an abstraction of how both simple characters mut be manipulated
     * in order to compose the new one: side-by-side, top-bottom, one boxing the other... the number does not
     * mean anything, but 2 registers within this table with the same number as compositionType are expected to
     * arrange the simple characters in the same way.
     */
    public static final class CharacterCompositionsTable extends DbTable {
        private CharacterCompositionsTable() {
            super("CharacterCompositions", new DbIntColumn("firstCharacter"), new DbIntColumn("secondCharacter"), new DbIntColumn("compositionType"));
        }

        public int getFirstCharacterColumnIndex() {
            return 1;
        }

        public int getSecondCharacterColumnIndex() {
            return 2;
        }

        public int getCompositionTypeColumnIndex() {
            return 3;
        }
    }

    public static final class ConversionsTable extends DbTable {

        private ConversionsTable() {
            super("Conversions", new DbIntColumn("sourceAlphabet"), new DbIntColumn("targetAlphabet"), new DbIntColumn("source"), new DbIntColumn("target"));
        }

        public int getSourceAlphabetColumnIndex() {
            return 1;
        }

        public int getTargetAlphabetColumnIndex() {
            return 2;
        }

        public int getSourceColumnIndex() {
            return 3;
        }

        public int getTargetColumnIndex() {
            return 4;
        }
    }

    public static final class CorrelationsTable extends DbTable {

        private CorrelationsTable() {
            super("Correlations", new DbIntColumn("correlationId"), new DbIntColumn("alphabet"), new DbIntColumn("symbolArray"));
        }

        public int getCorrelationIdColumnIndex() {
            return 1;
        }

        public int getAlphabetColumnIndex() {
            return 2;
        }

        public int getSymbolArrayColumnIndex() {
            return 3;
        }
    }

    public static final class CorrelationArraysTable extends DbTable {

        private CorrelationArraysTable() {
            super("CorrelationArrays", new DbIntColumn("arrayId"), new DbIntColumn("arrayPos"), new DbIntColumn("correlation"));
        }

        public int getArrayIdColumnIndex() {
            return 1;
        }

        public int getArrayPositionColumnIndex() {
            return 2;
        }

        public int getCorrelationColumnIndex() {
            return 3;
        }
    }

    public static final class KnowledgeTable extends DbTable {

        private KnowledgeTable() {
            super("Knowledge", new DbIntColumn("quizDefinition"), new DbIntColumn("acceptation"), new DbIntColumn("score"));
        }

        public int getQuizDefinitionColumnIndex() {
            return 1;
        }

        public int getAcceptationColumnIndex() {
            return 2;
        }

        public int getScoreColumnIndex() {
            return 3;
        }
    }

    public static final class LanguagesTable extends DbTable {

        private LanguagesTable() {
            super("Languages", new DbIntColumn("mainAlphabet"), new DbUniqueTextColumn("code"));
        }

        public int getMainAlphabetColumnIndex() {
            return 1;
        }

        public int getCodeColumnIndex() {
            return 2;
        }
    }

    public interface QuestionFieldFlags {

        /**
         * Once we have an acceptation, there are 3 kind of questions ways of retrieving the information for the question field.
         * <ul>
         * <li>Same acceptation: We just get the acceptation form the Database.</li>
         * <li>Same concept: Other acceptation matching the origina concept must be found. Depending on the alphabet, they will be synonymous or translations.</li>
         * <li>Apply rule: The given acceptation is the dictionary form, then the ruled acceptation with the given rule should be found.</li>
         * </ul>
         */
        int TYPE_MASK = 3;
        int TYPE_SAME_ACC = 0;
        int TYPE_SAME_CONCEPT = 1;
        int TYPE_APPLY_RULE = 2;

        /**
         * If set, question mask has to be displayed when performing the question.
         */
        int IS_ANSWER = 4;
    }

    public static final class QuestionFieldSets extends DbTable {

        private QuestionFieldSets() {
            super("QuestionFieldSets", new DbIntColumn("setId"), new DbIntColumn("alphabet"), new DbIntColumn("flags"), new DbIntColumn("rule"));
        }

        public int getSetIdColumnIndex() {
            return 1;
        }

        public int getAlphabetColumnIndex() {
            return 2;
        }

        /**
         * @see QuestionFieldFlags
         */
        public int getFlagsColumnIndex() {
            return 3;
        }

        /**
         * Only relevant if question type if 'apply rule'. Ignored in other cases.
         */
        public int getRuleColumnIndex() {
            return 4;
        }
    }

    public static final class QuizDefinitionsTable extends DbTable {

        private QuizDefinitionsTable() {
            super("QuizDefinitions", new DbIntColumn("bunch"), new DbIntColumn("questionFields"));
        }

        public int getBunchColumnIndex() {
            return 1;
        }

        public int getQuestionFieldsColumnIndex() {
            return 2;
        }
    }

    public static final class RuleSentenceMatchesTable extends DbTable {
        private RuleSentenceMatchesTable() {
            super("RuleSentenceMatches", new DbIntColumn("rule"), new DbIntColumn("sentence"));
        }

        public int getRuleColumnIndex() {
            return 1;
        }

        public int getSentenceColumnIndex() {
            return 2;
        }
    }

    public static final class RuledAcceptationsTable extends DbTable {

        private RuledAcceptationsTable() {
            super("RuledAcceptations", new DbIntColumn("agent"), new DbIntColumn("acceptation"));
        }

        public int getAgentColumnIndex() {
            return 1;
        }

        public int getAcceptationColumnIndex() {
            return 2;
        }
    }

    public static final class RuledConceptsTable extends DbTable {

        private RuledConceptsTable() {
            super("RuledConcepts", new DbIntColumn("rule"), new DbIntColumn("concept"));
        }

        public int getRuleColumnIndex() {
            return 1;
        }

        public int getConceptColumnIndex() {
            return 2;
        }
    }

    public static final class SearchHistoryTable extends DbTable {

        private SearchHistoryTable() {
            super("SearchHistory", new DbIntColumn("acceptation"));
        }

        public int getAcceptation() {
            return 1;
        }
    }

    public static final class SentencesTable extends DbTable {

        private SentencesTable() {
            super("Sentences", new DbIntColumn("concept"), new DbIntColumn("symbolArray"));
        }

        /**
         * Concept for this sentence.
         *
         * Sentences sharing the same concept means that they have the same meaning.
         * They can be defined for the same language (synonyms) or among different languages (translations).
         */
        public int getConceptColumnIndex() {
            return 1;
        }

        /**
         * Foreign key for the SymbolArrays table.
         *
         * This identifies the text to be displayed for this sentence.
         */
        public int getSymbolArrayColumnIndex() {
            return 2;
        }
    }

    public static final class SpanTable extends DbTable {

        private SpanTable() {
            super("SpanTable", new DbIntColumn("sentenceId"), new DbIntColumn("start"),
                    new DbIntColumn("length"), new DbIntColumn("acceptation"));
        }

        /**
         * Foreign key for the SentencesTable id column.
         * This is the unique identifier within the database for the sentence.
         */
        public int getSentenceIdColumnIndex() {
            return 1;
        }

        /**
         * Index within the symbol array where this span starts.
         * It's inclusive, which means that the char at the given index is also included within the span.
         */
        public int getStartColumnIndex() {
            return 2;
        }

        /**
         * The amount of characters included in this span.
         * This should always be a positive number, never zero.
         */
        public int getLengthColumnIndex() {
            return 3;
        }

        /**
         * Dynamic acceptation for this span.
         */
        public int getDynamicAcceptationColumnIndex() {
            return 4;
        }
    }

    public static final class StringQueriesTable extends DbTable {

        private StringQueriesTable() {
            super("StringQueryTable", new DbIntColumn("mainAcceptation"), new DbIntColumn("dynamicAcceptation"),
                    new DbIntColumn("strAlphabet"), new DbTextColumn("str"), new DbTextColumn("mainStr"));
        }

        public int getMainAcceptationColumnIndex() {
            return 1;
        }

        public int getDynamicAcceptationColumnIndex() {
            return 2;
        }

        public int getStringAlphabetColumnIndex() {
            return 3;
        }

        public int getStringColumnIndex() {
            return 4;
        }

        public int getMainStringColumnIndex() {
            return 5;
        }
    }

    public static final class SymbolArraysTable extends DbTable {

        private SymbolArraysTable() {
            super("SymbolArrays", new DbUniqueTextColumn("str"));
        }

        public int getStrColumnIndex() {
            return 1;
        }
    }

    /**
     * Determine the relationship between the characters used by the CharacterCompositionsTable and its unicode.
     *
     * Not all the characters found in the id, first or second characters of the {@link CharacterCompositionsTable}
     * are required to be in this table. Some of the composition resulting characters may be intermediate steps
     * in a chained character composition and may not have any unicode assigned. This table can be used in that
     * sense to determine which of them have a real representation.
     */
    public static final class UnicodeCharactersTable extends DbTable {
        private UnicodeCharactersTable() {
            super("UnicodeCharacters", new DbIntColumn("unicode"));
        }

        public int getUnicodeColumnIndex() {
            return 1;
        }
    }

    public interface Tables {
        AcceptationsTable acceptations = new AcceptationsTable();
        AgentsTable agents = new AgentsTable();
        AlphabetsTable alphabets = new AlphabetsTable();
        BunchAcceptationsTable bunchAcceptations = new BunchAcceptationsTable();
        BunchSetsTable bunchSets = new BunchSetsTable();
        CharacterCompositionsTable characterCompositions = new CharacterCompositionsTable();
        ComplementedConceptsTable complementedConcepts = new ComplementedConceptsTable();
        ConceptCompositionsTable conceptCompositions = new ConceptCompositionsTable();
        ConversionsTable conversions = new ConversionsTable();
        CorrelationsTable correlations = new CorrelationsTable();
        CorrelationArraysTable correlationArrays = new CorrelationArraysTable();
        KnowledgeTable knowledge = new KnowledgeTable();
        LanguagesTable languages = new LanguagesTable();
        QuestionFieldSets questionFieldSets = new QuestionFieldSets();
        QuizDefinitionsTable quizDefinitions = new QuizDefinitionsTable();
        RuleSentenceMatchesTable ruleSentenceMatches = new RuleSentenceMatchesTable();
        RuledAcceptationsTable ruledAcceptations = new RuledAcceptationsTable();
        RuledConceptsTable ruledConcepts = new RuledConceptsTable();
        SearchHistoryTable searchHistory = new SearchHistoryTable();
        SentencesTable sentences = new SentencesTable();
        SpanTable spans = new SpanTable();
        StringQueriesTable stringQueries = new StringQueriesTable();
        SymbolArraysTable symbolArrays = new SymbolArraysTable();
        UnicodeCharactersTable unicodeCharacters = new UnicodeCharactersTable();
    }

    private final ImmutableList<DbTable> _tablesV5 = new ImmutableList.Builder<DbTable>()
            .add(Tables.acceptations)
            .add(Tables.agents)
            .add(Tables.alphabets)
            .add(Tables.bunchAcceptations)
            .add(Tables.bunchSets)
            .add(Tables.complementedConcepts)
            .add(Tables.conceptCompositions)
            .add(Tables.conversions)
            .add(Tables.correlations)
            .add(Tables.correlationArrays)
            .add(Tables.knowledge)
            .add(Tables.languages)
            .add(Tables.questionFieldSets)
            .add(Tables.quizDefinitions)
            .add(Tables.ruleSentenceMatches)
            .add(Tables.ruledAcceptations)
            .add(Tables.ruledConcepts)
            .add(Tables.searchHistory)
            .add(Tables.sentences)
            .add(Tables.spans)
            .add(Tables.stringQueries)
            .add(Tables.symbolArrays)
            .build();

    private final ImmutableList<DbTable> _tablesV6 = _tablesV5
            .append(Tables.characterCompositions)
            .append(Tables.unicodeCharacters);

    private final ImmutableList<DbIndex> _indexes = new ImmutableList.Builder<DbIndex>()
            .add(new DbIndex(Tables.stringQueries, Tables.stringQueries.getDynamicAcceptationColumnIndex()))
            .add(new DbIndex(Tables.correlations, Tables.correlations.getCorrelationIdColumnIndex()))
            .add(new DbIndex(Tables.correlationArrays, Tables.correlationArrays.getArrayIdColumnIndex()))
            .add(new DbIndex(Tables.acceptations, Tables.acceptations.getConceptColumnIndex()))
            .build();

    private static final class SchemaVersion implements DbSchema {

        private final SchemaVersion _previous;
        private final ImmutableList<DbTable> _tables;
        private final ImmutableList<DbIndex> _indexes;

        SchemaVersion(SchemaVersion previous, ImmutableList<DbTable> tables, ImmutableList<DbIndex> indexes) {
            _previous = previous;
            _tables = tables;
            _indexes = indexes;
        }

        @Override
        public ImmutableList<DbTable> tables() {
            return _tables;
        }

        @Override
        public ImmutableList<DbIndex> indexes() {
            return _indexes;
        }

        void setup(MutableSchemaDatabase db) {
            for (DbTable table : _tables) {
                db.createTable(table);
            }

            for (DbIndex index : _indexes) {
                db.createIndex(index);
            }
        }

        void upgradeFromPreviousVersion(MutableSchemaDatabase db) {
            final DbSchemaDifference difference = DbSchemaUtils.difference(_previous, this);

            for (DbTable table : difference.newTables()) {
                db.createTable(table);
            }

            for (DbIndex index : difference.newIndexes()) {
                db.createIndex(index);
            }
        }
    }

    private final SchemaVersion _schemaVersion5 = new SchemaVersion(null, _tablesV5, _indexes);
    private final SchemaVersion _schemaVersion6 = new SchemaVersion(_schemaVersion5, _tablesV6, _indexes);

    private final ImmutableList<DbSchema> _schemaVersions = new ImmutableList.Builder<DbSchema>()
            .add(_schemaVersion5)
            .add(_schemaVersion6)
            .build();

    private LangbookDbSchema() {
    }

    /**
     * Retrieves the schema version
     * @param version Must be at least {@value #FIRST_VERSION_CODE}.
     * @return The schema version.
     */
    public DbSchema schemaVersion(int version) {
        return _schemaVersions.valueAt(version - FIRST_VERSION_CODE);
    }

    public int currentSchemaVersionCode() {
        return _schemaVersions.size() + FIRST_VERSION_CODE - 1;
    }

    public void setup(MutableSchemaDatabase db) {
        ((SchemaVersion) TraversableUtils.last(_schemaVersions)).setup(db);
    }

    public void upgradeDatabaseVersion(MutableSchemaDatabase db, int oldVersion, int newVersion) {
        for (int currentVersion = oldVersion + 1; currentVersion <= newVersion; currentVersion++) {
            final SchemaVersion newSchema = (SchemaVersion) schemaVersion(currentVersion);
            newSchema.upgradeFromPreviousVersion(db);
        }
    }

    private static LangbookDbSchema _instance;

    public static LangbookDbSchema getInstance() {
        if (_instance == null) {
            _instance = new LangbookDbSchema();
        }

        return _instance;
    }
}
