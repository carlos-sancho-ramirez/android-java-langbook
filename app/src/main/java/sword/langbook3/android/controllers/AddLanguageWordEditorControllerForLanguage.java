package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.MapGetter;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.R;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddLanguageWordEditorControllerForLanguage implements WordEditorActivity.Controller {

    @NonNull
    private final String _languageCode;

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final ImmutableList<AlphabetId> _alphabets;

    public AddLanguageWordEditorControllerForLanguage(
            @NonNull String code,
            @NonNull LanguageId language,
            @NonNull ImmutableList<AlphabetId> alphabets) {
        ensureValidArguments(code.matches(LanguageCodeRules.REGEX));
        ensureNonNull(language);
        ensureValidArguments(!alphabets.isEmpty() && alphabets.toSet().size() == alphabets.size() && !alphabets.map(AlphabetId::getConceptId).contains(language.getConceptId()));

        _languageCode = code;
        _language = language;
        _alphabets = alphabets;
    }

    private ImmutableCorrelation<AlphabetId> getArgumentCorrelation() {
        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        for (AlphabetId alphabet : _alphabets) {
            builder.put(alphabet, null);
        }

        return builder.build();
    }

    @Override
    public void setTitle(@NonNull Activity activity) {
        activity.setTitle(R.string.newLanguageNameActivityTitle);
    }

    @Override
    public void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        // Nothing to be done
    }

    @NonNull
    @Override
    public UpdateFieldsResult updateFields(@NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts) {
        return WordEditorControllerUtils.updateFieldsForAddLanguage(conversions, texts, getArgumentCorrelation());
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        final AddLanguageCorrelationPickerControllerForLanguage controller = new AddLanguageCorrelationPickerControllerForLanguage(_languageCode, _language, _alphabets, texts);
        controller.fire(presenter, WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER && resultCode == RESULT_OK) {
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
        dest.writeString(_languageCode);
        LanguageIdParceler.write(dest, _language);
        dest.writeInt(_alphabets.size());
        for (AlphabetId alphabet : _alphabets) {
            AlphabetIdParceler.write(dest, alphabet);
        }
    }

    public static final Creator<AddLanguageWordEditorControllerForLanguage> CREATOR = new Creator<AddLanguageWordEditorControllerForLanguage>() {

        @Override
        public AddLanguageWordEditorControllerForLanguage createFromParcel(Parcel source) {
            final String languageCode = source.readString();
            final LanguageId language = LanguageIdParceler.read(source);
            final int alphabetCount = source.readInt();
            final ImmutableList.Builder<AlphabetId> alphabetsBuilder = new ImmutableList.Builder<>();
            for (int i = 0; i < alphabetCount; i++) {
                alphabetsBuilder.append(AlphabetIdParceler.read(source));
            }

            return new AddLanguageWordEditorControllerForLanguage(languageCode, language, alphabetsBuilder.build());
        }

        @Override
        public AddLanguageWordEditorControllerForLanguage[] newArray(int size) {
            return new AddLanguageWordEditorControllerForLanguage[size];
        }
    };
}
