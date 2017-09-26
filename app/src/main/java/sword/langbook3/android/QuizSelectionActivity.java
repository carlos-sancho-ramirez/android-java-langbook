package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class QuizSelectionActivity extends Activity {

    private static final class BundleKeys {
        static final String BUNCH = "b";
    }

    public static void open(Context context, int bunch) {
        Intent intent = new Intent(context, QuizSelectionActivity.class);
        intent.putExtra(BundleKeys.BUNCH, bunch);
        context.startActivity(intent);
    }

    private static class QuizTypeAdapter extends BaseAdapter {

        private final String[] _entries;
        private LayoutInflater _inflater;

        QuizTypeAdapter(String[] entries) {
            _entries = entries;
        }

        @Override
        public int getCount() {
            return _entries.length;
        }

        @Override
        public String getItem(int position) {
            return _entries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.quiz_type_item, parent, false);
            }
            else {
                view = convertView;
            }

            final TextView textView = view.findViewById(R.id.itemTextView);
            textView.setText(_entries[position]);

            return view;
        }
    }

    private int _bunch;
    private QuizTypeAdapter _quizTypeAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_selector_activity);

        _quizTypeAdapter = new QuizTypeAdapter(new String[] {
                " -- Select type --",
                "inter-alphabet",
                "translation",
                "synonym",
                "applied rule"
        });

        _bunch = getIntent().getIntExtra(BundleKeys.BUNCH, 0);
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        if (_bunch != 0) {
            final String bunchText = AcceptationDetailsActivity.readConceptText(db, _bunch);
            final TextView bunchField = findViewById(R.id.bunch);
            bunchField.setText(bunchText);
        }

        final Spinner quizTypeSpinner = findViewById(R.id.quizType);
        quizTypeSpinner.setAdapter(_quizTypeAdapter);
    }
}
