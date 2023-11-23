package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.Function;
import sword.collections.ImmutableSet;
import sword.collections.Traversable;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

public final class CorrelationPickerAdapter extends BaseAdapter {

    public static String toPlainText(ImmutableCorrelationArray<AlphabetId> correlationArray, Traversable<ImmutableCorrelation<AlphabetId>> knownCorrelations) {
        final Function<ImmutableCorrelation<AlphabetId>, String> mapFunc = correlation -> {
            final String str = correlation.reduce((a,b) -> a + '/' + b);
            return knownCorrelations.contains(correlation)? "<" + str + ">" : str;
        };

        return correlationArray.map(mapFunc).reduce((a, b) -> a + " + " + b);
    }

    private final ImmutableSet<ImmutableCorrelationArray<AlphabetId>> _entries;
    private final ImmutableSet<ImmutableCorrelation<AlphabetId>> _knownCorrelations;

    private LayoutInflater _inflater;

    public CorrelationPickerAdapter(ImmutableSet<ImmutableCorrelationArray<AlphabetId>> entries, ImmutableSet<ImmutableCorrelation<AlphabetId>> knownCorrelations) {
        _entries = entries;
        _knownCorrelations = knownCorrelations;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public ImmutableCorrelationArray<AlphabetId> getItem(int position) {
        return _entries.valueAt(position);
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

            view = _inflater.inflate(R.layout.correlation_picker_entry, parent, false);
        }
        else {
            view = convertView;
        }

        final String text = toPlainText(_entries.valueAt(position), _knownCorrelations);
        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(text);

        return view;
    }
}
