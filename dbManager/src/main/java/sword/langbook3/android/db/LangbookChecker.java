package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.SearchResult;

public interface LangbookChecker<LanguageId, AlphabetId> extends QuizzesChecker<LanguageId, AlphabetId>, DefinitionsChecker, SentencesChecker<LanguageId, AlphabetId> {
    ImmutableSet<String> findConversionConflictWords(ConversionProposal<AlphabetId> newConversion);
    AcceptationDetailsModel<AlphabetId> getAcceptationsDetails(int staticAcceptation, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult> getSearchHistory();

    default boolean allValidAlphabets(Correlation<AlphabetId> texts) {
        final ImmutableSet<LanguageId> languages = texts.keySet().map(this::getLanguageFromAlphabet).toSet().toImmutable();
        return !languages.contains(null) && languages.size() == 1;
    }
}
