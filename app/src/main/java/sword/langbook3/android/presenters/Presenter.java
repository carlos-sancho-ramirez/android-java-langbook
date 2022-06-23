package sword.langbook3.android.presenters;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import sword.collections.ImmutableSet;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.ConversionEditorActivity;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.FixedTextAcceptationPickerActivity;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.LinkageMechanismSelectorActivity;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.SourceAlphabetPickerActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.models.Conversion;

public interface Presenter {
    void finish();
    void finish(@NonNull AcceptationId acceptation);
    void finish(@NonNull Conversion<AlphabetId> conversion);
    void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray);
    void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray, @NonNull ImmutableSet<BunchId> bunchSet);

    void setTitle(String title);
    void setTitle(@StringRes int title, String param1, String param2);

    void displayFeedback(@StringRes int message);
    void displayFeedback(@StringRes int message, String param);
    void displayFeedback(@StringRes int message, String param1, String param2);

    void openAcceptationConfirmation(int requestCode, @NonNull AcceptationConfirmationActivity.Controller controller);
    void openAcceptationPicker(int requestCode, @NonNull AcceptationPickerActivity.Controller controller);
    void openCharacterCompositionDefinitionEditor(int requestCode, @NonNull CharacterCompositionDefinitionEditorActivity.Controller controller);
    void openConversionEditor(int requestCode, @NonNull ConversionEditorActivity.Controller controller);
    void openCorrelationPicker(int requestCode, @NonNull CorrelationPickerActivity.Controller controller);
    void openFixedTextAcceptationPicker(int requestCode, @NonNull FixedTextAcceptationPickerActivity.Controller controller);
    void openLanguagePicker(int requestCode, @NonNull LanguagePickerActivity.Controller controller);
    void openLinkageMechanismSelector(int requestCode, @NonNull LinkageMechanismSelectorActivity.Controller controller);
    void openSourceAlphabetPicker(int requestCode, @NonNull SourceAlphabetPickerActivity.Controller controller);
    void openMatchingBunchesPicker(int requestCode, @NonNull MatchingBunchesPickerActivity.Controller controller);
    void openWordEditor(int requestCode, @NonNull WordEditorActivity.Controller controller);
}
