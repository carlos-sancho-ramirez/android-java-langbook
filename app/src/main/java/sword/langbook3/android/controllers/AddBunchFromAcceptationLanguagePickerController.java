package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

public final class AddBunchFromAcceptationLanguagePickerController extends AbstractLanguagePickerController {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    private final String _searchQuery;

    public AddBunchFromAcceptationLanguagePickerController(@NonNull AcceptationId acceptationToBeIncluded, String searchQuery) {
        ensureNonNull(acceptationToBeIncluded);
        _acceptationToBeIncluded = acceptationToBeIncluded;
        _searchQuery = searchQuery;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
        dest.writeString(_searchQuery);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivityDelegate.Controller controller = new AddBunchFromAcceptationWordEditorController(_acceptationToBeIncluded, language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
    }

    public static final Parcelable.Creator<AddBunchFromAcceptationLanguagePickerController> CREATOR = new Parcelable.Creator<AddBunchFromAcceptationLanguagePickerController>() {

        @Override
        public AddBunchFromAcceptationLanguagePickerController createFromParcel(Parcel source) {
            final AcceptationId acceptationToBeIncluded = AcceptationIdParceler.read(source);
            final String searchQuery = source.readString();
            return new AddBunchFromAcceptationLanguagePickerController(acceptationToBeIncluded, searchQuery);
        }

        @Override
        public AddBunchFromAcceptationLanguagePickerController[] newArray(int size) {
            return new AddBunchFromAcceptationLanguagePickerController[size];
        }
    };
}
