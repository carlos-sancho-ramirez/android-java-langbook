package sword.langbook3.android;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import sword.collections.ImmutableList;
import sword.database.DbQuery;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.SearchResult;

abstract class SearchActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    static final int MAX_RESULTS = 300;
    static final String AGENT_QUERY_PREFIX = "Agent ";

    static final int REQUEST_CODE_WELCOME = 1;
    static final int REQUEST_CODE_NEW_ACCEPTATION = 2;
    static final int REQUEST_CODE_OPEN_SETTINGS = 3;

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
            searchField.setOnTouchListener((v, event) -> {
                final int paddingRight = ((TextView) v).getTotalPaddingRight();
                if (event.getX() >= v.getWidth() - paddingRight) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        searchField.setText(null);
                    }
                    return true;
                }
                return false;
            });
        }
        else {
            searchField.setEnabled(false);
            searchField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSearchResults();
    }

    private void updateSearchResults() {
        if (TextUtils.isEmpty(_query)) {
            updateSearchResults(noQueryResults().map(result -> result));
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

    ImmutableList<SearchResult<AcceptationId, RuleId>> noQueryResults() {
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

    private ImmutableList<SearchResult<AgentId, RuleId>> agentSearchResults() {
        return DbManager.getInstance().getManager().getAgentIds().map(agentId -> {
            final String str = AGENT_QUERY_PREFIX + agentId.composeHumanReadableName();
            return new SearchResult<>(str, str, agentId, false);
        });
    }

    private boolean possibleString(String str) {
        return str != null && _query != null && ((str.length() > _query.length())?
                str.toLowerCase().startsWith(_query.toLowerCase()) :
                _query.toLowerCase().startsWith(str.toLowerCase()));
    }

    ImmutableList<SearchResult<CharacterId, Object>> queryCharacterResults(String query) {
        // Characters are not listed for all implementations
        return ImmutableList.empty();
    }

    abstract ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query);

    final void queryAllResults() {
        ImmutableList<SearchResult> results = ImmutableList.empty();
        results = results.appendAll(queryCharacterResults(_query).map(result -> result));
        results = results.appendAll(queryAcceptationResults(_query).map(result -> result));
        if (includeAgentsAsResult() && _query != null && possibleString(AGENT_QUERY_PREFIX)) {
            results = results.appendAll(agentSearchResults().filter(entry -> possibleString(entry.getStr())).map(result -> result));
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
        final Object itemId = _listAdapter.getItem(position).getId();
        if (itemId instanceof CharacterId) {
            CharacterDetailsActivity.open(this, (CharacterId) itemId);
        }
        else if (itemId instanceof AcceptationId) {
            onAcceptationSelected((AcceptationId) itemId);
        }
        else {
            AgentDetailsActivity.open(this, (AgentId) itemId);
        }
    }

    abstract void onAcceptationSelected(AcceptationId acceptation);
    abstract void openLanguagePicker(String query);

    @Override
    public void onClick(View v) {
        openLanguagePicker(_query);
    }
}
