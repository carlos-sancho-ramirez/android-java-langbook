package sword.langbook3.android.db;

import sword.collections.ImmutablePair;
import sword.database.Database;
import sword.database.DbDeleteQuery;
import sword.database.Deleter;

final class LangbookDeleter {

    private LangbookDeleter() {
    }

    static boolean deleteSymbolArray(Deleter db, SymbolArrayIdInterface id) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), id)
                .build();
        return db.delete(query);
    }

    static boolean deleteCorrelation(Deleter db, CorrelationIdInterface correlationId) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getCorrelationIdColumnIndex(), correlationId)
                .build();

        return db.delete(query);
    }

    static boolean deleteCorrelationArray(Deleter db, CorrelationArrayIdInterface correlationArrayId) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getArrayIdColumnIndex(), correlationArrayId)
                .build();

        return db.delete(query);
    }

    static boolean deleteAcceptation(Deleter db, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteAgent(Deleter db, AgentIdInterface id) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), id)
                .build();

        return db.delete(query);
    }

    static boolean deleteComplementedConcept(Deleter db, ConceptIdInterface complementedConcept) {
        final LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), complementedConcept)
                .build();

        return db.delete(query);
    }

    static <ConceptId> boolean deleteBunch(Deleter db, BunchIdInterface<ConceptId> bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .build();

        return db.delete(query);
    }

    static <ConceptId> boolean deleteBunchAcceptation(Deleter db, BunchIdInterface<ConceptId> bunch, AcceptationIdInterface acceptation, AgentIdInterface agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .where(table.getAgentColumnIndex(), agent)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptationsByAgent(Deleter db, AgentIdInterface agentId) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptationsByAcceptation(Deleter db, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static <ConceptId> boolean deleteBunchAcceptationsByAgentAndBunch(Deleter db, AgentIdInterface agentId, BunchIdInterface<ConceptId> bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .where(table.getBunchColumnIndex(), bunch)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchAcceptationsByAgentAndAcceptation(Deleter db, AgentIdInterface agentId, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchSet(Deleter db, BunchSetIdInterface setId) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .build();

        return db.delete(query);
    }

    static boolean deleteBunchSetBunch(Deleter db, BunchSetIdInterface setId, BunchIdInterface<?> bunch) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .where(table.getBunchColumnIndex(), bunch)
                .build();

        return db.delete(query);
    }

    static boolean deleteRuledConcept(Deleter db, ConceptIdInterface id) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), id)
                .build();
        return db.delete(query);
    }

    static boolean deleteRuledAcceptation(Deleter db, AcceptationIdInterface id) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), id)
                .build();
        return db.delete(query);
    }

    static boolean deleteRuledAcceptationByAgent(Deleter db, AgentIdInterface agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .build();
        return db.delete(query);
    }

    static boolean deleteStringQueriesForDynamicAcceptation(Deleter db, AcceptationIdInterface dynamicAcceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();
        return db.delete(query);
    }

    static boolean deleteKnowledge(Deleter db, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteKnowledge(Deleter db, QuizIdInterface quizId, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .build();

        return db.delete(query);
    }

    static boolean deleteKnowledgeForQuiz(Deleter db, QuizIdInterface quizId) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .build();

        return db.delete(query);
    }

    static boolean deleteQuiz(Deleter db, QuizIdInterface id) {
        final LangbookDbSchema.QuizDefinitionsTable table = LangbookDbSchema.Tables.quizDefinitions;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), id)
                .build();

        return db.delete(query);
    }

    static boolean deleteSearchHistoryForAcceptation(Deleter db, AcceptationIdInterface acceptationId) {
        final LangbookDbSchema.SearchHistoryTable table = LangbookDbSchema.Tables.searchHistory;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
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

    static boolean deleteSentence(Deleter db, SentenceIdInterface sentenceId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .build();
        return db.delete(query);
    }

    static boolean deleteSpansBySentenceId(Deleter db, SentenceIdInterface sentenceId) {
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(spans)
                .where(spans.getSentenceIdColumnIndex(), sentenceId)
                .build();
        return db.delete(query);
    }

    static boolean deleteSpansByDynamicAcceptation(Deleter db, AcceptationIdInterface dynamicAcceptation) {
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(spans)
                .where(spans.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();
        return db.delete(query);
    }

    static boolean deleteSpanBySentenceAndDynamicAcceptation(Deleter db, SentenceIdInterface sentence, AcceptationIdInterface dynamicAcceptation) {
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(spans)
                .where(spans.getSentenceIdColumnIndex(), sentence)
                .where(spans.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .build();
        return db.delete(query);
    }

    static <ConceptId> boolean deleteRuleSentenceMatch(Deleter db, RuleIdInterface<ConceptId> rule, SentenceIdInterface sentenceId) {
        final LangbookDbSchema.RuleSentenceMatchesTable matches = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(matches)
                .where(matches.getRuleColumnIndex(), rule)
                .where(matches.getSentenceColumnIndex(), sentenceId)
                .build();
        return db.delete(query);
    }

    static boolean deleteRuleSentenceMatchesBySentenceId(Deleter db, SentenceIdInterface sentenceId) {
        final LangbookDbSchema.RuleSentenceMatchesTable matches = LangbookDbSchema.Tables.ruleSentenceMatches;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(matches)
                .where(matches.getSentenceColumnIndex(), sentenceId)
                .build();
        return db.delete(query);
    }

    static <ConceptId, AlphabetId extends AlphabetIdInterface<ConceptId>> boolean deleteConversionRegister(Deleter db, ImmutablePair<AlphabetId, AlphabetId> alphabets, SymbolArrayIdInterface sourceSymbolArrayId, SymbolArrayIdInterface targetSymbolArrayId) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getSourceAlphabetColumnIndex(), alphabets.left)
                .where(table.getTargetAlphabetColumnIndex(), alphabets.right)
                .where(table.getSourceColumnIndex(), sourceSymbolArrayId)
                .where(table.getTargetColumnIndex(), targetSymbolArrayId)
                .build();
        return db.delete(query);
    }

    static <ConceptId, AlphabetId extends AlphabetIdInterface<ConceptId>> boolean deleteConversion(Database db, AlphabetId sourceAlphabet, AlphabetId targetAlphabet) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .where(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .build();
        return db.delete(query);
    }

    static <ConceptId, LanguageId extends LanguageIdInterface<ConceptId>> boolean deleteLanguage(Database db, LanguageId language) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), language)
                .build();

        return db.delete(query);
    }

    static <ConceptId, LanguageId extends LanguageIdInterface<ConceptId>> boolean deleteAlphabetsForLanguage(Database db, LanguageId language) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getLanguageColumnIndex(), language)
                .build();
        return db.delete(query);
    }

    static <ConceptId, AlphabetId extends AlphabetIdInterface<ConceptId>> boolean deleteAlphabet(Database db, AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .build();
        return db.delete(query);
    }

    static <ConceptId, AlphabetId extends AlphabetIdInterface<ConceptId>> boolean deleteAlphabetFromCorrelations(Database db, AlphabetId alphabet) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getAlphabetColumnIndex(), alphabet)
                .build();
        return db.delete(query);
    }

    static <ConceptId, AlphabetId extends AlphabetIdInterface<ConceptId>> boolean deleteAlphabetFromStringQueries(Database db, AlphabetId alphabet) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), alphabet)
                .build();
        return db.delete(query);
    }

    static <CharacterId extends CharacterIdInterface> boolean deleteCharacterComposition(Database db, CharacterId characterId) {
        final LangbookDbSchema.CharacterCompositionsTable table = LangbookDbSchema.Tables.characterCompositions;
        final DbDeleteQuery query = new DbDeleteQueryBuilder(table)
                .where(table.getIdColumnIndex(), characterId)
                .build();
        return db.delete(query);
    }
}
