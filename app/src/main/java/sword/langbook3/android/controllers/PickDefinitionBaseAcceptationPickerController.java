package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.AcceptationPickerActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class PickDefinitionBaseAcceptationPickerController implements AcceptationPickerActivityDelegate.Controller {

    private final ConceptId _definingConcept;

    public PickDefinitionBaseAcceptationPickerController(@NonNull ConceptId definingConcept) {
        ensureNonNull(definingConcept);
        _definingConcept = definingConcept;
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new PickConceptLanguagePickerController(query)
                .fire(presenter, AcceptationPickerActivityDelegate.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        if (_definingConcept.equals(DbManager.getInstance().getManager().conceptFromAcceptation(acceptation))) {
            presenter.displayFeedback(R.string.alreadyUsedAsDefiningConcept);
        }
        else {
            presenter.openAcceptationConfirmation(AcceptationPickerActivityDelegate.REQUEST_CODE_CONFIRM, new AcceptationConfirmationController(acceptation));
        }
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK && (requestCode == AcceptationPickerActivityDelegate.REQUEST_CODE_NEW_ACCEPTATION || requestCode == AcceptationPickerActivityDelegate.REQUEST_CODE_CONFIRM)) {
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
        ConceptIdParceler.write(dest, _definingConcept);
    }

    public static final Creator<PickDefinitionBaseAcceptationPickerController> CREATOR = new Creator<PickDefinitionBaseAcceptationPickerController>() {

        @Override
        public PickDefinitionBaseAcceptationPickerController createFromParcel(Parcel source) {
            final ConceptId definingConcept = ConceptIdParceler.read(source);
            return new PickDefinitionBaseAcceptationPickerController(definingConcept);
        }

        @Override
        public PickDefinitionBaseAcceptationPickerController[] newArray(int size) {
            return new PickDefinitionBaseAcceptationPickerController[size];
        }
    };
}
