package sword.langbook3.android.db;

import sword.database.Database;
import sword.database.DbDeleteQuery;
import sword.database.Deleter;
import sword.langbook3.android.collections.ImmutableIntPair;

final class LangbookDeleter {

    private LangbookDeleter() {
    }

    static boolean deleteSymbolArray(Deleter db, int id) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();
        return db.delete(query);
    }

    static boolean deleteCorrelation(Deleter db, int correlationId) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getCorrelationIdColumnIndex(), correlationId)
                .build();

        return db.delete(query);
    }

    static boolean deleteCorrelationArray(Deleter db, int correlationArrayId) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getArrayIdColumnIndex(), correlationArrayId)
                .build();

        return db.delete(query);
    }

    static boolean deleteAcceptation(Deleter db, int acceptation) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteAgent(Deleter db, int id) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();

        return db.delete(query);
    }

    static boolean deleteComplementedConcept(Deleter db, int complementedConcept) {
        final LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), complementedConcept)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunch(Deleter db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptation(Deleter db, int bunch, int acceptation, int agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .where(table.getAgentColumnIndex(), agent)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptationsByAgent(Deleter db, int agentId) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptationsByAgentAndBunch(Deleter db, int agentId, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .where(table.getBunchColumnIndex(), bunch)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptationsByAgentAndAcceptation(Deleter db, int agentId, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchSet(Deleter db, int setId) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .build();

        return db.delete(query);
    }

    static boolean deleteRuledAcceptation(Deleter db, int id) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();
        return db.delete(query);
    }

    static boolean deleteRuledAcceptationByAgent(Deleter db, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .build();
        return db.delete(query);
    }

    static boolean deleteStringQueriesForDynamicAcceptation(Deleter db, int dynamicAcceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();
        return db.delete(query);
    }

    static boolean deleteKnowledge(Deleter db, int acceptation) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteKnowledge(Deleter db, int quizId, int acceptation) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteKnowledgeForQuiz(Deleter db, int quizId) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .build();

        return db.delete(query);
    }

    static boolean deleteQuiz(Deleter db, int id) {
        final LangbookDbSchema.QuizDefinitionsTable table = LangbookDbSchema.Tables.quizDefinitions;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();

        return db.delete(query);
    }

    static boolean deleteSearchHistoryForAcceptation(Deleter db, int acceptationId) {
        final LangbookDbSchema.SearchHistoryTable table = LangbookDbSchema.Tables.searchHistory;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAcceptation(), acceptationId)
                .build();

        return db.delete(query);
    }

    static boolean deleteSpan(Deleter db, int id) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .build();

        return db.delete(query);
    }

    static boolean deleteSentence(Deleter db, int sentenceId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .build();
        return db.delete(query);
    }

    static boolean deleteSpansBySentenceId(Deleter db, int sentenceId) {
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(spans)
                .where(spans.getSentenceIdColumnIndex(), sentenceId)
                .build();
        return db.delete(query);
    }

    static boolean deleteSpansByDynamicAcceptation(Deleter db, int dynamicAcceptation) {
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(spans)
                .where(spans.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();
        return db.delete(query);
    }

    static boolean deleteConversionRegister(Deleter db, ImmutableIntPair alphabets, int sourceSymbolArrayId, int targetSymbolArrayId) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getSourceAlphabetColumnIndex(), alphabets.left)
                .where(table.getTargetAlphabetColumnIndex(), alphabets.right)
                .where(table.getSourceColumnIndex(), sourceSymbolArrayId)
                .where(table.getTargetColumnIndex(), targetSymbolArrayId)
                .build();
        return db.delete(query);
    }

    static boolean deleteConversion(Database db, int sourceAlphabet, int targetAlphabet) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .where(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .build();
        return db.delete(query);
    }

    static boolean deleteLanguage(Database db, int language) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), language)
                .build();
        return db.delete(query);
    }

    static boolean deleteAlphabetsForLanguage(Database db, int language) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getLanguageColumnIndex(), language)
                .build();
        return db.delete(query);
    }

    static boolean deleteAlphabet(Database db, int alphabet) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .build();
        return db.delete(query);
    }

    static boolean deleteAlphabetFromCorrelations(Database db, int alphabet) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getAlphabetColumnIndex(), alphabet)
                .build();
        return db.delete(query);
    }

    static boolean deleteAlphabetFromStringQueries(Database db, int alphabet) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), alphabet)
                .build();
        return db.delete(query);
    }
}
