package sword.langbook3.android.controllers;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.Set;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class PickConceptMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    public PickConceptMatchingBunchesPickerController(
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        presenter.finish(_correlationArray, selectedBunches.toImmutable());
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        // This controller does not start any activity
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<PickConceptMatchingBunchesPickerController> CREATOR = new Creator<PickConceptMatchingBunchesPickerController>() {

        @Override
        public PickConceptMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new PickConceptMatchingBunchesPickerController(correlationArray);
        }

        @Override
        public PickConceptMatchingBunchesPickerController[] newArray(int size) {
            return new PickConceptMatchingBunchesPickerController[size];
        }
    };
}
