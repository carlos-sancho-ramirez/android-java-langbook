package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchLanguagePickerController extends AbstractLanguagePickerController {

    @NonNull
    private final BunchId _bunch;
    private final String _searchQuery;

    public AddAcceptationInBunchLanguagePickerController(@NonNull BunchId bunch, String searchQuery) {
        ensureNonNull(bunch);
        _bunch = bunch;
        _searchQuery = searchQuery;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _bunch);
        dest.writeString(_searchQuery);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller = new AddAcceptationInBunchWordEditorController(_bunch, language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
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
