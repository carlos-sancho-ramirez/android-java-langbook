package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.MapGetter;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;

public final class PickSentenceSpanWordEditorController implements WordEditorActivityDelegate.Controller, Fireable {

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final String _text;

    public PickSentenceSpanWordEditorController(
            @NonNull LanguageId language,
            @NonNull String text) {
        ensureNonNull(language, text);
        _language = language;
        _text = text;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ImmutableSet<AlphabetId> alphabets = manager.findAlphabetsByLanguage(_language);

        if (alphabets.size() == 1) {
            final ImmutableCorrelation<AlphabetId> correlation = ImmutableCorrelation.<AlphabetId>empty().put(alphabets.first(), _text);
            complete(presenter, requestCode, correlation);
        }
        else {
            presenter.openWordEditor(requestCode, this);
        }
    }

    @Override
    public void setTitle(@NonNull ActivityInterface activity) {
        // Nothing to be done
    }

    @Override
    public void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        WordEditorControllerUtils.updateConvertedTexts(_language, texts, conversions);
    }

    @NonNull
    @Override
    public UpdateFieldsResult updateFields(@NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts) {
        return WordEditorControllerUtils.updateFields(conversions, _language, _text, texts);
    }

    private void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        new PickConceptCorrelationPickerController(texts)
                    .fire(presenter, requestCode);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        if (texts.contains(_text)) {
            complete(presenter, WordEditorActivityDelegate.REQUEST_CODE_CORRELATION_PICKER, texts);
        }
        else {
            presenter.displayFeedback(R.string.expectedTextNotPresentError, _text);
        }
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == WordEditorActivityDelegate.REQUEST_CODE_CORRELATION_PICKER && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _language);
        dest.writeString(_text);
    }

    public static final Creator<PickSentenceSpanWordEditorController> CREATOR = new Creator<PickSentenceSpanWordEditorController>() {

        @Override
        public PickSentenceSpanWordEditorController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final String text = source.readString();
            return new PickSentenceSpanWordEditorController(language, text);
        }

        @Override
        public PickSentenceSpanWordEditorController[] newArray(int size) {
            return new PickSentenceSpanWordEditorController[size];
        }
    };
}
