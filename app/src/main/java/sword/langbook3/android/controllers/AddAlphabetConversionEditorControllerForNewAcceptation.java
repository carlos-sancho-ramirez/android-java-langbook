package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.ConversionEditorActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdSetParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddAlphabetConversionEditorControllerForNewAcceptation implements ConversionEditorActivity.Controller {

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _targetAlphabetAcceptationCorrelationArray;

    @NonNull
    private final Set<BunchId> _targetAlphabetAcceptationBunches;

    @NonNull
    private final AlphabetId _sourceAlphabet;

    public AddAlphabetConversionEditorControllerForNewAcceptation(
            @NonNull LanguageId language,
            @NonNull ImmutableCorrelationArray<AlphabetId> targetAlphabetAcceptationCorrelationArray,
            @NonNull Set<BunchId> targetAlphabetAcceptationBunches,
            @NonNull AlphabetId sourceAlphabet) {
        ensureNonNull(language, targetAlphabetAcceptationCorrelationArray, targetAlphabetAcceptationBunches, sourceAlphabet);
        ensureValidArguments(!targetAlphabetAcceptationCorrelationArray.isEmpty());
        _language = language;
        _targetAlphabetAcceptationCorrelationArray = targetAlphabetAcceptationCorrelationArray;
        _targetAlphabetAcceptationBunches = targetAlphabetAcceptationBunches;
        _sourceAlphabet = sourceAlphabet;
    }

    private boolean checkConflicts(@NonNull Presenter presenter, @NonNull LangbookDbChecker checker, ConversionProposal<AlphabetId> newConversion) {
        ImmutableSet<String> wordsInConflict = checker.findConversionConflictWords(newConversion);
        final ImmutableCorrelation<AlphabetId> correlation = _targetAlphabetAcceptationCorrelationArray.concatenateTexts();
        if (correlation.containsKey(_sourceAlphabet)) {
            final String sourceWord = correlation.get(_sourceAlphabet);
            if (newConversion.convert(sourceWord) == null) {
                wordsInConflict = wordsInConflict.add(sourceWord);
            }
        }

        if (wordsInConflict.isEmpty()) {
            return true;
        }
        else {
            final String firstWord = wordsInConflict.first();
            if (wordsInConflict.size() == 1) {
                presenter.displayFeedback(R.string.unableToConvertOneWord, firstWord);
            }
            else if (wordsInConflict.size() == 2) {
                presenter.displayFeedback(R.string.unableToConvertTwoWords, firstWord, wordsInConflict.valueAt(1));
            }
            else {
                presenter.displayFeedback(R.string.unableToConvertSeveralWords, firstWord, "" + (wordsInConflict.size() - 1));
            }

            return false;
        }
    }

    @Override
    public void load(@NonNull Presenter presenter, @NonNull Procedure<Conversion<AlphabetId>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();

        final String sourceText = checker.readConceptText(_sourceAlphabet.getConceptId(), preferredAlphabet);

        final StringBuilder sb = new StringBuilder();
        for (ImmutableMap<AlphabetId, String> corr : _targetAlphabetAcceptationCorrelationArray) {
            sb.append(corr.containsKey(preferredAlphabet)?
                    corr.get(preferredAlphabet) : corr.valueAt(0));
        }
        presenter.setTitle(R.string.conversionEditorTitle, sourceText, sb.toString());

        final AlphabetId targetSuggestionAlphabet = AlphabetIdManager.conceptAsAlphabetId(checker.getNextAvailableConceptId());
        procedure.apply(new Conversion<>(_sourceAlphabet, targetSuggestionAlphabet, ImmutableHashMap.empty()));
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Conversion<AlphabetId> conversion) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        if (checkConflicts(presenter, manager, conversion)) {
            final ConceptId concept = manager.getNextAvailableConceptId();
            ensureValidArguments(concept.equals(conversion.getTargetAlphabet().getConceptId()));
            final AcceptationId targetAlphabetAcceptation = manager.addAcceptation(concept, _targetAlphabetAcceptationCorrelationArray);

            for (BunchId bunch : _targetAlphabetAcceptationBunches) {
                manager.addAcceptationInBunch(bunch, targetAlphabetAcceptation);
            }

            final boolean ok = DbManager.getInstance().getManager().addAlphabetAsConversionTarget(conversion);
            presenter.displayFeedback(ok? R.string.includeAlphabetFeedback : R.string.includeAlphabetKo);

            if (ok) {
                presenter.finish();
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _language);
        CorrelationArrayParceler.write(dest, _targetAlphabetAcceptationCorrelationArray);
        BunchIdSetParceler.write(dest, _targetAlphabetAcceptationBunches);
        AlphabetIdParceler.write(dest, _sourceAlphabet);
    }

    public static final Creator<AddAlphabetConversionEditorControllerForNewAcceptation> CREATOR = new Creator<AddAlphabetConversionEditorControllerForNewAcceptation>() {

        @Override
        public AddAlphabetConversionEditorControllerForNewAcceptation createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            final Set<BunchId> bunches = BunchIdSetParceler.read(source);
            final AlphabetId sourceAlphabet = AlphabetIdParceler.read(source);
            return new AddAlphabetConversionEditorControllerForNewAcceptation(language, correlationArray, bunches, sourceAlphabet);
        }

        @Override
        public AddAlphabetConversionEditorControllerForNewAcceptation[] newArray(int size) {
            return new AddAlphabetConversionEditorControllerForNewAcceptation[size];
        }
    };
}
