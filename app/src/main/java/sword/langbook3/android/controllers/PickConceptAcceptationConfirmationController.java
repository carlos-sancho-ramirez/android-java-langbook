package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class PickConceptAcceptationConfirmationController implements AcceptationConfirmationActivity.Controller {

    @NonNull
    private final AcceptationId _acceptation;

    public PickConceptAcceptationConfirmationController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @NonNull
    @Override
    public AcceptationId getAcceptation() {
        return _acceptation;
    }

    @Override
    public void confirm(@NonNull Presenter presenter) {
        presenter.finish(_acceptation);
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
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<PickConceptAcceptationConfirmationController> CREATOR = new Creator<PickConceptAcceptationConfirmationController>() {

        @Override
        public PickConceptAcceptationConfirmationController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new PickConceptAcceptationConfirmationController(acceptation);
        }

        @Override
        public PickConceptAcceptationConfirmationController[] newArray(int size) {
            return new PickConceptAcceptationConfirmationController[size];
        }
    };
}
