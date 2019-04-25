package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import sword.collections.IntValueMap;
import sword.collections.MutableIntValueMap;
import sword.collections.Procedure;
import sword.langbook3.android.models.SentenceSpan;

final class SpanEditorAdapter extends BaseAdapter {

    private final String sentenceText;
    private final MutableIntValueMap<SentenceSpan> spans;
    private final Procedure<IntValueMap<SentenceSpan>> observer;
    private LayoutInflater inflater;

    SpanEditorAdapter(String sentenceText, MutableIntValueMap<SentenceSpan> spans, Procedure<IntValueMap<SentenceSpan>> observer) {
        this.sentenceText = sentenceText;
        this.spans = spans;
        this.observer = observer;
    }

    @Override
    public int getCount() {
        return spans.size();
    }

    @Override
    public SentenceSpan getItem(int position) {
        return spans.keyAt(position);
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
            convertView = inflater.inflate(R.layout.span_editor_span_entry, parent, false);
        }

        final SentenceSpan span = getItem(position);
        final CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        checkBox.setText(sentenceText.substring(span.range.min(), span.range.max() + 1));
        checkBox.setChecked(spans.valueAt(position) != 0);
        checkBox.setOnCheckedChangeListener((v, state) -> {
            spans.put(span, state? 1 : 0);
            observer.apply(spans);
        });

        convertView.findViewById(R.id.removeButton).setOnClickListener(v -> {
            spans.removeAt(position);
            observer.apply(spans);
            notifyDataSetChanged();
        });

        return convertView;
    }
}
