package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.MapGetter;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;

public final class PickConceptWordEditorController implements WordEditorActivityDelegate.Controller {

    @NonNull
    private final LanguageId _language;
    private final String _searchQuery;

    public PickConceptWordEditorController(
            @NonNull LanguageId language,
            String searchQuery) {
        ensureNonNull(language);
        _language = language;
        _searchQuery = searchQuery;
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
        return WordEditorControllerUtils.updateFields(conversions, _language, _searchQuery, texts);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        new PickConceptCorrelationPickerController(texts)
                .fire(presenter, WordEditorActivityDelegate.REQUEST_CODE_CORRELATION_PICKER);
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
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _language);
        dest.writeString(_searchQuery);
    }

    public static final Creator<PickConceptWordEditorController> CREATOR = new Creator<PickConceptWordEditorController>() {

        @Override
        public PickConceptWordEditorController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final String searchQuery = source.readString();
            return new PickConceptWordEditorController(language, searchQuery);
        }

        @Override
        public PickConceptWordEditorController[] newArray(int size) {
            return new PickConceptWordEditorController[size];
        }
    };
}
