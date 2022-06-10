package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.db.AcceptationId;

public final class AddCharacterCompositionDefinitionAcceptationPickerController implements AcceptationPickerActivity.Controller {

    @Override
    public void createAcceptation(@NonNull Activity activity, String query) {
        new AddCharacterCompositionDefinitionLanguagePickerController(query)
                .fire(activity, AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Activity activity, @NonNull AcceptationId acceptation) {
        AcceptationConfirmationActivity.open(activity, AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AddCharacterCompositionDefinitionAcceptationConfirmationController(acceptation));
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
