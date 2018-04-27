package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;

final class LanguagePickerAdapter extends BaseAdapter {

    private final ImmutableIntKeyMap<String> _entries;
    private LayoutInflater _inflater;

    LanguagePickerAdapter(ImmutableIntKeyMap<String> entries) {
        _entries = entries;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public Integer getItem(int position) {
        return _entries.keyAt(position);
    }

    @Override
    public long getItemId(int position) {
        return _entries.keyAt(position);
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

        final TextView textView = view.findViewById(R.id.searchResultTextView);
        textView.setText(_entries.valueAt(position));

        return view;
    }
}
