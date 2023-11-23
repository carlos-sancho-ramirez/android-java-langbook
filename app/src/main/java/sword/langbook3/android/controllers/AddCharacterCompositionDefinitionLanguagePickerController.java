package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

public final class AddCharacterCompositionDefinitionLanguagePickerController extends AbstractLanguagePickerController {

    private final String _searchQuery;

    public AddCharacterCompositionDefinitionLanguagePickerController(String searchQuery) {
        _searchQuery = searchQuery;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_searchQuery);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivityDelegate.Controller controller = new AddCharacterCompositionDefinitionWordEditorController(language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
    }

    public static final Creator<AddCharacterCompositionDefinitionLanguagePickerController> CREATOR = new Creator<AddCharacterCompositionDefinitionLanguagePickerController>() {

        @Override
        public AddCharacterCompositionDefinitionLanguagePickerController createFromParcel(Parcel source) {
            final String searchQuery = source.readString();
            return new AddCharacterCompositionDefinitionLanguagePickerController(searchQuery);
        }

        @Override
        public AddCharacterCompositionDefinitionLanguagePickerController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionLanguagePickerController[size];
        }
    };
}
