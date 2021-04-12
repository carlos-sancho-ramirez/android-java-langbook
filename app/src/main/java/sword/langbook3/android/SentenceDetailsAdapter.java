package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableMap;
import sword.langbook3.android.db.SentenceId;

final class SentenceDetailsAdapter extends BaseAdapter {

    private final ImmutableMap<SentenceId, String> sentences;

    private LayoutInflater inflater;

    SentenceDetailsAdapter(ImmutableMap<SentenceId, String> sentences) {
        this.sentences = sentences;
    }

    @Override
    public int getCount() {
        return sentences.size();
    }

    public SentenceId getSentenceIdAt(int position) {
        return sentences.keyAt(position);
    }

    @Override
    public String getItem(int position) {
        return sentences.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            if (inflater == null) {
                inflater = LayoutInflater.from(parent.getContext());
            }
            convertView = inflater.inflate(R.layout.sentence_details_sentence_entry, parent, false);
        }

        final TextView textField = convertView.findViewById(R.id.textField);
        textField.setText(sentences.valueAt(position));

        return convertView;
    }
}
