package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.collections.MutableList;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.R;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.collections.MinimumSizeArrayLengthFunction;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddLanguageCorrelationPickerControllerForAlphabet extends AbstractCorrelationPickerController {

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

    public AddLanguageCorrelationPickerControllerForAlphabet(
            @NonNull String languageCode,
            @NonNull LanguageId language,
            @NonNull ImmutableList<AlphabetId> alphabets,
            @NonNull ImmutableCorrelationArray<AlphabetId> languageCorrelationArray,
            @NonNull ImmutableList<ImmutableCorrelationArray<AlphabetId>> alphabetCorrelationArrays,
            @NonNull ImmutableCorrelation<AlphabetId> alphabetTexts) {
        super(alphabetTexts);
        ensureValidArguments(languageCode.matches(LanguageCodeRules.REGEX));
        ensureNonNull(language);
        ensureValidArguments(!alphabets.isEmpty() && alphabets.toSet().size() == alphabets.size() && !alphabets.map(AlphabetId::getConceptId).contains(language.getConceptId()));
        ensureNonNull(languageCorrelationArray, alphabetCorrelationArrays);
        ensureValidArguments(alphabetCorrelationArrays.size() < alphabets.size());
        ensureValidArguments(alphabetTexts.size() == alphabets.size() && TraversableUtils.allMatch(alphabets, alphabetTexts::containsKey));

        _languageCode = languageCode;
        _language = language;
        _alphabets = alphabets;
        _languageCorrelationArray = languageCorrelationArray;
        _alphabetCorrelationArrays = alphabetCorrelationArrays;
    }

    private void storeIntoDatabase(@NonNull ImmutableList<ImmutableCorrelationArray<AlphabetId>> alphabetCorrelationArrays) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final LanguageCreationResult<LanguageId, AlphabetId> langPair = manager.addLanguage(_languageCode);
        final LanguageId language = langPair.language;
        final AlphabetId mainAlphabet = langPair.mainAlphabet;
        final int alphabetCount = _alphabets.size();

        final ImmutableList.Builder<AlphabetId> alphabetsBuilder = new ImmutableList.Builder<>();
        alphabetsBuilder.append(mainAlphabet);

        for (int i = 1; i < alphabetCount; i++) {
            final AlphabetId alphabet = AlphabetIdManager.conceptAsAlphabetId(manager.getNextAvailableConceptId());
            if (!manager.addAlphabetCopyingFromOther(alphabet, mainAlphabet)) {
                throw new AssertionError();
            }
            alphabetsBuilder.append(alphabet);
        }
        final ImmutableList<AlphabetId> alphabets = alphabetsBuilder.build();

        if (manager.addAcceptation(language.getConceptId(), _languageCorrelationArray) == null) {
            throw new AssertionError();
        }

        for (int i = 0; i < alphabetCount; i++) {
            if (manager.addAcceptation(alphabets.valueAt(i).getConceptId(), alphabetCorrelationArrays.valueAt(i)) == null) {
                throw new AssertionError();
            }
        }
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        final ImmutableList<ImmutableCorrelationArray<AlphabetId>> alphabetCorrelationArrays = _alphabetCorrelationArrays.append(selectedOption);
        if (alphabetCorrelationArrays.size() == _alphabets.size()) {
            storeIntoDatabase(alphabetCorrelationArrays);
            presenter.displayFeedback(R.string.addLanguageFeedback);
            presenter.finish();
        }
        else {
            final WordEditorActivity.Controller controller = new AddLanguageWordEditorControllerForAlphabet(_languageCode, _language, _alphabets, _languageCorrelationArray, alphabetCorrelationArrays, R.string.newAuxAlphabetNameActivityTitle);
            presenter.openWordEditor(requestCode, controller);
        }
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
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddLanguageCorrelationPickerControllerForAlphabet> CREATOR = new Creator<AddLanguageCorrelationPickerControllerForAlphabet>() {

        @Override
        public AddLanguageCorrelationPickerControllerForAlphabet createFromParcel(Parcel source) {
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

            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddLanguageCorrelationPickerControllerForAlphabet(languageCode, language, alphabets.toImmutable(), languageCorrelationArray, alphabetCorrelationArrays.toImmutable(), texts);
        }

        @Override
        public AddLanguageCorrelationPickerControllerForAlphabet[] newArray(int size) {
            return new AddLanguageCorrelationPickerControllerForAlphabet[size];
        }
    };
}
