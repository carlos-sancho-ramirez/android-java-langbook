package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdBundler;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.SearchResult;

public final class AcceptationPickerActivity extends SearchActivity {

    private static final int REQUEST_CODE_VIEW_DETAILS = 1;
    private AcceptationId _confirmDynamicAcceptation;

    interface ArgKeys {
        String CONCEPT = BundleKeys.CONCEPT;
    }

    interface SavedKeys {
        String CONFIRM_DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
    }

    interface ResultKeys {
        String CONCEPT_USED = BundleKeys.CONCEPT_USED;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
    }

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, AcceptationPickerActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, ConceptId concept) {
        final Intent intent = new Intent(activity, AcceptationPickerActivity.class);
        ConceptIdBundler.writeAsIntentExtra(intent, ArgKeys.CONCEPT, concept);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _confirmDynamicAcceptation = AcceptationIdBundler.read(savedInstanceState, SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION);
        }
    }

    @Override
    void openLanguagePicker(String query) {
        final ConceptId concept = ConceptIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CONCEPT);
        LanguagePickerActivity.open(this, REQUEST_CODE_NEW_ACCEPTATION, query, concept);
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
            if (requestCode == REQUEST_CODE_VIEW_DETAILS) {
                AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.STATIC_ACCEPTATION, AcceptationIdBundler.readAsIntentExtra(data, AcceptationDetailsActivity.ResultKeys.ACCEPTATION));
                AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
            }
            else {
                // When a new acceptation has been created
                AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.STATIC_ACCEPTATION, AcceptationIdBundler.readAsIntentExtra(data, LanguagePickerActivity.ResultKeys.ACCEPTATION));
                intent.putExtra(ResultKeys.CONCEPT_USED, true);
            }

            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AcceptationIdBundler.write(outState, SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
    }
}
