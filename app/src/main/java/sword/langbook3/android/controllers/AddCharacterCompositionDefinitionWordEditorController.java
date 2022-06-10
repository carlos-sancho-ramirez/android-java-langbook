package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.collections.MapGetter;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.models.Conversion;

import static android.app.Activity.RESULT_OK;
import static sword.collections.SortUtils.equal;

public final class AddCharacterCompositionDefinitionWordEditorController implements WordEditorActivity.Controller {

    @NonNull
    private final LanguageId _language;
    private final String _searchQuery;

    public AddCharacterCompositionDefinitionWordEditorController(
            @NonNull LanguageId language,
            String searchQuery) {
        _language = language;
        _searchQuery = searchQuery;
    }

    private ImmutableCorrelation<AlphabetId> getArgumentCorrelation() {
        return ImmutableCorrelation.empty();
    }

    private ImmutableIntSet findFieldsWhereStringQueryIsValid(
            @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversionMap,
            String queryText,
            ImmutableMap<AlphabetId, AlphabetId> conversions,
            ImmutableIntKeyMap<AlphabetId> fieldIndexAlphabetRelationMap,
            MutableMap<AlphabetId, String> queryConvertedTexts) {
        final MutableIntSet queryTextIsValid = MutableIntArraySet.empty();

        if (queryText != null) {
            final int editableFieldCount = fieldIndexAlphabetRelationMap.size();
            final int fieldConversionCount = conversions.size();

            for (int editableFieldIndex = 0; editableFieldIndex < editableFieldCount; editableFieldIndex++) {
                final AlphabetId alphabet = fieldIndexAlphabetRelationMap.valueAt(editableFieldIndex);

                boolean isValid = true;
                final MutableMap<AlphabetId, String> localQueryConvertedTexts = MutableHashMap.empty();

                ImmutableSet<String> sourceTexts = null;
                final MutableSet<AlphabetId> sourceTextAlphabets = MutableHashSet.empty();

                for (int conversionIndex = 0; conversionIndex < fieldConversionCount; conversionIndex++) {
                    if (equal(conversions.valueAt(conversionIndex), alphabet)) {
                        final ImmutablePair<AlphabetId, AlphabetId> pair = new ImmutablePair<>(alphabet, conversions.keyAt(conversionIndex));
                        final Conversion<AlphabetId> conversion = conversionMap.get(pair);

                        final String convertedText = conversion.convert(queryText);
                        if (convertedText == null) {
                            isValid = false;
                        }
                        else {
                            localQueryConvertedTexts.put(conversions.keyAt(conversionIndex), convertedText);
                        }

                        if (sourceTexts == null || !sourceTexts.isEmpty()) {
                            final ImmutableSet<String> possibleTexts = conversion.findSourceTexts(queryText);
                            sourceTexts = (sourceTexts == null)? possibleTexts : sourceTexts.filter(possibleTexts::contains);
                            sourceTextAlphabets.add(conversions.keyAt(conversionIndex));
                        }
                    }
                }

                if (isValid) {
                    final int fieldIndex = fieldIndexAlphabetRelationMap.keyAt(editableFieldIndex);
                    queryTextIsValid.add(fieldIndex);

                    for (Map.Entry<AlphabetId, String> entry : localQueryConvertedTexts.entries()) {
                        queryConvertedTexts.put(entry.key(), entry.value());
                    }
                }
                else if (sourceTexts != null && !sourceTexts.isEmpty()) {
                    for (AlphabetId targetAlphabet : sourceTextAlphabets) {
                        queryConvertedTexts.put(targetAlphabet, queryText);
                    }
                    queryConvertedTexts.put(alphabet, sourceTexts.valueAt(0));
                }
            }
        }

        return queryTextIsValid.toImmutable();
    }

    @Override
    public void setTitle(@NonNull Activity activity) {
        // Nothing to be done
    }

    @Override
    public void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ImmutableSet<AlphabetId> alphabets = manager.findAlphabetsByLanguage(_language);
        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = manager.findConversions(alphabets);

