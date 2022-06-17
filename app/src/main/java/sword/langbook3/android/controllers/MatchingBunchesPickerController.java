package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

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
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

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

    private void finishActivityReturningAcceptation(@NonNull Presenter presenter, AcceptationId acceptation) {
        presenter.displayFeedback(R.string.newAcceptationFeedback);
        presenter.finish(acceptation);
    }

    @Override
    public void loadBunches(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

        if (bunches.isEmpty()) {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final AcceptationId accId = addAcceptation(manager);
            finishActivityReturningAcceptation(presenter, accId);
        }
        else {
            procedure.apply(bunches);
        }
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final AcceptationId accId = addAcceptation(manager);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, accId);
        }
        finishActivityReturningAcceptation(presenter, accId);
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
