package sword.langbook3.android.activities.delegates;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.collections.Map;
import sword.collections.Procedure;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.ConversionEditorAdapter;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class ConversionEditorActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {
    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    interface ResultKeys {
        String CONVERSION = BundleKeys.CONVERSION;
    }

    private Activity _activity;
    private Presenter _presenter;
    private Controller _controller;
    private Conversion<AlphabetId> _conversion;
    private ConversionEditorActivityState _state;
    private ConversionEditorAdapter _adapter;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        _presenter = new DefaultPresenter(activity);
        activity.setContentView(R.layout.conversion_details_activity);

        _state = (savedInstanceState == null)? new ConversionEditorActivityState() : savedInstanceState.getParcelable(SavedKeys.STATE);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.load(_presenter, conversion -> {
            _conversion = conversion;

            final ListView listView = activity.findViewById(R.id.listView);
            _adapter = new ConversionEditorAdapter(conversion, _state.getRemoved(), _state.getAdded(), _state.getDisabled());
            listView.setAdapter(_adapter);
            listView.setOnItemClickListener(this);
            listView.setOnItemLongClickListener(this);

            if (_state.shouldDisplayModificationDialog()) {
                openModificationDialog();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        activity.getMenuInflater().inflate(R.menu.conversion_editor_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemAdd) {
            _state.startModification();
            openModificationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemSave) {
            _controller.complete(_presenter, _state.getResultingConversion(_conversion));
            return true;
        }

        return false;
    }

    private void openModificationDialog() {
        final AlertDialog dialog = _activity.newAlertDialogBuilder()
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (_state.applyModification(_conversion)) {
                        _adapter.notifyDataSetChanged();
                    }
                    else {
                        _activity.showToast(R.string.noChangeRequired);
                    }
                })
                .setOnCancelListener(d -> _state.cancelModification())
                .create();

        final View view = LayoutInflater.from(dialog.getContext()).inflate(R.layout.conversion_editor_modification, null);
        final EditText source = view.findViewById(R.id.sourceEditText);
        source.setText(_state.getSourceModificationText());
        source.addTextChangedListener(new SourceTextListener());

        final EditText target = view.findViewById(R.id.targetEditText);
        target.setText(_state.getTargetModificationText());
        target.addTextChangedListener(new TargetTextListener());

        dialog.setView(view);
        dialog.show();
    }

    private final class SourceTextListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to be done
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nothing to be done
        }

        @Override
        public void afterTextChanged(Editable s) {
            _state.updateSourceModificationText(s.toString());
        }
    }

    private final class TargetTextListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to be done
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nothing to be done
        }

        @Override
        public void afterTextChanged(Editable s) {
            _state.updateTargetModificationText(s.toString());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ConversionEditorAdapter adapter = (ConversionEditorAdapter) parent.getAdapter();
        final ConversionEditorAdapter.Entry entry = adapter.getItem(position);
        if (entry.toggleDisabledOnClick()) {
            _state.toggleEnabled(entry.getSource());
        }
        else {
            final int convPos = entry.getConversionPosition();
            if (convPos < 0) {
                throw new AssertionError();
            }

            _state.toggleRemoved(convPos);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final ConversionEditorAdapter adapter = (ConversionEditorAdapter) parent.getAdapter();
        final ConversionEditorAdapter.Entry entry = adapter.getItem(position);
        final Map<String, String> added = _state.getAdded();
        final String key = entry.getSource();
        if (added.containsKey(key)) {
            _state.startModification();
            _state.updateSourceModificationText(key);
            _state.updateTargetModificationText(added.get(key));
            openModificationDialog();
        }
        else {
            final int convPos = entry.getConversionPosition();
            if (convPos >= 0) {
                final Map<String, String> map = _conversion.getMap();
                _state.startModification();
                _state.updateSourceModificationText(map.keyAt(position));
                _state.updateTargetModificationText(map.valueAt(position));
                openModificationDialog();
            }
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Presenter presenter, @NonNull Procedure<Conversion<AlphabetId>> procedure);
        void complete(@NonNull Presenter presenter, @NonNull Conversion<AlphabetId> conversion);
    }
}
