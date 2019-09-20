package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

public final class AcceptationPickerActivity extends SearchActivity {

    private static final int REQUEST_CODE_VIEW_DETAILS = 1;

    interface ArgKeys {
        String CONCEPT = BundleKeys.CONCEPT;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONCEPT_USED = BundleKeys.CONCEPT_USED;
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
    void openLanguagePicker(int requestCode, String query) {
        final int concept = getIntent().getIntExtra(ArgKeys.CONCEPT, 0);
        LanguagePickerActivity.open(this, requestCode, query, concept);
    }

    @Override
    void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation) {
        AcceptationDetailsActivity.open(this, REQUEST_CODE_VIEW_DETAILS, staticAcceptation, dynamicAcceptation, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Intent intent = new Intent();
            if (requestCode == REQUEST_CODE_VIEW_DETAILS) {
                intent.putExtra(ResultKeys.ACCEPTATION, data.getIntExtra(AcceptationDetailsActivity.ResultKeys.STATIC_ACCEPTATION, 0));
            }
            else {
                // When a new acceptation has been created
                intent.putExtra(ResultKeys.ACCEPTATION, data.getIntExtra(LanguagePickerActivity.ResultKeys.ACCEPTATION, 0));
                intent.putExtra(ResultKeys.CONCEPT_USED, true);
            }

            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
