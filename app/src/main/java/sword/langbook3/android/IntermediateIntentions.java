package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.controllers.DefineCorrelationArrayLanguagePickerController;
import sword.langbook3.android.controllers.PickConceptAcceptationPickerController;
import sword.langbook3.android.controllers.PickDefinitionBaseAcceptationPickerController;
import sword.langbook3.android.controllers.PickDefinitionComplementAcceptationPickerController;
import sword.langbook3.android.controllers.PickSentenceSpanFixedTextAcceptationPickerController;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.presenters.PickSentenceSpanIntentionFirstPresenter;
import sword.langbook3.android.presenters.Presenter;

/**
 * Set of intentions used by another intentions.
 *
 * These intentions read the database, but does not modify it.
 * All selected or new defined content, will be returned in the {@link Activity#onActivityResult(int, int, Intent)}
 */
public final class IntermediateIntentions {

    /**
     * Allow the user to define a correlation array.
     *
     * The resulting array can be collected by implementing the {@link Activity#onActivityResult(int, int, Intent)}.
     * If the requestCode matches the given one and the result code is {@link Activity#RESULT_OK},
     * then the data should not be null and include at least the bundle key {@value BundleKeys#CORRELATION_ARRAY}.
     *
     * @param presenter Presenter to be used
     * @param requestCode Request code
     */
    public static void defineCorrelationArray(@NonNull Presenter presenter, int requestCode) {
        new DefineCorrelationArrayLanguagePickerController()
                .fire(presenter, requestCode);
    }

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
        presenter.openAcceptationPicker(requestCode, new PickConceptAcceptationPickerController());
    }

    /**
     * Intermediate intention that allow the user to select an acceptation that will be used as a rule, or defining a new one.
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
    public static void pickRule(@NonNull Presenter presenter, int requestCode) {
        presenter.openAcceptationPicker(requestCode, new PickConceptAcceptationPickerController());
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

    /**
     * Allow the user to select an existing acceptation matching the given text,
     * or create a new acceptation with the given text.
     *
     * This intention is really similar to other acceptation pickers but it
     * differs in the fact that the given text is not modifiable by the user,
     * and must be present in, at least, one of the alphabets of the acceptation.
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
     * In case there is no acceptation defined in the database matching the
     * given text, and there is only one language with only one alphabet, and
     * no agent matcher matches the given text, the user input is not required
     * at all because the only possibility would be creating a new acceptation
     * matching the text. In this specific case, the result of this intention
     * will be returned on this method synchronously. Developers using this
     * method should check if the returned value is different from null. If so,
     * the intention is finished and no call to {@link Activity#onActivityResult(int, int, Intent)}
     * should be expected.
     *
     * @param activity Current activity in foreground.
     * @param requestCode Request code.
     * @param text Text that the acceptation picked must match.
     * @return A new acceptation definition matching the given text, or null if user input is required.
     */
    @CheckResult
    public static AcceptationDefinition pickSentenceSpan(@NonNull Activity activity, int requestCode, String text) {
        final PickSentenceSpanIntentionFirstPresenter presenter = new PickSentenceSpanIntentionFirstPresenter(activity);
        new PickSentenceSpanFixedTextAcceptationPickerController(text)
                .fire(presenter, requestCode);
        return presenter.immediateResult;
    }

    private IntermediateIntentions() {
    }
}
