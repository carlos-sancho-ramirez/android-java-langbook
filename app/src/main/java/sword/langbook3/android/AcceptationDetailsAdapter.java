package sword.langbook3.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AcceptationDetailsAdapter extends BaseAdapter {

    public static abstract class Item {

        private String _text;

        Item(String text) {
            if (text == null) {
                throw new IllegalArgumentException();
            }

            _text = text;
        }

        String getText() {
            return _text;
        }

        abstract void navigate(Context context);
    }

    /**
     * Non-navigable item with different UI representation.
     * Used as title of each section.
     */
    static final class HeaderItem extends Item {

        HeaderItem(String text) {
            super(text);
        }

        @Override
        void navigate(Context context) {
            // This item does not navigate
        }
    }

    /**
     * Item including an staticAcceptation and a text to be displayed.
     * This will open a new {@link AcceptationDetailsActivity} on clicking on the item.
     */
    static final class AcceptationNavigableItem extends Item {

        private final int _id;

        AcceptationNavigableItem(int id, String text) {
            super(text);
            _id = id;
        }

        @Override
        void navigate(Context context) {
            AcceptationDetailsActivity.open(context, _id, _id);
        }
    }

    /**
     * Item that is displayed as a navigable item but that does not implement the navigate method,
     * preventing any navigation.
     */
    static final class NonNavigableItem extends Item {

        NonNavigableItem(String text) {
            super(text);
        }

        @Override
        void navigate(Context context) {
            // This item does not navigate
        }
    }

    private final Item[] _items;
    private LayoutInflater _inflater;

    AcceptationDetailsAdapter(Item[] items) {
        _items = items;
    }

    @Override
    public int getCount() {
        return _items.length;
    }

    @Override
    public Item getItem(int i) {
        return _items[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (_inflater == null) {
            _inflater = LayoutInflater.from(viewGroup.getContext());
        }

        final View view;
        if (convertView == null) {
            view = _inflater.inflate(R.layout.acceptation_details_item, viewGroup, false);
        }
        else {
            view = convertView;
        }

        final TextView tv = view.findViewById(R.id.itemTextView);
        tv.setText(_items[position].getText());

        return view;
    }
}
