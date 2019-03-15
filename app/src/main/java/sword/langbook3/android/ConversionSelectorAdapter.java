package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;

final class ConversionSelectorAdapter extends BaseAdapter {

    private final ImmutableSet<ImmutableIntPair> _conversions;
    private final ImmutableIntKeyMap<String> _alphabetTexts;
    private LayoutInflater _inflater;

    ConversionSelectorAdapter(ImmutableSet<ImmutableIntPair> conversions, ImmutableIntKeyMap<String> alphabetTexts) {
        _conversions = conversions;
        _alphabetTexts = alphabetTexts;
    }

    @Override
    public int getCount() {
        return _conversions.size();
    }

    @Override
    public ImmutableIntPair getItem(int position) {
        return _conversions.valueAt(position);
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
            convertView = _inflater.inflate(R.layout.conversion_selector_entry, parent, false);
        }

        final ImmutableIntPair pair = _conversions.valueAt(position);
        final String text = _alphabetTexts.get(pair.left) + " -> " + _alphabetTexts.get(pair.right);

        final TextView textView = convertView.findViewById(R.id.textView);
        textView.setText(text);

        return convertView;
    }
}
