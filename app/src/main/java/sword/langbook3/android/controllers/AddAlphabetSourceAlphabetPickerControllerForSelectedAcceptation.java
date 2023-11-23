package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.SourceAlphabetPickerActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation implements SourceAlphabetPickerActivityDelegate.Controller {

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final AlphabetId _targetAlphabet;

    public AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation(@NonNull LanguageId language, @NonNull AlphabetId targetAlphabet) {
        ensureNonNull(language, targetAlphabet);
        _language = language;
        _targetAlphabet = targetAlphabet;
    }

    @Override
    public void load(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<AlphabetId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        procedure.apply(checker.readAlphabetsForLanguage(_language, preferredAlphabet));
    }

    private void addAlphabetCopyingFromSource(@NonNull Presenter presenter, @NonNull AlphabetId sourceAlphabet) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final boolean ok = manager.addAlphabetCopyingFromOther(_targetAlphabet, sourceAlphabet);
        presenter.displayFeedback(ok? R.string.includeAlphabetFeedback : R.string.includeAlphabetKo);

        if (ok) {
            presenter.finish();
        }
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull AlphabetId sourceAlphabet, @NonNull CreationOption creationOption) {
        if (creationOption == CreationOption.DEFINE_CONVERSION) {
            presenter.openConversionEditor(SourceAlphabetPickerActivityDelegate.REQUEST_CODE_NEW_CONVERSION, new AddAlphabetConversionEditorControllerForSelectedAcceptation(sourceAlphabet, _targetAlphabet));
        }
        else {
            addAlphabetCopyingFromSource(presenter, sourceAlphabet);
        }
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == SourceAlphabetPickerActivityDelegate.REQUEST_CODE_NEW_CONVERSION && resultCode == Activity.RESULT_OK) {
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
        AlphabetIdParceler.write(dest, _targetAlphabet);
    }

    public static final Parcelable.Creator<AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation> CREATOR = new Parcelable.Creator<AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation>() {

        @Override
        public AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final AlphabetId targetAlphabet = AlphabetIdParceler.read(source);
            return new AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation(language, targetAlphabet);
        }

        @Override
        public AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation[] newArray(int size) {
            return new AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation[size];
        }
    };
}
