package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableList;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.db.QuizId;
import sword.langbook3.android.db.QuizIdBundler;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.QuestionFieldDetails;

public final class QuizSelectorActivity extends Activity implements ListView.OnItemClickListener, ListView.MultiChoiceModeListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_EDITOR = 1;

    private interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    public static void open(Context context, BunchId bunch) {
        Intent intent = new Intent(context, QuizSelectorActivity.class);
        if (bunch != null) {
            BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.BUNCH, bunch);
        }
        context.startActivity(intent);
    }

    private QuizSelectorActivityState _state;

    private AlphabetId _preferredAlphabet;
    private BunchId _bunch;
    private ImmutableMap<RuleId, String> _ruleTexts;

    private boolean _finishIfEmptyWhenStarting;
    private boolean _activityStarted;
    private ListView _listView;
    private ActionMode _listActionMode;

    private String getRuleText(LangbookDbChecker checker, RuleId rule) {
        if (_ruleTexts == null) {
            _ruleTexts = checker.readAllRules(_preferredAlphabet);
        }

        return _ruleTexts.get(rule);
    }

    private static int getTypeStringResId(QuestionFieldDetails<AlphabetId, RuleId> field) {
        switch (field.getType()) {
            case QuestionFieldFlags.TYPE_SAME_ACC:
                return R.string.questionTypeSameAcceptation;

            case QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return R.string.questionTypeSameConcept;

            case QuestionFieldFlags.TYPE_APPLY_RULE:
                return R.string.questionTypeAppliedRule;
        }

        return 0;
    }

    private QuizSelectorAdapter.Item[] composeAdapterItems(LangbookDbChecker checker, BunchId bunch) {
        final ImmutableMap<QuizId, ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>>> resultMap = checker.readQuizSelectorEntriesForBunch(bunch);
        final ImmutableMap<AlphabetId, String> allAlphabets = checker.readAllAlphabets(_preferredAlphabet);
        final int quizCount = resultMap.size();
        final QuizSelectorAdapter.Item[] items = new QuizSelectorAdapter.Item[quizCount];

        for (int i = 0; i < quizCount; i++) {
            final QuizId quizId = resultMap.keyAt(i);
            final ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>> set = resultMap.valueAt(i);

            final ImmutableList<String> fieldTexts = set.map(field -> {
                final MutableList<String> texts = MutableList.empty();
                texts.append(allAlphabets.get(field.alphabet, "?"));
                texts.append(getString(getTypeStringResId(field)));

                if (field.getType() == QuestionFieldFlags.TYPE_APPLY_RULE) {
                    texts.append(getRuleText(checker, field.rule));
                }
                return "(" + texts.reduce((a, b) -> a + ", " + b) + ")";
            });

            final String questionText = set.indexes().filterNot(index -> set.valueAt(index).isAnswer()).map(fieldTexts::valueAt).reduce((a, b) -> a + "\n" + b);
            final String answerText = set.indexes().filter(index -> set.valueAt(index).isAnswer()).map(fieldTexts::valueAt).reduce((a, b) -> a + "\n" + b);
            items[i] = new QuizSelectorAdapter.Item(quizId, questionText, answerText, checker.readQuizProgress(quizId));
        }

        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_selector_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _bunch = BunchIdBundler.readAsIntentExtra(getIntent(), ArgKeys.BUNCH);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new QuizSelectorActivityState();
        }

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        _activityStarted = true;

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(DbManager.getInstance().getManager(), _bunch);

        if (items.length == 0) {
            if (!_state.firstActionExecuted()) {
                QuizEditorActivity.open(this, REQUEST_CODE_EDITOR, _bunch);
            }
            else if (_finishIfEmptyWhenStarting) {
                finish();
            }
        }

        _listView.setAdapter(new QuizSelectorAdapter(items));
        _listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        _listView.setMultiChoiceModeListener(this);

        ImmutableIntSet selection = _state.getListSelection();
        if (selection.isEmpty()) {
            _state.setReady();
        }
        else {
            for (int position : selection) {
                _listView.setItemChecked(position, true);
            }

            if (_state.shouldDisplayDeleteDialog()) {
                showDeleteConfirmationDialog();
            }
        }

        _finishIfEmptyWhenStarting = false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final QuizId quizId = ((QuizSelectorAdapter) parent.getAdapter()).getItem(position).getQuizId();
        QuizResultActivity.open(this, quizId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.quiz_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemNewQuizDefinition) {
            QuizEditorActivity.open(this, REQUEST_CODE_EDITOR, _bunch);
            return true;
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDITOR) {
            final QuizId quizId = (data != null)? QuizIdBundler.readAsIntentExtra(data, QuizEditorActivity.ResultKeys.QUIZ) : null;
            if (resultCode == RESULT_OK && quizId != null) {
                QuizResultActivity.open(this, quizId);
            }
            else if (_activityStarted) {
                if (_listView.getAdapter().isEmpty()) {
                    finish();
                }
            }
            else {
                _finishIfEmptyWhenStarting = true;
            }
        }
    }

    @Override
    protected void onStop() {
        _activityStarted = false;
        super.onStop();
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        _state.changeListSelection(position, checked);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        _listActionMode = mode;
        menu.add(getString(R.string.menuItemDelete));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (_listActionMode != mode) {
            throw new AssertionError();
        }

        _state.setDeletingState();
        showDeleteConfirmationDialog();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (_listActionMode != null) {
            _state.clearQuizSelection();
            _listActionMode = null;
        }
    }

    private void showDeleteConfirmationDialog() {
        final String message = getString(R.string.deleteQuizConfirmationText);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteState())
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final QuizSelectorAdapter adapter = (QuizSelectorAdapter) _listView.getAdapter();
        final ImmutableList<QuizId> quizzes = _state.getListSelection().map(position -> adapter.getItem(position).getQuizId());
        _state.clearDeleteStateAndSelection();
        final ActionMode listActionMode = _listActionMode;
        _listActionMode = null;
        listActionMode.finish();

        final LangbookDbManager manager = DbManager.getInstance().getManager();
        for (QuizId quizId : quizzes) {
            manager.removeQuiz(quizId);
        }
        showFeedback(getString(R.string.deleteQuizzesFeedback));

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(manager, _bunch);

        if (items.length == 0) {
            finish();
        }

        _listView.setAdapter(new QuizSelectorAdapter(items));
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
