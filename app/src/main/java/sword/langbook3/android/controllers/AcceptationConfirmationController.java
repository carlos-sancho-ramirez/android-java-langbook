package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AcceptationIdParceler;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AcceptationConfirmationController implements AcceptationConfirmationActivity.Controller {

    @NonNull
    private final AcceptationId _acceptation;

    public AcceptationConfirmationController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @Override
    public void confirm(@NonNull Activity activity) {
        final Intent intent = new Intent();
        AcceptationIdBundler.writeAsIntentExtra(intent, AcceptationConfirmationActivity.ResultKeys.ACCEPTATION, _acceptation);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
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

    public static final Parcelable.Creator<AcceptationConfirmationController> CREATOR = new Parcelable.Creator<AcceptationConfirmationController>() {

        @Override
        public AcceptationConfirmationController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AcceptationConfirmationController(acceptation);
        }

        @Override
        public AcceptationConfirmationController[] newArray(int size) {
            return new AcceptationConfirmationController[size];
        }
    };
}
