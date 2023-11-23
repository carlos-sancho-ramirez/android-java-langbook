package sword.langbook3.android.controllers;

import static sword.collections.SortUtils.equal;

import androidx.annotation.NonNull;

import sword.collections.ImmutableHashMap;
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
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate.Controller.FieldConversion;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate.Controller.UpdateFieldsResult;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.models.Conversion;

final class WordEditorControllerUtils {

    static ImmutableIntSet findFieldsWhereStringQueryIsValid(
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
                    queryTextIsValid.add(fieldIndexAlphabetRelationMap.keyAt(editableFieldIndex));

                    for (Map.Entry<AlphabetId, String> entry : localQueryConvertedTexts.entries()) {
                        queryConvertedTexts.put(entry.key(), entry.value());
                    }
                }
                else if (sourceTexts != null && !sourceTexts.isEmpty()) {
                    for (AlphabetId targetAlphabet : sourceTextAlphabets) {
                        queryConvertedTexts.put(targetAlphabet, queryText);
                    }
                    queryConvertedTexts.put(alphabet, sourceTexts.first());
                }
            }
        }

        return queryTextIsValid.toImmutable();
    }

    @NonNull
    static UpdateFieldsResult updateFields(
            @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions,
            @NonNull LanguageId language,
            String queryText,
            ImmutableList<String> texts) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();

        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<AlphabetId, String> fieldNames = checker.readAlphabetsForLanguage(language, preferredAlphabet);
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

        boolean autoSelectText = false;
        final ImmutableList<String> newTexts;
        if (texts == null) {
            final ImmutableList.Builder<String> newTextsBuilder = new ImmutableList.Builder<>((currentSize, newSize) -> fieldCount);

            final ImmutableIntSet queryTextIsValid = findFieldsWhereStringQueryIsValid(conversions, queryText, fieldConversionsMap, fieldIndexAlphabetRelationMap, queryConvertedTexts);

            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final String proposedText = queryTextIsValid.contains(fieldIndex)? queryText :
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

    static void updateConvertedTexts(LanguageId language, @NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ImmutableSet<AlphabetId> alphabets = manager.findAlphabetsByLanguage(language);
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

    static UpdateFieldsResult updateFieldsForAddLanguage(
            @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions,
            ImmutableList<String> texts,
            @NonNull ImmutableCorrelation<AlphabetId> existingTexts) {
        final ImmutableMap<AlphabetId, String> fieldNames = existingTexts.keySet().assign(alphabet -> "");
        final ImmutableMap<AlphabetId, AlphabetId> fieldConversionsMap = ImmutableHashMap.empty();

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

        boolean autoSelectText = false;
        final ImmutableList<String> newTexts;
        if (texts == null) {
            final ImmutableList.Builder<String> newTextsBuilder = new ImmutableList.Builder<>((currentSize, newSize) -> fieldCount);

            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final String existingText = existingTexts.get(fieldNames.keyAt(fieldIndex), null);
                final String proposedText = (existingText != null)? existingText :
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

    private WordEditorControllerUtils() {
    }
}
