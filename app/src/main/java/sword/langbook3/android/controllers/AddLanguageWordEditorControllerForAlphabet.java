package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.MapGetter;
import sword.collections.MutableList;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.collections.MinimumSizeArrayLengthFunction;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;

public final class AddLanguageWordEditorControllerForAlphabet implements WordEditorActivityDelegate.Controller {

    @NonNull
    private final String _languageCode;

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final ImmutableList<AlphabetId> _alphabets;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _languageCorrelationArray;

    @NonNull
    private final ImmutableList<ImmutableCorrelationArray<AlphabetId>> _alphabetCorrelationArrays;

    @StringRes
    private final int _title;
    private final ImmutableCorrelation<AlphabetId> _correlation;

    public AddLanguageWordEditorControllerForAlphabet(
            @NonNull String code,
            @NonNull LanguageId language,
            @NonNull ImmutableList<AlphabetId> alphabets,
            @NonNull ImmutableCorrelationArray<AlphabetId> languageCorrelationArray,
            @NonNull ImmutableList<ImmutableCorrelationArray<AlphabetId>> alphabetCorrelationArrays,
            @StringRes int title) {
        ensureValidArguments(code.matches(LanguageCodeRules.REGEX));
        ensureNonNull(language);
        ensureValidArguments(!alphabets.isEmpty() && alphabets.toSet().size() == alphabets.size() && !alphabets.map(AlphabetId::getConceptId).contains(language.getConceptId()));
        ensureNonNull(languageCorrelationArray, alphabetCorrelationArrays);
        ensureValidArguments(alphabetCorrelationArrays.size() < alphabets.size());

        _languageCode = code;
        _language = language;
        _alphabets = alphabets;
        _languageCorrelationArray = languageCorrelationArray;
        _alphabetCorrelationArrays = alphabetCorrelationArrays;
        _title = title;

        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        for (AlphabetId alphabet : alphabets) {
            builder.put(alphabet, null);
        }
        _correlation = builder.build();
    }

    @Override
    public void setTitle(@NonNull ActivityInterface activity) {
        activity.setTitle(_title);
    }

    @Override
    public void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        // Nothing to be done
    }

    @NonNull
    @Override
    public UpdateFieldsResult updateFields(@NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts) {
        return WordEditorControllerUtils.updateFieldsForAddLanguage(conversions, texts, _correlation);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        final AddLanguageCorrelationPickerControllerForAlphabet controller = new AddLanguageCorrelationPickerControllerForAlphabet(_languageCode, _language, _alphabets, _languageCorrelationArray, _alphabetCorrelationArrays, texts);
        controller.fire(presenter, WordEditorActivityDelegate.REQUEST_CODE_CORRELATION_PICKER);
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
        dest.writeString(_languageCode);
        LanguageIdParceler.write(dest, _language);
        dest.writeInt(_alphabets.size());
        for (AlphabetId alphabet : _alphabets) {
            AlphabetIdParceler.write(dest, alphabet);
        }
        CorrelationArrayParceler.write(dest, _languageCorrelationArray);
        dest.writeInt(_alphabetCorrelationArrays.size());
        for (ImmutableCorrelationArray<AlphabetId> correlationArray : _alphabetCorrelationArrays) {
            CorrelationArrayParceler.write(dest, correlationArray);
        }
        dest.writeInt(_title);
    }

    public static final Creator<AddLanguageWordEditorControllerForAlphabet> CREATOR = new Creator<AddLanguageWordEditorControllerForAlphabet>() {

        @Override
        public AddLanguageWordEditorControllerForAlphabet createFromParcel(Parcel source) {
            final String languageCode = source.readString();
            final LanguageId language = LanguageIdParceler.read(source);

            final int alphabetCount = source.readInt();
            final MutableList<AlphabetId> alphabets = MutableList.empty(new MinimumSizeArrayLengthFunction(alphabetCount));
            for (int i = 0; i < alphabetCount; i++) {
                alphabets.append(AlphabetIdParceler.read(source));
            }

            final ImmutableCorrelationArray<AlphabetId> languageCorrelationArray = CorrelationArrayParceler.read(source);
            final int completedAlphabets = source.readInt();
            final MutableList<ImmutableCorrelationArray<AlphabetId>> alphabetCorrelationArrays = MutableList.empty(new MinimumSizeArrayLengthFunction(completedAlphabets));
            for (int i = 0; i < completedAlphabets; i++) {
                alphabetCorrelationArrays.append(CorrelationArrayParceler.read(source));
            }

            final int title = source.readInt();
            return new AddLanguageWordEditorControllerForAlphabet(languageCode, language, alphabets.toImmutable(), languageCorrelationArray, alphabetCorrelationArrays.toImmutable(), title);
        }

        @Override
        public AddLanguageWordEditorControllerForAlphabet[] newArray(int size) {
            return new AddLanguageWordEditorControllerForAlphabet[size];
        }
    };
}
