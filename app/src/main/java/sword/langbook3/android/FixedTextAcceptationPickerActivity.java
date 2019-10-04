package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

import sword.collections.ImmutableIntKeyMap;
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
        return DbManager.getInstance().getManager().findAcceptationAndRulesFromText(query, getSearchRestrictionType());
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
    void openLanguagePicker(int requestCode, String query) {
        LanguagePickerActivity.open(this, requestCode, query);
    }

    @Override
    void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation) {
        AcceptationDetailsActivity.open(this, REQUEST_CODE_VIEW_DETAILS, staticAcceptation, dynamicAcceptation, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Intent intent = new Intent();
            final int staticAcc;
            final int dynamicAcc;
            if (requestCode == REQUEST_CODE_VIEW_DETAILS) {
                staticAcc = data.getIntExtra(AcceptationDetailsActivity.ResultKeys.STATIC_ACCEPTATION, 0);
                dynamicAcc = data.getIntExtra(AcceptationDetailsActivity.ResultKeys.DYNAMIC_ACCEPTATION, 0);
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
}
