package sword.langbook3.android;

import sword.collections.ImmutableList;
import sword.langbook3.android.db.DbIndex;
import sword.langbook3.android.db.DbIntColumn;
import sword.langbook3.android.db.DbSchema;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.db.DbTextColumn;
import sword.langbook3.android.db.DbUniqueTextColumn;

public final class LangbookDbSchema implements DbSchema {

    public static final class AcceptationsTable extends DbTable {

        private AcceptationsTable() {
            super("Acceptations", new DbIntColumn("word"), new DbIntColumn("concept"), new DbIntColumn("correlationArray"));
        }

        public int getWordColumnIndex() {
            return 1;
        }

        public int getConceptColumnIndex() {
            return 2;
        }

        public int getCorrelationArrayColumnIndex() {
            return 3;
        }
    }

    public static final class AgentsTable extends DbTable {

        private AgentsTable() {
            super("Agents", new DbIntColumn("target"), new DbIntColumn("sourceSet"), new DbIntColumn("diffSet"),
                    new DbIntColumn("matcher"), new DbIntColumn("adder"), new DbIntColumn("rule"), new DbIntColumn("flags"));
        }

        public int getTargetBunchColumnIndex() {
            return 1;
        }

        public int getSourceBunchSetColumnIndex() {
            return 2;
        }

        public int getDiffBunchSetColumnIndex() {
            return 3;
        }

        public int getMatcherColumnIndex() {
            return 4;
        }

        public int getAdderColumnIndex() {
            return 5;
        }

        public int getRuleColumnIndex() {
            return 6;
        }

        public int getFlagsColumnIndex() {
            return 7;
        }

        public int nullReference() {
            return 0;
        }
    }

    public static final class AgentSetsTable extends DbTable {

        private AgentSetsTable() {
            super("AgentSets", new DbIntColumn("setId"), new DbIntColumn("agent"));
        }

        public int getSetIdColumnIndex() {
            return 1;
        }

        public int getAgentColumnIndex() {
            return 2;
        }

        public int nullReference() {
            return 0;
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

    public static final class BunchAcceptationsTable extends DbTable {

        private BunchAcceptationsTable() {
            super("BunchAcceptations", new DbIntColumn("bunch"), new DbIntColumn("acceptation"), new DbIntColumn("agentSet"));
        }

        public int getBunchColumnIndex() {
            return 1;
        }

        public int getAcceptationColumnIndex() {
            return 2;
        }

        public int getAgentSetColumnIndex() {
            return 3;
        }
    }

    public static final class BunchConceptsTable extends DbTable {

        private BunchConceptsTable() {
            super("BunchConcepts", new DbIntColumn("bunch"), new DbIntColumn("concept"));
        }

        public int getBunchColumnIndex() {
            return 1;
        }

        public int getConceptColumnIndex() {
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
            return 0;
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
         * <li>Same acceptation: We just get the acceptation form the Database.</li>
         * <li>Same concept: Other acceptation matching the origina concept must be found. Depending on the alphabet, they will be synonymous or translations.</li>
         * <li>Apply rule: The given acceptation is the dictionary form, then the ruled acceptation with the given rule should be found.</li>
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
            super("RuledConcepts", new DbIntColumn("agent"), new DbIntColumn("concept"));
        }

        public int getAgentColumnIndex() {
            return 1;
        }

        public int getConceptColumnIndex() {
            return 2;
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

    public interface Tables {
        AcceptationsTable acceptations = new AcceptationsTable();
        AgentsTable agents = new AgentsTable();
        AgentSetsTable agentSets = new AgentSetsTable();
        AlphabetsTable alphabets = new AlphabetsTable();
        BunchAcceptationsTable bunchAcceptations = new BunchAcceptationsTable();
        BunchConceptsTable bunchConcepts = new BunchConceptsTable();
        BunchSetsTable bunchSets = new BunchSetsTable();
        ConversionsTable conversions = new ConversionsTable();
        CorrelationsTable correlations = new CorrelationsTable();
        CorrelationArraysTable correlationArrays = new CorrelationArraysTable();
        KnowledgeTable knowledge = new KnowledgeTable();
        LanguagesTable languages = new LanguagesTable();
        QuestionFieldSets questionFieldSets = new QuestionFieldSets();
        QuizDefinitionsTable quizDefinitions = new QuizDefinitionsTable();
        RuledAcceptationsTable ruledAcceptations = new RuledAcceptationsTable();
        RuledConceptsTable ruledConcepts = new RuledConceptsTable();
        StringQueriesTable stringQueries = new StringQueriesTable();
        SymbolArraysTable symbolArrays = new SymbolArraysTable();
    }

    private final ImmutableList<DbTable> _tables = new ImmutableList.Builder<DbTable>()
            .add(Tables.acceptations)
            .add(Tables.agents)
            .add(Tables.agentSets)
            .add(Tables.alphabets)
            .add(Tables.bunchAcceptations)
            .add(Tables.bunchConcepts)
            .add(Tables.bunchSets)
            .add(Tables.conversions)
            .add(Tables.correlations)
            .add(Tables.correlationArrays)
            .add(Tables.knowledge)
            .add(Tables.languages)
            .add(Tables.questionFieldSets)
            .add(Tables.quizDefinitions)
            .add(Tables.ruledAcceptations)
            .add(Tables.ruledConcepts)
            .add(Tables.stringQueries)
            .add(Tables.symbolArrays)
            .build();

    private final ImmutableList<DbIndex> _indexes = new ImmutableList.Builder<DbIndex>()
            .add(new DbIndex(Tables.stringQueries, Tables.stringQueries.getDynamicAcceptationColumnIndex()))
            .add(new DbIndex(Tables.acceptations, Tables.acceptations.getConceptColumnIndex()))
            .build();

    private LangbookDbSchema() {
    }

    @Override
    public ImmutableList<DbTable> tables() {
        return _tables;
    }

    @Override
    public ImmutableList<DbIndex> indexes() {
        return _indexes;
    }

    private static LangbookDbSchema _instance;

    public static LangbookDbSchema getInstance() {
        if (_instance == null) {
            _instance = new LangbookDbSchema();
        }

        return _instance;
    }
}
