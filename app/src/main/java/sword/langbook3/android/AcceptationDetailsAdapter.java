package sword.langbook3.android;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

public final class AcceptationDetailsAdapter extends BaseAdapter {

    public interface ItemTypes {
        int UNKNOWN = 0;
        int BUNCH_WHERE_INCLUDED = 1;
        int ACCEPTATION_INCLUDED = 2;
        int CHARACTER_COMPOSITION_DEFINITION = 3;
    }

    public static abstract class Item {

        private final int _type;
        private String _text;

        Item(int type, String text) {
            if (text == null) {
                throw new IllegalArgumentException();
            }

            _type = type;
            _text = text;
        }

        public String getText() {
            return _text;
        }

        public abstract void navigate(@NonNull ActivityExtensions activity, int requestCode);

        boolean isEnabled() {
            return false;
        }

        int getLayout() {
            return R.layout.acceptation_details_item;
        }

        int getTextColorRes() {
            return R.color.agentStaticTextColor;
        }

        public int getItemType() {
            return _type;
        }
    }

    /**
     * Non-navigable item with different UI representation.
     * Used as title of each section.
     */
    public static final class HeaderItem extends Item {

        public HeaderItem(String text) {
            super(ItemTypes.UNKNOWN, text);
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
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
    public static final class AcceptationNavigableItem extends Item {

        private final AcceptationId _id;
        private final boolean _dynamic;

        public AcceptationNavigableItem(AcceptationId id, String text, boolean dynamic) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
            _dynamic = dynamic;
        }

        public AcceptationNavigableItem(int itemType, AcceptationId id, String text, boolean dynamic) {
            super(itemType, text);
            _id = id;
            _dynamic = dynamic;
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
            AcceptationDetailsActivity.open(activity, requestCode, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }

        public boolean isDynamic() {
            return _dynamic;
        }

        public AcceptationId getId() {
            return _id;
        }

        @Override
        int getTextColorRes() {
            return _dynamic? R.color.agentDynamicTextColor : R.color.agentStaticTextColor;
        }
    }

    public static final class CharacterPickerNavigableItem extends Item {

        private final String _characterString;

        public CharacterPickerNavigableItem(String characterString, String text) {
            super(ItemTypes.UNKNOWN, text);
            _characterString = characterString;
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
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
    public static final class CorrelationNavigableItem extends Item {

        private final CorrelationId _id;

        public CorrelationNavigableItem(CorrelationId id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
            CorrelationDetailsActivity.open(activity, requestCode, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }
    }

    public static final class AgentNavigableItem extends Item {

        private final AgentId _id;

        public AgentNavigableItem(AgentId id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
            AgentDetailsActivity.open(activity, _id);
        }

        @Override
        boolean isEnabled() {
            return true;
        }
    }

    public static final class SentenceNavigableItem extends Item {

        private final SentenceId _id;

        public SentenceNavigableItem(SentenceId id, String text) {
            super(ItemTypes.UNKNOWN, text);
            _id = id;
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
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
    public static final class NonNavigableItem extends Item {

        public NonNavigableItem(String text) {
            super(ItemTypes.UNKNOWN, text);
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
            // This item does not navigate
        }
    }

    public static final class CharacterCompositionDefinitionItem extends Item {

        private final CharacterCompositionDefinitionRegister _register;

        public CharacterCompositionDefinitionItem(CharacterCompositionDefinitionRegister register) {
            super(ItemTypes.CHARACTER_COMPOSITION_DEFINITION, "");
            ensureNonNull(register);
            _register = register;
        }

        @Override
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
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

    public static final class CorrelationArrayItem extends Item {

        private final ImmutableList<CorrelationId> _correlationIds;
        private final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> _correlations;
        private final AlphabetId _mainAlphabet;
        private final AlphabetId _pronunciationAlphabet;
        private final boolean _isNavigable;

        public CorrelationArrayItem(
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
        public void navigate(@NonNull ActivityExtensions activity, int requestCode) {
            // This item does not navigate
        }

        @Override
        int getLayout() {
            return R.layout.correlation_array_container;
        }

        void updateView(ActivityExtensions activity, int requestCode, LinearLayout view) {
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

    private final ActivityExtensions _activityAdapter;
    private final int _correlationArrayRequestCode;
    private LayoutInflater _inflater;

    public AcceptationDetailsAdapter(ActivityExtensions activity, int correlationArrayRequestCode, ImmutableList<Item> items) {
        final MutableIntSet viewTypeSet = MutableIntArraySet.empty();
        boolean allEnabled = true;
        for (Item item : items) {
            viewTypeSet.add(item.getLayout());
            allEnabled &= item.isEnabled();
        }

        _items = items;
        _allItemsEnabled = allEnabled;
        _viewTypes = viewTypeSet.toImmutable();

        _activityAdapter = activity;
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
            caItem.updateView(_activityAdapter, _correlationArrayRequestCode, (LinearLayout) view);
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
