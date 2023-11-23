package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.ImmutableSet;
import sword.langbook3.android.activities.delegates.CorrelationPickerActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class AddAcceptationCorrelationPickerController extends AbstractCorrelationPickerController {

    public AddAcceptationCorrelationPickerController(@NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new AddAcceptationMatchingBunchesPickerController(selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        complete(presenter, CorrelationPickerActivityDelegate.REQUEST_CODE_NEXT_STEP, selectedOption);
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options, int selection, int requestCode, int resultCode, Intent data) {
        if (requestCode == CorrelationPickerActivityDelegate.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddAcceptationCorrelationPickerController> CREATOR = new Creator<AddAcceptationCorrelationPickerController>() {

        @Override
        public AddAcceptationCorrelationPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddAcceptationCorrelationPickerController(texts);
        }

        @Override
        public AddAcceptationCorrelationPickerController[] newArray(int size) {
            return new AddAcceptationCorrelationPickerController[size];
        }
    };
}
