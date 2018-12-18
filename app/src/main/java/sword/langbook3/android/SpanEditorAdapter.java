package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableSet;

final class SpanEditorAdapter extends BaseAdapter {

    private final ImmutableSet<SpanEditorActivityState.SentenceSpan> spans;
    private LayoutInflater inflater;

    SpanEditorAdapter(ImmutableSet<SpanEditorActivityState.SentenceSpan> spans) {
        this.spans = spans;
    }

    @Override
    public int getCount() {
        return spans.size();
    }

    @Override
    public SpanEditorActivityState.SentenceSpan getItem(int position) {
        return spans.valueAt(position);
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

        final SpanEditorActivityState.SentenceSpan span = getItem(position);
        final TextView textView = convertView.findViewById(R.id.text);
        textView.setText(span.range.toString() + " - " + span.acceptation);
        return convertView;
    }
}
