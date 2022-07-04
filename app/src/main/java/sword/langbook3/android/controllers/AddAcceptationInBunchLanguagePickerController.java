package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchLanguagePickerController implements LanguagePickerActivity.Controller, Fireable {

    @NonNull
    private final BunchId _bunch;
    private final String _searchQuery;

    public AddAcceptationInBunchLanguagePickerController(@NonNull BunchId bunch, String searchQuery) {
        ensureNonNull(bunch);
        _bunch = bunch;
        _searchQuery = searchQuery;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimized, as we are only interested in checking if there is just one language or not.
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<LanguageId, String> languages = DbManager.getInstance().getManager().readAllLanguages(preferredAlphabet);

        if (languages.size() == 1) {
            complete(presenter, requestCode, languages.keyAt(0));
        }
        else {
            presenter.openLanguagePicker(requestCode, this);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _bunch);
        dest.writeString(_searchQuery);
    }

    private void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller = new AddAcceptationInBunchWordEditorController(_bunch, language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull LanguageId language) {
        complete(presenter, LanguagePickerActivity.REQUEST_CODE_NEW_WORD, language);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguagePickerActivity.REQUEST_CODE_NEW_WORD && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    public static final Creator<AddAcceptationInBunchLanguagePickerController> CREATOR = new Creator<AddAcceptationInBunchLanguagePickerController>() {

        @Override
        public AddAcceptationInBunchLanguagePickerController createFromParcel(Parcel source) {
            final BunchId bunch = BunchIdParceler.read(source);
            final String searchQuery = source.readString();
            return new AddAcceptationInBunchLanguagePickerController(bunch, searchQuery);
        }

        @Override
        public AddAcceptationInBunchLanguagePickerController[] newArray(int size) {
            return new AddAcceptationInBunchLanguagePickerController[size];
        }
    };
}
