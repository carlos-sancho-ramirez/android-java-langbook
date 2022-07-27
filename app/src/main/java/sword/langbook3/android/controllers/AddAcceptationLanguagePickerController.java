package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

public final class AddAcceptationLanguagePickerController extends AbstractLanguagePickerController {

    private final String _searchQuery;

    public AddAcceptationLanguagePickerController(String searchQuery) {
        _searchQuery = searchQuery;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_searchQuery);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller = new AddAcceptationWordEditorController(language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
    }

    public static final Creator<AddAcceptationLanguagePickerController> CREATOR = new Creator<AddAcceptationLanguagePickerController>() {

        @Override
        public AddAcceptationLanguagePickerController createFromParcel(Parcel source) {
            final String searchQuery = source.readString();
            return new AddAcceptationLanguagePickerController(searchQuery);
        }

        @Override
        public AddAcceptationLanguagePickerController[] newArray(int size) {
            return new AddAcceptationLanguagePickerController[size];
        }
    };
}
