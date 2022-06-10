package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.SearchResult;

public final class AcceptationPickerActivity extends SearchActivity {

    public static final int REQUEST_CODE_VIEW_DETAILS = 1;

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface SavedKeys {
        String CONFIRM_DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
    }

    public interface ResultKeys {
        String CONCEPT_USED = BundleKeys.CONCEPT_USED;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
    }

    public static void open(Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, AcceptationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private Controller _controller;
    private AcceptationId _confirmDynamicAcceptation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        if (savedInstanceState != null) {
            _confirmDynamicAcceptation = AcceptationIdBundler.read(savedInstanceState, SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION);
        }
    }

    @Override
    void openLanguagePicker(String query) {
        _controller.createAcceptation(this, query);
    }

    @Override
    void onAcceptationSelected(AcceptationId acceptation) {
        _confirmDynamicAcceptation = acceptation;
        _controller.selectAcceptation(this, acceptation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data, _confirmDynamicAcceptation);
    }

    @Override
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        AcceptationIdBundler.write(outState, SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
    }

    public interface Controller extends Parcelable {
        void createAcceptation(@NonNull Activity activity, String queryText);
        void selectAcceptation(@NonNull Activity activity, @NonNull AcceptationId acceptation);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation);
    }
}
