package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.AcceptationPickerActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchAcceptationPickerController implements AcceptationPickerActivityDelegate.Controller {

    @NonNull
    private final BunchId _bunch;

    public AddAcceptationInBunchAcceptationPickerController(@NonNull BunchId bunch) {
        ensureNonNull(bunch);
        _bunch = bunch;
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new AddAcceptationInBunchLanguagePickerController(_bunch, query)
                .fire(presenter, AcceptationPickerActivityDelegate.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        if (DbManager.getInstance().getManager().isAcceptationStaticallyInBunch(_bunch, acceptation)) {
            presenter.displayFeedback(R.string.acceptationAlreadyIncludedInBunch);
        }
        else {
            presenter.openAcceptationConfirmation(AcceptationPickerActivityDelegate.REQUEST_CODE_CONFIRM, new AddAcceptationInBunchAcceptationConfirmationController(_bunch, acceptation));
        }
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _bunch);
    }

    public static final Creator<AddAcceptationInBunchAcceptationPickerController> CREATOR = new Creator<AddAcceptationInBunchAcceptationPickerController>() {

        @Override
        public AddAcceptationInBunchAcceptationPickerController createFromParcel(Parcel source) {
            final BunchId bunch = BunchIdParceler.read(source);
            return new AddAcceptationInBunchAcceptationPickerController(bunch);
        }

        @Override
        public AddAcceptationInBunchAcceptationPickerController[] newArray(int size) {
            return new AddAcceptationInBunchAcceptationPickerController[size];
        }
    };
}
