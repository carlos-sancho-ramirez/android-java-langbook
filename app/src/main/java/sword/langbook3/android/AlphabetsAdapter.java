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
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AlphabetId;

final class AlphabetsAdapter extends BaseAdapter {
    private final ImmutableIntKeyMap<String> _languages;
    private final ImmutableMap<AlphabetId, String> _alphabets;

    private final ImmutableIntSet _nextSectionHeader;
    private final ImmutableList<AlphabetId> _alphabetList;
    private final ImmutableIntList _languageList;

    private LayoutInflater _inflater;

    AlphabetsAdapter(
            ImmutableIntKeyMap<ImmutableSet<AlphabetId>> map,
            ImmutableIntKeyMap<String> languages,
            ImmutableMap<AlphabetId, String> alphabets) {
        _languages = languages;
        _alphabets = alphabets;

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        final ImmutableList.Builder<AlphabetId> alphabetListBuilder = new ImmutableList.Builder<>();
        final ImmutableIntList.Builder languageListBuilder = new ImmutableIntList.Builder();
        final int langCount = map.size();
        int acc = 0;
        for (int i = 0; i < langCount; i++) {
            builder.add(acc);
            acc += map.valueAt(i).size() + 1;

            alphabetListBuilder.append(null);
            languageListBuilder.append(map.keyAt(i));
            for (AlphabetId alphabetId : map.valueAt(i)) {
                alphabetListBuilder.append(alphabetId);
                languageListBuilder.append(0);
            }
        }

        _nextSectionHeader = builder.build();
        _alphabetList = alphabetListBuilder.build();
        _languageList = languageListBuilder.build();
    }

    @Override
    public int getCount() {
        return _alphabetList.size();
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
    public Object getItem(int position) {
        final AlphabetId alphabetId = _alphabetList.valueAt(position);
        return (alphabetId != null)? alphabetId : _languageList.valueAt(position);
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
        final String text;
        if (viewType == ViewTypes.LANGUAGE) {
            final int languageId = _languageList.valueAt(position);
            text = _languages.get(languageId);
        }
        else {
            final AlphabetId alphabetId = _alphabetList.valueAt(position);
            text = _alphabets.get(alphabetId);
        }

        tv.setText(text);
        return convertView;
    }
}
