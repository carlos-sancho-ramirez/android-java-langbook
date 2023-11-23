package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.MapGetter;
import sword.collections.MutableHashMap;
import sword.collections.MutableMap;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;

public final class EditAcceptationWordEditorController implements WordEditorActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _acceptation;

    public EditAcceptationWordEditorController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @Override
    public void setTitle(@NonNull ActivityInterface activity) {
        // Nothing to be done
    }

    private LanguageId getLanguage() {
        return DbManager.getInstance().getManager().readAcceptationTextsAndLanguage(_acceptation).right;
    }

    @Override
    public void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        WordEditorControllerUtils.updateConvertedTexts(getLanguage(), texts, conversions);
    }

    @NonNull
    @Override
    public UpdateFieldsResult updateFields(@NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> result = checker.readAcceptationTextsAndLanguage(_acceptation);
        final ImmutableCorrelation<AlphabetId> existingTexts = result.left;
        final LanguageId language = result.right;

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

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        new EditAcceptationCorrelationPickerController(_acceptation, texts)
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
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<EditAcceptationWordEditorController> CREATOR = new Creator<EditAcceptationWordEditorController>() {

        @Override
        public EditAcceptationWordEditorController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new EditAcceptationWordEditorController(acceptation);
        }

        @Override
        public EditAcceptationWordEditorController[] newArray(int size) {
            return new EditAcceptationWordEditorController[size];
        }
    };
}
