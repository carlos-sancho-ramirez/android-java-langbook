package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAlphabetLanguagePickerController extends AbstractLanguagePickerController {

    @NonNull
    private final LanguageId _alphabetLanguage;
    private final String _searchQuery;

    public AddAlphabetLanguagePickerController(@NonNull LanguageId alphabetLanguage, String searchQuery) {
        ensureNonNull(alphabetLanguage);
        _alphabetLanguage = alphabetLanguage;
        _searchQuery = searchQuery;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller = new AddAlphabetWordEditorController(_alphabetLanguage,language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
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
