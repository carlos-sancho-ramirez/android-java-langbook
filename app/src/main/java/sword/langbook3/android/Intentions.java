package sword.langbook3.android;

import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AddAcceptationInBunchAcceptationPickerController;
import sword.langbook3.android.controllers.AddAcceptationLanguagePickerController;
import sword.langbook3.android.controllers.AddAgentAgentEditorController;
import sword.langbook3.android.controllers.AddAgentAgentEditorControllerWithDiff;
import sword.langbook3.android.controllers.AddAgentAgentEditorControllerWithSource;
import sword.langbook3.android.controllers.AddAgentAgentEditorControllerWithTarget;
import sword.langbook3.android.controllers.AddAlphabetAcceptationPickerController;
import sword.langbook3.android.controllers.AddBunchFromAcceptationAcceptationPickerController;
import sword.langbook3.android.controllers.AddCharacterCompositionDefinitionAcceptationPickerController;
import sword.langbook3.android.controllers.AddDefinitionDefinitionEditorController;
import sword.langbook3.android.controllers.AddLanguageLanguageAdderController;
import sword.langbook3.android.controllers.AddSentenceSentenceEditorController;
import sword.langbook3.android.controllers.AddSynonymSentenceSentenceEditorController;
import sword.langbook3.android.controllers.EditAcceptationWordEditorController;
import sword.langbook3.android.controllers.EditAgentAgentEditorController;
import sword.langbook3.android.controllers.EditConversionConversionEditorController;
import sword.langbook3.android.controllers.EditSentenceSentenceEditorController;
import sword.langbook3.android.controllers.LinkAcceptationAcceptationPickerController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.presenters.DefaultPresenter;

public final class Intentions {

    public static void addCharacterCompositionDefinition(@NonNull ActivityExtensions activity, int requestCode) {
        AcceptationPickerActivity.open(activity, requestCode, new AddCharacterCompositionDefinitionAcceptationPickerController());
    }

    public static void addLanguage(@NonNull ActivityExtensions activity, int requestCode) {
        LanguageAdderActivity.open(activity, requestCode, new AddLanguageLanguageAdderController());
    }

    public static void addAcceptation(@NonNull ActivityExtensions activity, int requestCode, String query) {
        new AddAcceptationLanguagePickerController(query).fire(new DefaultPresenter(activity), requestCode);
    }

    public static void addAcceptationInBunch(@NonNull ActivityExtensions activity, int requestCode, BunchId bunch) {
        AcceptationPickerActivity.open(activity, requestCode, new AddAcceptationInBunchAcceptationPickerController(bunch));
    }

    /**
     * Allow the user to add a new agent into the database.
     *
     * @param context Context to be used to start the activity.
     */
    public static void addAgent(@NonNull ContextExtensions context) {
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
    public static void addAgentWithTarget(@NonNull ContextExtensions context, @NonNull BunchId target) {
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
    public static void addAgentWithSource(@NonNull ContextExtensions context, @NonNull BunchId source) {
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
    public static void addAgentWithDiff(@NonNull ContextExtensions context, @NonNull BunchId diff) {
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
    public static void addAlphabet(@NonNull ActivityExtensions activity, int requestCode, @NonNull LanguageId language) {
        AcceptationPickerActivity.open(activity, requestCode, new AddAlphabetAcceptationPickerController(language));
    }

    /**
     * Allow the user to select an acceptation that will be used as a bunch, and the given acceptation will be included inside.
     * @param activity Activity to be used to open the intention.
     * @param requestCode Request code to be used when opening this intention.
     * @param acceptation Acceptation that will be included in the selected bunch.
     */
    public static void addBunchFromAcceptation(@NonNull ActivityExtensions activity, int requestCode, @NonNull AcceptationId acceptation) {
        AcceptationPickerActivity.open(activity, requestCode, new AddBunchFromAcceptationAcceptationPickerController(acceptation));
    }

    public static void addDefinition(@NonNull ActivityExtensions activity, int requestCode, @NonNull AcceptationId acceptation) {
        DefinitionEditorActivity.open(activity, requestCode, new AddDefinitionDefinitionEditorController(acceptation));
    }

    /**
     * Allow the user to create a new sentence.
     *
     * As sentences are always created from the acceptation details, it is
     * expected that the user will create a sentence including that acceptation.
     *
     * @param activity Activity to be used to open the intention.
     * @param requestCode Request code to be used when opening this intention.
     * @param acceptation Acceptation expected to be found in the new sentence.
     */
    public static void addSentence(@NonNull ActivityExtensions activity, int requestCode, @NonNull AcceptationId acceptation) {
        SentenceEditorActivity.open(activity, requestCode, new AddSentenceSentenceEditorController(acceptation));
    }

    /**
     * Allow the user to create a new sentence which will be a synonym or a translation of an existing one.
     *
     * @param activity Activity to be used to start the new activity.
     * @param requestCode request code.
     * @param concept Concept for the existing sentence.
     */
    public static void addSynonymSentence(@NonNull ActivityExtensions activity, int requestCode, @NonNull ConceptId concept) {
        SentenceEditorActivity.open(activity, requestCode, new AddSynonymSentenceSentenceEditorController(concept));
    }

    public static void editAcceptation(@NonNull ContextExtensions context, AcceptationId acceptation) {
        WordEditorActivity.open(context, new EditAcceptationWordEditorController(acceptation));
    }

    public static void editAgent(@NonNull ContextExtensions context, @NonNull AgentId agent) {
        AgentEditorActivity.open(context, new EditAgentAgentEditorController(agent));
    }

    public static void editConversion(@NonNull ActivityExtensions activity, int requestCode, @NonNull AlphabetId sourceAlphabet, @NonNull AlphabetId targetAlphabet) {
        ConversionEditorActivity.open(activity, requestCode, new EditConversionConversionEditorController(sourceAlphabet, targetAlphabet));
    }

    /**
     * Allow the user to edit an existing sentence.
     *
     * @param activity Activity to be used to open the new intention.
     * @param requestCode Request code to be used when opening the new intention.
     * @param sentence Identifier for the sentence to be edited.
     */
    public static void editSentence(@NonNull ActivityExtensions activity, int requestCode, @NonNull SentenceId sentence) {
        SentenceEditorActivity.open(activity, requestCode, new EditSentenceSentenceEditorController(sentence));
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
    public static void linkAcceptation(@NonNull ActivityExtensions activity, int requestCode, @NonNull AcceptationId sourceAcceptation) {
        AcceptationPickerActivity.open(activity, requestCode, new LinkAcceptationAcceptationPickerController(sourceAcceptation));
    }

    private Intentions() {
    }
}
