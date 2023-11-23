package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.LanguagePickerAdapter;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class LanguagePickerActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements ListView.OnItemClickListener {
    public static final int REQUEST_CODE_NEW_WORD = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CHARACTER_COMPOSITION_TYPE_ID = BundleKeys.CHARACTER_COMPOSITION_TYPE_ID;
        String CORRELATION_ARRAY = BundleKeys.CORRELATION_ARRAY;
    }

    private Activity _activity;
    private Controller _controller;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.language_picker_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        final ListView listView = activity.findViewById(R.id.listView);
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        listView.setAdapter(new LanguagePickerAdapter(DbManager.getInstance().getManager().readAllLanguages(preferredAlphabet)));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final LanguageId languageId = ((LanguagePickerAdapter) parent.getAdapter()).getItem(position);
        _controller.complete(new DefaultPresenter(_activity), languageId);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void complete(@NonNull Presenter presenter, @NonNull LanguageId language);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);
    }
}
