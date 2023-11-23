package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class AcceptationPickerActivityDelegate<Activity extends ActivityExtensions> extends SearchActivityDelegate<Activity> {

    public static final int REQUEST_CODE_CONFIRM = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface SavedKeys {
        String CONFIRM_DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
    }

    interface ResultKeys {
        String CHARACTER_COMPOSITION_TYPE_ID = BundleKeys.CHARACTER_COMPOSITION_TYPE_ID;
        String CONCEPT_USED = BundleKeys.CONCEPT_USED;
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
    }

    private AcceptationPickerActivityDelegate.Controller _controller;
    private AcceptationId _confirmDynamicAcceptation;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        super.onCreate(activity, savedInstanceState);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        if (savedInstanceState != null) {
            _confirmDynamicAcceptation = AcceptationIdBundler.read(savedInstanceState, SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION);
        }
    }

    @Override
    void openLanguagePicker(String query) {
        _controller.createAcceptation(new DefaultPresenter(_activity), query);
    }

    @Override
    void onAcceptationSelected(AcceptationId acceptation) {
        _confirmDynamicAcceptation = acceptation;
        _controller.selectAcceptation(new DefaultPresenter(_activity), acceptation);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, requestCode, resultCode, data, _confirmDynamicAcceptation);
    }

    @Override
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        super.onSaveInstanceState(activity, outState);
        AcceptationIdBundler.write(outState, SavedKeys.CONFIRM_DYNAMIC_ACCEPTATION, _confirmDynamicAcceptation);
    }

    public interface Controller extends Parcelable {
        void createAcceptation(@NonNull Presenter presenter, String queryText);
        void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation);
    }
}
