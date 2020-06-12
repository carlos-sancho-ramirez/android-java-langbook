package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.database.DbQuery;
import sword.langbook3.android.models.SearchResult;

public final class FixedTextAcceptationPickerActivity extends SearchActivity {

    private static final int REQUEST_CODE_VIEW_DETAILS = 1;

    interface ResultKeys {
        String STATIC_ACCEPTATION = BundleKeys.ACCEPTATION;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
    }

    public static void open(Activity activity, int requestCode, String text) {
        final Intent intent = new Intent(activity, FixedTextAcceptationPickerActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableIntKeyMap<String> _ruleTexts;
    private int _confirmDynamicAcceptation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _confirmDynamicAcceptation = savedInstanceState.getInt(AcceptationPickerActivity.SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION);
        }
    }

    @Override
    boolean isQueryModifiable() {
        return false;
    }

    @Override
    int getSearchRestrictionType() {
        return DbQuery.RestrictionStringTypes.EXACT;
    }

    @Override
    ImmutableList<SearchResult> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationAndRulesFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    SearchResultAdapter createAdapter(ImmutableList<SearchResult> results) {
        if (_ruleTexts == null) {
            final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            _ruleTexts = DbManager.getInstance().getManager().readAllRules(preferredAlphabet);
        }

        return new SearchResultAdapter(results, _ruleTexts);
    }

    @Override
    void openLanguagePicker(String query) {
        LanguagePickerActivity.open(this, REQUEST_CODE_NEW_ACCEPTATION, query);
    }

    @Override
    void onAcceptationSelected(int acceptation) {
        _confirmDynamicAcceptation = acceptation;
        AcceptationDetailsActivity.open(this, REQUEST_CODE_VIEW_DETAILS, acceptation, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Intent intent = new Intent();
            final int staticAcc;
            final int dynamicAcc;
            if (requestCode == REQUEST_CODE_VIEW_DETAILS) {
                staticAcc = data.getIntExtra(AcceptationDetailsActivity.ResultKeys.ACCEPTATION, 0);
                dynamicAcc = _confirmDynamicAcceptation;
            }
            else {
                staticAcc = data.getIntExtra(LanguagePickerActivity.ResultKeys.ACCEPTATION, 0);
                dynamicAcc = staticAcc;
            }

            intent.putExtra(ResultKeys.STATIC_ACCEPTATION, staticAcc);
            intent.putExtra(ResultKeys.DYNAMIC_ACCEPTATION, dynamicAcc);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(AcceptationPickerActivity.SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
    }
}
