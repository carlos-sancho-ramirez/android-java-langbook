package sword.langbook3.android;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;

public final class AcceptationDetailsAdapter extends BaseAdapter {

    public interface ItemTypes {
        int UNKNOWN = 0;
        int BUNCH_WHERE_INCLUDED = 1;
        int ACCEPTATION_INCLUDED = 2;
    }

    static abstract class Item {

        private final int _type;
        private String _text;

        Item(int type, String text) {
            if (text == null) {
                throw new IllegalArgumentException();
            }

            _type = type;
            _text = text;
        }

        String getText() {
            return _text;
        }

        abstract void navigate(Activity activity, int requestCode);

        boolean isEnabled() {
            return false;
        }

        int getLayout() {
            return R.layout.acceptation_details_item;
        }

        int getTextColorRes() {
            return R.color.agentStaticTextColor;
        }

        int getItemType() {
            return _type;
        }
    }

    /**
     * Non-navigable item with different UI representation.
     * Used as title of each section.
     */
    static final class HeaderItem extends Item {

        HeaderItem(String text) {
            super(ItemTypes.UNKNOWN, text);
        }

        @Override
        void navigate(Activity activity, int requestCode) {
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
        private final boolean _dynamic;

        AcceptationNavigableItem(int id, String text, boolean dynamic) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
            _dynamic = dynamic;
        }

        AcceptationNavigableItem(int itemType, int id, String text, boolean dynamic) {
            super(itemType, text);
            _id = id;
            _dynamic = dynamic;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            AcceptationDetailsActivity.open(activity, requestCode, _id, _id, false);
        }

        @Override
        boolean isEnabled() {
            return true;
        }

        boolean isDynamic() {
            return _dynamic;
        }

        int getId() {
            return _id;
        }

        @Override
        int getTextColorRes() {
            return _dynamic? R.color.agentDynamicTextColor : R.color.agentStaticTextColor;
        }
    }

    /**
     * Item including an staticAcceptation and a text to be displayed.
     * This will open a new {@link AcceptationDetailsActivity} on clicking on the item.
     */
    static final class CorrelationNavigableItem extends Item {

        private final int _id;

        CorrelationNavigableItem(int id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            CorrelationDetailsActivity.open(activity, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }
    }

    /**
     * Item including a dynamic acceptation and its text representation.
     * This will open a new {@link RuleTableActivity} on clicking the item.
     */
    static final class RuleNavigableItem extends Item {

        private final int _id;

        RuleNavigableItem(int id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            RuleTableActivity.open(activity, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }

        @Override
        int getTextColorRes() {
            return R.color.agentDynamicTextColor;
        }
    }

    static final class AgentNavigableItem extends Item {

        private final int _id;

        AgentNavigableItem(int id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            AgentDetailsActivity.open(activity, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }
    }

    static final class SentenceNavigableItem extends Item {

        private final int _id;

        SentenceNavigableItem(int id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            SentenceDetailsActivity.open(activity, _id);
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
            super(ItemTypes.UNKNOWN, text);
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            // This item does not navigate
        }
    }

    static final class CorrelationArrayItem extends Item {

        private final ImmutableIntList _correlationIds;
        private final ImmutableIntKeyMap<ImmutableIntKeyMap<String>> _correlations;
        private final int _mainAlphabet;
        private final int _pronunciationAlphabet;

        CorrelationArrayItem(
                ImmutableIntList correlationIds,
                ImmutableIntKeyMap<ImmutableIntKeyMap<String>> correlations,
                int mainAlphabet,
                int pronunciationAlphabet) {
            super(ItemTypes.UNKNOWN, "");

            if (mainAlphabet == pronunciationAlphabet) {
                throw new IllegalArgumentException();
            }

            if (correlationIds == null) {
                correlationIds = ImmutableIntList.empty();
            }
            else if (correlationIds.anyMatch(id -> {
                final ImmutableIntKeyMap<String> corr = correlations.get(id, null);
                final ImmutableIntSet keys = (corr != null)? corr.keySet() : null;
                return keys == null || !keys.contains(mainAlphabet) || !keys.contains(pronunciationAlphabet);
            })) {
                throw new IllegalArgumentException();
            }

            _correlationIds = correlationIds;
            _correlations = correlations;
            _mainAlphabet = mainAlphabet;
            _pronunciationAlphabet = pronunciationAlphabet;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            // This item does not navigate
        }

        @Override
        int getLayout() {
            return R.layout.correlation_array_container;
        }

        void updateView(LinearLayout view) {
            view.removeAllViews();
            final LayoutInflater inflater = LayoutInflater.from(view.getContext());
            for (int correlationId : _correlationIds) {
                inflater.inflate(R.layout.correlation_container, view, true);
                final LinearLayout corrLayout = (LinearLayout) view.getChildAt(view.getChildCount() - 1);

                final ImmutableIntKeyMap<String> correlation = _correlations.get(correlationId);
                final String mainText = correlation.get(_mainAlphabet);
                final String pronunciationText = correlation.get(_pronunciationAlphabet);

                final TextView mainTv = corrLayout.findViewById(R.id.mainText);
                mainTv.setText(mainText);

                final TextView pronunciationTv = corrLayout.findViewById(R.id.pronunciationText);
                final String furiganaText = pronunciationText.equals(mainText)? "" : pronunciationText;
                pronunciationTv.setText(furiganaText);

                corrLayout.setOnClickListener(v -> {
                    CorrelationDetailsActivity.open(v.getContext(), correlationId);
                });
            }
        }
    }

    private final ImmutableList<Item> _items;
    private final boolean _allItemsEnabled;
    private final ImmutableIntSet _viewTypes;
    private LayoutInflater _inflater;

    AcceptationDetailsAdapter(ImmutableList<Item> items) {
        final MutableIntSet viewTypeSet = MutableIntArraySet.empty();
        boolean allEnabled = true;
        for (Item item : items) {
            viewTypeSet.add(item.getLayout());
            allEnabled &= item.isEnabled();
        }

        _items = items;
        _allItemsEnabled = allEnabled;
        _viewTypes = viewTypeSet.toImmutable();
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public Item getItem(int i) {
        return _items.get(i);
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
        return _items.get(position).isEnabled();
    }

    @Override
    public int getViewTypeCount() {
        return _viewTypes.size();
    }

    @Override
    public int getItemViewType(int position) {
        final int viewType = _viewTypes.indexOf(_items.get(position).getLayout());
        if (viewType < 0) {
            throw new AssertionError("Layout not found in view types");
        }

        return viewType;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (_inflater == null) {
            _inflater = LayoutInflater.from(viewGroup.getContext());
        }

        final Item item = _items.get(position);
        final View view;
        if (convertView == null) {
            view = _inflater.inflate(item.getLayout(), viewGroup, false);
        }
        else {
            view = convertView;
        }

        final TextView tv = view.findViewById(R.id.itemTextView);
        if (tv != null) {
            tv.setText(item.getText());
            tv.setTextColor(tv.getContext().getResources().getColor(item.getTextColorRes()));
        }
        else if (item instanceof CorrelationArrayItem) {
            final CorrelationArrayItem caItem = (CorrelationArrayItem) item;
            caItem.updateView((LinearLayout) view);
        }
        else {
            throw new AssertionError("Unable to handle item");
        }

        return view;
    }
}
