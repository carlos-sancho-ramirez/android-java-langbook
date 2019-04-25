package sword.langbook3.android;

import android.app.Activity;
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

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.database.Database;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.db.Conversion;

import static sword.langbook3.android.CorrelationPickerActivity.NO_ACCEPTATION;
import static sword.langbook3.android.CorrelationPickerActivity.NO_CONCEPT;
import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.LangbookReadableDatabase.findAlphabetsByLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.findConversions;
import static sword.langbook3.android.db.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAcceptationTextsAndLanguage;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAlphabetsForLanguage;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_CORRELATION_PICKER = 1;
    private static final int REQUEST_CODE_CHECK_CONVERSION = 2;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String ALPHABETS = BundleKeys.ALPHABETS;
        String CONCEPT = BundleKeys.CONCEPT;
        String LANGUAGE = BundleKeys.LANGUAGE;
        String SEARCH_QUERY = BundleKeys.SEARCH_QUERY;
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
    private ImmutableIntPairMap _fieldIndexAlphabetRelationMap;
    private int _existingAcceptation = NO_ACCEPTATION;
    private final SyncCacheMap<ImmutableIntPair, Conversion> _conversions =
            new SyncCacheMap<>(pair -> getConversion(DbManager.getInstance().getDatabase(), pair));

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

    public static void open(Activity activity, int requestCode, IntSet alphabets, int concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);

        final int alphabetCount = alphabets.size();
        final int[] alphabetArray = new int[alphabetCount];
        for (int i = 0; i < alphabetCount; i++) {
            alphabetArray[i] = alphabets.valueAt(i);
        }

        intent.putExtra(ArgKeys.ALPHABETS, alphabetArray);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, int acceptation) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    private static final class FieldConversion {
        final int sourceField;
        final Conversion conversion;

        FieldConversion(int sourceField, Conversion conversion) {
            this.sourceField = sourceField;
            this.conversion = conversion;
        }
    }

    private ImmutableIntSet getAlphabets() {
        final int[] alphabets = getIntent().getIntArrayExtra(ArgKeys.ALPHABETS);
        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        for (int alphabet : alphabets) {
            builder.add(alphabet);
        }

        return builder.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

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

    private int getLanguage(Database db) {
        if (_existingAcceptation != 0) {
            final ImmutablePair<ImmutableIntKeyMap<String>, Integer> result = readAcceptationTextsAndLanguage(db, _existingAcceptation);
            return result.right;
        }
        else {
            return getIntent().getIntExtra(ArgKeys.LANGUAGE, 0);
        }
    }

    private void updateConvertedTexts() {
        final Database db = DbManager.getInstance().getDatabase();
        final int language = getLanguage(db);
        final ImmutableIntSet alphabets = findAlphabetsByLanguage(db, language);
        final ImmutableIntPairMap conversionMap = findConversions(db, alphabets);

        final int alphabetCount = alphabets.size();
        for (int targetFieldIndex = 0; targetFieldIndex < alphabetCount; targetFieldIndex++) {
            final int targetAlphabet = alphabets.valueAt(targetFieldIndex);
            final int sourceAlphabet = conversionMap.get(targetAlphabet, 0);
            final ImmutableIntPair alphabetPair = new ImmutableIntPair(sourceAlphabet, targetAlphabet);
            final int sourceFieldIndex = (sourceAlphabet != 0)? alphabets.indexOf(sourceAlphabet) : -1;
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
            ImmutableIntPairMap conversions, ImmutableIntPairMap fieldIndexAlphabetRelationMap,
            MutableIntKeyMap<String> queryConvertedTexts) {
        final MutableIntSet queryTextIsValid = MutableIntArraySet.empty();

        if (queryText != null) {
            final int editableFieldCount = fieldIndexAlphabetRelationMap.size();
            final int fieldConversionCount = conversions.size();

            for (int editableFieldIndex = 0; editableFieldIndex < editableFieldCount; editableFieldIndex++) {
                final int alphabet = fieldIndexAlphabetRelationMap.valueAt(editableFieldIndex);

                boolean isValid = true;
                final MutableIntKeyMap<String> localQueryConvertedTexts = MutableIntKeyMap.empty();

                ImmutableSet<String> sourceTexts = null;
                final MutableIntSet sourceTextAlphabets = MutableIntArraySet.empty();

                for (int conversionIndex = 0; conversionIndex < fieldConversionCount; conversionIndex++) {
                    if (conversions.valueAt(conversionIndex) == alphabet) {
                        final ImmutableIntPair pair = new ImmutableIntPair(alphabet, conversions.keyAt(conversionIndex));
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

                    for (IntKeyMap.Entry<String> entry : localQueryConvertedTexts.entries()) {
                        queryConvertedTexts.put(entry.key(), entry.value());
                    }
                }
                else if (sourceTexts != null && !sourceTexts.isEmpty()) {
                    for (int targetAlphabet : sourceTextAlphabets) {
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
        final Database db = DbManager.getInstance().getDatabase();
        final ImmutableIntKeyMap<String> existingTexts;
        final int language;
        if (_existingAcceptation != 0) {
            final ImmutablePair<ImmutableIntKeyMap<String>, Integer> result = readAcceptationTextsAndLanguage(db,
                    _existingAcceptation);
            existingTexts = result.left;
            language = result.right;
        }
        else {
            existingTexts = ImmutableIntKeyMap.empty();
            language = getIntent().getIntExtra(ArgKeys.LANGUAGE, 0);
        }

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableIntKeyMap<String> fieldNames;
        final ImmutableIntPairMap fieldConversions;
        if (language == 0) {
            fieldNames = getAlphabets().assign(alphabet -> "");
            fieldConversions = ImmutableIntPairMap.empty();
        }
        else {
            fieldNames = readAlphabetsForLanguage(db, language, preferredAlphabet);
            fieldConversions = findConversions(db, fieldNames.keySet());
        }

        final LayoutInflater inflater = getLayoutInflater();
        final int fieldCount = fieldNames.size();
        final String queryText = getIntent().getStringExtra(ArgKeys.SEARCH_QUERY);

        final ImmutableIntKeyMap.Builder<FieldConversion> builder = new ImmutableIntKeyMap.Builder<>();
        final ImmutableIntPairMap.Builder indexAlphabetBuilder = new ImmutableIntPairMap.Builder();

        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            final int alphabet = fieldNames.keyAt(fieldIndex);
            final int conversionIndex = fieldConversions.keySet().indexOf(alphabet);
            if (conversionIndex >= 0) {
                final ImmutableIntPair pair = new ImmutableIntPair(fieldConversions.valueAt(conversionIndex), fieldConversions.keyAt(conversionIndex));
                final Conversion conversion = _conversions.get(pair);
                final int sourceFieldIndex = fieldNames.keySet().indexOf(fieldConversions.valueAt(conversionIndex));
                builder.put(fieldIndex, new FieldConversion(sourceFieldIndex, conversion));
            }
            else {
                indexAlphabetBuilder.put(fieldIndex, alphabet);
            }
        }

        final ImmutableIntPairMap fieldIndexAlphabetRelationMap = indexAlphabetBuilder.build();
        final MutableIntKeyMap<String> queryConvertedTexts = MutableIntKeyMap.empty();

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
                final int target = fieldNames.keyAt(fieldIndex);
                final int source = fieldConversions.get(target);
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
            final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
            for (IntPairMap.Entry entry : _fieldIndexAlphabetRelationMap.entries()) {
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
