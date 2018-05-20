package sword.langbook3.android;

import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbInserter;

public final class LangbookDbInserter {

    private final DbInserter db;

    public LangbookDbInserter(DbInserter db) {
        this.db = db;
    }

    public static Integer insertSymbolArray(DbInserter db, String str) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getStrColumnIndex(), str)
                .build();
        return db.insert(query);
    }

    public static void insertAlphabet(DbInserter db, int id, int language) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getLanguageColumnIndex(), language)
                .build();

        if (db.insert(query) != id) {
            throw new AssertionError();
        }
    }

    public static void insertLanguage(DbInserter db, int id, String code, int mainAlphabet) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getCodeColumnIndex(), code)
                .put(table.getMainAlphabetColumnIndex(), mainAlphabet)
                .build();
        db.insert(query);
    }

    public static void insertConversion(DbInserter db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .put(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        db.insert(query);
    }

    public static void insertCorrelation(DbInserter db, int correlationId, IntPairMap correlation) {
        final int mapLength = correlation.size();
        if (mapLength == 0) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        for (int i = 0; i < mapLength; i++) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getCorrelationIdColumnIndex(), correlationId)
                    .put(table.getAlphabetColumnIndex(), correlation.keyAt(i))
                    .put(table.getSymbolArrayColumnIndex(), correlation.valueAt(i))
                    .build();
            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    public static void insertCorrelationArray(DbInserter db, int arrayId, int... correlation) {
        if (correlation.length == 0) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final int arrayLength = correlation.length;
        for (int i = 0; i < arrayLength; i++) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getArrayIdColumnIndex(), arrayId)
                    .put(table.getArrayPositionColumnIndex(), i)
                    .put(table.getCorrelationColumnIndex(), correlation[i])
                    .build();
            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    public static int insertAcceptation(DbInserter db, int word, int concept, int correlationArray) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getWordColumnIndex(), word)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return db.insert(query);
    }

    public static void insertBunchConcept(DbInserter db, int bunch, int concept) {
        final LangbookDbSchema.BunchConceptsTable table = LangbookDbSchema.Tables.bunchConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getConceptColumnIndex(), concept)
                .build();
        db.insert(query);
    }

    public static void insertBunchAcceptation(DbInserter db, int bunch, int acceptation, int agentSet) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
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

        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
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

    public static Integer insertAgent(DbInserter db, int targetBunch, int sourceBunchSetId,
                            int diffBunchSetId, int matcherId, int adderId, int rule, int flags) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getTargetBunchColumnIndex(), targetBunch)
                .put(table.getSourceBunchSetColumnIndex(), sourceBunchSetId)
                .put(table.getDiffBunchSetColumnIndex(), diffBunchSetId)
                .put(table.getMatcherColumnIndex(), matcherId)
                .put(table.getAdderColumnIndex(), adderId)
                .put(table.getRuleColumnIndex(), rule)
                .put(table.getFlagsColumnIndex(), flags)
                .build();
        return db.insert(query);
    }

    public static void insertAgentSet(DbInserter db, int setId, IntSet agentSet) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
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

    public static void insertRuledConcept(DbInserter db, int ruledConcept, int agent, int baseConcept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledConcept)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getConceptColumnIndex(), baseConcept)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    public static void insertRuledAcceptation(DbInserter db, int ruledAcceptation, int agent, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), ruledAcceptation)
                .put(table.getAgentColumnIndex(), agent)
                .put(table.getAcceptationColumnIndex(), baseAcceptation)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }
}
