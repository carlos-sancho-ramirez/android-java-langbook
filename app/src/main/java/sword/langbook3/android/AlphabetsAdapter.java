package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LanguageId;

final class AlphabetsAdapter extends BaseAdapter {
    private final ImmutableMap<Object, String> _concepts;
    private final ImmutableList<Object> _conceptList;

    private LayoutInflater _inflater;

    AlphabetsAdapter(
            ImmutableMap<LanguageId, ImmutableSet<AlphabetId>> map,
            ImmutableMap<LanguageId, String> languages,
            ImmutableMap<AlphabetId, String> alphabets) {
        final ImmutableMap.Builder<Object, String> conceptsBuilder = new ImmutableHashMap.Builder<>();
        for (Map.Entry<LanguageId, String> entry : languages.entries()) {
            conceptsBuilder.put(entry.key(), entry.value());
        }

        for (Map.Entry<AlphabetId, String> entry : alphabets.entries()) {
            conceptsBuilder.put(entry.key(), entry.value());
        }
        _concepts = conceptsBuilder.build();

        final ImmutableList.Builder<Object> conceptListBuilder = new ImmutableList.Builder<>();
        final int langCount = map.size();
        for (int i = 0; i < langCount; i++) {
            conceptListBuilder.append(map.keyAt(i));
            for (AlphabetId alphabetId : map.valueAt(i)) {
                conceptListBuilder.append(alphabetId);
            }
        }

        _conceptList = conceptListBuilder.build();
    }

    @Override
    public int getCount() {
        return _conceptList.size();
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
        return (_conceptList.get(position) instanceof LanguageId)? ViewTypes.LANGUAGE : ViewTypes.ALPHABET;
    }

    @Override
    public Object getItem(int position) {
        return _conceptList.valueAt(position);
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
        final String text = _concepts.get(_conceptList.valueAt(position));

        tv.setText(text);
        return convertView;
    }
}
