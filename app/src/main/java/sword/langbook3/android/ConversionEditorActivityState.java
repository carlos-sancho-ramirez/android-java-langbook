package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntSet;
import sword.collections.Map;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSortedMap;
import sword.collections.SortUtils;

import static sword.langbook3.android.EqualUtils.equal;

public final class ConversionEditorActivityState implements Parcelable {

    private final MutableIntSet _removed;
    private final MutableMap<String, String> _added;
    private boolean _modifying;
    private String _sourceModificationText;
    private String _targetModificationText;

    private ConversionEditorActivityState(MutableIntSet removed, MutableMap<String, String> added,
            boolean modifying, String sourceModificationText, String targetModificationText) {
        _removed = removed;
        _added = added;
        _modifying = modifying;
        _sourceModificationText = sourceModificationText;
        _targetModificationText = targetModificationText;
    }

    ConversionEditorActivityState() {
        this(MutableIntArraySet.empty(), MutableSortedMap.empty(ConversionEditorAdapter.sortFunc), false, null, null);
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

    boolean shouldDisplayModificationDialog() {
        return _modifying;
    }

    void startModification() {
        if (_modifying) {
            throw new UnsupportedOperationException();
        }

        _modifying = true;
    }

    String getSourceModificationText() {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        return _sourceModificationText;
    }

    void updateSourceModificationText(String text) {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        _sourceModificationText = text;
    }

    String getTargetModificationText() {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        return _targetModificationText;
    }

    void updateTargetModificationText(String text) {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        _targetModificationText = text;
    }

    void cancelModification() {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        _modifying = false;
        _sourceModificationText = null;
        _targetModificationText = null;
    }

    boolean applyModification(ImmutableSet<ImmutablePair<String, String>> conversion) {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        final boolean changed;
        final String source = _sourceModificationText;
        final String target = _targetModificationText;
        if (_added.containsKey(source)) {
            if (equal(_added.get(source), target)) {
                changed = false;
            }
            else {
                _added.put(source, target);
                changed = true;
            }
        }
        else {
            final int conversionSize = conversion.size();
            int conversionIndex;
            for (conversionIndex = 0; conversionIndex < conversionSize; conversionIndex++) {
                if (equal(conversion.valueAt(conversionIndex).left, source)) {
                    break;
                }
            }

            if (conversionIndex < conversionSize) {
                final String oldTarget = conversion.valueAt(conversionIndex).right;
                if (equal(oldTarget, target)) {
                    changed = false;
                }
                else {
                    _removed.add(conversionIndex);
                    _added.put(source, target);
                    changed = true;
                }
            }
            else {
                _added.put(source, target);
                changed = true;
            }
        }

        _modifying = false;
        _sourceModificationText = null;
        _targetModificationText = null;
        return changed;
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

        dest.writeInt(_modifying? 1 : 0);
        if (_modifying) {
            dest.writeString(_sourceModificationText);
            dest.writeString(_targetModificationText);
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

            final boolean modifying = in.readInt() != 0;
            final String sourceModificationText = modifying? in.readString() : null;
            final String targetModificationText = modifying? in.readString() : null;
            return new ConversionEditorActivityState(removed, added, modifying, sourceModificationText, targetModificationText);
        }

        @Override
        public ConversionEditorActivityState[] newArray(int size) {
            return new ConversionEditorActivityState[size];
        }
    };
}
