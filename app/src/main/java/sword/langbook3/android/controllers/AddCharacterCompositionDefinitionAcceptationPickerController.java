package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

public final class AddCharacterCompositionDefinitionAcceptationPickerController implements AcceptationPickerActivity.Controller {

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new AddCharacterCompositionDefinitionLanguagePickerController(query)
                .fire(presenter, AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.conceptFromAcceptation(acceptation);
        if (manager.isConceptDefinedAsCharacterCompositionType(concept)) {
            presenter.displayFeedback(R.string.conceptAlreadyUsedAsCharacterCompositionDefinitionError);
        }
        else {
            presenter.openAcceptationConfirmation(AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AddCharacterCompositionDefinitionAcceptationConfirmationController(acceptation));
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if ((requestCode == AcceptationPickerActivity.REQUEST_CODE_CONFIRM || requestCode == AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION) && resultCode == Activity.RESULT_OK) {
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
        // Nothing to be done
    }

    public static final Creator<AddCharacterCompositionDefinitionAcceptationPickerController> CREATOR = new Creator<AddCharacterCompositionDefinitionAcceptationPickerController>() {

        @Override
        public AddCharacterCompositionDefinitionAcceptationPickerController createFromParcel(Parcel source) {
            return new AddCharacterCompositionDefinitionAcceptationPickerController();
        }

        @Override
        public AddCharacterCompositionDefinitionAcceptationPickerController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionAcceptationPickerController[size];
        }
    };
}
