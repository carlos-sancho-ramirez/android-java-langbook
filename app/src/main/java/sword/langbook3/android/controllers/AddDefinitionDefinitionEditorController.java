package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.DefinitionEditorActivity;
import sword.langbook3.android.IntermediateIntentions;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.ParcelableBunchIdSet;
import sword.langbook3.android.db.ParcelableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddDefinitionDefinitionEditorController implements DefinitionEditorActivity.Controller {

    @NonNull
    private final AcceptationId _acceptation;

    public AddDefinitionDefinitionEditorController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @Override
    public void setTitle(@NonNull Activity activity) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final String acceptationText = DbManager.getInstance().getManager().getAcceptationDisplayableText(_acceptation, preferredAlphabet);
        activity.setTitle(activity.getString(R.string.definitionEditorActivityTitle, acceptationText));
    }

    @Override
    public void pickBaseConcept(@NonNull Presenter presenter) {
        final ConceptId definingConcept = DbManager.getInstance().getManager().conceptFromAcceptation(_acceptation);
        IntermediateIntentions.pickDefinitionBase(presenter, DefinitionEditorActivity.REQUEST_CODE_PICK_BASE, definingConcept);
    }

    private static ImmutableSet<ConceptId> getComplementConcepts(@NonNull State state) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        return state.getComplements()
                .filter(item -> item instanceof AcceptationId)
                .map(item -> checker.conceptFromAcceptation((AcceptationId)  item))
                .toSet();
    }

    @Override
    public void pickComplement(@NonNull Presenter presenter, @NonNull State state) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ConceptId definingConcept = checker.conceptFromAcceptation(_acceptation);
        final ImmutableSet<ConceptId> complementConcepts = getComplementConcepts(state);
        IntermediateIntentions.pickDefinitionComplement(presenter, DefinitionEditorActivity.REQUEST_CODE_PICK_COMPLEMENT, definingConcept, complementConcepts);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.conceptFromAcceptation(_acceptation);
        final Object base = state.getBase();
        final ConceptId baseConcept;
        if (base instanceof AcceptationId) {
            baseConcept = manager.conceptFromAcceptation((AcceptationId) base);
            if (getComplementConcepts(state).contains(baseConcept)) {
                presenter.displayFeedback(R.string.complementsContainsBaseError);
                return;
            }
        }
        else if (base instanceof AcceptationDefinition) {
            final AcceptationDefinition definition = (AcceptationDefinition) base;
            baseConcept = manager.getNextAvailableConceptId();
            final AcceptationId baseAcceptation = manager.addAcceptation(baseConcept, definition.correlationArray);
            for (BunchId bunch : definition.bunchSet) {
                manager.addAcceptationInBunch(bunch, baseAcceptation);
            }
        }
        else {
            presenter.displayFeedback(R.string.baseConceptMissing);
            return;
        }

        final ImmutableSet<ConceptId> complements = state.getComplements().map(complement -> {
            if (complement instanceof AcceptationId) {
                return manager.conceptFromAcceptation((AcceptationId) complement);
            }
            else {
                final AcceptationDefinition definition = (AcceptationDefinition) complement;
                final ConceptId complementConcept = manager.getNextAvailableConceptId();
                final AcceptationId complementAcceptation = manager.addAcceptation(complementConcept, definition.correlationArray);
                for (BunchId bunch : definition.bunchSet) {
                    manager.addAcceptationInBunch(bunch, complementAcceptation);
                }
                return complementConcept;
            }
        }).toSet();

        manager.addDefinition(baseConcept, concept, complements);
        presenter.displayFeedback(R.string.includeSupertypeOk);
        presenter.finish();
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state) {
        if (resultCode == Activity.RESULT_OK) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, BundleKeys.ACCEPTATION);
            final Object item;
            if (acceptation == null) {
                final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                final ParcelableBunchIdSet bunchIdSet = data.getParcelableExtra(BundleKeys.BUNCH_SET);
                item = new AcceptationDefinition(parcelableCorrelationArray.get(), bunchIdSet.get());
            }
            else {
                item = acceptation;
            }

            if (requestCode == DefinitionEditorActivity.REQUEST_CODE_PICK_BASE) {
                state.setBase(item);
            }
            else {
                ensureValidArguments(requestCode == DefinitionEditorActivity.REQUEST_CODE_PICK_COMPLEMENT);
                state.addComplement(item);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Parcelable.Creator<AddDefinitionDefinitionEditorController> CREATOR = new Parcelable.Creator<AddDefinitionDefinitionEditorController>() {

        @Override
        public AddDefinitionDefinitionEditorController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddDefinitionDefinitionEditorController(acceptation);
        }

        @Override
        public AddDefinitionDefinitionEditorController[] newArray(int size) {
            return new AddDefinitionDefinitionEditorController[size];
        }
    };
}
