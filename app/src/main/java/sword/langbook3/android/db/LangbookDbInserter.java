package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.IntTraverser;
import sword.database.DbInsertQuery;
import sword.database.DbInserter;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.db.LangbookReadableDatabase.AgentRegister;
import sword.langbook3.android.models.QuestionFieldDetails;

import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;

public final class LangbookDbInserter {

    private final DbInserter db;

    public LangbookDbInserter(DbInserter db) {
        this.db = db;
    }

    public static Integer insertSymbolArray(DbInserter db, String str) {
        final LangbookDbSchema.SymbolArraysTable table = Tables.symbolArrays;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStrColumnIndex(), str)
                .build();
        return db.insert(query);
    }

    public static void insertAlphabet(DbInserter db, int id, int language) {
        final LangbookDbSchema.AlphabetsTable table = Tables.alphabets;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getLanguageColumnIndex(), language)
                .build();

        if (db.insert(query) != id) {
            throw new AssertionError();
        }
    }

    public static void insertLanguage(DbInserter db, int id, String code, int mainAlphabet) {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getCodeColumnIndex(), code)
                .put(table.getMainAlphabetColumnIndex(), mainAlphabet)
                .build();
        db.insert(query);
    }

    public static void insertConversion(DbInserter db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        final LangbookDbSchema.ConversionsTable table = Tables.conversions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .put(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        db.insert(query);
    }

    static void insertCorrelationEntry(DbInserter db, int correlationId, int alphabet, int symbolArray) {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getCorrelationIdColumnIndex(), correlationId)
                .put(table.getAlphabetColumnIndex(), alphabet)
                .put(table.getSymbolArrayColumnIndex(), symbolArray)
                .build();
        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertCorrelation(DbInserter db, int correlationId, IntPairMap correlation) {
        final int mapLength = correlation.size();
        if (mapLength == 0) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < mapLength; i++) {
            insertCorrelationEntry(db, correlationId, correlation.keyAt(i), correlation.valueAt(i));
        }
    }

    static void insertCorrelationArray(DbInserter db, int arrayId, IntList array) {
        final IntTraverser iterator = array.iterator();
        if (!array.iterator().hasNext()) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        for (int i = 0; iterator.hasNext(); i++) {
            final int correlation = iterator.next();
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getArrayIdColumnIndex(), arrayId)
                    .put(table.getArrayPositionColumnIndex(), i)
                    .put(table.getCorrelationColumnIndex(), correlation)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    public static int insertAcceptation(DbInserter db, int concept, int correlationArray) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return db.insert(query);
    }

    public static void insertBunchConcept(DbInserter db, int bunch, int concept) {
        final LangbookDbSchema.BunchConceptsTable table = Tables.bunchConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getConceptColumnIndex(), concept)
                .build();
        db.insert(query);
    }

    public static void insertBunchAcceptation(DbInserter db, int bunch, int acceptation, int agentSet) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getAgentSetColumnIndex(), agentSet)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertBunchSet(DbInserter db, int setId, IntSet bunches) {
        if (bunches.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        for (int bunch : bunches) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getBunchColumnIndex(), bunch)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    public static Integer insertAgent(DbInserter db, AgentRegister register) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getTargetBunchColumnIndex(), register.targetBunch)
                .put(table.getSourceBunchSetColumnIndex(), register.sourceBunchSetId)
                .put(table.getDiffBunchSetColumnIndex(), register.diffBunchSetId)
                .put(table.getStartMatcherColumnIndex(), register.startMatcherId)
                .put(table.getStartAdderColumnIndex(), register.startAdderId)
                .put(table.getEndMatcherColumnIndex(), register.endMatcherId)
                .put(table.getEndAdderColumnIndex(), register.endAdderId)
                .put(table.getRuleColumnIndex(), register.rule)
                .build();
        return db.insert(query);
    }

    public static void insertAgentSet(DbInserter db, int setId, IntSet agentSet) {
        final LangbookDbSchema.AgentSetsTable table = Tables.agentSets;
        for (int agent : agentSet) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getAgentColumnIndex(), agent)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    public static void insertRuledConcept(DbInserter db, int ruledConcept, int rule, int baseConcept) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledConcept)
                .put(table.getRuleColumnIndex(), rule)
                .put(table.getConceptColumnIndex(), baseConcept)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertRuledAcceptation(DbInserter db, int ruledAcceptation, int agent, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledAcceptation)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getAcceptationColumnIndex(), baseAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertStringQuery(DbInserter db, String str,
            String mainStr, int mainAcceptation, int dynAcceptation, int strAlphabet) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStringColumnIndex(), str)
                .put(table.getMainStringColumnIndex(), mainStr)
                .put(table.getMainAcceptationColumnIndex(), mainAcceptation)
                .put(table.getDynamicAcceptationColumnIndex(), dynAcceptation)
                .put(table.getStringAlphabetColumnIndex(), strAlphabet)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertQuestionFieldSet(DbInserter db, int setId, Iterable<QuestionFieldDetails> fields) {
        final LangbookDbSchema.QuestionFieldSets table = Tables.questionFieldSets;
        for (QuestionFieldDetails field : fields) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getAlphabetColumnIndex(), field.alphabet)
                    .put(table.getRuleColumnIndex(), field.rule)
                    .put(table.getFlagsColumnIndex(), field.flags)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    static int insertQuizDefinition(DbInserter db, int bunch, int setId) {
        final LangbookDbSchema.QuizDefinitionsTable table = Tables.quizDefinitions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getQuestionFieldsColumnIndex(), setId)
                .build();

        return db.insert(query);
    }

    public static void insertAllPossibilities(DbInserter db, int quizId, IntSet acceptations) {
        final LangbookDbSchema.KnowledgeTable table = Tables.knowledge;
        for (int acceptation : acceptations) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getQuizDefinitionColumnIndex(), quizId)
                    .put(table.getAcceptationColumnIndex(), acceptation)
                    .put(table.getScoreColumnIndex(), NO_SCORE)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    public static void insertSearchHistoryEntry(DbInserter db, int acceptation) {
        final LangbookDbSchema.SearchHistoryTable table = Tables.searchHistory;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getAcceptation(), acceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertSpan(DbInserter db, int symbolArray, ImmutableIntRange range, int acceptation) {
        if (range == null || range.min() < 0) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.SpanTable table = Tables.spans;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSymbolArray(), symbolArray)
                .put(table.getStart(), range.min())
                .put(table.getLength(), range.size())
                .put(table.getAcceptation(), acceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertSentenceMeaning(DbInserter db, int symbolArray, int meaning) {
        final LangbookDbSchema.SentenceMeaningTable table = Tables.sentenceMeaning;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), symbolArray)
                .put(table.getMeaning(), meaning)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }
}
