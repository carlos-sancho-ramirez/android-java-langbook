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
    private LayoutInflater _inflater;

    CorrelationPickerAdapter(ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> entries) {
        _entries = entries;
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

            view = _inflater.inflate(R.layout.search_result, parent, false);
        }
        else {
            view = convertView;
        }

        ImmutableList<ImmutableIntKeyMap<String>> array = _entries.valueAt(position);
        final String text = array.map(correlation -> correlation.reduce((a,b) -> a + '/' + b)).reduce((a,b) -> a + '+' + b);

        final TextView textView = view.findViewById(R.id.searchResultTextView);
        textView.setText(text);

        return view;
    }
}
