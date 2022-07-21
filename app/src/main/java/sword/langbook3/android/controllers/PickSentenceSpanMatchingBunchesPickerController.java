package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.Set;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

public final class PickSentenceSpanMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    public PickSentenceSpanMatchingBunchesPickerController(
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        presenter.finish(_correlationArray, selectedBunches.toImmutable());
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        // This controller does not start any activity
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<PickSentenceSpanMatchingBunchesPickerController> CREATOR = new Creator<PickSentenceSpanMatchingBunchesPickerController>() {

        @Override
        public PickSentenceSpanMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new PickSentenceSpanMatchingBunchesPickerController(correlationArray);
        }

        @Override
        public PickSentenceSpanMatchingBunchesPickerController[] newArray(int size) {
            return new PickSentenceSpanMatchingBunchesPickerController[size];
        }
    };
}
