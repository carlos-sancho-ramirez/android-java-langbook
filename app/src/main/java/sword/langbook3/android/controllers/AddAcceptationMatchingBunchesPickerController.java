package sword.langbook3.android.controllers;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.MutableList;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class AddAcceptationMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    public AddAcceptationMatchingBunchesPickerController(
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = manager.addAcceptation(concept, _correlationArray);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, acceptation);
        }

        presenter.displayFeedback(R.string.newAcceptationFeedback);
        presenter.finish(acceptation);
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
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

    public static final Creator<AddAcceptationMatchingBunchesPickerController> CREATOR = new Creator<AddAcceptationMatchingBunchesPickerController>() {

        @Override
        public AddAcceptationMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddAcceptationMatchingBunchesPickerController(correlationArray);
        }

        @Override
        public AddAcceptationMatchingBunchesPickerController[] newArray(int size) {
            return new AddAcceptationMatchingBunchesPickerController[size];
        }
    };
}
