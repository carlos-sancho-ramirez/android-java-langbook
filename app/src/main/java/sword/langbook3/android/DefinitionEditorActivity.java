package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntSet;
import sword.langbook3.android.db.LangbookChecker;

public final class DefinitionEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_PICK_BASE = 1;
    private static final int REQUEST_CODE_PICK_COMPLEMENT = 2;

    private interface SavedKeys {
        String STATE = "state";
    }

    public interface ResultKeys {
        String VALUES = "values";
    }

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, DefinitionEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    private State _state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.definition_editor_activity);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new State();
        }

        findViewById(R.id.baseConceptChangeButton).setOnClickListener(this);
        findViewById(R.id.complementsAddButton).setOnClickListener(this);
        findViewById(R.id.saveButton).setOnClickListener(this);

        updateUi();
    }

    private void updateUi() {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final TextView baseConceptTextView = findViewById(R.id.baseConceptText);

        final String text = (_state.baseConcept == 0)? null : checker.readConceptText(_state.baseConcept, preferredAlphabet);
        baseConceptTextView.setText(text);

        final LinearLayout complementsPanel = findViewById(R.id.complementsPanel);
        complementsPanel.removeAllViews();

        final LayoutInflater inflater = LayoutInflater.from(this);
        for (int complementConcept : _state.complements) {
            inflater.inflate(R.layout.definition_editor_complement_entry, complementsPanel, true);
            final TextView textView = complementsPanel.getChildAt(complementsPanel.getChildCount() - 1).findViewById(R.id.text);
            textView.setText(checker.readConceptText(complementConcept, preferredAlphabet));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.baseConceptChangeButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_BASE);
                return;

            case R.id.complementsAddButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_COMPLEMENT);
                return;

            case R.id.saveButton:
                saveAndFinish();
                return;
        }
    }

    private void saveAndFinish() {
        if (_state.baseConcept == 0) {
            Toast.makeText(this, R.string.baseConceptMissing, Toast.LENGTH_SHORT).show();
        }
        else {
            final Intent intent = new Intent();
            intent.putExtra(ResultKeys.VALUES, _state);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_BASE && resultCode == RESULT_OK && data != null) {
            final LangbookChecker checker = DbManager.getInstance().getManager();
            final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
            _state.baseConcept = (pickedAcceptation != 0)? checker.conceptFromAcceptation(pickedAcceptation) : 0;
            updateUi();
        }
        else if (requestCode == REQUEST_CODE_PICK_COMPLEMENT && resultCode == RESULT_OK && data != null) {
            final LangbookChecker checker = DbManager.getInstance().getManager();
            final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
            _state.complements = _state.complements.add(checker.conceptFromAcceptation(pickedAcceptation));
            updateUi();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        outBundle.putParcelable(SavedKeys.STATE, _state);
    }

    public static final class State implements Parcelable {
        public int baseConcept;
        public ImmutableIntSet complements = ImmutableIntArraySet.empty();

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(baseConcept);

            final int complementCount = (complements != null)? complements.size() : 0;
            dest.writeInt(complementCount);

            if (complementCount > 0) {
                for (int complement : complements) {
                    dest.writeInt(complement);
                }
            }
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                final State state = new State();
                state.baseConcept = in.readInt();

                final int complementCount = in.readInt();
                for (int i = 0; i < complementCount; i++) {
                    state.complements = state.complements.add(in.readInt());
                }

                return state;
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }
}
