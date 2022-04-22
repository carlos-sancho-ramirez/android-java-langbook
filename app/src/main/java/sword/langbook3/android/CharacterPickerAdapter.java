package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableList;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.models.IdentifiableResult;

final class CharacterPickerAdapter extends BaseAdapter {

    private final ImmutableList<IdentifiableResult<CharacterId>> _items;
    private LayoutInflater _inflater;

    CharacterPickerAdapter(ImmutableList<IdentifiableResult<CharacterId>> items) {
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
    public IdentifiableResult<CharacterId> getItem(int position) {
        return _items.valueAt(position);
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
            convertView = _inflater.inflate(R.layout.character_picker_item, parent, false);
        }

        final IdentifiableResult<CharacterId> item = _items.valueAt(position);
        convertView.<TextView>findViewById(R.id.textView).setText(item.text);
        return convertView;
    }
}
