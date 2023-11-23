package sword.langbook3.android.presenters;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.collections.ImmutableSet;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate;
import sword.langbook3.android.activities.delegates.AcceptationPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.CharacterCompositionDefinitionEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.ConversionEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.CorrelationPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.FixedTextAcceptationPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.LanguagePickerActivityDelegate;
import sword.langbook3.android.activities.delegates.LinkageMechanismSelectorActivityDelegate;
import sword.langbook3.android.activities.delegates.MatchingBunchesPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.SourceAlphabetPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.SpanEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.models.Conversion;

public interface Presenter {
    void finish();
    void finish(@NonNull AcceptationId acceptation);
    void finish(@NonNull Conversion<AlphabetId> conversion);
    void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray);
    void finish(@NonNull ImmutableCorrelationArray<AlphabetId> correlationArray, @NonNull ImmutableSet<BunchId> bunchSet);
    void finish(@NonNull SentenceId sentence);

    void setTitle(String title);
    void setTitle(@StringRes int title, String param1, String param2);

    void displayFeedback(@StringRes int message);
    void displayFeedback(@StringRes int message, String param);
    void displayFeedback(@StringRes int message, String param1, String param2);

    void openAcceptationConfirmation(int requestCode, @NonNull AcceptationConfirmationActivityDelegate.Controller controller);
    void openAcceptationPicker(int requestCode, @NonNull AcceptationPickerActivityDelegate.Controller controller);
    void openCharacterCompositionDefinitionEditor(int requestCode, @NonNull CharacterCompositionDefinitionEditorActivityDelegate.Controller controller);
    void openConversionEditor(int requestCode, @NonNull ConversionEditorActivityDelegate.Controller controller);
    void openCorrelationPicker(int requestCode, @NonNull CorrelationPickerActivityDelegate.Controller controller);
    void openFixedTextAcceptationPicker(int requestCode, @NonNull FixedTextAcceptationPickerActivityDelegate.Controller controller);
    void openLanguagePicker(int requestCode, @NonNull LanguagePickerActivityDelegate.Controller controller);
    void openLinkageMechanismSelector(int requestCode, @NonNull LinkageMechanismSelectorActivityDelegate.Controller controller);
    void openSourceAlphabetPicker(int requestCode, @NonNull SourceAlphabetPickerActivityDelegate.Controller controller);
    void openSpanEditor(int requestCode, @NonNull SpanEditorActivityDelegate.Controller controller);
    void openMatchingBunchesPicker(int requestCode, @NonNull MatchingBunchesPickerActivityDelegate.Controller controller);
    void openWordEditor(int requestCode, @NonNull WordEditorActivityDelegate.Controller controller);

    AcceptationDefinition fireFixedTextAcceptationPicker(int requestCode, @NonNull String text);
}