        final int alphabetCount = alphabets.size();
        for (int targetFieldIndex = 0; targetFieldIndex < alphabetCount; targetFieldIndex++) {
            final AlphabetId targetAlphabet = alphabets.valueAt(targetFieldIndex);
            final AlphabetId sourceAlphabet = conversionMap.get(targetAlphabet, null);
            final ImmutablePair<AlphabetId, AlphabetId> alphabetPair = new ImmutablePair<>(sourceAlphabet, targetAlphabet);
            final int sourceFieldIndex = (sourceAlphabet != null)? alphabets.indexOf(sourceAlphabet) : -1;
            if (sourceFieldIndex >= 0) {
                final String sourceText = texts[sourceFieldIndex];
                texts[targetFieldIndex] = (sourceText != null)? conversions.get(alphabetPair).convert(sourceText) : null;
            }
        }
    }

    @NonNull
    @Override
    public UpdateFieldsResult updateFields(@NonNull Activity activity, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableCorrelation<AlphabetId> existingTexts = getArgumentCorrelation();

        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<AlphabetId, String> fieldNames = checker.readAlphabetsForLanguage(_language, preferredAlphabet);
        final ImmutableMap<AlphabetId, AlphabetId> fieldConversionsMap = checker.findConversions(fieldNames.keySet());

        final ImmutableIntKeyMap.Builder<FieldConversion> builder = new ImmutableIntKeyMap.Builder<>();
        final ImmutableIntKeyMap.Builder<AlphabetId> indexAlphabetBuilder = new ImmutableIntKeyMap.Builder<>();

        final int fieldCount = fieldNames.size();
        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            final AlphabetId alphabet = fieldNames.keyAt(fieldIndex);
            final int conversionIndex = fieldConversionsMap.keySet().indexOf(alphabet);
            if (conversionIndex >= 0) {
                final ImmutablePair<AlphabetId, AlphabetId> pair = new ImmutablePair<>(fieldConversionsMap.valueAt(conversionIndex), fieldConversionsMap.keyAt(conversionIndex));
                final Conversion<AlphabetId> conversion = conversions.get(pair);
                final int sourceFieldIndex = fieldNames.keySet().indexOf(fieldConversionsMap.valueAt(conversionIndex));
                builder.put(fieldIndex, new FieldConversion(sourceFieldIndex, conversion));
            }
            else {
                indexAlphabetBuilder.put(fieldIndex, alphabet);
            }
        }

        final ImmutableIntKeyMap<AlphabetId> fieldIndexAlphabetRelationMap = indexAlphabetBuilder.build();
        final MutableMap<AlphabetId, String> queryConvertedTexts = MutableHashMap.empty();

        final String queryText = _searchQuery;
        boolean autoSelectText = false;
        final ImmutableList<String> newTexts;
        if (texts == null) {
            final ImmutableList.Builder<String> newTextsBuilder = new ImmutableList.Builder<>((currentSize, newSize) -> fieldCount);

            final ImmutableIntSet queryTextIsValid = findFieldsWhereStringQueryIsValid(conversions, queryText, fieldConversionsMap, fieldIndexAlphabetRelationMap, queryConvertedTexts);

            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final String existingText = existingTexts.get(fieldNames.keyAt(fieldIndex), null);
                final String proposedText;
                proposedText = (existingText != null)? existingText :
                        queryTextIsValid.contains(fieldIndex)? queryText :
                                queryConvertedTexts.get(fieldNames.keyAt(fieldIndex), null);
                newTextsBuilder.append(proposedText);
                autoSelectText |= proposedText != null;
            }
            newTexts = newTextsBuilder.build();
        }
        else {
            newTexts = texts;
        }

        return new UpdateFieldsResult(fieldNames, newTexts, builder.build(), fieldConversionsMap, fieldIndexAlphabetRelationMap, autoSelectText);
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        new AddCharacterCompositionDefinitionCorrelationPickerController(texts)
                .fire(activity, WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER);
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
        LanguageIdParceler.write(dest, _language);
        dest.writeString(_searchQuery);
    }

    public static final Creator<AddCharacterCompositionDefinitionWordEditorController> CREATOR = new Creator<AddCharacterCompositionDefinitionWordEditorController>() {

        @Override
        public AddCharacterCompositionDefinitionWordEditorController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final String searchQuery = source.readString();
            return new AddCharacterCompositionDefinitionWordEditorController(language, searchQuery);
        }

        @Override
        public AddCharacterCompositionDefinitionWordEditorController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionWordEditorController[size];
        }
    };
}
