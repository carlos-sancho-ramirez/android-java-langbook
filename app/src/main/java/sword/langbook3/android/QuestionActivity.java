package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;

import sword.langbook3.android.QuizSelectionActivity.QuizTypes;
import sword.langbook3.android.QuizSelectionActivity.StringPair;

public class QuestionActivity extends Activity {

    private static final class BundleKeys {
        static final String QUIZ_TYPE = "qt";
        static final String BUNCH = "b";
        static final String SOURCE_ALPHABET = "sa";
        static final String AUX = "aux";
    }

    private static final class SavedKeys {
        static final String ACCEPTATION = "acc";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int quizType, int bunch, int sourceAlphabet, int aux) {
        Intent intent = new Intent(context, QuestionActivity.class);
        intent.putExtra(BundleKeys.QUIZ_TYPE, quizType);
        intent.putExtra(BundleKeys.BUNCH, bunch);
        intent.putExtra(BundleKeys.SOURCE_ALPHABET, sourceAlphabet);
        intent.putExtra(BundleKeys.AUX, aux);
        context.startActivity(intent);
    }

    private int _acceptation;

    private int[] readAllPossibleInterAlphabetAcceptations(SQLiteDatabase db, int bunch, int sourceAlphabet, int targetAlphabet) {
        int[] result = new int[0];
        if (sourceAlphabet != targetAlphabet) {
            final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
            final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

            final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
            final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
            final String staAccField = strings.getColumnName(strings.getMainAcceptationColumnIndex());
            final Cursor cursor = db.rawQuery("SELECT J0." + dynAccField +
                " FROM " + strings.getName() + " AS J0" +
                    " JOIN " + strings.getName() + " AS J1 ON J0." + dynAccField + "=J1." + dynAccField +
                    " JOIN " + bunchAcceptations.getName() + " AS J2 ON J0." + dynAccField + "=J2." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) +
                " WHERE J0." + dynAccField + "=J0." + staAccField +
                    " AND J0." + alphabetField + "=?" +
                    " AND J1." + alphabetField + "=?" +
                    " AND J2." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?",
                            new String[]{Integer.toString(sourceAlphabet), Integer.toString(
                                    targetAlphabet), Integer.toString(bunch)}
                    );

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        result = new int[cursor.getCount()];
                        int index = 0;
                        do {
                            result[index++] = cursor.getInt(0);
                        } while (cursor.moveToNext());
                    }
                }
                finally {
                    cursor.close();
                }
            }
        }

        return result;
    }

    private StringPair readInterAlphabetQuestionTexts(SQLiteDatabase db, int acceptation, int sourceAlphabet, int targetAlphabet) {
        StringPair result = null;
        if (sourceAlphabet != targetAlphabet) {
            final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

            final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
            final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
            final Cursor cursor = db.rawQuery("SELECT " + alphabetField + ',' + strings.getColumnName(strings.getStringColumnIndex()) +
                            " FROM " + strings.getName() +
                            " WHERE " + dynAccField + "=?",
                    new String[]{Integer.toString(acceptation)});

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String question = null;
                        String answer = null;
                        do {
                            final int alphabet = cursor.getInt(0);
                            if (alphabet == sourceAlphabet) {
                                question = cursor.getString(1);
                            }
                            else if (alphabet == targetAlphabet) {
                                answer = cursor.getString(1);
                            }

                            if (question != null && answer != null) {
                                result = new StringPair(question, answer);
                            }
                        } while (result == null && cursor.moveToNext());
                    }
                }
                finally {
                    cursor.close();
                }
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unable to find texts for acceptation " + acceptation + " (" + sourceAlphabet + " -> " + targetAlphabet + ")");
        }

        return result;
    }

    private int[] readAllPossibleAcceptations(SQLiteDatabase db, int quizType, int bunch, int sourceAlphabet, int aux) {
        switch (quizType) {
            case QuizTypes.interAlphabet:
                return readAllPossibleInterAlphabetAcceptations(db, bunch, sourceAlphabet, aux);
        }

        return new int[0];
    }

    private StringPair readQuestionTexts(SQLiteDatabase db, int quizType, int acceptation, int sourceAlphabet, int aux) {
        switch (quizType) {
            case QuizTypes.interAlphabet:
                return readInterAlphabetQuestionTexts(db, acceptation, sourceAlphabet, aux);
        }

        throw new UnsupportedOperationException("Unsupported quiz type");
    }

    private int selectAcceptation(int[] acceptations) {
        final long currentTimeMillis = System.currentTimeMillis();
        final int size = acceptations.length;
        final int index = (int) (currentTimeMillis % size);
        return acceptations[index];
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question_activity);

        if (savedInstanceState != null) {
            _acceptation = savedInstanceState.getInt(SavedKeys.ACCEPTATION, 0);
        }

        final int quizType = getIntent().getIntExtra(BundleKeys.QUIZ_TYPE, 0);
        final int bunch = getIntent().getIntExtra(BundleKeys.BUNCH, 0);
        final int sourceAlphabet = getIntent().getIntExtra(BundleKeys.SOURCE_ALPHABET, 0);
        final int aux = getIntent().getIntExtra(BundleKeys.AUX, 0);

        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        if (_acceptation == 0) {
            final int[] acceptations = readAllPossibleAcceptations(db, quizType, bunch, sourceAlphabet, aux);
            _acceptation = selectAcceptation(acceptations);
        }

        final StringPair texts = readQuestionTexts(db, quizType, _acceptation, sourceAlphabet, aux);

        final TextView questionTextView = findViewById(R.id.questionText);
        questionTextView.setText(texts.source);

        final TextView answerTextView = findViewById(R.id.answerText);
        answerTextView.setText(texts.target);
    }
}
