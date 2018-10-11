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
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbValue;

import static sword.langbook3.android.CorrelationPickerActivity.NO_ACCEPTATION;
import static sword.langbook3.android.CorrelationPickerActivity.NO_CONCEPT;
import static sword.langbook3.android.EqualUtils.equal;
import static sword.langbook3.android.LangbookDatabaseUtils.convertText;
import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.readAcceptationTextsAndLanguage;
import static sword.langbook3.android.LangbookReadableDatabase.readAlphabetsForLanguage;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_CORRELATION_PICKER = 1;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
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

    public static void open(Activity activity, int requestCode, int acceptation) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableIntPairMap findConversions(IntSet alphabets) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final MutableIntSet foundAlphabets = MutableIntSet.empty();
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        for (List<DbValue> row : DbManager.getInstance().attach(query)) {
            final int source = row.get(0).toInt();
            final int target = row.get(1).toInt();

            if (foundAlphabets.contains(target)) {
                throw new AssertionError();
            }
            foundAlphabets.add(target);

            if (alphabets.contains(target)) {
                if (!alphabets.contains(source)) {
                    throw new AssertionError();
                }

                builder.put(target, source);
            }
        }

        return builder.build();
    }

    private static final class FieldConversion {
        final int sourceField;
        final ImmutableList<ImmutablePair<String, String>> textPairs;

        FieldConversion(int sourceField, ImmutableList<ImmutablePair<String, String>> textPairs) {
            this.sourceField = sourceField;
            this.textPairs = textPairs;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

        _formPanel = findViewById(R.id.formPanel);
        findViewById(R.id.nextButton).setOnClickListener(this);

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
    }

    private static void setConversionText(EditText editText, String text) {
        if (text == null) {
            editText.setText(R.string.wordEditorWrongConversionFieldText);
        }
        else {
            editText.setText(text);
        }
    }

    private void updateFields() {
        _formPanel.removeAllViews();
        final Database db = DbManager.getInstance().getDatabase();
        _existingAcceptation = getIntent().getIntExtra(ArgKeys.ACCEPTATION, NO_ACCEPTATION);
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

        if (language == 0) {
            throw new AssertionError();
        }

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableIntKeyMap<String> fieldNames = readAlphabetsForLanguage(db, language, preferredAlphabet);
        final ImmutableIntPairMap fieldConversions = findConversions(fieldNames.keySet());

        final LayoutInflater inflater = getLayoutInflater();
        final int fieldCount = fieldNames.size();

        boolean autoSelectText = false;
        if (_texts == null) {
            _texts = new String[fieldCount];
        }

        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            _texts[fieldIndex] = existingTexts.get(fieldNames.keyAt(fieldIndex), null);

            if (fieldCount == 1 && _texts[0] == null) {
                _texts[0] = getIntent().getStringExtra(ArgKeys.SEARCH_QUERY);
                autoSelectText = true;
            }
        }

        final ImmutableIntKeyMap.Builder<FieldConversion> builder = new ImmutableIntKeyMap.Builder<>();
        final ImmutableIntPairMap.Builder indexAlphabetBuilder = new ImmutableIntPairMap.Builder();

        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            inflater.inflate(R.layout.word_editor_field_entry, _formPanel, true);
            View fieldEntry = _formPanel.getChildAt(fieldIndex);

            final TextView textView = fieldEntry.findViewById(R.id.fieldName);
            textView.setText(fieldNames.valueAt(fieldIndex));

            final EditText editText = fieldEntry.findViewById(R.id.fieldValue);
            final int alphabet = fieldNames.keyAt(fieldIndex);
            final int conversionIndex = fieldConversions.keySet().indexOf(alphabet);
            if (conversionIndex >= 0) {
                final ImmutableIntPair pair = new ImmutableIntPair(fieldConversions.valueAt(conversionIndex), fieldConversions.keyAt(conversionIndex));
                final ImmutableList<ImmutablePair<String, String>> conversion = getConversion(DbManager.getInstance().getDatabase(), pair);
                final int sourceFieldIndex = fieldNames.keySet().indexOf(fieldConversions.valueAt(conversionIndex));
                builder.put(fieldIndex, new FieldConversion(sourceFieldIndex, conversion));

                setConversionText(editText, _texts[fieldIndex]);
                editText.setEnabled(false);
            }
            else {
                final String text = _texts[fieldIndex];
                editText.setText(_texts[fieldIndex]);
                if (autoSelectText && text != null) {
                    editText.setSelection(0, text.length());
                }

                editText.addTextChangedListener(new FieldTextWatcher(fieldIndex));
                indexAlphabetBuilder.put(fieldIndex, alphabet);
            }
        }

        _fieldConversions = builder.build();
        _fieldIndexAlphabetRelationMap = indexAlphabetBuilder.build();
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
                String convertedText = convertText(entry.value().textPairs, newText);
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
