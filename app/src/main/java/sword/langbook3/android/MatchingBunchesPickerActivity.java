package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.Set;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.ImmutableCorrelation;

public final class MatchingBunchesPickerActivity extends Activity implements View.OnClickListener {

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface ResultKeys {
        String BUNCH_SET = BundleKeys.BUNCH_SET;
    }

    private Controller _controller;
    private MatchingBunchesPickerAdapter _adapter;

    public static void open(Activity activity, int requestCode, Controller controller) {
        final Intent intent = new Intent(activity, MatchingBunchesPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matching_bunches_picker_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        final ListView listView = findViewById(R.id.listView);
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_controller.getTexts(), preferredAlphabet);

        if (bunches.isEmpty()) {
            final Intent intent = new Intent();
            BunchIdBundler.writeListAsIntentExtra(intent, ResultKeys.BUNCH_SET, ImmutableList.empty());
            setResult(RESULT_OK, intent);
            finish();
        }
        else {
            _adapter = new MatchingBunchesPickerAdapter(bunches);
            listView.setAdapter(_adapter);

            findViewById(R.id.nextButton).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        _controller.complete(this, _adapter.getCheckedBunches());
    }

    public interface Controller extends Parcelable {
        @NonNull
        ImmutableCorrelation<AlphabetId> getTexts();
        void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches);
    }
}
