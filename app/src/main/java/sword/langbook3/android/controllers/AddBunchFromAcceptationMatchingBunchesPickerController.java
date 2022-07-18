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
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddBunchFromAcceptationMatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller, Fireable {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    public AddBunchFromAcceptationMatchingBunchesPickerController(
            @NonNull AcceptationId acceptationToBeIncluded,
            @NonNull ImmutableCorrelation<AlphabetId> correlation,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        ensureNonNull(acceptationToBeIncluded, correlation, correlationArray);
        _acceptationToBeIncluded = acceptationToBeIncluded;
        _correlation = correlation;
        _correlationArray = correlationArray;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimised as no texts are required
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlationArray.concatenateTexts(), preferredAlphabet);

        if (bunches.isEmpty()) {
            complete(presenter, bunches.keySet());
        }
        else {
            presenter.openMatchingBunchesPicker(requestCode, this);
        }
    }

    @Override
    public void loadBunches(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);
        procedure.apply(bunches);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId accId = manager.addAcceptation(concept, _correlationArray);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, accId);
        }

        final BunchId pickedBunch = conceptAsBunchId(concept);
        if (!manager.addAcceptationInBunch(pickedBunch, _acceptationToBeIncluded)) {
            throw new AssertionError();
        }

        presenter.displayFeedback(R.string.newAcceptationFeedback);
        presenter.finish();
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
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
        CorrelationParceler.write(dest, _correlation);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Parcelable.Creator<AddBunchFromAcceptationMatchingBunchesPickerController> CREATOR = new Parcelable.Creator<AddBunchFromAcceptationMatchingBunchesPickerController>() {

        @Override
        public AddBunchFromAcceptationMatchingBunchesPickerController createFromParcel(Parcel source) {
            final AcceptationId acceptationToBeIncluded = AcceptationIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddBunchFromAcceptationMatchingBunchesPickerController(acceptationToBeIncluded, correlation, correlationArray);
        }

        @Override
        public AddBunchFromAcceptationMatchingBunchesPickerController[] newArray(int size) {
            return new AddBunchFromAcceptationMatchingBunchesPickerController[size];
        }
    };
}
