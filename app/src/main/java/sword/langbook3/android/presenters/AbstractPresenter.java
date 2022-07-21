package sword.langbook3.android.presenters;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.ConversionEditorActivity;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.FixedTextAcceptationPickerActivity;
import sword.langbook3.android.IntermediateIntentions;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.LinkageMechanismSelectorActivity;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.SourceAlphabetPickerActivity;
import sword.langbook3.android.WordEditorActivity;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

abstract class AbstractPresenter implements Presenter {

    @NonNull
    final Activity _activity;

    public AbstractPresenter(@NonNull Activity activity) {
        ensureNonNull(activity);
        _activity = activity;
    }

    @Override
    public void displayFeedback(int message) {
        Toast.makeText(_activity, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void displayFeedback(int message, String param) {
        final String text = _activity.getString(message, param);
        Toast.makeText(_activity, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void displayFeedback(int message, String param1, String param2) {
        final String text = _activity.getString(message, param1, param2);
        Toast.makeText(_activity, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openAcceptationConfirmation(int requestCode, @NonNull AcceptationConfirmationActivity.Controller controller) {
        AcceptationConfirmationActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openAcceptationPicker(int requestCode, @NonNull AcceptationPickerActivity.Controller controller) {
        AcceptationPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openCharacterCompositionDefinitionEditor(int requestCode, @NonNull CharacterCompositionDefinitionEditorActivity.Controller controller) {
        CharacterCompositionDefinitionEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openConversionEditor(int requestCode, @NonNull ConversionEditorActivity.Controller controller) {
        ConversionEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openCorrelationPicker(int requestCode, @NonNull CorrelationPickerActivity.Controller controller) {
        CorrelationPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openFixedTextAcceptationPicker(int requestCode, @NonNull FixedTextAcceptationPickerActivity.Controller controller) {
        FixedTextAcceptationPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openLanguagePicker(int requestCode, @NonNull LanguagePickerActivity.Controller controller) {
        LanguagePickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openLinkageMechanismSelector(int requestCode, @NonNull LinkageMechanismSelectorActivity.Controller controller) {
        LinkageMechanismSelectorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openSourceAlphabetPicker(int requestCode, @NonNull SourceAlphabetPickerActivity.Controller controller) {
        SourceAlphabetPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openWordEditor(int requestCode, @NonNull WordEditorActivity.Controller controller) {
        WordEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openMatchingBunchesPicker(int requestCode, @NonNull MatchingBunchesPickerActivity.Controller controller) {
        MatchingBunchesPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public AcceptationDefinition fireFixedTextAcceptationPicker(int requestCode, @NonNull String text) {
        return IntermediateIntentions.pickSentenceSpan(_activity, requestCode, text);
    }
}
