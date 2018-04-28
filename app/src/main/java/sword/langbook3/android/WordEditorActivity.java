package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.IntKeyMap;
import sword.collections.IntSet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.AcceptationDetailsActivity.preferredAlphabet;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_LANGUAGE_PICKER = 1;
    private static final int NO_LANGUAGE = 0;

    private interface BundleKeys {
        String SEARCH_QUERY = "searchQuery";
    }

    private interface SavedKeys {
        String LANGUAGE = "lang";
    }

    private LinearLayout _formPanel;

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

    private void updateFields() {
        if (_language != NO_LANGUAGE) {
            _formPanel.removeAllViews();
            final ImmutableIntKeyMap<String> fieldNames = readAlphabets();
            final ImmutableIntPairMap fieldConversions = findConversions(fieldNames.keySet());

            final LayoutInflater inflater = getLayoutInflater();
            for (IntKeyMap.Entry<String> entry : fieldNames.entries()) {
                inflater.inflate(R.layout.word_editor_field_entry, _formPanel, true);
                View fieldEntry = _formPanel.getChildAt(_formPanel.getChildCount() - 1);

                final TextView textView = fieldEntry.findViewById(R.id.fieldName);
                textView.setText(entry.getValue());

                if (fieldConversions.keySet().contains(entry.getKey())) {
                    final EditText editText = fieldEntry.findViewById(R.id.fieldValue);
                    editText.setEnabled(false);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SavedKeys.LANGUAGE, _language);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
    }
}
