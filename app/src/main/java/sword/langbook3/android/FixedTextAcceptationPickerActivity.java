package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.database.DbQuery;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.RuleId;
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

    private ImmutableMap<RuleId, String> _ruleTexts;
    private AcceptationId _confirmDynamicAcceptation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _confirmDynamicAcceptation = AcceptationIdBundler.read(savedInstanceState, AcceptationPickerActivity.SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION);
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
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationAndRulesFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    SearchResultAdapter createAdapter(ImmutableList<SearchResult> results) {
        if (_ruleTexts == null) {
            final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            _ruleTexts = DbManager.getInstance().getManager().readAllRules(preferredAlphabet);
        }

        return new SearchResultAdapter(results, _ruleTexts);
    }

    @Override
    void openLanguagePicker(String query) {
        LanguagePickerActivity.open(this, REQUEST_CODE_NEW_ACCEPTATION, query);
    }

    @Override
    void onAcceptationSelected(AcceptationId acceptation) {
        _confirmDynamicAcceptation = acceptation;
        AcceptationDetailsActivity.open(this, REQUEST_CODE_VIEW_DETAILS, acceptation, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Intent intent = new Intent();
            final AcceptationId staticAcc;
            final AcceptationId dynamicAcc;
            if (requestCode == REQUEST_CODE_VIEW_DETAILS) {
                staticAcc = AcceptationIdBundler.readAsIntentExtra(data, AcceptationDetailsActivity.ResultKeys.ACCEPTATION);
                dynamicAcc = _confirmDynamicAcceptation;
            }
            else {
                staticAcc = AcceptationIdBundler.readAsIntentExtra(data, LanguagePickerActivity.ResultKeys.ACCEPTATION);
                dynamicAcc = staticAcc;
            }

            AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.STATIC_ACCEPTATION, staticAcc);
            AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.DYNAMIC_ACCEPTATION, dynamicAcc);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AcceptationIdBundler.write(outState, AcceptationPickerActivity.SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
    }
}
