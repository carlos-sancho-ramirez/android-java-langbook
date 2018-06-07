package sword.langbook3.android;

import sword.langbook3.android.db.DbDeleteQuery;
import sword.langbook3.android.db.Deleter;

public final class LangbookDeleter {

    private final Deleter db;

    public LangbookDeleter(Deleter db) {
        this.db = db;
    }

    public static boolean deleteAgent(Deleter db, int id) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();

        return db.delete(query);
    }

    public static boolean deleteAgentSet(Deleter db, int setId) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .build();

        return db.delete(query);
    }

    public static boolean deleteBunchAcceptationsForAgentSet(Deleter db, int agentSetId) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAgentSetColumnIndex(), agentSetId)
                .build();

        return db.delete(query);
    }

    public static boolean deleteRuledAcceptation(Deleter db, int id) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();
        return db.delete(query);
    }

    public static boolean deleteStringQueriesForDynamicAcceptation(Deleter db, int dynamicAcceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();
        return db.delete(query);
    }
}