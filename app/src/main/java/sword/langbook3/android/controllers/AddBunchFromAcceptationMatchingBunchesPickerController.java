package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.MutableList;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddBunchFromAcceptationMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    public AddBunchFromAcceptationMatchingBunchesPickerController(
            @NonNull AcceptationId acceptationToBeIncluded,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
        ensureNonNull(acceptationToBeIncluded);
        _acceptationToBeIncluded = acceptationToBeIncluded;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId accId = manager.addAcceptation(concept, _correlationArray);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, accId);
        }

        final BunchId pickedBunch = conceptAsBunchId(concept);
        if (!manager.addAcceptationInBunch(pickedBunch, _acceptationToBeIncluded)) {
            throw new AssertionError();
        }

        presenter.displayFeedback(R.string.newAcceptationFeedback);
        presenter.finish();
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        // This controller does not start any activity
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Parcelable.Creator<AddBunchFromAcceptationMatchingBunchesPickerController> CREATOR = new Parcelable.Creator<AddBunchFromAcceptationMatchingBunchesPickerController>() {

        @Override
        public AddBunchFromAcceptationMatchingBunchesPickerController createFromParcel(Parcel source) {
            final AcceptationId acceptationToBeIncluded = AcceptationIdParceler.read(source);
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddBunchFromAcceptationMatchingBunchesPickerController(acceptationToBeIncluded, correlationArray);
        }

        @Override
        public AddBunchFromAcceptationMatchingBunchesPickerController[] newArray(int size) {
            return new AddBunchFromAcceptationMatchingBunchesPickerController[size];
        }
    };
}
