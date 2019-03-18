package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.IntSet;
import sword.collections.Map;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSortedMap;
import sword.collections.SortUtils;

public final class ConversionEditorActivityState implements Parcelable {

    private final MutableIntSet _removed;
    private final MutableMap<String, String> _added;

    private ConversionEditorActivityState(MutableIntSet removed, MutableMap<String, String> added) {
        _removed = removed;
        _added = added;
    }

    ConversionEditorActivityState() {
        this(MutableIntArraySet.empty(), MutableSortedMap.empty((a, b) -> SortUtils.compareCharSequenceByUnicode(b, a)));
    }

    IntSet getRemoved() {
        return _removed;
    }

    Map<String, String> getAdded() {
        return _added;
    }

    void toggleRemoved(int position) {
        if (_removed.contains(position)) {
            _removed.remove(position);
        }
        else {
            _removed.add(position);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int removedWordCount = _removed.isEmpty()? 0 : (_removed.max() / 32) + 1;
        dest.writeInt(removedWordCount);

        if (removedWordCount != 0) {
            int word = 0;
            int wordIndex = 0;
            for (int value : _removed) {
                final int valueWordIndex = value >>> 5;
                while (wordIndex < valueWordIndex) {
                    dest.writeInt(word);
                    word = 0;
                    wordIndex++;
                }

                word |= 1 << (value & 0x1F);
            }

            dest.writeInt(word);
        }

        final int mapSize = _added.size();
        dest.writeInt(mapSize);
        for (int i = 0; i < mapSize; i++) {
            dest.writeString(_added.keyAt(i));
            dest.writeString(_added.valueAt(i));
        }
    }

    public static final Creator<ConversionEditorActivityState> CREATOR = new Creator<ConversionEditorActivityState>() {
        @Override
        public ConversionEditorActivityState createFromParcel(Parcel in) {
            final int removedWordCount = in.readInt();
            final MutableIntSet removed = MutableIntArraySet.empty();
            for (int wordIndex = 0; wordIndex < removedWordCount; wordIndex++) {
                int word = in.readInt();
                for (int bitIndex = 0; word != 0; bitIndex++) {
                    if ((word & 1) != 0) {
                        removed.add(wordIndex * 32 + bitIndex);
                    }
                    word >>>= 1;
                }
            }

            final int mapSize = in.readInt();
            final MutableSortedMap<String, String> added = MutableSortedMap.empty((a, b) -> SortUtils.compareCharSequenceByUnicode(b, a));
            for (int mapIndex = 0; mapIndex < mapSize; mapIndex++) {
                final String key = in.readString();
                final String value = in.readString();
                added.put(key, value);
            }

            return new ConversionEditorActivityState(removed, added);
        }

        @Override
        public ConversionEditorActivityState[] newArray(int size) {
            return new ConversionEditorActivityState[size];
        }
    };
}
