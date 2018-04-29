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

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.collections.IntSet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.AcceptationDetailsActivity.preferredAlphabet;
import static sword.langbook3.android.EqualUtils.equal;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_LANGUAGE_PICKER = 1;
    private static final int NO_LANGUAGE = 0;

    private interface BundleKeys {
        String SEARCH_QUERY = "searchQuery";
    }

    private interface SavedKeys {
        String LANGUAGE = "lang";
        String TEXTS = "texts";
    }

    private LinearLayout _formPanel;
    private ImmutableIntKeyMap<FieldConversion> _fieldConversions;
    private String[] _texts;

    private int _language = NO_LANGUAGE;

    public static void open(Context context, String searchQuery) {
        final Intent intent = new Intent(context, WordEditorActivity.class);
        intent.putExtra(BundleKeys.SEARCH_QUERY, searchQuery);
        context.startActivity(intent);
    }

    private ImmutableIntKeyMap<String> readAlphabets() {
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .where(alphabets.getLanguageColumnIndex(), _language)
                .select(
                        alphabets.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex());
        final MutableIntSet foundAlphabets = MutableIntSet.empty();
        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            final int id = row.get(0).toInt();
            final int strAlphabet = row.get(1).toInt();

            if (strAlphabet == preferredAlphabet || !foundAlphabets.contains(id)) {
                foundAlphabets.add(id);
                result.put(id, row.get(2).toText());
            }
        }

        return result.toImmutable();
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
        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
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

    private static final class StringPair {
        final String source;
        final String target;

        StringPair(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }

    private static final class FieldConversion {
        final int sourceField;
        final ImmutableList<StringPair> textPairs;

        FieldConversion(int sourceField, ImmutableList<StringPair> textPairs) {
            this.sourceField = sourceField;
            this.textPairs = textPairs;
        }
    }

    private ImmutableList<StringPair> readConversion(int source, int target) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;
        final LangbookDbSchema.SymbolArraysTable symbols = LangbookDbSchema.Tables.symbolArrays;

        final int off1Symbols = conversions.columns().size();
        final int off2Symbols = off1Symbols + symbols.columns().size();

        final DbQuery query = new DbQuery.Builder(conversions)
                .join(symbols, conversions.getSourceColumnIndex(), symbols.getIdColumnIndex())
                .join(symbols, conversions.getTargetColumnIndex(), symbols.getIdColumnIndex())
                .where(conversions.getSourceAlphabetColumnIndex(), source)
                .where(conversions.getTargetAlphabetColumnIndex(), target)
                .select(
                        off1Symbols + symbols.getStrColumnIndex(),
                        off2Symbols + symbols.getStrColumnIndex());

        final ImmutableList.Builder<StringPair> builder = new ImmutableList.Builder<>();
        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            final String sourceText = row.get(0).toText();
            final String targetText = row.get(1).toText();
            builder.add(new StringPair(sourceText, targetText));
        }

        return builder.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

        _formPanel = findViewById(R.id.formPanel);
        findViewById(R.id.nextButton).setOnClickListener(this);

        if (savedInstanceState == null) {
            _language = NO_LANGUAGE;
            LanguagePickerActivity.open(this, REQUEST_CODE_LANGUAGE_PICKER);
        }
        else {
            _language = savedInstanceState.getInt(SavedKeys.LANGUAGE, NO_LANGUAGE);
            _texts = savedInstanceState.getStringArray(SavedKeys.TEXTS);
            updateFields();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LANGUAGE_PICKER) {
            if (resultCode == RESULT_OK) {
                _language = data.getIntExtra(LanguagePickerActivity.ResultKeys.LANGUAGE, 0);
                updateFields();
            }
            else {
                finish();
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

    private void updateFields() {
        if (_language != NO_LANGUAGE) {
            _formPanel.removeAllViews();
            final ImmutableIntKeyMap<String> fieldNames = readAlphabets();
            final ImmutableIntPairMap fieldConversions = findConversions(fieldNames.keySet());

            final LayoutInflater inflater = getLayoutInflater();
            final int fieldCount = fieldNames.size();

            if (_texts == null) {
                _texts = new String[fieldCount];
            }

            final ImmutableIntKeyMap.Builder<FieldConversion> builder = new ImmutableIntKeyMap.Builder<>();
            for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
                inflater.inflate(R.layout.word_editor_field_entry, _formPanel, true);
                View fieldEntry = _formPanel.getChildAt(fieldIndex);

                final TextView textView = fieldEntry.findViewById(R.id.fieldName);
                textView.setText(fieldNames.valueAt(fieldIndex));

                final EditText editText = fieldEntry.findViewById(R.id.fieldValue);
                final int alphabet = fieldNames.keyAt(fieldIndex);
                final int conversionIndex = fieldConversions.keySet().indexOf(alphabet);
                if (conversionIndex >= 0) {
                    final ImmutableList<StringPair> conversion = readConversion(
                            fieldConversions.valueAt(conversionIndex), fieldConversions.keyAt(conversionIndex));
                    final int sourceFieldIndex = fieldNames.keySet().indexOf(fieldConversions.valueAt(conversionIndex));
                    builder.put(fieldIndex, new FieldConversion(sourceFieldIndex, conversion));

                    setConversionText(editText, _texts[fieldIndex]);
                    editText.setEnabled(false);
                }
                else {
                    editText.setText(_texts[fieldIndex]);
                    editText.addTextChangedListener(new FieldTextWatcher(fieldIndex));
                }
            }

            _fieldConversions = builder.build();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SavedKeys.LANGUAGE, _language);
        outState.putStringArray(SavedKeys.TEXTS, _texts);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private String convertText(ImmutableList<StringPair> pairs, String text) {
        String result = "";
        while (text.length() > 0) {
            boolean found = false;
            for (StringPair pair : pairs) {
                if (text.startsWith(pair.source)) {
                    result += pair.target;
                    text = text.substring(pair.source.length());
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return result;
    }

    private void updateText(int fieldIndex, String newText) {
        String oldText = _texts[fieldIndex];
        _texts[fieldIndex] = newText;

        for (IntKeyMap.Entry<FieldConversion> entry : _fieldConversions.entries()) {
            if (entry.getValue().sourceField == fieldIndex && !equal(oldText, newText)) {
                String convertedText = convertText(entry.getValue().textPairs, newText);
                _texts[entry.getKey()] = convertedText;

                final EditText editText = _formPanel.getChildAt(entry.getKey()).findViewById(R.id.fieldValue);
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
