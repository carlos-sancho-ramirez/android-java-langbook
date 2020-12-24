package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AlphabetId;

final class AlphabetsAdapter extends BaseAdapter {
    private final ImmutableIntKeyMap<String> _languages;
    private final ImmutableMap<AlphabetId, String> _alphabets;

    private final ImmutableIntSet _nextSectionHeader;
    private final ImmutableIntList _keyList;

    private LayoutInflater _inflater;

    AlphabetsAdapter(
            ImmutableIntKeyMap<ImmutableSet<AlphabetId>> map,
            ImmutableIntKeyMap<String> languages,
            ImmutableMap<AlphabetId, String> alphabets) {
        _languages = languages;
        _alphabets = alphabets;

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        final ImmutableIntList.Builder keyListBuilder = new ImmutableIntList.Builder();
        final int langCount = map.size();
        int acc = 0;
        for (int i = 0; i < langCount; i++) {
            builder.add(acc);
            acc += map.valueAt(i).size() + 1;

            keyListBuilder.append(map.keyAt(i));
            for (AlphabetId alphabetId : map.valueAt(i)) {
                keyListBuilder.append(alphabetId.key);
            }
        }

        _nextSectionHeader = builder.build();
        _keyList = keyListBuilder.build();
    }

    @Override
    public int getCount() {
        return _keyList.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    interface ViewTypes {
        int LANGUAGE = 0;
        int ALPHABET = 1;
    }

    @Override
    public int getItemViewType(int position) {
        return _nextSectionHeader.contains(position)? ViewTypes.LANGUAGE : ViewTypes.ALPHABET;
    }

    @Override
    public Integer getItem(int position) {
        return _keyList.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int viewType = getItemViewType(position);
        if (convertView == null) {
            if (_inflater == null) {
                _inflater = LayoutInflater.from(parent.getContext());
            }
            final int layout = (viewType == ViewTypes.LANGUAGE)?
                    R.layout.alphabets_adapter_language_entry : R.layout.alphabets_adapter_alphabet_entry;
            convertView = _inflater.inflate(layout, parent, false);
        }

        final TextView tv = convertView.findViewById(R.id.text);
        final int id = _keyList.valueAt(position);
        final String text = (viewType == ViewTypes.LANGUAGE)? _languages.get(id) : _alphabets.get(new AlphabetId(id));
        tv.setText(text);

        return convertView;
    }
}
