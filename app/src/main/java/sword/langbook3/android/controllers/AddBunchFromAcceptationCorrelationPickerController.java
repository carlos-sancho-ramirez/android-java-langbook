package sword.langbook3.android.controllers;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddBunchFromAcceptationCorrelationPickerController extends AbstractCorrelationPickerController {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    public AddBunchFromAcceptationCorrelationPickerController(
            @NonNull AcceptationId acceptationToBeIncluded,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
        ensureNonNull(acceptationToBeIncluded);
        _acceptationToBeIncluded = acceptationToBeIncluded;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new AddBunchFromAcceptationMatchingBunchesPickerController(_acceptationToBeIncluded, selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
        CorrelationParceler.write(dest, _texts);
    }

    public static final Parcelable.Creator<AddBunchFromAcceptationCorrelationPickerController> CREATOR = new Parcelable.Creator<AddBunchFromAcceptationCorrelationPickerController>() {

        @Override
        public AddBunchFromAcceptationCorrelationPickerController createFromParcel(Parcel source) {
            final AcceptationId acceptationToBeIncluded = AcceptationIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddBunchFromAcceptationCorrelationPickerController(acceptationToBeIncluded, texts);
        }

        @Override
        public AddBunchFromAcceptationCorrelationPickerController[] newArray(int size) {
            return new AddBunchFromAcceptationCorrelationPickerController[size];
        }
    };
}
