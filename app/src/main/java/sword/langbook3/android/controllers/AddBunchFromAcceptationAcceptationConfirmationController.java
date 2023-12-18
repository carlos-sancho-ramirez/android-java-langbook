package sword.langbook3.android.controllers;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddBunchFromAcceptationAcceptationConfirmationController implements AcceptationConfirmationActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    @NonNull
    private final AcceptationId _acceptation;

    public AddBunchFromAcceptationAcceptationConfirmationController(
            @NonNull AcceptationId acceptationToBeIncluded,
            @NonNull AcceptationId acceptation) {
        ensureNonNull(acceptationToBeIncluded, acceptation);
        _acceptationToBeIncluded = acceptationToBeIncluded;
        _acceptation = acceptation;
    }

    @NonNull
    @Override
    public AcceptationId getAcceptation() {
        return _acceptation;
    }

    @Override
    public void confirm(@NonNull Presenter presenter) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final BunchId pickedBunch = conceptAsBunchId(manager.conceptFromAcceptation(_acceptation));
        if (!manager.addAcceptationInBunch(pickedBunch, _acceptationToBeIncluded)) {
            throw new AssertionError();
        }

        presenter.displayFeedback(R.string.includeInBunchOk);
        presenter.finish();
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        // This controller did not open any activity
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<AddBunchFromAcceptationAcceptationConfirmationController> CREATOR = new Creator<AddBunchFromAcceptationAcceptationConfirmationController>() {

        @Override
        public AddBunchFromAcceptationAcceptationConfirmationController createFromParcel(Parcel source) {
            final AcceptationId acceptationToBeIncluded = AcceptationIdParceler.read(source);
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddBunchFromAcceptationAcceptationConfirmationController(acceptationToBeIncluded, acceptation);
        }

        @Override
        public AddBunchFromAcceptationAcceptationConfirmationController[] newArray(int size) {
            return new AddBunchFromAcceptationAcceptationConfirmationController[size];
        }
    };
}
