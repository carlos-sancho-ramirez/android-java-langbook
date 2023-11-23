package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class LinkAcceptationAcceptationConfirmationController implements AcceptationConfirmationActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _sourceAcceptation;

    @NonNull
    private final AcceptationId _targetAcceptation;

    public LinkAcceptationAcceptationConfirmationController(@NonNull AcceptationId sourceAcceptation, @NonNull AcceptationId targetAcceptation) {
        ensureNonNull(sourceAcceptation, targetAcceptation);
        _sourceAcceptation = sourceAcceptation;
        _targetAcceptation = targetAcceptation;
    }

    @NonNull
    @Override
    public AcceptationId getAcceptation() {
        return _targetAcceptation;
    }

    @Override
    public void confirm(@NonNull Presenter presenter) {
        presenter.openLinkageMechanismSelector(AcceptationConfirmationActivityDelegate.REQUEST_CODE_NEXT_STEP, new LinkAcceptationLinkageMechanismSelectorController(_sourceAcceptation, _targetAcceptation));
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == AcceptationConfirmationActivityDelegate.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _sourceAcceptation);
        AcceptationIdParceler.write(dest, _targetAcceptation);
    }

    public static final Creator<LinkAcceptationAcceptationConfirmationController> CREATOR = new Creator<LinkAcceptationAcceptationConfirmationController>() {

        @Override
        public LinkAcceptationAcceptationConfirmationController createFromParcel(Parcel source) {
            final AcceptationId sourceAcceptation = AcceptationIdParceler.read(source);
            final AcceptationId targetAcceptation = AcceptationIdParceler.read(source);
            return new LinkAcceptationAcceptationConfirmationController(sourceAcceptation, targetAcceptation);
        }

        @Override
        public LinkAcceptationAcceptationConfirmationController[] newArray(int size) {
            return new LinkAcceptationAcceptationConfirmationController[size];
        }
    };
}
