package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;

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

public final class AddAlphabetWordEditorController implements WordEditorActivityDelegate.Controller {

    @NonNull
    private final LanguageId _alphabetLanguage;

    @NonNull
    private final LanguageId _language;
    private final String _searchQuery;

    public AddAlphabetWordEditorController(
            @NonNull LanguageId alphabetLanguage,
            @NonNull LanguageId language,
            String searchQuery) {

        _alphabetLanguage = alphabetLanguage;
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
        new AddAlphabetCorrelationPickerController(_alphabetLanguage, texts)
                .fire(presenter, WordEditorActivityDelegate.REQUEST_CODE_CORRELATION_PICKER);
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == WordEditorActivityDelegate.REQUEST_CODE_CORRELATION_PICKER && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _alphabetLanguage);
        LanguageIdParceler.write(dest, _language);
        dest.writeString(_searchQuery);
    }

    public static final Creator<AddAlphabetWordEditorController> CREATOR = new Creator<AddAlphabetWordEditorController>() {

        @Override
        public AddAlphabetWordEditorController createFromParcel(Parcel source) {
            final LanguageId alphabetLanguage = LanguageIdParceler.read(source);
            final LanguageId language = LanguageIdParceler.read(source);
            final String searchQuery = source.readString();
            return new AddAlphabetWordEditorController(alphabetLanguage, language, searchQuery);
        }

        @Override
        public AddAlphabetWordEditorController[] newArray(int size) {
            return new AddAlphabetWordEditorController[size];
        }
    };
}
