package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.MutableList;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationMatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller, Fireable {

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    public AddAcceptationMatchingBunchesPickerController(
            @NonNull ImmutableCorrelation<AlphabetId> correlation,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        ensureNonNull(correlation, correlationArray);
        _correlation = correlation;
        _correlationArray = correlationArray;
    }

    @Override
    public void fire(@NonNull Activity activity, int requestCode) {
        // TODO: This can be optimised as no texts are required
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

        if (bunches.isEmpty()) {
            complete(activity, bunches.keySet());
        }
        else {
            MatchingBunchesPickerActivity.open(activity, requestCode, this);
        }
    }

    @Override
    public void loadBunches(@NonNull Activity activity, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);
        procedure.apply(bunches);
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = manager.addAcceptation(concept, _correlationArray);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, acceptation);
        }

        Toast.makeText(activity, R.string.newAcceptationFeedback, Toast.LENGTH_SHORT).show();

        final Intent intent = new Intent();
        AcceptationIdBundler.writeAsIntentExtra(intent, MatchingBunchesPickerActivity.ResultKeys.ACCEPTATION, acceptation);
        activity.setResult(RESULT_OK, intent);
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
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<AddAcceptationMatchingBunchesPickerController> CREATOR = new Creator<AddAcceptationMatchingBunchesPickerController>() {

        @Override
        public AddAcceptationMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddAcceptationMatchingBunchesPickerController(correlation, correlationArray);
        }

        @Override
        public AddAcceptationMatchingBunchesPickerController[] newArray(int size) {
            return new AddAcceptationMatchingBunchesPickerController[size];
        }
    };
}
