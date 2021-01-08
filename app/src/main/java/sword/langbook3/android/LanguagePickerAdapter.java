package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableMap;
import sword.langbook3.android.db.LanguageId;

final class LanguagePickerAdapter extends BaseAdapter {

    private final ImmutableMap<LanguageId, String> _entries;
    private LayoutInflater _inflater;

    LanguagePickerAdapter(ImmutableMap<LanguageId, String> entries) {
        _entries = entries;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public LanguageId getItem(int position) {
        return _entries.keyAt(position);
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

            view = _inflater.inflate(R.layout.language_picker_entry, parent, false);
        }
        else {
            view = convertView;
        }

        final TextView textView = view.findViewById(R.id.searchResultTextView);
        textView.setText(_entries.valueAt(position));

        return view;
    }
}
