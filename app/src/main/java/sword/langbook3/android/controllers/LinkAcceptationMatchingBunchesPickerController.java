package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.MutableList;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class LinkAcceptationMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    @NonNull
    private final ConceptId _concept;

    public LinkAcceptationMatchingBunchesPickerController(
            @NonNull ConceptId concept,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
        ensureNonNull(concept);
        _concept = concept;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        final MutableList<BunchId> bunchList = selectedBunches.toList().mutate();
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final AcceptationId acceptation = manager.addAcceptation(_concept, _correlationArray);
        for (BunchId bunch : bunchList) {
            manager.addAcceptationInBunch(bunch, acceptation);
        }

        presenter.displayFeedback(R.string.newAcceptationFeedback);
        presenter.finish();
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        // This controller does not start any activity
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<LinkAcceptationMatchingBunchesPickerController> CREATOR = new Creator<LinkAcceptationMatchingBunchesPickerController>() {

        @Override
        public LinkAcceptationMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new LinkAcceptationMatchingBunchesPickerController(concept, correlationArray);
        }

        @Override
        public LinkAcceptationMatchingBunchesPickerController[] newArray(int size) {
            return new LinkAcceptationMatchingBunchesPickerController[size];
        }
    };
}
