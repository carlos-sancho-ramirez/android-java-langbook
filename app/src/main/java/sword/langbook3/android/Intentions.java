package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AddAcceptationLanguagePickerController;
import sword.langbook3.android.controllers.AddCharacterCompositionDefinitionAcceptationPickerController;
import sword.langbook3.android.controllers.AddLanguageLanguageAdderController;
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
