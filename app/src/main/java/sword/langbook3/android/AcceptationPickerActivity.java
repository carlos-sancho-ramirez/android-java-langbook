package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;

public final class AcceptationPickerActivity extends SearchActivity {

    interface ArgKeys {
        String CONCEPT = BundleKeys.CONCEPT;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONCEPT_USED = BundleKeys.CONCEPT_USED;
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
        final Intent intent = new Intent();
        intent.putExtra(ResultKeys.ACCEPTATION, staticAcceptation);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Intent intent = new Intent();
            intent.putExtra(ResultKeys.ACCEPTATION, data.getIntExtra(LanguagePickerActivity.ResultKeys.ACCEPTATION, 0));
            intent.putExtra(ResultKeys.CONCEPT_USED, true);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
