package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

public final class LinkAcceptationCorrelationPickerController extends AbstractCorrelationPickerController {

    @NonNull
    private final ConceptId _concept;

    public LinkAcceptationCorrelationPickerController(
            @NonNull ConceptId concept,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
        _concept = concept;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new LinkAcceptationMatchingBunchesPickerController(_concept, selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<LinkAcceptationCorrelationPickerController> CREATOR = new Creator<LinkAcceptationCorrelationPickerController>() {

        @Override
        public LinkAcceptationCorrelationPickerController createFromParcel(Parcel source) {
            final ConceptId conceptId = ConceptIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new LinkAcceptationCorrelationPickerController(conceptId, texts);
        }

        @Override
        public LinkAcceptationCorrelationPickerController[] newArray(int size) {
            return new LinkAcceptationCorrelationPickerController[size];
        }
    };
}
