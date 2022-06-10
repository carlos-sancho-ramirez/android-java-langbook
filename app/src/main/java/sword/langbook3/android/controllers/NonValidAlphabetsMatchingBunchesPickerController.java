package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

// TODO: Check if this controller is really needed
public final class NonValidAlphabetsMatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller {

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    public NonValidAlphabetsMatchingBunchesPickerController(@NonNull ImmutableCorrelation<AlphabetId> correlation) {
        ensureNonNull(correlation);
        _correlation = correlation;
    }

    @Override
    public void loadBunches(@NonNull Activity activity, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

        if (bunches.isEmpty()) {
            activity.setResult(RESULT_OK);
            activity.finish();
        }
        else {
            procedure.apply(bunches);
        }
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches) {
        activity.setResult(RESULT_OK);
        activity.finish();
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        // This controller does not start any activity
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationParceler.write(dest, _correlation);
    }

    public static final Creator<NonValidAlphabetsMatchingBunchesPickerController> CREATOR = new Creator<NonValidAlphabetsMatchingBunchesPickerController>() {

        @Override
        public NonValidAlphabetsMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            return new NonValidAlphabetsMatchingBunchesPickerController(correlation);
        }

        @Override
        public NonValidAlphabetsMatchingBunchesPickerController[] newArray(int size) {
            return new NonValidAlphabetsMatchingBunchesPickerController[size];
        }
    };
}
