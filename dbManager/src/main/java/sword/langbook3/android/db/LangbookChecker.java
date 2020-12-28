package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.SearchResult;

public interface LangbookChecker<AlphabetId> extends QuizzesChecker<AlphabetId>, DefinitionsChecker, SentencesChecker<AlphabetId> {
    ImmutableSet<String> findConversionConflictWords(ConversionProposal<AlphabetId> newConversion);
    AcceptationDetailsModel<AlphabetId> getAcceptationsDetails(int staticAcceptation, AlphabetId preferredAlphabet);
    ImmutableList<SearchResult> getSearchHistory();

    default boolean allValidAlphabets(Correlation<AlphabetId> texts) {
        final ImmutableSet<Integer> languages = texts.toImmutable().keySet().map(this::getLanguageFromAlphabet).toSet();
        return !languages.anyMatch(lang -> lang == null) && languages.size() == 1;
    }
}
