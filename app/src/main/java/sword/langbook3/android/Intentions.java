package sword.langbook3.android;

import android.app.Activity;

import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AddCharacterCompositionDefinitionAcceptationPickerController;
import sword.langbook3.android.controllers.AddLanguageLanguageAdderController;
import sword.langbook3.android.controllers.EditAcceptationWordEditorController;
import sword.langbook3.android.db.AcceptationId;

public final class Intentions {

    public static void addCharacterCompositionDefinition(@NonNull Activity activity, int requestCode) {
        AcceptationPickerActivity.open(activity, requestCode, new AddCharacterCompositionDefinitionAcceptationPickerController());
    }

    public static void addLanguage(@NonNull Activity activity, int requestCode) {
        LanguageAdderActivity.open(activity, requestCode, new AddLanguageLanguageAdderController());
    }

    public static void editAcceptation(@NonNull Activity activity, AcceptationId acceptation) {
        WordEditorActivity.open(activity, new EditAcceptationWordEditorController(acceptation));
    }

    private Intentions() {
    }
}
