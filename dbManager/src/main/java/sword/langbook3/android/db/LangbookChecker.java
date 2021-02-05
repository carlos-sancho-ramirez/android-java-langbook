package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.SearchResult;

public interface LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> extends QuizzesChecker<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId>, DefinitionsChecker, SentencesChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, AcceptationId> {
    ImmutableSet<String> findConversionConflictWords(ConversionProposal<AlphabetId> newConversion);
    AcceptationDetailsModel<LanguageId, AlphabetId, CorrelationId, AcceptationId> getAcceptationsDetails(AcceptationId staticAcceptation, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult<AcceptationId>> getSearchHistory();

    default boolean allValidAlphabets(Correlation<AlphabetId> texts) {
        final ImmutableSet<LanguageId> languages = texts.keySet().map(this::getLanguageFromAlphabet).toSet().toImmutable();
        return !languages.contains(null) && languages.size() == 1;
    }
}
