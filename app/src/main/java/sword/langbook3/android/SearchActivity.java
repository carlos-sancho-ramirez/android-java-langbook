package sword.langbook3.android;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.List;
import sword.database.DbQuery;
import sword.database.DbValue;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.db.SearchResult;

import static sword.langbook3.android.db.LangbookReadableDatabase.findAcceptationFromText;

abstract class SearchActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    static final String AGENT_QUERY_PREFIX = "Agent ";

    static final int REQUEST_CODE_WELCOME = 1;
    private static final int REQUEST_CODE_NEW_ACCEPTATION = 2;

    private ListView _listView;
    private SearchResultAdapter _listAdapter;
    private String _query;

    interface ArgKeys {
        String TEXT = BundleKeys.TEXT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);

        findViewById(R.id.addWordButton).setOnClickListener(this);
        _query = getIntent().getStringExtra(ArgKeys.TEXT);

        prepareSearchField(findViewById(R.id.searchField));
    }

    private void prepareSearchField(EditText searchField) {
        if (_query != null) {
            searchField.setText(_query);
        }

        if (isQueryModifiable()) {
            searchField.addTextChangedListener(this);
        }
        else {
            searchField.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSearchResults();
    }

    private void updateSearchResults() {
        if (TextUtils.isEmpty(_query)) {
            updateSearchResults(noQueryResults());
        }
        else {
            queryAllResults();
        }
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
        updateSearchResults();
    }

    ImmutableList<SearchResult> noQueryResults() {
        return ImmutableList.empty();
    }

    boolean isQueryModifiable() {
        return true;
    }

    int getSearchRestrictionType() {
        return DbQuery.RestrictionStringTypes.STARTS_WITH;
    }

    boolean includeAgentsAsResult() {
        return false;
    }

    private ImmutableIntSet getAgentIds() {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());
        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        for (List<DbValue> row : DbManager.getInstance().attach(query)) {
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

    private boolean possibleString(String str) {
        return str != null && _query != null && ((str.length() > _query.length())?
                str.toLowerCase().startsWith(_query.toLowerCase()) :
                _query.toLowerCase().startsWith(str.toLowerCase()));
    }

    ImmutableList<SearchResult> queryAcceptationResults(String query) {
        return findAcceptationFromText(DbManager.getInstance().getDatabase(), query, getSearchRestrictionType());
    }

    final void queryAllResults() {
        ImmutableList<SearchResult> results = queryAcceptationResults(_query);
        if (includeAgentsAsResult() && _query != null && possibleString(AGENT_QUERY_PREFIX)) {
            results = results.appendAll(agentSearchResults().filter(entry -> possibleString(entry.getStr())));
        }

        updateSearchResults(results);
    }

    SearchResultAdapter createAdapter(ImmutableList<SearchResult> results) {
        return new SearchResultAdapter(results, null);
    }

    private void updateSearchResults(ImmutableList<SearchResult> results) {
        _listAdapter = createAdapter(results);
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
