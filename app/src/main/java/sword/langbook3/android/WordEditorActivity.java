package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.Map;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.controllers.WordEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.CorrelationBundler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdBundler;
import sword.langbook3.android.models.Conversion;

import static sword.collections.SortUtils.equal;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_CORRELATION_PICKER = 1;
    private static final int REQUEST_CODE_CHECK_CONVERSION = 2;

    public interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CORRELATION_MAP = BundleKeys.CORRELATION_MAP;
        String LANGUAGE = BundleKeys.LANGUAGE;
        String SEARCH_QUERY = BundleKeys.SEARCH_QUERY;
        String TITLE = BundleKeys.TITLE;
        String EVALUATE_CONVERSIONS = BundleKeys.EVALUATE_CONVERSIONS;
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String TEXTS = "texts";
    }

    private Controller _controller;
    private LinearLayout _formPanel;
    private ImmutableIntKeyMap<FieldConversion> _fieldConversions;
    private String[] _texts;
    private ImmutableIntKeyMap<AlphabetId> _fieldIndexAlphabetRelationMap;
    private AcceptationId _existingAcceptation;
    private final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> _conversions =
            new SyncCacheMap<>(DbManager.getInstance().getManager()::getConversion);

    /**
     * Open a wizard to fulfill the plain texts for an specific language, and select an specific correlation array.
     * Using this method will not take into account the conversions. This method is intended to define agent adders.
     */
    public static void open(Activity activity, int requestCode, LanguageId language) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        LanguageIdBundler.writeAsIntentExtra(intent, ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(null, null, false));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, LanguageId language, String searchQuery, ConceptId concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        LanguageIdBundler.writeAsIntentExtra(intent, ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.SEARCH_QUERY, searchQuery);
        intent.putExtra(ArgKeys.EVALUATE_CONVERSIONS, true);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(concept, null, true));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, LanguageId language, ConceptId concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        LanguageIdBundler.writeAsIntentExtra(intent, ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.EVALUATE_CONVERSIONS, true);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(concept, null, true));activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, String title, Correlation<AlphabetId> correlation, ConceptId concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.TITLE, title);
        CorrelationBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION_MAP, correlation);
        intent.putExtra(ArgKeys.EVALUATE_CONVERSIONS, true);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(concept, null, true));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Context context, AcceptationId acceptation) {
        final Intent intent = new Intent(context, WordEditorActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        intent.putExtra(ArgKeys.EVALUATE_CONVERSIONS, true);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(null, acceptation, true));
        context.startActivity(intent);
    }

    private static final class FieldConversion {
        final int sourceField;
        final Conversion<AlphabetId> conversion;

        FieldConversion(int sourceField, Conversion<AlphabetId> conversion) {
            this.sourceField = sourceField;
            this.conversion = conversion;
        }
    }

    private ImmutableCorrelation<AlphabetId> getArgumentCorrelation() {
        final Correlation<AlphabetId> correlation = CorrelationBundler.readAsIntentExtra(getIntent(), ArgKeys.CORRELATION_MAP);
        return (correlation != null)? correlation.toImmutable() : ImmutableCorrelation.empty();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

        _controller = getIntent().getParcelableExtra(MatchingBunchesPickerActivity.ArgKeys.CONTROLLER);
        final String givenTitle = getIntent().getStringExtra(ArgKeys.TITLE);
        if (givenTitle != null) {
            setTitle(givenTitle);
        }

        _formPanel = findViewById(R.id.formPanel);
        findViewById(R.id.nextButton).setOnClickListener(this);

        _existingAcceptation = AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);

        if (savedInstanceState != null) {
            _texts = savedInstanceState.getStringArray(SavedKeys.TEXTS);
        }

        updateFields();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHECK_CONVERSION) {
            _conversions.clear();
            updateConvertedTexts();
            updateFields();
        }
    }

    private LanguageId getLanguage(LangbookDbChecker checker) {
        if (_existingAcceptation != null) {
            final ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> result = checker.readAcceptationTextsAndLanguage(_existingAcceptation);
            return result.right;
        }
        else {
            return LanguageIdBundler.readAsIntentExtra(getIntent(), ArgKeys.LANGUAGE);
        }
    }

    private void updateConvertedTexts() {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final LanguageId language = getLanguage(manager);
        final ImmutableSet<AlphabetId> alphabets = manager.findAlphabetsByLanguage(language);
        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = manager.findConversions(alphabets);

        final int alphabetCount = alphabets.size();
        for (int targetFieldIndex = 0; targetFieldIndex < alphabetCount; targetFieldIndex++) {
            final AlphabetId targetAlphabet = alphabets.valueAt(targetFieldIndex);
            final AlphabetId sourceAlphabet = conversionMap.get(targetAlphabet, null);
            final ImmutablePair<AlphabetId, AlphabetId> alphabetPair = new ImmutablePair<>(sourceAlphabet, targetAlphabet);
            final int sourceFieldIndex = (sourceAlphabet != null)? alphabets.indexOf(sourceAlphabet) : -1;
            if (sourceFieldIndex >= 0) {
                final String sourceText = _texts[sourceFieldIndex];
                _texts[targetFieldIndex] = (sourceText != null)? _conversions.get(alphabetPair).convert(sourceText) : null;
            }
        }
    }

    private static void setConversionText(EditText editText, String text) {
        if (text == null) {
            editText.setText(R.string.wordEditorWrongConversionFieldText);
        }
        else {
            editText.setText(text);
        }
    }

    private ImmutableIntSet findFieldsWhereStringQueryIsValid(String queryText,
            ImmutableMap<AlphabetId, AlphabetId> conversions, ImmutableIntKeyMap<AlphabetId> fieldIndexAlphabetRelationMap,
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
                        final Conversion<AlphabetId> conversion = _conversions.get(pair);

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

    private boolean mustEvaluateConversions() {
        return getIntent().getBooleanExtra(ArgKeys.EVALUATE_CONVERSIONS, false);
    }

    private void updateFields() {
        _formPanel.removeAllViews();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableCorrelation<AlphabetId> existingTexts;
        final LanguageId language;
        if (_existingAcceptation != null) {
            final ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> result = checker.readAcceptationTextsAndLanguage(_existingAcceptation);
            existingTexts = result.left;
            language = result.right;
        }
        else {
            existingTexts = getArgumentCorrelation();
            language = LanguageIdBundler.readAsIntentExtra(getIntent(), ArgKeys.LANGUAGE);
        }

        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<AlphabetId, String> fieldNames;
        final ImmutableMap<AlphabetId, AlphabetId> fieldConversions;
        if (language == null) {
            fieldNames = getArgumentCorrelation().keySet().assign(alphabet -> "");
            fieldConversions = ImmutableHashMap.empty();
        }
        else if (mustEvaluateConversions()) {
            fieldNames = checker.readAlphabetsForLanguage(language, preferredAlphabet);
            fieldConversions = checker.findConversions(fieldNames.keySet());
        }
        else {
            final ImmutableMap<AlphabetId, String> alphabetNames = checker.readAlphabetsForLanguage(language, preferredAlphabet);
            final ImmutableMap<AlphabetId, AlphabetId> conversionMap = checker.findConversions(alphabetNames.keySet());
            fieldNames = alphabetNames.filterByKeyNot(conversionMap::containsKey);
            fieldConversions = ImmutableHashMap.empty();
        }

        final LayoutInflater inflater = getLayoutInflater();
        final int fieldCount = fieldNames.size();
        final String queryText = getIntent().getStringExtra(ArgKeys.SEARCH_QUERY);

        final ImmutableIntKeyMap.Builder<FieldConversion> builder = new ImmutableIntKeyMap.Builder<>();
        final ImmutableIntKeyMap.Builder<AlphabetId> indexAlphabetBuilder = new ImmutableIntKeyMap.Builder<>();

        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            final AlphabetId alphabet = fieldNames.keyAt(fieldIndex);
            final int conversionIndex = fieldConversions.keySet().indexOf(alphabet);
            if (conversionIndex >= 0) {
                final ImmutablePair<AlphabetId, AlphabetId> pair = new ImmutablePair<>(fieldConversions.valueAt(conversionIndex), fieldConversions.keyAt(conversionIndex));
                final Conversion<AlphabetId> conversion = _conversions.get(pair);
                final int sourceFieldIndex = fieldNames.keySet().indexOf(fieldConversions.valueAt(conversionIndex));
                builder.put(fieldIndex, new FieldConversion(sourceFieldIndex, conversion));
            }
            else {
                indexAlphabetBuilder.put(fieldIndex, alphabet);
            }
        }

        final ImmutableIntKeyMap<AlphabetId> fieldIndexAlphabetRelationMap = indexAlphabetBuilder.build();
        final MutableMap<AlphabetId, String> queryConvertedTexts = MutableHashMap.empty();

        boolean autoSelectText = false;
        if (_texts == null) {
            _texts = new String[fieldCount];

            final ImmutableIntSet queryTextIsValid = findFieldsWhereStringQueryIsValid(queryText, fieldConversions, fieldIndexAlphabetRelationMap, queryConvertedTexts);

            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                final String existingText = existingTexts.get(fieldNames.keyAt(fieldIndex), null);
                final String proposedText;
                proposedText = (existingText != null)? existingText :
                        queryTextIsValid.contains(fieldIndex)? queryText :
                                queryConvertedTexts.get(fieldNames.keyAt(fieldIndex), null);
                _texts[fieldIndex] = proposedText;
                autoSelectText |= proposedText != null;
            }
        }

        final ImmutableIntSet editableFields = fieldIndexAlphabetRelationMap.keySet();
        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            final boolean isEditable = editableFields.contains(fieldIndex);
            final int layoutId = isEditable? R.layout.word_editor_field_entry : R.layout.word_editor_converted_entry;
            inflater.inflate(layoutId, _formPanel, true);
            View fieldEntry = _formPanel.getChildAt(fieldIndex);

            final TextView textView = fieldEntry.findViewById(R.id.fieldName);
            textView.setText(fieldNames.valueAt(fieldIndex));

            final EditText editText = fieldEntry.findViewById(R.id.fieldValue);
            final String text = _texts[fieldIndex];
            editText.setText(text);

            if (isEditable) {
                if (autoSelectText && text != null) {
                    editText.setSelection(0, text.length());
                }

                editText.addTextChangedListener(new FieldTextWatcher(fieldIndex));
            }
            else {
                final AlphabetId target = fieldNames.keyAt(fieldIndex);
                final AlphabetId source = fieldConversions.get(target);
                fieldEntry.findViewById(R.id.checkConversionButton).setOnClickListener(view ->
                        ConversionDetailsActivity.open(this, REQUEST_CODE_CHECK_CONVERSION, source, target));
            }
        }

        _fieldConversions = builder.build();
        _fieldIndexAlphabetRelationMap = fieldIndexAlphabetRelationMap;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArray(SavedKeys.TEXTS, _texts);
    }

    @Override
    public void onClick(View v) {
        boolean allValid = true;
        for (String text : _texts) {
            if (text == null || text.length() == 0) {
                allValid = false;
                break;
            }
        }

        if (allValid) {
            final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
            for (IntKeyMap.Entry<AlphabetId> entry : _fieldIndexAlphabetRelationMap.entries()) {
                builder.put(entry.value(), _texts[entry.key()]);
            }

            _controller.complete(this, builder.build());
        }
        else {
            Toast.makeText(this, R.string.wordEditorWrongTextError, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateText(int fieldIndex, String newText) {
        String oldText = _texts[fieldIndex];
        _texts[fieldIndex] = newText;

        for (IntKeyMap.Entry<FieldConversion> entry : _fieldConversions.entries()) {
            if (entry.value().sourceField == fieldIndex && !equal(oldText, newText)) {
                String convertedText = entry.value().conversion.convert(newText);
                _texts[entry.key()] = convertedText;

                final EditText editText = _formPanel.getChildAt(entry.key()).findViewById(R.id.fieldValue);
                setConversionText(editText, convertedText);
            }
        }
    }

    private final class FieldTextWatcher implements TextWatcher {

        final int fieldIndex;

        FieldTextWatcher(int fieldIndex) {
            this.fieldIndex = fieldIndex;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to be done
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nothing to be done
        }

        @Override
        public void afterTextChanged(Editable s) {
            updateText(fieldIndex, s.toString());
        }
    }

    public interface Controller extends Parcelable {
        void complete(@NonNull Activity activity, @NonNull ImmutableCorrelation<AlphabetId> texts);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
