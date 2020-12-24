package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.Function;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AlphabetId;

final class CorrelationPickerAdapter extends BaseAdapter {

    private final ImmutableSet<ImmutableList<ImmutableMap<AlphabetId, String>>> _entries;
    private final ImmutableSet<ImmutableMap<AlphabetId, String>> _knownCorrelations;

    private LayoutInflater _inflater;

    CorrelationPickerAdapter(ImmutableSet<ImmutableList<ImmutableMap<AlphabetId, String>>> entries, ImmutableSet<ImmutableMap<AlphabetId, String>> knownCorrelations) {
        _entries = entries;
        _knownCorrelations = knownCorrelations;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public ImmutableList<ImmutableMap<AlphabetId, String>> getItem(int position) {
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

        ImmutableList<ImmutableMap<AlphabetId, String>> array = _entries.valueAt(position);
        final Function<ImmutableMap<AlphabetId, String>, String> mapFunc = correlation -> {
            final String str = correlation.reduce((a,b) -> a + '/' + b);
            return _knownCorrelations.contains(correlation)? "<" + str + ">" : str;
        };
        final String text = array.map(mapFunc).reduce((a,b) -> a + " + " + b);

        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(text);

        return view;
    }
}
