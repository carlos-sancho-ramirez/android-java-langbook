package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;

final class ConversionDetailsAdapter extends BaseAdapter {

    private final ImmutableSet<ImmutablePair<String, String>> _conversion;
    private LayoutInflater _inflater;

    ConversionDetailsAdapter(ImmutableSet<ImmutablePair<String, String>> conversion) {
        _conversion = conversion;
    }

    @Override
    public int getCount() {
        return _conversion.size();
    }

    @Override
    public ImmutablePair<String, String> getItem(int position) {
        return _conversion.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            if (_inflater == null) {
                _inflater = LayoutInflater.from(parent.getContext());
            }
            convertView = _inflater.inflate(R.layout.conversion_details_entry, parent, false);
        }

        final ImmutablePair<String, String> pair = getItem(position);
        final String text = pair.left + " -> " + pair.right;

        final TextView textView = convertView.findViewById(R.id.textView);
        textView.setText(text);

        return convertView;
    }
}
