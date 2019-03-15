package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.MutableIntArraySet;

final class MatchingBunchesPickerAdapter extends BaseAdapter {

    private final ImmutableIntKeyMap<String> _entries;
    private final MutableIntArraySet _checked = MutableIntArraySet.empty();
    private LayoutInflater _inflater;

    MatchingBunchesPickerAdapter(ImmutableIntKeyMap<String> entries) {
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

            view = _inflater.inflate(R.layout.matching_bunches_picker_entry, parent, false);
        }
        else {
            view = convertView;
        }

        final CheckBox checkBox = view.findViewById(R.id.checkBox);
        checkBox.setText(_entries.valueAt(position));

        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(_checked.contains(position));
        checkBox.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) _checked.add(position);
            else _checked.remove(position);
        });

        return view;
    }

    public ImmutableIntSet getCheckedBunches() {
        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        for (int position : _checked) {
            builder.add(_entries.keyAt(position));
        }

        return builder.build();
    }
}
