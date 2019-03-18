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
import sword.collections.SortFunction;
import sword.collections.SortUtils;

final class ConversionEditorAdapter extends BaseAdapter {

    private static final SortFunction<String> sortFunc = (a, b) -> SortUtils.compareCharSequenceByUnicode(b, a);

    private final ImmutableSet<ImmutablePair<String, String>> _conversion;
    private final IntSet _removed;
    private final Map<String, String> _added;

    private ImmutableList<Entry> _entries;
    private LayoutInflater _inflater;

    ConversionEditorAdapter(ImmutableSet<ImmutablePair<String, String>> conversion, IntSet removed, Map<String, String> added) {
        _conversion = conversion;
        _removed = removed;
        _added = added;

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
            final boolean added = !conversionKeys.contains(key);
            final boolean removed = _removed.contains(convIndex);

            final String text;
            if (!added) {
                final ImmutablePair<String, String> pair = _conversion.valueAt(convIndex);
                if (pair.left != key) {
                    throw new AssertionError("conversion not properly sorted");
                }
                text = key + " -> " + pair.right;
                convIndex++;
            }
            else {
                text = key + " -> " + _added.get(key);
            }

            final Entry entry = added? new AddedEntry(text) :
                    removed? new RemovedEntry(convIndex - 1, text) : new NormalEntry(convIndex - 1, text);
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

    public interface Entry {
        int getBackgroundColor();
        int getConversionPosition();
        String getText();
        boolean isRemoved();
    }

    private static final class NormalEntry implements Entry {
        final int mPosition;
        final String mText;

        NormalEntry(int position, String text) {
            mPosition = position;
            mText = text;
        }

        @Override
        public int getBackgroundColor() {
            return 0;
        }

        @Override
        public int getConversionPosition() {
            return mPosition;
        }

        @Override
        public String getText() {
            return mText;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }
    }

    private static final class RemovedEntry implements Entry {
        final int mPosition;
        final String mText;

        RemovedEntry(int position, String text) {
            mPosition = position;
            mText = text;
        }

        @Override
        public int getConversionPosition() {
            return mPosition;
        }

        @Override
        public int getBackgroundColor() {
            return 0x40FF0000;
        }

        @Override
        public String getText() {
            return mText;
        }

        @Override
        public boolean isRemoved() {
            return true;
        }
    }

    private static final class AddedEntry implements Entry {
        final String mText;

        AddedEntry(String text) {
            mText = text;
        }

        @Override
        public int getBackgroundColor() {
            return 0x4000FF00;
        }

        @Override
        public int getConversionPosition() {
            return -1;
        }

        @Override
        public String getText() {
            return mText;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }
    }
}
