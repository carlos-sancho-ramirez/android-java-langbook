package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableList;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.models.CharacterPickerItem;

final class CharacterPickerAdapter extends BaseAdapter {

    private final ImmutableList<CharacterPickerItem<CharacterId>> _items;
    private LayoutInflater _inflater;

    CharacterPickerAdapter(ImmutableList<CharacterPickerItem<CharacterId>> items) {
        if (items == null) {
            throw new IllegalArgumentException();
        }

        _items = items;
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public CharacterPickerItem<CharacterId> getItem(int position) {
        return _items.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return _items.valueAt(position).isComposed;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            if (_inflater == null) {
                _inflater = LayoutInflater.from(parent.getContext());
            }
            convertView = _inflater.inflate(R.layout.character_picker_item, parent, false);
        }

        final CharacterPickerItem<CharacterId> item = _items.valueAt(position);
        convertView.<TextView>findViewById(R.id.textView).setText(item.text);
        convertView.findViewById(R.id.isPartFlagView).setVisibility(item.isCompositionPart? View.VISIBLE : View.INVISIBLE);
        convertView.findViewById(R.id.isComposedFlagView).setVisibility(item.isComposed? View.VISIBLE : View.INVISIBLE);
        return convertView;
    }
}
