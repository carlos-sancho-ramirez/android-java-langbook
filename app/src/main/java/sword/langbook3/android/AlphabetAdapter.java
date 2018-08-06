package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public final class AlphabetAdapter extends BaseAdapter {
    private final Item[] _entries;
    private LayoutInflater _inflater;

    AlphabetAdapter(Item[] entries) {
        _entries = entries;
    }

    @Override
    public int getCount() {
        return _entries.length;
    }

    @Override
    public Item getItem(int position) {
        return _entries[position];
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
        textView.setText(_entries[position].name);

        return view;
    }

    public static final class Item {
        final int id;
        final String name;

        Item(int id, String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof Item)) {
                return false;
            }

            final Item that = (Item) other;
            return id == that.id && name.equals(that.name);
        }
    }
}
