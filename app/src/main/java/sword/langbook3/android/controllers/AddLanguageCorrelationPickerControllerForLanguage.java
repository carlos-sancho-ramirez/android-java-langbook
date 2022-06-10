package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.CorrelationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.R;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdComparator;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AddLanguageCorrelationPickerControllerForLanguage implements CorrelationPickerActivity.Controller {

    @NonNull
    private final String _languageCode;

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final ImmutableList<AlphabetId> _alphabets;

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _languageTexts;

    public AddLanguageCorrelationPickerControllerForLanguage(
            @NonNull String languageCode,
            @NonNull LanguageId language,
            @NonNull ImmutableList<AlphabetId> alphabets,
            @NonNull ImmutableCorrelation<AlphabetId> languageTexts) {
        ensureValidArguments(languageCode.matches(LanguageCodeRules.REGEX));
        ensureNonNull(language);
        ensureValidArguments(!alphabets.isEmpty() && alphabets.toSet().size() == alphabets.size() && !alphabets.map(AlphabetId::getConceptId).contains(language.getConceptId()));
        ensureValidArguments(languageTexts.size() == alphabets.size() && TraversableUtils.allMatch(alphabets, languageTexts::containsKey));

        _languageCode = languageCode;
        _language = language;
        _alphabets = alphabets;
        _languageTexts = languageTexts;
    }

    void fire(@NonNull Activity activity, int requestCode) {
        // TODO: This can be optimised as we only need to know if the size of options is 1 or not
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _languageTexts.checkPossibleCorrelationArrays(new AlphabetIdComparator());

        if (options.size() == 1) {
            complete(activity, requestCode, options.valueAt(0));
        }
        else {
            CorrelationPickerActivity.open(activity, requestCode, this);
        }
    }

    private ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> findExistingCorrelations(
            @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options) {
        final ImmutableSet.Builder<ImmutableCorrelation<AlphabetId>> correlationsBuilder = new ImmutableHashSet.Builder<>();
        for (ImmutableCorrelationArray<AlphabetId> option : options) {
            for (ImmutableCorrelation<AlphabetId> correlation : option) {
                correlationsBuilder.add(correlation);
            }
        }
        final ImmutableSet<ImmutableCorrelation<AlphabetId>> correlations = correlationsBuilder.build();

        final ImmutableMap.Builder<ImmutableCorrelation<AlphabetId>, CorrelationId> builder = new ImmutableHashMap.Builder<>();
        for (ImmutableCorrelation<AlphabetId> correlation : correlations) {
            final CorrelationId id = DbManager.getInstance().getManager().findCorrelation(correlation);
            if (id != null) {
                builder.put(correlation, id);
            }
        }

        return builder.build();
    }

    @Override
    public void load(@NonNull Activity activity, boolean firstTime, @NonNull Procedure2<ImmutableSet<ImmutableCorrelationArray<AlphabetId>>, ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId>> procedure) {
        final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options = _languageTexts.checkPossibleCorrelationArrays(new AlphabetIdComparator());
        final ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> knownCorrelations = findExistingCorrelations(options);
        procedure.apply(options, knownCorrelations);
    }

    private void complete(@NonNull Activity activity, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        final String title = activity.getString(R.string.newMainAlphabetNameActivityTitle);
        final WordEditorActivity.Controller controller = new AddLanguageWordEditorControllerForAlphabet(_languageCode, _language, _alphabets, selectedOption, ImmutableList.empty(), title);
        WordEditorActivity.open(activity, requestCode, controller);
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        complete(activity, CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP, selectedOption);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options,  int selection, int requestCode, int resultCode, Intent data) {
        if (requestCode == CorrelationPickerActivity.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK);
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
        CorrelationParceler.write(dest, _languageTexts);
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
            final ImmutableCorrelation<AlphabetId> languageTexts = CorrelationParceler.read(source).toImmutable();
            return new AddLanguageCorrelationPickerControllerForLanguage(languageCode, language, alphabetsBuilder.build(), languageTexts);
        }

        @Override
        public AddLanguageCorrelationPickerControllerForLanguage[] newArray(int size) {
            return new AddLanguageCorrelationPickerControllerForLanguage[size];
        }
    };
}
