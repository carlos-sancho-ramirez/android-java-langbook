package sword.langbook3.android;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

public class SearchActivity extends Activity implements TextWatcher {

    private DbManager _dbManager;
    private ListView _listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        _listView = findViewById(R.id.listView);

        final EditText searchField = findViewById(R.id.searchField);
        searchField.addTextChangedListener(this);

        _dbManager = new DbManager(this);
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
        querySearchResults(editable.toString());
    }

    private void querySearchResults(String query) {
        final DbManager.StringQueriesTable table = DbManager.Tables.stringQueries;
        Cursor cursor = _dbManager.getReadableDatabase().rawQuery("SELECT " +
                table.getColumnName(table.getStringColumnIndex()) + ',' +
                table.getColumnName(table.getMainStringColumnIndex()) + ',' +
                table.getColumnName(table.getMainAcceptationColumnIndex()) + ',' +
                table.getColumnName(table.getDynamicAcceptationColumnIndex()) +
                " FROM " + table.getName() + " WHERE " + table.getColumnName(table.getStringColumnIndex()) + " LIKE '" + query + "%'", null);

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
        _listView.setAdapter(new SearchResultAdapter(results));
    }
}
