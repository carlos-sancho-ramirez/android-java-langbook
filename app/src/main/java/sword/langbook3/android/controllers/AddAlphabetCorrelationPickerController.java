package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAlphabetCorrelationPickerController extends AbstractCorrelationPickerController {

    @NonNull
    private final LanguageId _alphabetLanguage;

    public AddAlphabetCorrelationPickerController(
            @NonNull LanguageId alphabetLanguage,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
        ensureNonNull(alphabetLanguage);
        _alphabetLanguage = alphabetLanguage;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new AddAlphabetMatchingBunchesPickerController(_alphabetLanguage, selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _alphabetLanguage);
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddAlphabetCorrelationPickerController> CREATOR = new Creator<AddAlphabetCorrelationPickerController>() {

        @Override
        public AddAlphabetCorrelationPickerController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddAlphabetCorrelationPickerController(language, texts);
        }

        @Override
        public AddAlphabetCorrelationPickerController[] newArray(int size) {
            return new AddAlphabetCorrelationPickerController[size];
        }
    };
}
