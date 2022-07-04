package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AddAcceptationInBunchAcceptationPickerController;
import sword.langbook3.android.controllers.AddAcceptationLanguagePickerController;
import sword.langbook3.android.controllers.AddAgentAgentEditorController;
import sword.langbook3.android.controllers.AddAgentAgentEditorControllerWithDiff;
import sword.langbook3.android.controllers.AddAgentAgentEditorControllerWithSource;
import sword.langbook3.android.controllers.AddAgentAgentEditorControllerWithTarget;
import sword.langbook3.android.controllers.AddAlphabetAcceptationPickerController;
import sword.langbook3.android.controllers.AddCharacterCompositionDefinitionAcceptationPickerController;
import sword.langbook3.android.controllers.AddDefinitionDefinitionEditorController;
import sword.langbook3.android.controllers.AddLanguageLanguageAdderController;
import sword.langbook3.android.controllers.AddSentenceSpanFixedTextAcceptationPickerController;
import sword.langbook3.android.controllers.EditAcceptationWordEditorController;
import sword.langbook3.android.controllers.EditAgentAgentEditorController;
import sword.langbook3.android.controllers.EditConversionConversionEditorController;
import sword.langbook3.android.controllers.LinkAcceptationAcceptationPickerController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.AddSentenceSpanIntentionFirstPresenter;
import sword.langbook3.android.presenters.DefaultPresenter;

public final class Intentions {

    public static void addCharacterCompositionDefinition(@NonNull Activity activity, int requestCode) {
        AcceptationPickerActivity.open(activity, requestCode, new AddCharacterCompositionDefinitionAcceptationPickerController());
    }

    public static void addLanguage(@NonNull Activity activity, int requestCode) {
        LanguageAdderActivity.open(activity, requestCode, new AddLanguageLanguageAdderController());
    }

    public static void addAcceptation(@NonNull Activity activity, int requestCode, String query) {
        new AddAcceptationLanguagePickerController(query).fire(new DefaultPresenter(activity), requestCode);
    }

    public static void addAcceptationInBunch(@NonNull Activity activity, int requestCode, BunchId bunch) {
        AcceptationPickerActivity.open(activity, requestCode, new AddAcceptationInBunchAcceptationPickerController(bunch));
    }

    /**
     * Allow the user to add a new agent into the database.
     *
     * @param context Context to be used to start the activity.
     */
    public static void addAgent(@NonNull Context context) {
        AgentEditorActivity.open(context, new AddAgentAgentEditorController());
    }

    /**
     * Allow the user to add a new agent into the database.
     *
     * This intention will provide a initial value for the target bunch of the new created agent.
     * This will not prevent the user to change the target bunch if required.
     *
     * @param context Context to be used to start the activity.
     * @param target Bunch to be used as target bunch in the new created agent.
     */
    public static void addAgentWithTarget(@NonNull Context context, @NonNull BunchId target) {
        AgentEditorActivity.open(context, new AddAgentAgentEditorControllerWithTarget(target));
    }

    /**
     * Allow the user to add a new agent into the database.
     *
     * This intention will provide a initial value for the source bunch of the new created agent.
     * This will not prevent the user to change the source bunch if required.
     *
     * @param context Context to be used to start the activity.
     * @param source Bunch to be used as source bunch in the new created agent.
     */
    public static void addAgentWithSource(@NonNull Context context, @NonNull BunchId source) {
        AgentEditorActivity.open(context, new AddAgentAgentEditorControllerWithSource(source));
    }

    /**
     * Allow the user to add a new agent into the database.
     *
     * This intention will provide a initial value for the diff bunch of the new created agent.
     * This will not prevent the user to change the diff bunch if required.
     *
     * @param context Context to be used to start the activity.
     * @param diff Bunch to be used as diff bunch in the new created agent.
     */
    public static void addAgentWithDiff(@NonNull Context context, @NonNull BunchId diff) {
        AgentEditorActivity.open(context, new AddAgentAgentEditorControllerWithDiff(diff));
    }

