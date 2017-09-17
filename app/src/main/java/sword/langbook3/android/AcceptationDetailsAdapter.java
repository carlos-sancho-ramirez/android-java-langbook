package sword.langbook3.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

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

        boolean isEnabled() {
            return false;
        }

        int getLayout() {
            return R.layout.acceptation_details_item;
        }
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

        int getLayout() {
            return R.layout.acceptation_details_header;
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

        @Override
        boolean isEnabled() {
            return true;
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
    private final boolean _allItemsEnabled;
    private final int[] _viewTypes;
    private LayoutInflater _inflater;

    AcceptationDetailsAdapter(Item[] items) {
        final Set<Integer> viewTypeSet = new HashSet<>();
        boolean allEnabled = true;
        for (Item item : items) {
            viewTypeSet.add(item.getLayout());
            allEnabled &= item.isEnabled();
        }

        final int[] viewTypes = new int[viewTypeSet.size()];
        int index = 0;
        for (int layout : viewTypeSet) {
            viewTypes[index++] = layout;
        }

        _items = items;
        _allItemsEnabled = allEnabled;
        _viewTypes = viewTypes;
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
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return _allItemsEnabled;
    }

    @Override
    public boolean isEnabled(int position) {
        return _items[position].isEnabled();
    }

    @Override
    public int getViewTypeCount() {
        return _viewTypes.length;
    }

    @Override
    public int getItemViewType(int position) {
        final int layout = _items[position].getLayout();
        final int viewTypeCount = _viewTypes.length;

        for (int i = 0; i < viewTypeCount; i++) {
            if (layout == _viewTypes[i]) {
                return i;
            }
        }

        throw new AssertionError("Layout not found in view types");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (_inflater == null) {
            _inflater = LayoutInflater.from(viewGroup.getContext());
        }

        final Item item = _items[position];
        final View view;
        if (convertView == null) {
            view = _inflater.inflate(item.getLayout(), viewGroup, false);
        }
        else {
            view = convertView;
        }

        final TextView tv = view.findViewById(R.id.itemTextView);
        tv.setText(_items[position].getText());

        return view;
    }
}
