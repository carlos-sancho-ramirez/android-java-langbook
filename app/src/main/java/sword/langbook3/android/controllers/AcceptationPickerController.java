package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;

public final class AcceptationPickerController implements AcceptationPickerActivity.Controller {

    private final ConceptId _concept;

    public AcceptationPickerController(ConceptId concept) {
        _concept = concept;
    }

    @Override
    public void createAcceptation(@NonNull Activity activity, String query) {
        LanguagePickerActivity.open(activity, AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION, new LanguagePickerController(_concept, query));
    }

    @Override
    public void selectAcceptation(@NonNull Activity activity, @NonNull AcceptationId acceptation) {
        AcceptationConfirmationActivity.open(activity, AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AcceptationConfirmationController(acceptation));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK) {
            final Intent intent = new Intent();
            if (requestCode == AcceptationPickerActivity.REQUEST_CODE_CONFIRM) {
                AcceptationIdBundler.writeAsIntentExtra(intent, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, AcceptationIdBundler.readAsIntentExtra(data, AcceptationConfirmationActivity.ResultKeys.ACCEPTATION));
                AcceptationIdBundler.writeAsIntentExtra(intent, AcceptationPickerActivity.ResultKeys.DYNAMIC_ACCEPTATION, confirmDynamicAcceptation);
            }
            else {
                // When a new acceptation has been created
                AcceptationIdBundler.writeAsIntentExtra(intent, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, AcceptationIdBundler.readAsIntentExtra(data, LanguagePickerActivity.ResultKeys.ACCEPTATION));
                intent.putExtra(AcceptationPickerActivity.ResultKeys.CONCEPT_USED, true);
            }

            activity.setResult(Activity.RESULT_OK, intent);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
    }

    public static final Parcelable.Creator<AcceptationPickerController> CREATOR = new Parcelable.Creator<AcceptationPickerController>() {

        @Override
        public AcceptationPickerController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            return new AcceptationPickerController(concept);
        }

        @Override
        public AcceptationPickerController[] newArray(int size) {
            return new AcceptationPickerController[size];
        }
    };
}
