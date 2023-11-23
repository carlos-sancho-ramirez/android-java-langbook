package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

public final class LinkAcceptationLanguagePickerController extends AbstractLanguagePickerController {

    @NonNull
    private final ConceptId _concept;
    private final String _searchQuery;

    public LinkAcceptationLanguagePickerController(@NonNull ConceptId concept, String searchQuery) {
        ensureNonNull(concept);
        _concept = concept;
        _searchQuery = searchQuery;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
        dest.writeString(_searchQuery);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivityDelegate.Controller controller = new LinkAcceptationWordEditorController(_concept, language, _searchQuery);
        presenter.openWordEditor(requestCode, controller);
    }

    public static final Creator<LinkAcceptationLanguagePickerController> CREATOR = new Creator<LinkAcceptationLanguagePickerController>() {

        @Override
        public LinkAcceptationLanguagePickerController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            final String searchQuery = source.readString();
            return new LinkAcceptationLanguagePickerController(concept, searchQuery);
        }

        @Override
        public LinkAcceptationLanguagePickerController[] newArray(int size) {
            return new LinkAcceptationLanguagePickerController[size];
        }
    };
}
