package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchAcceptationConfirmationController implements AcceptationConfirmationActivity.Controller {

    @NonNull
    private final BunchId _bunch;

    @NonNull
    private final AcceptationId _acceptation;

    public AddAcceptationInBunchAcceptationConfirmationController(@NonNull BunchId bunch, @NonNull AcceptationId acceptation) {
        ensureNonNull(bunch, acceptation);
        _bunch = bunch;
        _acceptation = acceptation;
    }

    @NonNull
    @Override
    public AcceptationId getAcceptation() {
        return _acceptation;
    }

    @Override
    public void confirm(@NonNull Presenter presenter) {
        if (!DbManager.getInstance().getManager().addAcceptationInBunch(_bunch, _acceptation)) {
            throw new AssertionError();
        }

        presenter.displayFeedback(R.string.includeInBunchOk);
        presenter.finish();
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        // This controller did not open any activity
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _bunch);
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<AddAcceptationInBunchAcceptationConfirmationController> CREATOR = new Creator<AddAcceptationInBunchAcceptationConfirmationController>() {

        @Override
        public AddAcceptationInBunchAcceptationConfirmationController createFromParcel(Parcel source) {
            final BunchId bunch = BunchIdParceler.read(source);
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddAcceptationInBunchAcceptationConfirmationController(bunch, acceptation);
        }

        @Override
        public AddAcceptationInBunchAcceptationConfirmationController[] newArray(int size) {
            return new AddAcceptationInBunchAcceptationConfirmationController[size];
        }
    };
}
