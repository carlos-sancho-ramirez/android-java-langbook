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
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAlphabetLanguagePickerController implements LanguagePickerActivity.Controller, Fireable {

    @NonNull
    private final LanguageId _alphabetLanguage;
    private final String _searchQuery;

    public AddAlphabetLanguagePickerController(@NonNull LanguageId alphabetLanguage, String searchQuery) {
        ensureNonNull(alphabetLanguage);
        _alphabetLanguage = alphabetLanguage;
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

    private void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller = new AddAlphabetWordEditorController(_alphabetLanguage,language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull LanguageId language) {
        complete(presenter, LanguagePickerActivity.REQUEST_CODE_NEW_WORD, language);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguagePickerActivity.REQUEST_CODE_NEW_WORD && resultCode == Activity.RESULT_OK) {
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
        LanguageIdParceler.write(dest, _alphabetLanguage);
        dest.writeString(_searchQuery);
    }

    public static final Creator<AddAlphabetLanguagePickerController> CREATOR = new Creator<AddAlphabetLanguagePickerController>() {

        @Override
        public AddAlphabetLanguagePickerController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final String searchQuery = source.readString();
            return new AddAlphabetLanguagePickerController(language, searchQuery);
        }

        @Override
        public AddAlphabetLanguagePickerController[] newArray(int size) {
            return new AddAlphabetLanguagePickerController[size];
        }
    };
}
