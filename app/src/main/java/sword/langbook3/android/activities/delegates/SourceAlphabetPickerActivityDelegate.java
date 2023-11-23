package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.langbook3.android.AlphabetAdapter;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class SourceAlphabetPickerActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> {
    public static final int REQUEST_CODE_NEW_CONVERSION = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private Controller _controller;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        final Presenter presenter = new DefaultPresenter(activity);
        activity.setContentView(R.layout.source_alphabet_picker_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.load(presenter, sourceAlphabetTexts -> {
            final AlphabetAdapter adapter = new AlphabetAdapter(sourceAlphabetTexts);
            final Spinner sourceAlphabetSpinner = activity.findViewById(R.id.sourceAlphabetSpinner);
            sourceAlphabetSpinner.setAdapter(adapter);
            activity.findViewById(R.id.confirmButton).setOnClickListener(v -> {
                final Controller.CreationOption creationOption = activity.<CheckBox>findViewById(R.id.defineConversionCheckBox).isChecked()?
                        Controller.CreationOption.DEFINE_CONVERSION : Controller.CreationOption.COPY;
                final AlphabetId sourceAlphabet = sourceAlphabetTexts.keyAt(activity.<Spinner>findViewById(R.id.sourceAlphabetSpinner).getSelectedItemPosition());
                _controller.complete(presenter, sourceAlphabet, creationOption);
            });
        });
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<AlphabetId, String>> procedure);
        void complete(@NonNull Presenter presenter, @NonNull AlphabetId sourceAlphabet, @NonNull Controller.CreationOption creationOption);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);

        enum CreationOption {
            COPY,
            DEFINE_CONVERSION
        }
    }
}
