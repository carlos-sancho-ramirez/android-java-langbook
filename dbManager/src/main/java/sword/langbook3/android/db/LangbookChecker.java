package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.SearchResult;

public interface LangbookChecker extends QuizzesChecker, DefinitionsChecker, SentencesChecker {
    ImmutableSet<String> findConversionConflictWords(ConversionProposal newConversion);
    AcceptationDetailsModel getAcceptationsDetails(int staticAcceptation, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult> getSearchHistory();

    default boolean allValidAlphabets(Correlation texts) {
        final ImmutableSet<Integer> languages = texts.toImmutable().keySet().map(this::getLanguageFromAlphabet).toSet();
        return !languages.anyMatch(lang -> lang == null) && languages.size() == 1;
    }
}
