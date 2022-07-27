package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

public final class PickSentenceSpanCorrelationPickerController extends AbstractCorrelationPickerController {

    public PickSentenceSpanCorrelationPickerController(@NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new PickSentenceSpanMatchingBunchesPickerController(selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<PickSentenceSpanCorrelationPickerController> CREATOR = new Creator<PickSentenceSpanCorrelationPickerController>() {

        @Override
        public PickSentenceSpanCorrelationPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new PickSentenceSpanCorrelationPickerController(texts);
        }

        @Override
        public PickSentenceSpanCorrelationPickerController[] newArray(int size) {
            return new PickSentenceSpanCorrelationPickerController[size];
        }
    };
}
