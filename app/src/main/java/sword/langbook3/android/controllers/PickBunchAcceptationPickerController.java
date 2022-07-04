package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.presenters.Presenter;

public final class PickBunchAcceptationPickerController implements AcceptationPickerActivity.Controller {

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new PickConceptLanguagePickerController(query)
                .fire(presenter, AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        presenter.openAcceptationConfirmation(AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new PickConceptAcceptationConfirmationController(acceptation));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK && (requestCode == AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION || requestCode == AcceptationPickerActivity.REQUEST_CODE_CONFIRM)) {
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

    public static final Creator<PickBunchAcceptationPickerController> CREATOR = new Creator<PickBunchAcceptationPickerController>() {

        @Override
        public PickBunchAcceptationPickerController createFromParcel(Parcel source) {
            return new PickBunchAcceptationPickerController();
        }

        @Override
        public PickBunchAcceptationPickerController[] newArray(int size) {
            return new PickBunchAcceptationPickerController[size];
        }
    };
}
