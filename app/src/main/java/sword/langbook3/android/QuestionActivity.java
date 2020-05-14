package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.MutableIntPairMap;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;

public final class QuestionActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final int INITIAL_SCORE = (MIN_ALLOWED_SCORE + MAX_ALLOWED_SCORE) / 2;
    private static final int SCORE_INCREMENT = 1;
    private static final int SCORE_DECREMENT = 2;

    private static final long CLICK_MILLIS_TIME_INTERVAL = 600;

    private interface ArgKeys {
        String QUIZ = BundleKeys.QUIZ;
    }

    private interface SavedKeys {
        String ACCEPTATION = "acc";
        String IS_ANSWER_VISIBLE = "av";
        String GOOD_ANSWER_COUNT = "ga";
        String BAD_ANSWER_COUNT = "ba";
        String LEAVE_DIALOG_PRESENT = "ldp";
    }

    interface ReturnKeys {
        String GOOD_ANSWER_COUNT = BundleKeys.GOOD_ANSWER_COUNT;
        String BAD_ANSWER_COUNT = BundleKeys.BAD_ANSWER_COUNT;
    }

    public static void open(Activity activity, int requestCode, int quizId) {
        Intent intent = new Intent(activity, QuestionActivity.class);
        intent.putExtra(ArgKeys.QUIZ, quizId);
        activity.startActivityForResult(intent, requestCode);
    }

    private MutableIntPairMap _knowledge = MutableIntPairMap.empty();

    private int _quizId;
    private QuizDetails _quizDetails;
    private TextView[] _fieldTextViews;

    private int _goodAnswerCount;
    private int _badAnswerCount;
    private int _acceptation;

    private boolean _isAnswerVisible;

    private AlertDialog _dialog;
    private TextView _scoreTextView;
    private long _lastClickTime;
    private ImmutableIntSet _possibleAcceptations;
    private int _dbWriteVersion;

    private void readQuizDefinition(LangbookChecker checker) {
        _quizDetails = checker.getQuizDetails(_quizId);
        _fieldTextViews = new TextView[_quizDetails.fields.size()];
    }

    private String readFieldText(LangbookChecker checker, int index) {
        return checker.readQuestionFieldText(_acceptation, _quizDetails.fields.get(index));
    }

    private void selectAcceptation() {
        final int size = _possibleAcceptations.size();
        final int ponderationThreshold = MIN_ALLOWED_SCORE + (MAX_ALLOWED_SCORE - MIN_ALLOWED_SCORE) * 3 / 4;

        final MutableIntPairMap ponders = MutableIntPairMap.empty();
        int ponderationCount = 0;
        for (int i = 0; i < size; i++) {
            final int acceptation = _possibleAcceptations.valueAt(i);
            ponders.put(acceptation, ponderationCount);
            final int score = _knowledge.get(acceptation, INITIAL_SCORE);
            final int diff = ponderationThreshold - score;
            ponderationCount += (diff > 0)? diff * diff : 1;
        }

        final long currentTimeMillis = System.currentTimeMillis();
        final int randomIndex = (int) (currentTimeMillis % ponderationCount);

        int min = 0;
        int max = size;
        do {
            int middle = min + (max - min) / 2;
            if (ponders.valueAt(middle) <= randomIndex) {
                min = middle;
            }
            else {
                max = middle;
            }
        } while(max - min > 1);

        _acceptation = ponders.keyAt(min);
    }

    private void updateTextFields() {
        final ImmutableList<QuestionFieldDetails> fields = _quizDetails.fields;
        final int fieldCount = fields.size();
        for (int i = 0; i < fieldCount; i++) {
            final boolean shouldDisplayText = _isAnswerVisible || !fields.get(i).isAnswer();
            final String text = shouldDisplayText? readFieldText(DbManager.getInstance().getManager(), i) : "?";
            _fieldTextViews[i].setText(text);
        }

        _scoreTextView.setText("" + _goodAnswerCount + " - " + _badAnswerCount);
    }

    private void toggleAnswerVisibility() {
        final Button revealAnswerButton = findViewById(R.id.revealAnswerButton);
        final LinearLayout rateButtonBar = findViewById(R.id.rateButtonBar);

        final ImmutableList<QuestionFieldDetails> fields = _quizDetails.fields;
        if (!_isAnswerVisible) {
            LangbookChecker checker = null;
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).isAnswer()) {
                    if (checker == null) {
                        checker = DbManager.getInstance().getManager();
                    }

                    _fieldTextViews[i].setText(readFieldText(checker, i));
                }
            }

            revealAnswerButton.setVisibility(View.GONE);
            rateButtonBar.setVisibility(View.VISIBLE);
            _isAnswerVisible = true;
        }
        else {
            for (int i = 0; i < fields.size(); i++) {
                if (fields.get(i).isAnswer()) {
                    _fieldTextViews[i].setText("?");
                }
            }

            revealAnswerButton.setVisibility(View.VISIBLE);
            rateButtonBar.setVisibility(View.GONE);
            _isAnswerVisible = false;
        }
    }

    private final View.OnClickListener mFieldClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (_isAnswerVisible) {
                AcceptationDetailsActivity.open(QuestionActivity.this, _acceptation);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question_activity);

        _quizId = getIntent().getIntExtra(ArgKeys.QUIZ, 0);
        findViewById(R.id.revealAnswerButton).setOnClickListener(this);
        findViewById(R.id.goodAnswerButton).setOnClickListener(this);
        findViewById(R.id.badAnswerButton).setOnClickListener(this);

        boolean leaveDialogPresent = false;
        if (savedInstanceState != null) {
            _acceptation = savedInstanceState.getInt(SavedKeys.ACCEPTATION, 0);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_COUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_COUNT);
            _isAnswerVisible = savedInstanceState.getBoolean(SavedKeys.IS_ANSWER_VISIBLE);
            leaveDialogPresent = savedInstanceState.getBoolean(SavedKeys.LEAVE_DIALOG_PRESENT);
        }

        if (leaveDialogPresent) {
            showLeaveConfirmation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final int dbWriteVersion = DbManager.getInstance().getDatabase().getWriteVersion();
        if (dbWriteVersion != _dbWriteVersion) {
            _dbWriteVersion = dbWriteVersion;

            final LangbookChecker checker = DbManager.getInstance().getManager();
            readQuizDefinition(checker);
            readCurrentKnowledge(checker);

            if (_possibleAcceptations.isEmpty()) {
                Toast.makeText(this, R.string.noValidQuestions, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (_acceptation == 0 || !_possibleAcceptations.contains(_acceptation)) {
                selectAcceptation();
                _isAnswerVisible = false;
            }

            final LinearLayout fieldsPanel = findViewById(R.id.fieldsPanel);
            fieldsPanel.removeAllViews();

            final LayoutInflater inflater = getLayoutInflater();
            final ImmutableList<QuestionFieldDetails> fields = _quizDetails.fields;
            for (int i = 0; i < fields.size(); i++) {
                inflater.inflate(R.layout.question_field, fieldsPanel, true);
                _fieldTextViews[i] = (TextView) fieldsPanel.getChildAt(fieldsPanel.getChildCount() - 1);
                if (!fields.get(i).isAnswer()) {
                    _fieldTextViews[i].setOnClickListener(mFieldClickListener);
                }
            }
            _scoreTextView = findViewById(R.id.scoreTextView);

            updateTextFields();

            final View revealAnswerButton = findViewById(R.id.revealAnswerButton);
            final LinearLayout rateButtonBar = findViewById(R.id.rateButtonBar);

            if (_isAnswerVisible) {
                revealAnswerButton.setVisibility(View.GONE);
                rateButtonBar.setVisibility(View.VISIBLE);
            }
            else {
                revealAnswerButton.setVisibility(View.VISIBLE);
                rateButtonBar.setVisibility(View.GONE);
            }
        }
    }

    private void readCurrentKnowledge(LangbookChecker checker) {
        final ImmutableIntPairMap knowledgeMap = checker.getCurrentKnowledge(_quizId);
        _possibleAcceptations = knowledgeMap.keySet();
        _knowledge = knowledgeMap.filter(score -> score != NO_SCORE).mutate();
    }

    private void registerGoodAnswer() {
        final int foundScore = _knowledge.get(_acceptation, NO_SCORE);

        final int newScore;
        if (foundScore == NO_SCORE) {
            newScore = INITIAL_SCORE + SCORE_INCREMENT;
        }
        else {
            final int newProposedScore = foundScore + SCORE_INCREMENT;
            newScore = (newProposedScore > MAX_ALLOWED_SCORE)? MAX_ALLOWED_SCORE : newProposedScore;
        }

        _knowledge.put(_acceptation, newScore);
        DbManager.getInstance().getManager().updateScore(_quizId, _acceptation, newScore);

        _goodAnswerCount++;
    }

    private void registerBadAnswer() {
        final int foundScore = _knowledge.get(_acceptation, NO_SCORE);

        final int newScore;
        if (foundScore == NO_SCORE) {
            newScore = INITIAL_SCORE - SCORE_DECREMENT;
        }
        else {
            final int newProposedScore = foundScore - SCORE_DECREMENT;
            newScore = (newProposedScore >= MIN_ALLOWED_SCORE)? newProposedScore : MIN_ALLOWED_SCORE;
        }

        _knowledge.put(_acceptation, newScore);
        DbManager.getInstance().getManager().updateScore(_quizId, _acceptation, newScore);

        _badAnswerCount++;
    }

    @Override
    public void onClick(View view) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - _lastClickTime > CLICK_MILLIS_TIME_INTERVAL) {
            _lastClickTime = currentTime;
            switch (view.getId()) {
                case R.id.revealAnswerButton:
                    toggleAnswerVisibility();
                    break;

                case R.id.goodAnswerButton:
                    registerGoodAnswer();
                    selectAcceptation();
                    updateTextFields();
                    toggleAnswerVisibility();
                    break;

                case R.id.badAnswerButton:
                    registerBadAnswer();
                    selectAcceptation();
                    updateTextFields();
                    toggleAnswerVisibility();
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(SavedKeys.ACCEPTATION, _acceptation);
        out.putBoolean(SavedKeys.IS_ANSWER_VISIBLE, _isAnswerVisible);
        out.putInt(SavedKeys.GOOD_ANSWER_COUNT, _goodAnswerCount);
        out.putInt(SavedKeys.BAD_ANSWER_COUNT, _badAnswerCount);
        out.putBoolean(SavedKeys.LEAVE_DIALOG_PRESENT, _dialog != null);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final Intent intent = new Intent();
            intent.putExtra(ReturnKeys.GOOD_ANSWER_COUNT, _goodAnswerCount);
            intent.putExtra(ReturnKeys.BAD_ANSWER_COUNT, _badAnswerCount);
            setResult(Activity.RESULT_CANCELED, intent);
            finish();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        _dialog = null;
    }

    private void showLeaveConfirmation() {
        _dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.questionLeaveConfirmation)
                .setPositiveButton(R.string.yes, this)
                .setNegativeButton(R.string.no, this)
                .setOnDismissListener(this)
                .create();
        _dialog.show();
    }

    @Override
    public void onBackPressed() {
        showLeaveConfirmation();
    }
}
