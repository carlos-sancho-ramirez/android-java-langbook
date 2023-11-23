package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.ImmutableMap;
import sword.collections.MutableList;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class AddAcceptationInBunchMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    @NonNull
    private final BunchId _bunch;

    public AddAcceptationInBunchMatchingBunchesPickerController(
            @NonNull BunchId bunch,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
        ensureNonNull(bunch);
        _bunch = bunch;
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
            complete(presenter, requestCode, bunches.keySet());
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
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
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
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
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
