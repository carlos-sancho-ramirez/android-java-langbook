package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableIntArraySet;
import sword.langbook3.android.db.BunchId;

public final class MatchingBunchesPickerAdapter extends BaseAdapter {

    private final ImmutableMap<BunchId, String> _entries;
    private final MutableIntArraySet _checked = MutableIntArraySet.empty();
    private LayoutInflater _inflater;

    public MatchingBunchesPickerAdapter(ImmutableMap<BunchId, String> entries) {
        _entries = entries;
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public BunchId getItem(int position) {
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

    public ImmutableSet<BunchId> getCheckedBunches() {
        final ImmutableSet.Builder<BunchId> builder = new ImmutableHashSet.Builder<>();
        for (int position : _checked) {
            builder.add(_entries.keyAt(position));
        }

        return builder.build();
    }
}
