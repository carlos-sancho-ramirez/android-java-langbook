package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

final class QuizSelectorAdapter extends BaseAdapter {

    private final Item[] _items;

    QuizSelectorAdapter(Item[] items) {
        if (items == null) {
            throw new IllegalArgumentException();
        }

        _items = items;
    }

    @Override
    public int getCount() {
        return _items.length;
    }

    @Override
    public Item getItem(int position) {
        return _items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.quiz_selector_entry, parent, false);
        }

        final Item item = _items[position];
        TextView qtv = convertView.findViewById(R.id.questionText);
        qtv.setText(item._questionText);

        TextView atv = convertView.findViewById(R.id.answerText);
        atv.setText(item._answerText);

        convertView.findViewById(R.id.knowledgeBarView).setBackground(item._progress.getDrawable());

        TextView ctv = convertView.findViewById(R.id.completenessTextView);
        ctv.setText(item._progress.getCompletenessString());

        return convertView;
    }

    static class Item {
        private final int _quizId;
        private final String _questionText;
        private final String _answerText;
        private final QuizSelectorActivity.Progress _progress;

        Item(int quizId, String questionText, String answerText, QuizSelectorActivity.Progress progress) {
            _quizId = quizId;
            _questionText = questionText;
            _answerText = answerText;
            _progress = progress;
        }

        int getQuizId() {
            return _quizId;
        }
    }
}
