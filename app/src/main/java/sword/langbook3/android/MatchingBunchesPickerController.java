package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.MutableList;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class MatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller {

    private final ConceptId _concept;

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    public MatchingBunchesPickerController(
            ConceptId concept,
            @NonNull ImmutableCorrelation<AlphabetId> correlation,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        ensureNonNull(correlation, correlationArray);
        _concept = concept;
        _correlation = correlation;
        _correlationArray = correlationArray;
    }

    private AcceptationId addAcceptation(LangbookDbManager manager) {
        final ConceptId concept = (_concept != null)? _concept : manager.getNextAvailableConceptId();
        return manager.addAcceptation(concept, _correlationArray);
    }

    private void finishActivityReturningAcceptation(@NonNull Activity activity, AcceptationId acceptation) {
        Toast.makeText(activity, R.string.newAcceptationFeedback, Toast.LENGTH_SHORT).show();

        final Intent intent = new Intent();
        AcceptationIdBundler.writeAsIntentExtra(intent, MatchingBunchesPickerActivity.ResultKeys.ACCEPTATION, acceptation);
        activity.setResult(RESULT_OK, intent);
        activity.finish();
    }

    @Override
    public void loadBunches(@NonNull Activity activity, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

        if (bunches.isEmpty()) {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final AcceptationId accId = addAcceptation(manager);
            finishActivityReturningAcceptation(activity, accId);
        }
        else {
            procedure.apply(bunches);
        }
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final AcceptationId accId = addAcceptation(manager);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, accId);
        }
        finishActivityReturningAcceptation(activity, accId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
        CorrelationParceler.write(dest, _correlation);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Parcelable.Creator<MatchingBunchesPickerController> CREATOR = new Parcelable.Creator<MatchingBunchesPickerController>() {

        @Override
        public MatchingBunchesPickerController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new MatchingBunchesPickerController(concept, correlation, correlationArray);
        }

        @Override
        public MatchingBunchesPickerController[] newArray(int size) {
            return new MatchingBunchesPickerController[size];
        }
    };
}
