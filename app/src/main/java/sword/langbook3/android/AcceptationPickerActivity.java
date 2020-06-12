package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.langbook3.android.models.SearchResult;

public final class AcceptationPickerActivity extends SearchActivity {

    private static final int REQUEST_CODE_VIEW_DETAILS = 1;
    private int _confirmDynamicAcceptation;

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

    public static void open(Activity activity, int requestCode, int concept) {
        final Intent intent = new Intent(activity, AcceptationPickerActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _confirmDynamicAcceptation = savedInstanceState.getInt(SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION);
        }
    }

    @Override
    void openLanguagePicker(String query) {
        final int concept = getIntent().getIntExtra(ArgKeys.CONCEPT, 0);
        LanguagePickerActivity.open(this, REQUEST_CODE_NEW_ACCEPTATION, query, concept);
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
            if (requestCode == REQUEST_CODE_VIEW_DETAILS) {
                intent.putExtra(ResultKeys.STATIC_ACCEPTATION, data.getIntExtra(AcceptationDetailsActivity.ResultKeys.ACCEPTATION, 0));
                intent.putExtra(ResultKeys.DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
            }
            else {
                // When a new acceptation has been created
                intent.putExtra(ResultKeys.STATIC_ACCEPTATION, data.getIntExtra(LanguagePickerActivity.ResultKeys.ACCEPTATION, 0));
                intent.putExtra(ResultKeys.CONCEPT_USED, true);
            }

            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    ImmutableList<SearchResult> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
    }
}
