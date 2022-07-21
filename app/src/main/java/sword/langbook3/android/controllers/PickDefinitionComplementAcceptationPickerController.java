package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashSet;
import sword.collections.MutableSet;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.collections.MinimumSizeArrayLengthFunction;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class PickDefinitionComplementAcceptationPickerController implements AcceptationPickerActivity.Controller {

    @NonNull
    private final ConceptId _definingConcept;

    @NonNull
    private final ImmutableSet<ConceptId> _complementConcepts;

    public PickDefinitionComplementAcceptationPickerController(@NonNull ConceptId definingConcept, @NonNull ImmutableSet<ConceptId> complementConcepts) {
        ensureNonNull(definingConcept, complementConcepts);
        ensureValidArguments(!complementConcepts.contains(definingConcept));
        _definingConcept = definingConcept;
        _complementConcepts = complementConcepts;
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new PickConceptLanguagePickerController(query)
                .fire(presenter, AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ConceptId selectedConcept = checker.conceptFromAcceptation(acceptation);
        if (_definingConcept.equals(selectedConcept)) {
            presenter.displayFeedback(R.string.alreadyUsedAsDefiningConcept);
        }
        else if (_complementConcepts.contains(selectedConcept)) {
            presenter.displayFeedback(R.string.alreadyUsedAsDefinitionComplement);
        }
        else {
            presenter.openAcceptationConfirmation(AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AcceptationConfirmationController(acceptation));
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK && (requestCode == AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION || requestCode == AcceptationPickerActivity.REQUEST_CODE_CONFIRM)) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _definingConcept);
        dest.writeInt(_complementConcepts.size());
        for (ConceptId complementConcept : _complementConcepts) {
            ConceptIdParceler.write(dest, complementConcept);
        }
    }

    public static final Creator<PickDefinitionComplementAcceptationPickerController> CREATOR = new Creator<PickDefinitionComplementAcceptationPickerController>() {

        @Override
        public PickDefinitionComplementAcceptationPickerController createFromParcel(Parcel source) {
            final ConceptId definingConcept = ConceptIdParceler.read(source);
            final int complementCount = source.readInt();
            final MutableSet<ConceptId> complementConcepts = MutableHashSet.empty(new MinimumSizeArrayLengthFunction(complementCount));
            for (int index = 0; index < complementCount; index++) {
                complementConcepts.add(ConceptIdParceler.read(source));
            }

            return new PickDefinitionComplementAcceptationPickerController(definingConcept, complementConcepts.toImmutable());
        }

        @Override
        public PickDefinitionComplementAcceptationPickerController[] newArray(int size) {
            return new PickDefinitionComplementAcceptationPickerController[size];
        }
    };
}
