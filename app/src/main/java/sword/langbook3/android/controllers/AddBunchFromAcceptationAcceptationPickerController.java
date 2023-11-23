package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.AcceptationPickerActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdManager;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddBunchFromAcceptationAcceptationPickerController implements AcceptationPickerActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    public AddBunchFromAcceptationAcceptationPickerController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptationToBeIncluded = acceptation;
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new AddBunchFromAcceptationLanguagePickerController(_acceptationToBeIncluded, query)
                .fire(presenter, AcceptationPickerActivityDelegate.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final BunchId bunch = BunchIdManager.conceptAsBunchId(checker.conceptFromAcceptation(acceptation));
        if (checker.isAcceptationStaticallyInBunch(bunch, _acceptationToBeIncluded)) {
            presenter.displayFeedback(R.string.acceptationAlreadyIncludedInBunch);
        }
        else {
            presenter.openAcceptationConfirmation(AcceptationPickerActivityDelegate.REQUEST_CODE_CONFIRM, new AddBunchFromAcceptationAcceptationConfirmationController(_acceptationToBeIncluded, acceptation));
        }
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK && (requestCode == AcceptationPickerActivityDelegate.REQUEST_CODE_CONFIRM || requestCode == AcceptationPickerActivityDelegate.REQUEST_CODE_NEW_ACCEPTATION)) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
    }

    public static final Parcelable.Creator<AddBunchFromAcceptationAcceptationPickerController> CREATOR = new Parcelable.Creator<AddBunchFromAcceptationAcceptationPickerController>() {

        @Override
        public AddBunchFromAcceptationAcceptationPickerController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddBunchFromAcceptationAcceptationPickerController(acceptation);
        }

        @Override
        public AddBunchFromAcceptationAcceptationPickerController[] newArray(int size) {
            return new AddBunchFromAcceptationAcceptationPickerController[size];
        }
    };
}
