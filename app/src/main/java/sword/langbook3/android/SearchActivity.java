package sword.langbook3.android;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.Tables;

abstract class SearchActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    private static final int REQUEST_CODE_NEW_ACCEPTATION = 1;

    private ListView _listView;
    private SearchResultAdapter _listAdapter;
    private String _query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);

        findViewById(R.id.addWordButton).setOnClickListener(this);

        final EditText searchField = findViewById(R.id.searchField);
        searchField.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Nothing to be done
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Nothing to be done
    }

    @Override
    public void afterTextChanged(Editable editable) {
        _query = editable.toString();
        if (TextUtils.isEmpty(_query)) {
            updateSearchResults(new SearchResult[0]);
        }
        else {
            querySearchResults();
        }
    }

    private void querySearchResults() {
        final StringQueriesTable table = Tables.stringQueries;
        Cursor cursor = DbManager.getInstance().getReadableDatabase().rawQuery("SELECT " +
                table.columns().get(table.getStringColumnIndex()).name() + ',' +
                table.columns().get(table.getMainStringColumnIndex()).name() + ',' +
                table.columns().get(table.getMainAcceptationColumnIndex()).name() + ',' +
                table.columns().get(table.getDynamicAcceptationColumnIndex()).name() +
                " FROM " + table.name() + " WHERE " + table.columns().get(table.getStringColumnIndex()).name() + " LIKE '" + _query + "%'", null);

        SearchResult[] results = new SearchResult[0];
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    results = new SearchResult[cursor.getCount()];
                    int i = 0;
                    do {
                        final String str = cursor.getString(0);
                        final String mainStr = cursor.getString(1);
                        final int acc = cursor.getInt(2);
                        final int dynAcc = cursor.getInt(3);

                        results[i++] = new SearchResult(str, mainStr, acc, dynAcc);
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        updateSearchResults(results);
    }

    private void updateSearchResults(SearchResult[] results) {
        _listAdapter = new SearchResultAdapter(results);
        _listView.setAdapter(_listAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SearchResult item = _listAdapter.getItem(position);
        onAcceptationSelected(item.getAcceptation(), item.getDynamicAcceptation());
    }

    abstract void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation);
    abstract void openLanguagePicker(int requestCode, String query);

    @Override
    public void onClick(View v) {
        openLanguagePicker(REQUEST_CODE_NEW_ACCEPTATION, _query);
    }
}
