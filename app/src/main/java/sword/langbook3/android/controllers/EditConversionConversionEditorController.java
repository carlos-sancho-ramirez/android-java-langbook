package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Procedure;
import sword.langbook3.android.ConversionEditorActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class EditConversionConversionEditorController implements ConversionEditorActivity.Controller {

    @NonNull
    private final AlphabetId _sourceAlphabet;

    @NonNull
    private final AlphabetId _targetAlphabet;

    public EditConversionConversionEditorController(@NonNull AlphabetId sourceAlphabet, @NonNull AlphabetId targetAlphabet) {
        ensureNonNull(sourceAlphabet);
        ensureValidArguments(!sourceAlphabet.equals(targetAlphabet));
        ensureValidArguments(DbManager.getInstance().getManager().getLanguageFromAlphabet(sourceAlphabet).equals(DbManager.getInstance().getManager().getLanguageFromAlphabet(targetAlphabet)));

        _sourceAlphabet = sourceAlphabet;
        _targetAlphabet = targetAlphabet;
    }

    private boolean checkConflicts(@NonNull Presenter presenter, @NonNull LangbookDbChecker checker, ConversionProposal<AlphabetId> newConversion) {
        final ImmutableSet<String> wordsInConflict = checker.findConversionConflictWords(newConversion);

        if (wordsInConflict.isEmpty()) {
            return true;
        }
        else {
            final String firstWord = wordsInConflict.valueAt(0);
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
        presenter.setTitle(sourceText + " -> " + targetText);

        procedure.apply(checker.getConversion(new ImmutablePair<>(_sourceAlphabet, _targetAlphabet)));
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Conversion<AlphabetId> conversion) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        if (checkConflicts(presenter, manager, conversion)) {
            if (manager.replaceConversion(conversion)) {
                presenter.displayFeedback(R.string.updateConversionFeedback);
                presenter.finish();
            }
            else {
                throw new AssertionError("Unexpected");
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

    public static final Creator<EditConversionConversionEditorController> CREATOR = new Creator<EditConversionConversionEditorController>() {

        @Override
        public EditConversionConversionEditorController createFromParcel(Parcel source) {
            final AlphabetId sourceAlphabet = AlphabetIdParceler.read(source);
            final AlphabetId targetAlphabet = AlphabetIdParceler.read(source);
            return new EditConversionConversionEditorController(sourceAlphabet, targetAlphabet);
        }

        @Override
        public EditConversionConversionEditorController[] newArray(int size) {
            return new EditConversionConversionEditorController[size];
        }
    };
}
