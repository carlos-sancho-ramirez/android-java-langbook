package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.IntKeyMap;

public final class RuleAdapter extends BaseAdapter {
    private final ImmutableIntKeyMap<String> _entries;
    private LayoutInflater _inflater;

    RuleAdapter(ImmutableIntKeyMap<String> entries) {
        _entries = entries;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public IntKeyMap.Entry<String> getItem(int position) {
        return _entries.entries().valueAt(position);
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

            view = _inflater.inflate(R.layout.quiz_type_item, parent, false);
        }
        else {
            view = convertView;
        }

        final TextView textView = view.findViewById(R.id.itemTextView);
        textView.setText(_entries.valueAt(position));

        return view;
    }
}
