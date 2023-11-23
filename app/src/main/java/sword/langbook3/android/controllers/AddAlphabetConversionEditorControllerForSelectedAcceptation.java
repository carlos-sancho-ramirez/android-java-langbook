package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Procedure;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.ConversionEditorActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.presenters.Presenter;

public final class AddAlphabetConversionEditorControllerForSelectedAcceptation implements ConversionEditorActivityDelegate.Controller {

    @NonNull
    private final AlphabetId _sourceAlphabet;

    @NonNull
    private final AlphabetId _targetAlphabet;

    public AddAlphabetConversionEditorControllerForSelectedAcceptation(@NonNull AlphabetId sourceAlphabet, @NonNull AlphabetId targetAlphabet) {
        ensureNonNull(sourceAlphabet);
        ensureValidArguments(!sourceAlphabet.equals(targetAlphabet));

        _sourceAlphabet = sourceAlphabet;
        _targetAlphabet = targetAlphabet;
    }

    private boolean checkConflicts(@NonNull Presenter presenter, @NonNull LangbookDbChecker checker, ConversionProposal<AlphabetId> newConversion) {
        final ImmutableSet<String> wordsInConflict = checker.findConversionConflictWords(newConversion);

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
        final String targetText = checker.readConceptText(_targetAlphabet.getConceptId(), preferredAlphabet);
        presenter.setTitle(R.string.conversionEditorTitle, sourceText, targetText);

        procedure.apply(checker.getConversion(new ImmutablePair<>(_sourceAlphabet, _targetAlphabet)));
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Conversion<AlphabetId> conversion) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        if (checkConflicts(presenter, manager, conversion)) {
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
        AlphabetIdParceler.write(dest, _sourceAlphabet);
        AlphabetIdParceler.write(dest, _targetAlphabet);
    }

    public static final Creator<AddAlphabetConversionEditorControllerForSelectedAcceptation> CREATOR = new Creator<AddAlphabetConversionEditorControllerForSelectedAcceptation>() {

        @Override
        public AddAlphabetConversionEditorControllerForSelectedAcceptation createFromParcel(Parcel source) {
            final AlphabetId sourceAlphabet = AlphabetIdParceler.read(source);
            final AlphabetId targetAlphabet = AlphabetIdParceler.read(source);
            return new AddAlphabetConversionEditorControllerForSelectedAcceptation(sourceAlphabet, targetAlphabet);
        }

        @Override
        public AddAlphabetConversionEditorControllerForSelectedAcceptation[] newArray(int size) {
            return new AddAlphabetConversionEditorControllerForSelectedAcceptation[size];
        }
    };
}
