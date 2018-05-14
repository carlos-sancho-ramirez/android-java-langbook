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

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

abstract class SearchActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String AGENT_QUERY_PREFIX = "Agent ";
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
            updateSearchResults(ImmutableList.empty());
        }
        else {
            queryAllResults();
        }
    }

    private ImmutableList<SearchResult> querySearchResults() {
        final StringQueriesTable table = Tables.stringQueries;
        Cursor cursor = DbManager.getInstance().getReadableDatabase().rawQuery("SELECT " +
                table.columns().get(table.getStringColumnIndex()).name() + ',' +
                table.columns().get(table.getMainStringColumnIndex()).name() + ',' +
                table.columns().get(table.getMainAcceptationColumnIndex()).name() + ',' +
                table.columns().get(table.getDynamicAcceptationColumnIndex()).name() +
                " FROM " + table.name() + " WHERE " + table.columns().get(table.getStringColumnIndex()).name() + " LIKE '" + _query + "%'", null);

        ImmutableList<SearchResult> results = ImmutableList.empty();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>(cursor.getCount());
                    do {
                        final String str = cursor.getString(0);
                        final String mainStr = cursor.getString(1);
                        final int acc = cursor.getInt(2);
                        final int dynAcc = cursor.getInt(3);

                        builder.add(new SearchResult(str, mainStr, SearchResult.Types.ACCEPTATION, acc, dynAcc));
                    } while (cursor.moveToNext());
                    results = builder.build();
                }
            }
            finally {
                cursor.close();
            }
        }

        return results;
    }

    private ImmutableIntSet getAgentIds() {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            builder.add(row.get(0).toInt());
        }
        return builder.build();
    }

    private ImmutableList<SearchResult> agentSearchResults() {
        final ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>();
        for (int agentId : getAgentIds()) {
            String str = AGENT_QUERY_PREFIX + agentId;
            builder.add(new SearchResult(str, str, SearchResult.Types.AGENT, agentId, 0));
        }

        return builder.build();
    }

    boolean includeAgentsAsResult() {
        return false;
    }

    private boolean possibleString(String str) {
        return str != null && _query != null && ((str.length() > _query.length())?
                str.toLowerCase().startsWith(_query.toLowerCase()) :
                _query.toLowerCase().startsWith(str.toLowerCase()));
    }

    private void queryAllResults() {
        ImmutableList<SearchResult> results = querySearchResults();
        if (includeAgentsAsResult() && _query != null && possibleString(AGENT_QUERY_PREFIX)) {
            results = results.appendAll(agentSearchResults().filter(entry -> possibleString(entry.getStr())));
        }

        updateSearchResults(results);
    }

    private void updateSearchResults(ImmutableList<SearchResult> results) {
        _listAdapter = new SearchResultAdapter(results);
        _listView.setAdapter(_listAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SearchResult item = _listAdapter.getItem(position);
        switch (item.getType()) {
            case SearchResult.Types.ACCEPTATION:
                onAcceptationSelected(item.getId(), item.getAuxiliarId());
                break;

            case SearchResult.Types.AGENT:
                AgentDetailsActivity.open(this, item.getId());
                break;

            default:
                throw new AssertionError();
        }
    }

    abstract void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation);
    abstract void openLanguagePicker(int requestCode, String query);

    @Override
    public void onClick(View v) {
        openLanguagePicker(REQUEST_CODE_NEW_ACCEPTATION, _query);
    }
}
