package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;
import sword.collections.IntList;
import sword.collections.IntSet;
import sword.collections.IntTraverser;
import sword.collections.IntValueMap;
import sword.database.DbInsertQuery;
import sword.database.DbInserter;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.QuestionFieldDetails;

import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;

final class LangbookDbInserter {

    private LangbookDbInserter() {
    }

    static Integer insertSymbolArray(DbInserter db, String str) {
        final LangbookDbSchema.SymbolArraysTable table = Tables.symbolArrays;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStrColumnIndex(), str)
                .build();
        return db.insert(query);
    }

    static <AlphabetId extends AlphabetIdInterface> void insertAlphabet(DbInserter db, AlphabetId id, int language) {
        final LangbookDbSchema.AlphabetsTable table = Tables.alphabets;
        final DbInsertQuery.Builder builder = new DbInsertQuery.Builder(table);
        id.put(table.getIdColumnIndex(), builder);
        final DbInsertQuery query = builder
                .put(table.getLanguageColumnIndex(), language)
                .build();

        db.insert(query);
    }

    static <AlphabetId extends AlphabetIdInterface> void insertLanguage(DbInserter db, int id, String code, AlphabetId mainAlphabet) {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbInsertQuery.Builder builder = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getCodeColumnIndex(), code);
        mainAlphabet.put(table.getMainAlphabetColumnIndex(), builder);
        final DbInsertQuery query = builder.build();
        db.insert(query);
    }

    static <AlphabetId extends AlphabetIdInterface> void insertConversion(DbInserter db, AlphabetId sourceAlphabet, AlphabetId targetAlphabet, int source, int target) {
        final LangbookDbSchema.ConversionsTable table = Tables.conversions;
        final DbInsertQuery.Builder builder = new DbInsertQuery.Builder(table);
        sourceAlphabet.put(table.getSourceAlphabetColumnIndex(), builder);
        targetAlphabet.put(table.getTargetAlphabetColumnIndex(), builder);
        final DbInsertQuery query = builder
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        db.insert(query);
    }

    static <AlphabetId extends AlphabetIdInterface> void insertCorrelationEntry(DbInserter db, int correlationId, AlphabetId alphabet, int symbolArray) {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final DbInsertQuery.Builder builder = new DbInsertQuery.Builder(table)
                .put(table.getCorrelationIdColumnIndex(), correlationId)
                .put(table.getSymbolArrayColumnIndex(), symbolArray);
        alphabet.put(table.getAlphabetColumnIndex(), builder);
        final DbInsertQuery query = builder.build();
        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static <AlphabetId extends AlphabetIdInterface> void insertCorrelation(DbInserter db, int correlationId, IntValueMap<AlphabetId> correlation) {
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

    static int insertAcceptation(DbInserter db, int concept, int correlationArray) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return db.insert(query);
    }

    static void insertConceptCompositionEntry(DbInserter db, int compositionId, int item) {
        final LangbookDbSchema.ConceptCompositionsTable table = Tables.conceptCompositions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getComposedColumnIndex(), compositionId)
                .put(table.getItemColumnIndex(), item)
                .build();
        db.insert(query);
    }

    static void insertComplementedConcept(DbInserter db, int base, int complementedConcept, int complement) {
        final LangbookDbSchema.ComplementedConceptsTable table = Tables.complementedConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), complementedConcept)
                .put(table.getBaseColumnIndex(), base)
                .put(table.getComplementColumnIndex(), complement)
                .build();
        db.insert(query);
    }

    static void insertBunchAcceptation(DbInserter db, int bunch, int acceptation, int agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getAgentColumnIndex(), agent)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertBunchSet(DbInserter db, int setId, IntSet bunches) {
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

    static Integer insertAgent(DbInserter db, AgentRegister register) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getTargetBunchSetColumnIndex(), register.targetBunchSetId)
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

    static void insertRuledConcept(DbInserter db, int ruledConcept, int rule, int baseConcept) {
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

    static void insertRuledAcceptation(DbInserter db, int ruledAcceptation, int agent, int baseAcceptation) {
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

    static <AlphabetId extends AlphabetIdInterface> void insertStringQuery(DbInserter db, String str,
            String mainStr, int mainAcceptation, int dynAcceptation, AlphabetId strAlphabet) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbInsertQuery.Builder builder = new DbInsertQuery.Builder(table)
                .put(table.getStringColumnIndex(), str)
                .put(table.getMainStringColumnIndex(), mainStr)
                .put(table.getMainAcceptationColumnIndex(), mainAcceptation)
                .put(table.getDynamicAcceptationColumnIndex(), dynAcceptation);
        strAlphabet.put(table.getStringAlphabetColumnIndex(), builder);
        final DbInsertQuery query = builder.build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static <AlphabetId extends AlphabetIdInterface> void insertQuestionFieldSet(DbInserter db, int setId, Iterable<QuestionFieldDetails<AlphabetId>> fields) {
        final LangbookDbSchema.QuestionFieldSets table = Tables.questionFieldSets;
        for (QuestionFieldDetails<AlphabetId> field : fields) {
            final DbInsertQuery.Builder builder = new DbInsertQuery.Builder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getRuleColumnIndex(), field.rule)
                    .put(table.getFlagsColumnIndex(), field.flags);
            field.alphabet.put(table.getAlphabetColumnIndex(), builder);

            final DbInsertQuery query = builder.build();
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

    static void insertAllPossibilities(DbInserter db, int quizId, IntSet acceptations) {
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

    static void insertSearchHistoryEntry(DbInserter db, int acceptation) {
        final LangbookDbSchema.SearchHistoryTable table = Tables.searchHistory;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getAcceptation(), acceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertSpan(DbInserter db, int sentenceId, ImmutableIntRange range, int dynamicAcceptation) {
        if (range == null || range.min() < 0) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.SpanTable table = Tables.spans;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSentenceIdColumnIndex(), sentenceId)
                .put(table.getStartColumnIndex(), range.min())
                .put(table.getLengthColumnIndex(), range.size())
                .put(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static int insertSentence(DbInserter db, int concept, int symbolArray) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getSymbolArrayColumnIndex(), symbolArray)
                .build();

        return db.insert(query);
    }
}
