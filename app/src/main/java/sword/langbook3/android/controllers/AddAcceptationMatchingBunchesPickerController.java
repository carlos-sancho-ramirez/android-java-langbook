package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

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
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

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
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimised as no texts are required
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

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
        final AcceptationId acceptation = manager.addAcceptation(concept, _correlationArray);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, acceptation);
        }

        presenter.displayFeedback(R.string.newAcceptationFeedback);
        presenter.finish(acceptation);
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
