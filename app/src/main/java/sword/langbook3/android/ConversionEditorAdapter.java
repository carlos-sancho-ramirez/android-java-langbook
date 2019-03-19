package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntSet;
import sword.collections.Map;
import sword.collections.Set;
import sword.collections.SortFunction;
import sword.collections.SortUtils;

final class ConversionEditorAdapter extends BaseAdapter {

    public static final SortFunction<String> sortFunc = (a, b) -> SortUtils.compareCharSequenceByUnicode(b, a);

    private final ImmutableSet<ImmutablePair<String, String>> _conversion;
    private final IntSet _removed;
    private final Map<String, String> _added;
    private final Set<String> _disabled;

    private ImmutableList<Entry> _entries;
    private LayoutInflater _inflater;

    ConversionEditorAdapter(ImmutableSet<ImmutablePair<String, String>> conversion, IntSet removed, Map<String, String> added, Set<String> disabled) {
        _conversion = conversion;
        _removed = removed;
        _added = added;
        _disabled = disabled;

        updateEntries();
    }

    private void updateEntries() {
        final ImmutableSet<String> conversionKeys = _conversion.map(pair -> pair.left).toSet();
        final ImmutableSet<String> keys = conversionKeys.addAll(_added.keySet()).sort(sortFunc);

        final ImmutableList.Builder<Entry> builder = new ImmutableList.Builder<>();
        final int keyCount = keys.size();
        int convIndex = 0;
        for (int i = 0; i < keyCount; i++) {
            final String key = keys.valueAt(i);
            final boolean added = _added.containsKey(key);
            final boolean removed = conversionKeys.contains(key) && _removed.contains(convIndex);
            final boolean modified = removed && added;
            final boolean disabled = _disabled.contains(key);

            final String target;
            if (added) {
                target = _added.get(key);
            }
            else {
                final ImmutablePair<String, String> pair = _conversion.valueAt(convIndex);
                if (pair.left != key) {
                    throw new AssertionError("conversion not properly sorted");
                }
                target = pair.right;
            }

            final Entry entry = (modified && disabled)? new ModifiedDisabledEntry(convIndex, key, target) :
                    modified? new ModifiedEntry(convIndex, key, target) :
                    (added && disabled)? new AddedDisabledEntry(key, target) :
                    added? new AddedEntry(key, target) :
                    removed? new RemovedEntry(convIndex, key, target) : new NormalEntry(convIndex, key, target);

            if (conversionKeys.contains(key)) {
                convIndex++;
            }

            builder.add(entry);
        }

        _entries = builder.build();
    }

    @Override
    public int getCount() {
        return _entries.size();
    }

    @Override
    public Entry getItem(int position) {
        return _entries.valueAt(position);
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
            convertView = _inflater.inflate(R.layout.conversion_details_entry, parent, false);
        }

        final Entry entry = _entries.get(position);
        final TextView textView = convertView.findViewById(R.id.textView);
        textView.setText(entry.getText());
        textView.setBackgroundColor(entry.getBackgroundColor());

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        updateEntries();
        super.notifyDataSetChanged();
    }

    public static abstract class Entry {
        final int mPosition;
        final String mSource;
        final String mTarget;

        Entry(int position, String source, String target) {
            mPosition = position;
            mSource = source;
            mTarget = target;
        }

        abstract int getBackgroundColor();
        public abstract boolean toggleDisabledOnClick();

        int getConversionPosition() {
            return mPosition;
        }

        String getSource() {
            return mSource;
        }

        String getText() {
            return mSource + " -> " + mTarget;
        }
    }

    private static final class NormalEntry extends Entry {
        NormalEntry(int position, String source, String target) {
            super(position, source, target);
        }

        @Override
        public int getBackgroundColor() {
            return 0;
        }

        @Override
        public boolean toggleDisabledOnClick() {
            return false;
        }
    }

    private static final class RemovedEntry extends Entry {
        RemovedEntry(int position, String source, String target) {
            super(position, source, target);
        }

        @Override
        public int getBackgroundColor() {
            return 0x40FF0000;
        }

        @Override
        public boolean toggleDisabledOnClick() {
            return false;
        }
    }

    private static final class AddedEntry extends Entry {
        AddedEntry(String source, String target) {
            super(-1, source, target);
        }

        @Override
        public int getBackgroundColor() {
            return 0x4000FF00;
        }

        @Override
        public boolean toggleDisabledOnClick() {
            return true;
        }
    }

    private static final class AddedDisabledEntry extends Entry {
        AddedDisabledEntry(String source, String target) {
            super(-1, source, target);
        }

        @Override
        public int getBackgroundColor() {
            return 0x40666666;
        }

        @Override
        public boolean toggleDisabledOnClick() {
            return true;
        }
    }

    private static final class ModifiedEntry extends Entry {
        ModifiedEntry(int position, String source, String target) {
            super(position, source, target);
        }

        @Override
        public int getBackgroundColor() {
            return 0x400088FF;
        }

        @Override
        public boolean toggleDisabledOnClick() {
            return true;
        }
    }

    private static final class ModifiedDisabledEntry extends Entry {
        ModifiedDisabledEntry(int position, String source, String target) {
            super(position, source, target);
        }

        @Override
        public int getBackgroundColor() {
            return 0x40660066;
        }

        @Override
        public boolean toggleDisabledOnClick() {
            return true;
        }
    }
}
