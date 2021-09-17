package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.SearchResult;

public interface LangbookChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, QuizId, SentenceId> extends QuizzesChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId>, DefinitionsChecker<ConceptId>, RuledSentencesChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, SentenceId> {
    ImmutableSet<String> findConversionConflictWords(ConversionProposal<AlphabetId> newConversion);
    AcceptationDetailsModel<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> getAcceptationsDetails(AcceptationId staticAcceptation, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult<AcceptationId, RuleId>> getSearchHistory();

    default boolean allValidAlphabets(Correlation<AlphabetId> texts) {
        final ImmutableSet<LanguageId> languages = texts.keySet().map(this::getLanguageFromAlphabet).toSet().toImmutable();
        return !languages.contains(null) && languages.size() == 1;
    }
}
