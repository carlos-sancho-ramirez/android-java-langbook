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

        TextView tv = convertView.findViewById(R.id.textView);
        tv.setText(_items[position]._text);

        return convertView;
    }

    static class Item {
        private final int _quizId;
        private final String _text;

        Item(int quizId, String text) {
            _quizId = quizId;
            _text = text;
        }

        int getQuizId() {
            return _quizId;
        }
    }
}
