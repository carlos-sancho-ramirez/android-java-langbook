package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.FixedTextAcceptationPickerActivity;
import sword.langbook3.android.db.AcceptationId;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddSentenceSpanFixedTextAcceptationPickerController implements FixedTextAcceptationPickerActivity.Controller {

    @NonNull
    private final String _text;

    public AddSentenceSpanFixedTextAcceptationPickerController(@NonNull String text) {
        ensureNonNull(text);
        _text = text;
    }

    public void fire(@NonNull Activity activity, int requestCode) {
        // We should check if there is any acceptation matching the text,
        // if not, we should skip this step, as the only thing that the
        // user can do is clicking "Add".
        // TODO: Add the missing logic
        FixedTextAcceptationPickerActivity.open(activity, requestCode, this);
    }

    @NonNull
    @Override
    public String getText() {
        return _text;
    }

    @Override
    public void createAcceptation(@NonNull Activity activity) {
        new AddSentenceSpanLanguagePickerController(_text)
                .fire(activity, FixedTextAcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Activity activity, @NonNull AcceptationId acceptation) {
        AcceptationConfirmationActivity.open(activity, AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AcceptationConfirmationController(acceptation));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
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
        dest.writeString(_text);
    }

    public static final Creator<AddSentenceSpanFixedTextAcceptationPickerController> CREATOR = new Creator<AddSentenceSpanFixedTextAcceptationPickerController>() {

        @Override
        public AddSentenceSpanFixedTextAcceptationPickerController createFromParcel(Parcel source) {
            final String text = source.readString();
            return new AddSentenceSpanFixedTextAcceptationPickerController(text);
        }

        @Override
        public AddSentenceSpanFixedTextAcceptationPickerController[] newArray(int size) {
            return new AddSentenceSpanFixedTextAcceptationPickerController[size];
        }
    };
}
