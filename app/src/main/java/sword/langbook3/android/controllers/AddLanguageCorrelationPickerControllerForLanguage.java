package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.R;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddLanguageCorrelationPickerControllerForLanguage extends AbstractCorrelationPickerController {

    @NonNull
    private final String _languageCode;

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final ImmutableList<AlphabetId> _alphabets;

    public AddLanguageCorrelationPickerControllerForLanguage(
            @NonNull String languageCode,
            @NonNull LanguageId language,
            @NonNull ImmutableList<AlphabetId> alphabets,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
        ensureValidArguments(languageCode.matches(LanguageCodeRules.REGEX));
        ensureNonNull(language);
        ensureValidArguments(!alphabets.isEmpty() && alphabets.toSet().size() == alphabets.size() && !alphabets.map(AlphabetId::getConceptId).contains(language.getConceptId()));
        ensureValidArguments(texts.size() == alphabets.size() && alphabets.allMatch(texts::containsKey));

        _languageCode = languageCode;
        _language = language;
        _alphabets = alphabets;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        final WordEditorActivity.Controller controller = new AddLanguageWordEditorControllerForAlphabet(_languageCode, _language, _alphabets, selectedOption, ImmutableList.empty(), R.string.newMainAlphabetNameActivityTitle);
        presenter.openWordEditor(requestCode, controller);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_languageCode);
        LanguageIdParceler.write(dest, _language);
        dest.writeInt(_alphabets.size());
        for (AlphabetId alphabet : _alphabets) {
            AlphabetIdParceler.write(dest, alphabet);
        }
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddLanguageCorrelationPickerControllerForLanguage> CREATOR = new Creator<AddLanguageCorrelationPickerControllerForLanguage>() {

        @Override
        public AddLanguageCorrelationPickerControllerForLanguage createFromParcel(Parcel source) {
            final String languageCode = source.readString();
            final LanguageId language = LanguageIdParceler.read(source);
            final int alphabetCount = source.readInt();
            final ImmutableList.Builder<AlphabetId> alphabetsBuilder = new ImmutableList.Builder<>();
            for (int i = 0; i < alphabetCount; i++) {
                alphabetsBuilder.append(AlphabetIdParceler.read(source));
            }
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddLanguageCorrelationPickerControllerForLanguage(languageCode, language, alphabetsBuilder.build(), texts);
        }

        @Override
        public AddLanguageCorrelationPickerControllerForLanguage[] newArray(int size) {
            return new AddLanguageCorrelationPickerControllerForLanguage[size];
        }
    };
}
