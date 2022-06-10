package sword.langbook3.android;

import android.app.Activity;

import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AddCharacterCompositionDefinitionAcceptationPickerController;
import sword.langbook3.android.controllers.AddLanguageLanguageAdderController;

public final class Intentions {

    public static void addCharacterCompositionDefinition(@NonNull Activity activity, int requestCode) {
        AcceptationPickerActivity.open(activity, requestCode, new AddCharacterCompositionDefinitionAcceptationPickerController());
    }

    public static void addLanguage(@NonNull Activity activity, int requestCode) {
        LanguageAdderActivity.open(activity, requestCode, new AddLanguageLanguageAdderController());
    }

    private Intentions() {
    }
}