    /**
     * Creates a new Alphabet for the given language.
     *
     * The user will be asked to select an existing acceptation or create a new
     * acceptation providing. Afterwards, it will be asked to select an
     * alphabet, from the ones already existing in the given language, to be
     * used as source and a method to create the alphabet, which can be either
     * copy or creating a conversion.
     *
     * If a copy is requested, the flow will finish at that point, and a new
     * alphabet will be created. However, if the conversion method is requested,
     * the user will be requested to create the new conversion between both
     * languages. Only when the user would finish the conversion definition,
     * the alphabet will be created.
     *
     * @param activity Activity currently in foreground
     * @param requestCode Request code in order to update the view on finish.
     * @param language The language that the new alphabet will be linked to.
     */
    public static void addAlphabet(@NonNull Activity activity, int requestCode, @NonNull LanguageId language) {
        AcceptationPickerActivity.open(activity, requestCode, new AddAlphabetAcceptationPickerController(language));
    }

    public static void addDefinition(@NonNull Activity activity, int requestCode, @NonNull AcceptationId acceptation) {
        DefinitionEditorActivity.open(activity, requestCode, new AddDefinitionDefinitionEditorController(acceptation));
    }

    /**
     * Allow the user to select an existing acceptation matching the given text,
     * or create a new acceptation with the given text.
     *
     * This intention is really similar to {@link #addAcceptation(Activity, int, String)}.
     * But it differs in the fact that the given text is not modifiable by the user,
     * and must be present in, at least, one of the alphabets of the acceptation.
     *
     * This method will modify the database state only if the user decides to create a new acceptation.
     * In case of success, {@link Activity#onActivityResult(int, int, Intent)}
     * method will be called for the given activity, and the selected or new
     * created acceptation identifier will be available in the data coming on
     * that method with the bundle key {@value BundleKeys#ACCEPTATION}.
     *
     * If no input is required by the user at all, it may happen that this
     * method will not open any new screen. In that case, the result of this
     * intention will be returned on this method. Developers using this method
     * should check if the returned value is different from null. If so, the
     * intention is finished and no call to {@link Activity#onActivityResult(int, int, Intent)}
     * should be expected.
     *
     * @param activity Current activity in foreground.
     * @param requestCode Request code
     * @return A new created acceptation matching the given text, or null if user input is required.
     */
    @CheckResult
    public static AcceptationId addSentenceSpan(@NonNull Activity activity, int requestCode, String text) {
        final AddSentenceSpanIntentionFirstPresenter presenter = new AddSentenceSpanIntentionFirstPresenter(activity);
        new AddSentenceSpanFixedTextAcceptationPickerController(text)
                .fire(presenter, requestCode);
        return presenter.immediateResult;
    }

    public static void editAcceptation(@NonNull Activity activity, AcceptationId acceptation) {
        WordEditorActivity.open(activity, new EditAcceptationWordEditorController(acceptation));
    }

    public static void editAgent(@NonNull Context context, @NonNull AgentId agent) {
        AgentEditorActivity.open(context, new EditAgentAgentEditorController(agent));
    }

    public static void editConversion(@NonNull Activity activity, int requestCode, @NonNull AlphabetId sourceAlphabet, @NonNull AlphabetId targetAlphabet) {
        ConversionEditorActivity.open(activity, requestCode, new EditConversionConversionEditorController(sourceAlphabet, targetAlphabet));
    }

    /**
     * Allow the user to select an existing acceptation whose concept will be
     * merged with the one in the given source acceptation, or creating a new
     * acceptation with the given source acceptation concept.
     *
     * @param activity Current activity in foreground.
     * @param requestCode identifier to be used when opening the new activity.
     * @param sourceAcceptation Acceptation whose concept has to be shared.
     */
    public static void linkAcceptation(@NonNull Activity activity, int requestCode, @NonNull AcceptationId sourceAcceptation) {
        AcceptationPickerActivity.open(activity, requestCode, new LinkAcceptationAcceptationPickerController(sourceAcceptation));
    }

    private Intentions() {
    }
}
