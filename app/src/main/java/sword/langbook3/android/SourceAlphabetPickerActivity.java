package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class SourceAlphabetPickerActivity extends Activity {

    public static final int REQUEST_CODE_NEW_CONVERSION = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, SourceAlphabetPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private final Presenter _presenter = new DefaultPresenter(this);
    private Controller _controller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.source_alphabet_picker_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.load(_presenter, sourceAlphabetTexts -> {
            final AlphabetAdapter adapter = new AlphabetAdapter(sourceAlphabetTexts);
            final Spinner sourceAlphabetSpinner = findViewById(R.id.sourceAlphabetSpinner);
            sourceAlphabetSpinner.setAdapter(adapter);
            findViewById(R.id.confirmButton).setOnClickListener(v -> {
                final Controller.CreationOption creationOption = this.<CheckBox>findViewById(R.id.defineConversionCheckBox).isChecked()?
                        Controller.CreationOption.DEFINE_CONVERSION : Controller.CreationOption.COPY;
                final AlphabetId sourceAlphabet = sourceAlphabetTexts.keyAt(this.<Spinner>findViewById(R.id.sourceAlphabetSpinner).getSelectedItemPosition());
                _controller.complete(new DefaultPresenter(this), sourceAlphabet, creationOption);
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _controller.onActivityResult(this, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<AlphabetId, String>> procedure);
        void complete(@NonNull Presenter presenter, @NonNull AlphabetId sourceAlphabet, @NonNull CreationOption creationOption);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);

        enum CreationOption {
            COPY,
            DEFINE_CONVERSION
        }
    }
}
