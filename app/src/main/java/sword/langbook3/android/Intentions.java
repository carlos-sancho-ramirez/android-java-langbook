package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AddAcceptationLanguagePickerController;
import sword.langbook3.android.controllers.AddCharacterCompositionDefinitionAcceptationPickerController;
import sword.langbook3.android.controllers.AddLanguageLanguageAdderController;
import sword.langbook3.android.controllers.AddSentenceSpanFixedTextAcceptationPickerController;
import sword.langbook3.android.controllers.DefineCorrelationArrayLanguagePickerController;
import sword.langbook3.android.controllers.EditAcceptationWordEditorController;
import sword.langbook3.android.db.AcceptationId;

public final class Intentions {

    public static void addCharacterCompositionDefinition(@NonNull Activity activity, int requestCode) {
        AcceptationPickerActivity.open(activity, requestCode, new AddCharacterCompositionDefinitionAcceptationPickerController());
    }

    public static void addLanguage(@NonNull Activity activity, int requestCode) {
        LanguageAdderActivity.open(activity, requestCode, new AddLanguageLanguageAdderController());
    }

    public static void addAcceptation(@NonNull Activity activity, int requestCode, String query) {
        new AddAcceptationLanguagePickerController(query).fire(activity, requestCode);
    }

    /**
     * Allow the user to select an existing acceptation matching the given text, or create a new acceptation with the given text.
     *
     * This intention is really similar to {@link #addAcceptation(Activity, int, String)}.
     * But it differs in the fact that the given text is not modifiable by the user,
     * and must be present in at least one of the alphabets of the acceptation.
     *
     * This method will modify the database state only if the user decides to create a new acceptation.
     * In any case, in case of success, {@link Activity#onActivityResult(int, int, Intent)}
     * method will be called for the given activity, and the selected or new
     * created acceptation identifier will be available in the data coming on
     * that method with the bundle key {@value BundleKeys#ACCEPTATION}.
     *
     * @param activity Current activity in foreground.
     * @param requestCode Request code
     */
    public static void addSentenceSpan(@NonNull Activity activity, int requestCode, String text) {
        new AddSentenceSpanFixedTextAcceptationPickerController(text)
                .fire(activity, requestCode);
    }

    /**
     * Allow the user to define a correlation array.
     *
     * This method will not modify the database state.
     * The resulting array can be collected by implementing the {@link Activity#onActivityResult(int, int, Intent)}.
     * If the requestCode matches the given one and the result code is {@link Activity#RESULT_OK},
     * then the data should not be null and include at least the bundle key {@value BundleKeys#CORRELATION_ARRAY}.
     *
     * @param activity Current activity in foreground.
     * @param requestCode Request code
     */
    public static void defineCorrelationArray(@NonNull Activity activity, int requestCode) {
        new DefineCorrelationArrayLanguagePickerController()
                .fire(activity, requestCode);
    }

    public static void editAcceptation(@NonNull Activity activity, AcceptationId acceptation) {
        WordEditorActivity.open(activity, new EditAcceptationWordEditorController(acceptation));
    }

    private Intentions() {
    }
}
