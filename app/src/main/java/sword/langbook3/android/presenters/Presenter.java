package sword.langbook3.android.presenters;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.FixedTextAcceptationPickerActivity;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.LinkageMechanismSelectorActivity;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelationArray;

public interface Presenter {
    void finish();
    void finish(@NonNull AcceptationId acceptation);
    void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray);

    void displayFeedback(@StringRes int message);
    void displayFeedback(@StringRes int message, String param);

    void openAcceptationConfirmation(int requestCode, @NonNull AcceptationConfirmationActivity.Controller controller);
    void openCharacterCompositionDefinitionEditor(int requestCode, @NonNull CharacterCompositionDefinitionEditorActivity.Controller controller);
    void openCorrelationPicker(int requestCode, @NonNull CorrelationPickerActivity.Controller controller);
    void openFixedTextAcceptationPicker(int requestCode, @NonNull FixedTextAcceptationPickerActivity.Controller controller);
    void openLanguagePicker(int requestCode, @NonNull LanguagePickerActivity.Controller controller);
    void openLinkageMechanismSelector(int requestCode, @NonNull LinkageMechanismSelectorActivity.Controller controller);
    void openMatchingBunchesPicker(int requestCode, @NonNull MatchingBunchesPickerActivity.Controller controller);
    void openWordEditor(int requestCode, @NonNull WordEditorActivity.Controller controller);
}
