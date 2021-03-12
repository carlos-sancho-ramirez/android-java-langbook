package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableMap;
import sword.collections.Map;
import sword.langbook3.android.db.RuleId;

public final class RuleAdapter extends BaseAdapter {
    private final ImmutableMap<RuleId, String> _entries;
    private LayoutInflater _inflater;

    RuleAdapter(ImmutableMap<RuleId, String> entries) {
        _entries = entries;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public Map.Entry<RuleId, String> getItem(int position) {
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
