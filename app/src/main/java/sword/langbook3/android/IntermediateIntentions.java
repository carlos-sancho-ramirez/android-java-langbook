package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.controllers.PickBunchAcceptationPickerController;
import sword.langbook3.android.controllers.PickDefinitionBaseAcceptationPickerController;
import sword.langbook3.android.controllers.PickDefinitionComplementAcceptationPickerController;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.presenters.Presenter;

/**
 * Set of intentions used by another intentions.
 *
 * These intentions read the database, but does not modify it.
 * All selected or new defined content, will be returned in the {@link Activity#onActivityResult(int, int, Intent)}
 */
public final class IntermediateIntentions {

    /**
     * Intermediate intention that allow the user to select an acceptation that will be used as a bunch, or defining a new one.
     *
     * The type of the returned value will change regarding the user selected an
     * existing acceptation or defined a new one.
     *
     * If the user selected an existing acceptation, the result will be an
     * acceptation identifier and it will come assigned to the bundle key
     * {@link BundleKeys#ACCEPTATION}.
     *
     * If the user defined a new acceptation, all the following results will be returned.
     * <li>The correlation array, under the {@link BundleKeys#CORRELATION_ARRAY} bundle key</li>
     * <li>Any matching bunch, under the {@link BundleKeys#BUNCH_SET} bundle key</li>
     *
     * @param presenter Presenter to be used
     * @param requestCode Request code to identify the result in the {@link Activity#onActivityResult(int, int, Intent)}
     */
    public static void pickBunch(@NonNull Presenter presenter, int requestCode) {
        presenter.openAcceptationPicker(requestCode, new PickBunchAcceptationPickerController());
    }

    /**
     * Intermediate intention that allow the user to select an acceptation that will be used as a definition base, or defining a new one.
     *
     * This intention does not modify the database, it returns values instead.
     * The type of the value will change regarding the user selected an
     * existing acceptation or defined a new one.
     *
     * If the user selected an existing acceptation, the result will be an
     * acceptation identifier and it will come assigned to the bundle key
     * {@link BundleKeys#ACCEPTATION}.
     *
     * If the user defined a new acceptation, all the following results will be returned.
     * <li>The correlation array, under the {@link BundleKeys#CORRELATION_ARRAY} bundle key</li>
     * <li>Any matching bunch, under the {@link BundleKeys#BUNCH_SET} bundle key</li>
     *
     * This intention will prevent that the user can select an acceptation
     * whose concept would match the current concept under definition.
     *
     * @param presenter Presenter to be used
     * @param requestCode Request code to identify the result in the {@link Activity#onActivityResult(int, int, Intent)}
     * @param definingConcept Concept under definition. Any acceptation matching this concept will not be selected.
     */
    public static void pickDefinitionBase(@NonNull Presenter presenter, int requestCode, ConceptId definingConcept) {
        presenter.openAcceptationPicker(requestCode, new PickDefinitionBaseAcceptationPickerController(definingConcept));
    }

    /**
     * Intermediate intention that allow the user to select an acceptation that will be used as a definition base, or defining a new one.
     *
     * This intention does not modify the database, it returns values instead.
     * The type of the value will change regarding the user selected an
     * existing acceptation or defined a new one.
     *
     * If the user selected an existing acceptation, the result will be an
     * acceptation identifier and it will come assigned to the bundle key
     * {@link BundleKeys#ACCEPTATION}.
     *
     * If the user defined a new acceptation, all the following results will be returned.
     * <li>The correlation array, under the {@link BundleKeys#CORRELATION_ARRAY} bundle key</li>
     * <li>Any matching bunch, under the {@link BundleKeys#BUNCH_SET} bundle key</li>
     *
     * This intention will prevent that the user can select an acceptation
     * whose concept would match either the current concept under definition or
     * any of the concept already set as complement.
     *
     * @param presenter Presenter to be used
     * @param requestCode Request code to identify the result in the {@link Activity#onActivityResult(int, int, Intent)}
     * @param definingConcept Concept under definition. Any acceptation matching this concept will not be selected.
     */
    public static void pickDefinitionComplement(@NonNull Presenter presenter, int requestCode, ConceptId definingConcept, ImmutableSet<ConceptId> complementConcepts) {
        presenter.openAcceptationPicker(requestCode, new PickDefinitionComplementAcceptationPickerController(definingConcept, complementConcepts));
    }

    private IntermediateIntentions() {
    }
}
