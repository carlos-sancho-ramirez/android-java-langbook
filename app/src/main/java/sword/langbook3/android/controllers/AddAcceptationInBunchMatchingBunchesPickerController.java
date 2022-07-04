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
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAcceptationInBunchMatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller, Fireable {

    @NonNull
    private final BunchId _bunch;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    public AddAcceptationInBunchMatchingBunchesPickerController(
            @NonNull BunchId bunch,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        ensureNonNull(bunch, correlationArray);
        _bunch = bunch;
        _correlationArray = correlationArray;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimised as no texts are required
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlationArray.concatenateTexts(), preferredAlphabet);

        final int index = bunches.indexOfKey(_bunch);
        if (index >= 0) {
            bunches = bunches.removeAt(index);
        }

        if (bunches.isEmpty()) {
            complete(presenter, bunches.keySet());
        }
        else {
            presenter.openMatchingBunchesPicker(requestCode, this);
        }
    }

    private AcceptationId addAcceptation(LangbookDbManager manager) {
        final ConceptId concept = manager.getNextAvailableConceptId();
        return manager.addAcceptation(concept, _correlationArray);
    }

    @Override
    public void loadBunches(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlationArray.concatenateTexts(), preferredAlphabet);

        final int index = bunches.indexOfKey(_bunch);
        if (index >= 0) {
            bunches = bunches.removeAt(index);
        }

        procedure.apply(bunches);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        bunchList.append(_bunch);

        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final AcceptationId accId = addAcceptation(manager);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, accId);
        }

        presenter.displayFeedback(R.string.includeInBunchOk);
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
        BunchIdParceler.write(dest, _bunch);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<AddAcceptationInBunchMatchingBunchesPickerController> CREATOR = new Creator<AddAcceptationInBunchMatchingBunchesPickerController>() {

        @Override
        public AddAcceptationInBunchMatchingBunchesPickerController createFromParcel(Parcel source) {
            final BunchId bunch = BunchIdParceler.read(source);
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddAcceptationInBunchMatchingBunchesPickerController(bunch, correlationArray);
        }

        @Override
        public AddAcceptationInBunchMatchingBunchesPickerController[] newArray(int size) {
            return new AddAcceptationInBunchMatchingBunchesPickerController[size];
        }
    };
}
