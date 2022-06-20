package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.SourceAlphabetPickerActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdSetParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddAlphabetSourceAlphabetPickerControllerForNewAcceptation implements SourceAlphabetPickerActivity.Controller {

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _targetAlphabetAcceptationCorrelationArray;

    @NonNull
    private final Set<BunchId> _targetAlphabetAcceptationBunches;

    public AddAlphabetSourceAlphabetPickerControllerForNewAcceptation(
            @NonNull LanguageId language,
            @NonNull ImmutableCorrelationArray<AlphabetId> targetAlphabetAcceptationCorrelationArray,
            @NonNull Set<BunchId> targetAlphabetAcceptationBunches) {
        ensureNonNull(language, targetAlphabetAcceptationCorrelationArray, targetAlphabetAcceptationBunches);
        ensureValidArguments(!targetAlphabetAcceptationCorrelationArray.isEmpty());
        _language = language;
        _targetAlphabetAcceptationCorrelationArray = targetAlphabetAcceptationCorrelationArray;
        _targetAlphabetAcceptationBunches = targetAlphabetAcceptationBunches;
    }

    @Override
    public void load(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<AlphabetId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        procedure.apply(checker.readAlphabetsForLanguage(_language, preferredAlphabet));
    }

    @Override
    public void complete(
            @NonNull Presenter presenter, @NonNull AlphabetId sourceAlphabet, @NonNull CreationOption creationOption) {

        if (creationOption == CreationOption.DEFINE_CONVERSION) {
            presenter.openConversionEditor(SourceAlphabetPickerActivity.REQUEST_CODE_NEW_CONVERSION, new AddAlphabetConversionEditorControllerForNewAcceptation(_language, _targetAlphabetAcceptationCorrelationArray, _targetAlphabetAcceptationBunches, sourceAlphabet));
        }
        else {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId targetAlphabetAcceptation = manager.addAcceptation(concept, _targetAlphabetAcceptationCorrelationArray);

            for (BunchId bunch : _targetAlphabetAcceptationBunches) {
                manager.addAcceptationInBunch(bunch, targetAlphabetAcceptation);
            }

            final AlphabetId targetAlphabet = AlphabetIdManager.conceptAsAlphabetId(concept);
            final boolean ok = manager.addAlphabetCopyingFromOther(targetAlphabet, sourceAlphabet);
            presenter.displayFeedback(ok? R.string.includeAlphabetFeedback : R.string.includeAlphabetKo);

            if (ok) {
                presenter.finish();
            }
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == SourceAlphabetPickerActivity.REQUEST_CODE_NEW_CONVERSION && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
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
    }

    public static final Creator<AddAlphabetSourceAlphabetPickerControllerForNewAcceptation> CREATOR = new Creator<AddAlphabetSourceAlphabetPickerControllerForNewAcceptation>() {

        @Override
        public AddAlphabetSourceAlphabetPickerControllerForNewAcceptation createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            final Set<BunchId> bunches = BunchIdSetParceler.read(source);
            return new AddAlphabetSourceAlphabetPickerControllerForNewAcceptation(language, correlationArray, bunches);
        }

        @Override
        public AddAlphabetSourceAlphabetPickerControllerForNewAcceptation[] newArray(int size) {
            return new AddAlphabetSourceAlphabetPickerControllerForNewAcceptation[size];
        }
    };
}
