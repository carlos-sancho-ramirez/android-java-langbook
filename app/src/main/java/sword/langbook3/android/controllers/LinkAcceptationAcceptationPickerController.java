package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.activities.delegates.AcceptationPickerActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class LinkAcceptationAcceptationPickerController implements AcceptationPickerActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _sourceAcceptation;

    public LinkAcceptationAcceptationPickerController(@NonNull AcceptationId sourceAcceptation) {
        ensureNonNull(sourceAcceptation);
        _sourceAcceptation = sourceAcceptation;
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        final ConceptId concept = DbManager.getInstance().getManager().conceptFromAcceptation(_sourceAcceptation);
        new LinkAcceptationLanguagePickerController(concept, query)
                .fire(presenter, AcceptationPickerActivityDelegate.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        presenter.openAcceptationConfirmation(AcceptationPickerActivityDelegate.REQUEST_CODE_CONFIRM, new LinkAcceptationAcceptationConfirmationController(_sourceAcceptation, acceptation));
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
        AcceptationIdParceler.write(dest, _sourceAcceptation);
    }

    public static final Creator<LinkAcceptationAcceptationPickerController> CREATOR = new Creator<LinkAcceptationAcceptationPickerController>() {

        @Override
        public LinkAcceptationAcceptationPickerController createFromParcel(Parcel source) {
            final AcceptationId sourceAcceptation = AcceptationIdParceler.read(source);
            return new LinkAcceptationAcceptationPickerController(sourceAcceptation);
        }

        @Override
        public LinkAcceptationAcceptationPickerController[] newArray(int size) {
            return new LinkAcceptationAcceptationPickerController[size];
        }
    };
}
