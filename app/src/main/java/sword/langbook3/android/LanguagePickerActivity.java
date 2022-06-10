package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LanguageId;

public final class LanguagePickerActivity extends Activity implements ListView.OnItemClickListener {

    public static final int REQUEST_CODE_NEW_WORD = 1;

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CORRELATION_ARRAY = BundleKeys.CORRELATION_ARRAY;
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, LanguagePickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private Controller _controller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_picker_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        final ListView listView = findViewById(R.id.listView);
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        listView.setAdapter(new LanguagePickerAdapter(DbManager.getInstance().getManager().readAllLanguages(preferredAlphabet)));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final LanguageId languageId = ((LanguagePickerAdapter) parent.getAdapter()).getItem(position);
        _controller.complete(this, languageId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void complete(@NonNull Activity activity, @NonNull LanguageId language);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
