package sword.langbook3.android.presenters;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

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
import sword.langbook3.android.SpanEditorActivity;
import sword.langbook3.android.WordEditorActivity;
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
import sword.langbook3.android.interf.ActivityExtensions;

abstract class AbstractPresenter implements Presenter {

    @NonNull
    final ActivityExtensions _activity;

    public AbstractPresenter(@NonNull ActivityExtensions activity) {
        ensureNonNull(activity);
        _activity = activity;
    }

    @Override
    public void displayFeedback(int message) {
        _activity.showToast(message);
    }

    @Override
    public void displayFeedback(int message, String param) {
        final String text = _activity.getString(message, param);
        _activity.showToast(text);
    }

    @Override
    public void displayFeedback(int message, String param1, String param2) {
        final String text = _activity.getString(message, param1, param2);
        _activity.showToast(text);
    }

    @Override
    public void openAcceptationConfirmation(int requestCode, @NonNull AcceptationConfirmationActivityDelegate.Controller controller) {
        AcceptationConfirmationActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openAcceptationPicker(int requestCode, @NonNull AcceptationPickerActivityDelegate.Controller controller) {
        AcceptationPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openCharacterCompositionDefinitionEditor(int requestCode, @NonNull CharacterCompositionDefinitionEditorActivityDelegate.Controller controller) {
        CharacterCompositionDefinitionEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openConversionEditor(int requestCode, @NonNull ConversionEditorActivityDelegate.Controller controller) {
        ConversionEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openCorrelationPicker(int requestCode, @NonNull CorrelationPickerActivityDelegate.Controller controller) {
        CorrelationPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openFixedTextAcceptationPicker(int requestCode, @NonNull FixedTextAcceptationPickerActivityDelegate.Controller controller) {
        FixedTextAcceptationPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openLanguagePicker(int requestCode, @NonNull LanguagePickerActivityDelegate.Controller controller) {
        LanguagePickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openLinkageMechanismSelector(int requestCode, @NonNull LinkageMechanismSelectorActivityDelegate.Controller controller) {
        LinkageMechanismSelectorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openSourceAlphabetPicker(int requestCode, @NonNull SourceAlphabetPickerActivityDelegate.Controller controller) {
        SourceAlphabetPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openSpanEditor(int requestCode, @NonNull SpanEditorActivityDelegate.Controller controller) {
        SpanEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openWordEditor(int requestCode, @NonNull WordEditorActivityDelegate.Controller controller) {
        WordEditorActivity.open(_activity, requestCode, controller);
    }

    @Override
    public void openMatchingBunchesPicker(int requestCode, @NonNull MatchingBunchesPickerActivityDelegate.Controller controller) {
        MatchingBunchesPickerActivity.open(_activity, requestCode, controller);
    }

    @Override
    public AcceptationDefinition fireFixedTextAcceptationPicker(int requestCode, @NonNull String text) {
        return IntermediateIntentions.pickSentenceSpan(_activity, requestCode, text);
    }
}
