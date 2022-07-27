package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchCorrelationPickerController extends AbstractCorrelationPickerController {

    @NonNull
    private final BunchId _bunch;

    public AddAcceptationInBunchCorrelationPickerController(
            @NonNull BunchId bunch,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
        ensureNonNull(bunch);
        _bunch = bunch;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new AddAcceptationInBunchMatchingBunchesPickerController(_bunch, selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _bunch);
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddAcceptationInBunchCorrelationPickerController> CREATOR = new Creator<AddAcceptationInBunchCorrelationPickerController>() {

        @Override
        public AddAcceptationInBunchCorrelationPickerController createFromParcel(Parcel source) {
            final BunchId bunch = BunchIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddAcceptationInBunchCorrelationPickerController(bunch, texts);
        }

        @Override
        public AddAcceptationInBunchCorrelationPickerController[] newArray(int size) {
            return new AddAcceptationInBunchCorrelationPickerController[size];
        }
    };
}
