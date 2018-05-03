package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;

final class CorrelationPickerAdapter extends BaseAdapter {

    private final ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> _entries;
    private final ImmutableSet<ImmutableIntKeyMap<String>> _knownCorrelations;

    private LayoutInflater _inflater;

    CorrelationPickerAdapter(ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> entries, ImmutableSet<ImmutableIntKeyMap<String>> knownCorrelations) {
        _entries = entries;
        _knownCorrelations = knownCorrelations;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public ImmutableList<ImmutableIntKeyMap<String>> getItem(int position) {
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

        ImmutableList<ImmutableIntKeyMap<String>> array = _entries.valueAt(position);
        final String text = array.map(correlation -> {
            final String str = correlation.reduce((a,b) -> a + '/' + b);
            return _knownCorrelations.contains(correlation)? "<" + str + ">" : str;
        }).reduce((a,b) -> a + " + " + b);

        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(text);

        return view;
    }
}
