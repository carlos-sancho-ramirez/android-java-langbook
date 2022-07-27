package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

public final class PickConceptLanguagePickerController extends AbstractLanguagePickerController {

    private final String _searchQuery;

    public PickConceptLanguagePickerController(String searchQuery) {
        _searchQuery = searchQuery;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_searchQuery);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        presenter.openWordEditor(requestCode, new PickConceptWordEditorController(language, _searchQuery));
    }

    public static final Creator<PickConceptLanguagePickerController> CREATOR = new Creator<PickConceptLanguagePickerController>() {

        @Override
        public PickConceptLanguagePickerController createFromParcel(Parcel source) {
            final String searchQuery = source.readString();
            return new PickConceptLanguagePickerController(searchQuery);
        }

        @Override
        public PickConceptLanguagePickerController[] newArray(int size) {
            return new PickConceptLanguagePickerController[size];
        }
    };
}
