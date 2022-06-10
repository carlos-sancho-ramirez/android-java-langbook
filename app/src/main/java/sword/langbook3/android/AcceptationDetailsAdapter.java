package sword.langbook3.android;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AcceptationDetailsAdapter extends BaseAdapter {

    public interface ItemTypes {
        int UNKNOWN = 0;
        int BUNCH_WHERE_INCLUDED = 1;
        int ACCEPTATION_INCLUDED = 2;
        int CHARACTER_COMPOSITION_DEFINITION = 3;
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

        private final AcceptationId _id;
        private final boolean _dynamic;

        AcceptationNavigableItem(AcceptationId id, String text, boolean dynamic) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
            _dynamic = dynamic;
        }

        AcceptationNavigableItem(int itemType, AcceptationId id, String text, boolean dynamic) {
            super(itemType, text);
            _id = id;
            _dynamic = dynamic;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            AcceptationDetailsActivity.open(activity, requestCode, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }

        boolean isDynamic() {
            return _dynamic;
        }

        AcceptationId getId() {
            return _id;
        }

        @Override
        int getTextColorRes() {
            return _dynamic? R.color.agentDynamicTextColor : R.color.agentStaticTextColor;
        }
    }

    static final class CharacterPickerNavigableItem extends Item {

        private final String _characterString;

        CharacterPickerNavigableItem(String characterString, String text) {
            super(ItemTypes.UNKNOWN, text);
            _characterString = characterString;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            if (_characterString.length() == 1) {
                final CharacterId characterId = DbManager.getInstance().getManager().findCharacter(_characterString.charAt(0));
                CharacterDetailsActivity.open(activity, characterId);
            }
            else {
                CharacterPickerActivity.open(activity, _characterString);
            }
        }

        @Override
        boolean isEnabled() {
            return true;
        }
    }

    /**
     * Item including an staticAcceptation and a text to be displayed.
     * This will open a new {@link AcceptationDetailsActivity} on clicking on the item.
     */
    static final class CorrelationNavigableItem extends Item {

        private final CorrelationId _id;

        CorrelationNavigableItem(CorrelationId id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            CorrelationDetailsActivity.open(activity, requestCode, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }
    }

    static final class AgentNavigableItem extends Item {

        private final AgentId _id;

        AgentNavigableItem(AgentId id, String text) {
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

        private final SentenceId _id;

        SentenceNavigableItem(SentenceId id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            SentenceDetailsActivity.open(activity, requestCode, _id);
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

    static final class CharacterCompositionDefinitionItem extends Item {

        private final CharacterCompositionDefinitionRegister _register;

        CharacterCompositionDefinitionItem(CharacterCompositionDefinitionRegister register) {
            super(ItemTypes.CHARACTER_COMPOSITION_DEFINITION, "");
            ensureNonNull(register);
            _register = register;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            // This item does not navigate
        }

        @Override
        boolean isEnabled() {
            return true;
        }

        @Override
        int getLayout() {
            return R.layout.acceptation_details_character_composition_definition_item;
        }
    }

    static final class CorrelationArrayItem extends Item {

        private final ImmutableList<CorrelationId> _correlationIds;
        private final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> _correlations;
        private final AlphabetId _mainAlphabet;
        private final AlphabetId _pronunciationAlphabet;
        private final boolean _isNavigable;

        CorrelationArrayItem(
                ImmutableList<CorrelationId> correlationIds,
                ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations,
                AlphabetId mainAlphabet,
                AlphabetId pronunciationAlphabet,
                boolean isNavigable) {
            super(ItemTypes.UNKNOWN, "");

            if (mainAlphabet == null || pronunciationAlphabet == null || mainAlphabet.equals(pronunciationAlphabet)) {
                throw new IllegalArgumentException();
            }

            if (correlationIds == null) {
                correlationIds = ImmutableList.empty();
            }
            else if (correlationIds.anyMatch(id -> {
                final ImmutableMap<AlphabetId, String> corr = correlations.get(id, null);
                final ImmutableSet<AlphabetId> keys = (corr != null)? corr.keySet() : null;
                return keys == null || !keys.contains(mainAlphabet) || !keys.contains(pronunciationAlphabet);
            })) {
                throw new IllegalArgumentException();
            }

            _correlationIds = correlationIds;
            _correlations = correlations;
            _mainAlphabet = mainAlphabet;
            _pronunciationAlphabet = pronunciationAlphabet;
            _isNavigable = isNavigable;
        }

        @Override
        void navigate(Activity activity, int requestCode) {
            // This item does not navigate
        }

        @Override
        int getLayout() {
            return R.layout.correlation_array_container;
        }

        void updateView(Activity activity, int requestCode, LinearLayout view) {
            view.removeAllViews();
            final LayoutInflater inflater = LayoutInflater.from(view.getContext());
            for (CorrelationId correlationId : _correlationIds) {
                inflater.inflate(R.layout.correlation_container, view, true);
                final LinearLayout corrLayout = (LinearLayout) view.getChildAt(view.getChildCount() - 1);

                final ImmutableMap<AlphabetId, String> correlation = _correlations.get(correlationId);
                final String mainText = correlation.get(_mainAlphabet);
                final String pronunciationText = correlation.get(_pronunciationAlphabet);

                final TextView mainTv = corrLayout.findViewById(R.id.mainText);
                mainTv.setText(mainText);

                final TextView pronunciationTv = corrLayout.findViewById(R.id.pronunciationText);
                final String furiganaText = pronunciationText.equals(mainText)? "" : pronunciationText;
                pronunciationTv.setText(furiganaText);

                if (_isNavigable) {
                    corrLayout.setOnClickListener(v -> {
                        CorrelationDetailsActivity.open(activity, requestCode, correlationId);
                    });
                }
            }
        }
    }

    private final ImmutableList<Item> _items;
    private final boolean _allItemsEnabled;
    private final ImmutableIntSet _viewTypes;

    private final Activity _activity;
    private final int _correlationArrayRequestCode;
    private LayoutInflater _inflater;

    AcceptationDetailsAdapter(Activity activity, int correlationArrayRequestCode, ImmutableList<Item> items) {
        final MutableIntSet viewTypeSet = MutableIntArraySet.empty();
        boolean allEnabled = true;
        for (Item item : items) {
            viewTypeSet.add(item.getLayout());
            allEnabled &= item.isEnabled();
        }

        _items = items;
        _allItemsEnabled = allEnabled;
        _viewTypes = viewTypeSet.toImmutable();

        _activity = activity;
        _correlationArrayRequestCode = correlationArrayRequestCode;
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
            caItem.updateView(_activity, _correlationArrayRequestCode, (LinearLayout) view);
        }
        else if (item instanceof CharacterCompositionDefinitionItem) {
            view.findViewById(R.id.drawableHolder).setBackground(
                    new CharacterCompositionDefinitionDrawable(((CharacterCompositionDefinitionItem) item)._register));
        }
        else {
            throw new AssertionError("Unable to handle item");
        }

        return view;
    }
}
