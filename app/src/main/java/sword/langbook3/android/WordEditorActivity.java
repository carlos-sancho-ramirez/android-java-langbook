package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.Conversion;

import static sword.langbook3.android.CorrelationPickerActivity.NO_ACCEPTATION;
import static sword.langbook3.android.CorrelationPickerActivity.NO_CONCEPT;
import static sword.langbook3.android.collections.EqualUtils.equal;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_CORRELATION_PICKER = 1;
    private static final int REQUEST_CODE_CHECK_CONVERSION = 2;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONCEPT = BundleKeys.CONCEPT;
        String CORRELATION_MAP = BundleKeys.CORRELATION_MAP;
        String LANGUAGE = BundleKeys.LANGUAGE;
        String SEARCH_QUERY = BundleKeys.SEARCH_QUERY;
        String TITLE = BundleKeys.TITLE;
    }

    private interface SavedKeys {
        String TEXTS = "texts";
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private LinearLayout _formPanel;
    private ImmutableIntKeyMap<FieldConversion> _fieldConversions;
    private String[] _texts;
    private ImmutableIntKeyMap<AlphabetId> _fieldIndexAlphabetRelationMap;
    private int _existingAcceptation = NO_ACCEPTATION;
    private final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion> _conversions =
            new SyncCacheMap<>(DbManager.getInstance().getManager()::getConversion);

    public static void open(Activity activity, int requestCode, int language, String searchQuery, int concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        intent.putExtra(ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.SEARCH_QUERY, searchQuery);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, int language, int concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        intent.putExtra(ArgKeys.LANGUAGE, language);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, String title, Correlation correlation, int concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        intent.putExtra(ArgKeys.TITLE, title);
        intent.putExtra(ArgKeys.CORRELATION_MAP, new ParcelableCorrelation(correlation));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Context context, int acceptation) {
        final Intent intent = new Intent(context, WordEditorActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        context.startActivity(intent);
    }

    private static final class FieldConversion {
        final int sourceField;
        final Conversion conversion;

        FieldConversion(int sourceField, Conversion conversion) {
            this.sourceField = sourceField;
            this.conversion = conversion;
        }
    }

    private ImmutableCorrelation getArgumentCorrelation() {
        final ParcelableCorrelation parcelable = getIntent().getParcelableExtra(ArgKeys.CORRELATION_MAP);
        return (parcelable != null)? parcelable.get() : ImmutableCorrelation.empty();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

        final String givenTitle = getIntent().getStringExtra(ArgKeys.TITLE);
        if (givenTitle != null) {
            setTitle(givenTitle);
        }

        _formPanel = findViewById(R.id.formPanel);
        findViewById(R.id.nextButton).setOnClickListener(this);

        _existingAcceptation = getIntent().getIntExtra(ArgKeys.ACCEPTATION, NO_ACCEPTATION);

        if (savedInstanceState != null) {
            _texts = savedInstanceState.getStringArray(SavedKeys.TEXTS);
        }

        updateFields();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CORRELATION_PICKER && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
        else if (requestCode == REQUEST_CODE_CHECK_CONVERSION) {
            _conversions.clear();
            updateConvertedTexts();
            updateFields();
        }
    }

    private int getLanguage(LangbookChecker checker) {
        if (_existingAcceptation != 0) {
            final ImmutablePair<ImmutableCorrelation, Integer> result = checker.readAcceptationTextsAndLanguage(_existingAcceptation);
            return result.right;
        }
        else {
            return getIntent().getIntExtra(ArgKeys.LANGUAGE, 0);
        }
    }

    private void updateConvertedTexts() {
        final LangbookManager manager = DbManager.getInstance().getManager();
        final int language = getLanguage(manager);
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
                        final Conversion conversion = _conversions.get(pair);

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

    private void updateFields() {
        _formPanel.removeAllViews();
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final ImmutableCorrelation existingTexts;
        final int language;
        if (_existingAcceptation != 0) {
            final ImmutablePair<ImmutableCorrelation, Integer> result = checker.readAcceptationTextsAndLanguage(_existingAcceptation);
            existingTexts = result.left;
            language = result.right;
        }
        else {
            existingTexts = getArgumentCorrelation();
            language = getIntent().getIntExtra(ArgKeys.LANGUAGE, 0);
        }

        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<AlphabetId, String> fieldNames;
        final ImmutableMap<AlphabetId, AlphabetId> fieldConversions;
        if (language == 0) {
            fieldNames = getArgumentCorrelation().keySet().assign(alphabet -> "");
            fieldConversions = ImmutableHashMap.empty();
        }
        else {
            fieldNames = checker.readAlphabetsForLanguage(language, preferredAlphabet);
            fieldConversions = checker.findConversions(fieldNames.keySet());
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
                final Conversion conversion = _conversions.get(pair);
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
            final ImmutableCorrelation.Builder builder = new ImmutableCorrelation.Builder();
            for (IntKeyMap.Entry<AlphabetId> entry : _fieldIndexAlphabetRelationMap.entries()) {
                builder.put(entry.value(), _texts[entry.key()]);
            }

            if (_existingAcceptation == NO_ACCEPTATION) {
                CorrelationPickerActivity.open(this, REQUEST_CODE_CORRELATION_PICKER,
                        getIntent().getIntExtra(ArgKeys.CONCEPT, NO_CONCEPT), builder.build());
            }
            else {
                CorrelationPickerActivity.open(this, REQUEST_CODE_CORRELATION_PICKER,
                        builder.build(), _existingAcceptation);
            }
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
}
