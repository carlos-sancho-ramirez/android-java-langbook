package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.Set;
import sword.collections.Traverser;
import sword.database.DbInsertQuery;
import sword.database.DbInserter;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.QuestionFieldDetails;

import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;

final class LangbookDbInserter {

    private LangbookDbInserter() {
    }

    static <SymbolArrayId> SymbolArrayId insertSymbolArray(DbInserter db, IntSetter<SymbolArrayId> symbolArrayIdSetter, String str) {
        final LangbookDbSchema.SymbolArraysTable table = Tables.symbolArrays;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStrColumnIndex(), str)
                .build();
        final Integer intResult = db.insert(query);
        return (intResult != null)? symbolArrayIdSetter.getKeyFromInt(intResult) : null;
    }

    static <ConceptId> void insertAlphabet(DbInserter db, AlphabetIdInterface<ConceptId> id, LanguageIdInterface<ConceptId> language) {
        final LangbookDbSchema.AlphabetsTable table = Tables.alphabets;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getLanguageColumnIndex(), language)
                .build();
        db.insert(query);
    }

    static <ConceptId> void insertLanguage(DbInserter db, LanguageIdInterface<ConceptId> id, String code, AlphabetIdInterface<ConceptId> mainAlphabet) {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getCodeColumnIndex(), code)
                .put(table.getIdColumnIndex(), id)
                .put(table.getMainAlphabetColumnIndex(), mainAlphabet)
                .build();
        db.insert(query);
    }

    static <ConceptId> void insertConversion(DbInserter db, AlphabetIdInterface<ConceptId> sourceAlphabet, AlphabetIdInterface<ConceptId> targetAlphabet, SymbolArrayIdInterface source, SymbolArrayIdInterface target) {
        final LangbookDbSchema.ConversionsTable table = Tables.conversions;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .put(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        db.insert(query);
    }

    static <ConceptId> void insertCorrelationEntry(DbInserter db, CorrelationIdInterface correlationId, AlphabetIdInterface<ConceptId> alphabet, SymbolArrayIdInterface symbolArray) {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getCorrelationIdColumnIndex(), correlationId)
                .put(table.getSymbolArrayColumnIndex(), symbolArray)
                .put(table.getAlphabetColumnIndex(), alphabet)
                .build();
        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static <ConceptId> void insertCorrelation(DbInserter db, CorrelationIdInterface correlationId, Map<? extends AlphabetIdInterface<ConceptId>, ? extends SymbolArrayIdInterface> correlation) {
        final int mapLength = correlation.size();
        if (mapLength == 0) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < mapLength; i++) {
            insertCorrelationEntry(db, correlationId, correlation.keyAt(i), correlation.valueAt(i));
        }
    }

    static <CorrelationId extends CorrelationIdInterface> void insertCorrelationArray(DbInserter db, CorrelationArrayIdInterface arrayId, List<CorrelationId> array) {
        final Traverser<CorrelationId> iterator = array.iterator();
        if (!array.iterator().hasNext()) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        for (int i = 0; iterator.hasNext(); i++) {
            final CorrelationId correlation = iterator.next();
            final DbInsertQuery query = new DbInsertQueryBuilder(table)
                    .put(table.getArrayIdColumnIndex(), arrayId)
                    .put(table.getArrayPositionColumnIndex(), i)
                    .put(table.getCorrelationColumnIndex(), correlation)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    static <AcceptationId> AcceptationId insertAcceptation(DbInserter db, IntSetter<AcceptationId> acceptationIdSetter, ConceptIdInterface concept, CorrelationArrayIdInterface correlationArray) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return acceptationIdSetter.getKeyFromInt(db.insert(query));
    }

    static void insertConceptCompositionEntry(DbInserter db, ConceptIdInterface compositionId, ConceptIdInterface item) {
        final LangbookDbSchema.ConceptCompositionsTable table = Tables.conceptCompositions;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getComposedColumnIndex(), compositionId)
                .put(table.getItemColumnIndex(), item)
                .build();
        db.insert(query);
    }

    static void insertComplementedConcept(DbInserter db, ConceptIdInterface base, ConceptIdInterface complementedConcept, ConceptIdInterface complement) {
        final LangbookDbSchema.ComplementedConceptsTable table = Tables.complementedConcepts;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getIdColumnIndex(), complementedConcept)
                .put(table.getBaseColumnIndex(), base)
                .put(table.getComplementColumnIndex(), complement)
                .build();
        db.insert(query);
    }

    static <ConceptId> void insertBunchAcceptation(DbInserter db, BunchIdInterface<ConceptId> bunch, AcceptationIdInterface acceptation, int agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getAgentColumnIndex(), agent)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static <ConceptId, BunchId extends BunchIdInterface<ConceptId>> void insertBunchSet(DbInserter db, BunchSetIdInterface setId, Set<BunchId> bunches) {
        if (bunches.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        for (BunchId bunch : bunches) {
            final DbInsertQuery query = new DbInsertQueryBuilder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getBunchColumnIndex(), bunch)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    static <ConceptId> Integer insertAgent(DbInserter db, AgentRegister<? extends CorrelationIdInterface, ? extends BunchSetIdInterface, ? extends RuleIdInterface<ConceptId>> register) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
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

    static <ConceptId extends ConceptIdInterface> void insertRuledConcept(DbInserter db, ConceptId ruledConcept, RuleIdInterface<ConceptId> rule, ConceptId baseConcept) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getIdColumnIndex(), ruledConcept)
                .put(table.getRuleColumnIndex(), rule)
                .put(table.getConceptColumnIndex(), baseConcept)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertRuledAcceptation(DbInserter db, AcceptationIdInterface ruledAcceptation, int agent, AcceptationIdInterface baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getIdColumnIndex(), ruledAcceptation)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getAcceptationColumnIndex(), baseAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static <ConceptId> void insertStringQuery(DbInserter db, String str,
            String mainStr, AcceptationIdInterface mainAcceptation, AcceptationIdInterface dynAcceptation, AlphabetIdInterface<ConceptId> strAlphabet) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
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

    static <ConceptId, AlphabetId extends AlphabetIdInterface<ConceptId>, RuleId extends RuleIdInterface<ConceptId>> void insertQuestionFieldSet(DbInserter db, int setId, Iterable<QuestionFieldDetails<AlphabetId, RuleId>> fields) {
        final LangbookDbSchema.QuestionFieldSets table = Tables.questionFieldSets;
        for (QuestionFieldDetails<AlphabetId, RuleId> field : fields) {
            final DbInsertQuery query = new DbInsertQueryBuilder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getRuleColumnIndex(), field.rule)
                    .put(table.getFlagsColumnIndex(), field.flags)
                    .put(table.getAlphabetColumnIndex(), field.alphabet)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    static <ConceptId> int insertQuizDefinition(DbInserter db, BunchIdInterface<ConceptId> bunch, int setId) {
        final LangbookDbSchema.QuizDefinitionsTable table = Tables.quizDefinitions;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getQuestionFieldsColumnIndex(), setId)
                .build();

        return db.insert(query);
    }

    static <AcceptationId extends AcceptationIdInterface> void insertAllPossibilities(DbInserter db, int quizId, Set<AcceptationId> acceptations) {
        final LangbookDbSchema.KnowledgeTable table = Tables.knowledge;
        for (AcceptationId acceptation : acceptations) {
            final DbInsertQuery query = new DbInsertQueryBuilder(table)
                    .put(table.getQuizDefinitionColumnIndex(), quizId)
                    .put(table.getAcceptationColumnIndex(), acceptation)
                    .put(table.getScoreColumnIndex(), NO_SCORE)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    static void insertSearchHistoryEntry(DbInserter db, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.SearchHistoryTable table = Tables.searchHistory;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getAcceptation(), acceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertSpan(DbInserter db, int sentenceId, ImmutableIntRange range, AcceptationIdInterface dynamicAcceptation) {
        if (range == null || range.min() < 0) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.SpanTable table = Tables.spans;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getSentenceIdColumnIndex(), sentenceId)
                .put(table.getStartColumnIndex(), range.min())
                .put(table.getLengthColumnIndex(), range.size())
                .put(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static int insertSentence(DbInserter db, ConceptIdInterface concept, SymbolArrayIdInterface symbolArray) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final DbInsertQuery query = new DbInsertQueryBuilder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getSymbolArrayColumnIndex(), symbolArray)
                .build();

        return db.insert(query);
    }
}
