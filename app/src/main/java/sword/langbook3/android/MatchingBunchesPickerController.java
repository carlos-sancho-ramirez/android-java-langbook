package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.Set;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class MatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller {

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    public MatchingBunchesPickerController(@NonNull ImmutableCorrelation<AlphabetId> correlation) {
        ensureNonNull(correlation);
        _correlation = correlation;
    }

    @NonNull
    @Override
    public ImmutableCorrelation<AlphabetId> getTexts() {
        return _correlation;
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches) {
        final Intent intent = new Intent();
        BunchIdBundler.writeListAsIntentExtra(intent, MatchingBunchesPickerActivity.ResultKeys.BUNCH_SET, selectedBunches.toList());
        activity.setResult(RESULT_OK, intent);
        activity.finish();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationParceler.write(dest, _correlation);
    }

    public static final Parcelable.Creator<MatchingBunchesPickerController> CREATOR = new Parcelable.Creator<MatchingBunchesPickerController>() {

        @Override
        public MatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            return new MatchingBunchesPickerController(correlation);
        }

        @Override
        public MatchingBunchesPickerController[] newArray(int size) {
            return new MatchingBunchesPickerController[size];
        }
    };
}
