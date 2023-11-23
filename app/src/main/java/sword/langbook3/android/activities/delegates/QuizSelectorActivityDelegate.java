package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;

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

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableList;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.QuizEditorActivity;
import sword.langbook3.android.QuizResultActivity;
import sword.langbook3.android.QuizSelectorActivityState;
import sword.langbook3.android.QuizSelectorAdapter;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.QuizId;
import sword.langbook3.android.db.QuizIdBundler;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.QuestionFieldDetails;

public final class QuizSelectorActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements ListView.OnItemClickListener, ListView.MultiChoiceModeListener, DialogInterface.OnClickListener {
    private static final int REQUEST_CODE_EDITOR = 1;

    public interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    private Activity _activity;
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
            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC:
                return R.string.questionTypeSameAcceptation;

            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return R.string.questionTypeSameConcept;

            case LangbookDbSchema.QuestionFieldFlags.TYPE_APPLY_RULE:
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
                texts.append(_activity.getString(getTypeStringResId(field)));

                if (field.getType() == LangbookDbSchema.QuestionFieldFlags.TYPE_APPLY_RULE) {
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
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.quiz_selector_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _bunch = BunchIdBundler.readAsIntentExtra(activity.getIntent(), ArgKeys.BUNCH);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new QuizSelectorActivityState();
        }

        _listView = activity.findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);
    }

    @Override
    public void onStart(@NonNull Activity activity) {
        _activityStarted = true;

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(DbManager.getInstance().getManager(), _bunch);

        if (items.length == 0) {
            if (!_state.firstActionExecuted()) {
                QuizEditorActivity.open(activity, REQUEST_CODE_EDITOR, _bunch);
            }
            else if (_finishIfEmptyWhenStarting) {
                activity.finish();
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
        QuizResultActivity.open(_activity, quizId);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        activity.getMenuInflater().inflate(R.menu.quiz_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        if (item.getItemId() == R.id.menuItemNewQuizDefinition) {
            QuizEditorActivity.open(activity, REQUEST_CODE_EDITOR, _bunch);
            return true;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDITOR) {
            final QuizId quizId = (data != null)? QuizIdBundler.readAsIntentExtra(data, QuizEditorActivityDelegate.ResultKeys.QUIZ) : null;
            if (resultCode == RESULT_OK && quizId != null) {
                QuizResultActivity.open(activity, quizId);
            }
            else if (_activityStarted) {
                if (_listView.getAdapter().isEmpty()) {
                    activity.finish();
                }
            }
            else {
                _finishIfEmptyWhenStarting = true;
            }
        }
    }

    @Override
    public void onStop(@NonNull Activity activity) {
        _activityStarted = false;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        _state.changeListSelection(position, checked);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        _listActionMode = mode;
        menu.add(_activity.getString(R.string.menuItemDelete));
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
        final String message = _activity.getString(R.string.deleteQuizConfirmationText);
        _activity.newAlertDialogBuilder()
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
        showFeedback(_activity.getString(R.string.deleteQuizzesFeedback));

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(manager, _bunch);

        if (items.length == 0) {
            _activity.finish();
        }

        _listView.setAdapter(new QuizSelectorAdapter(items));
    }

    private void showFeedback(String message) {
        _activity.showToast(message);
    }
}
