package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntSet;
import sword.collections.Map;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.collections.MutableSortedMap;
import sword.collections.Set;
import sword.collections.SortUtils;
import sword.langbook3.android.LangbookReadableDatabase.Conversion;

import static sword.langbook3.android.EqualUtils.equal;
import static sword.langbook3.android.LangbookReadableDatabase.conversionKeySortFunction;

public final class ConversionEditorActivityState implements Parcelable {

    private final MutableIntSet _removed;
    private final MutableMap<String, String> _added;
    private final MutableSet<String> _disabled;

    private boolean _modifying;
    private String _sourceModificationText;
    private String _targetModificationText;

    private ConversionEditorActivityState(MutableIntSet removed, MutableMap<String, String> added, MutableSet<String> disabled,
            boolean modifying, String sourceModificationText, String targetModificationText) {
        _removed = removed;
        _added = added;
        _disabled = disabled;
        _modifying = modifying;
        _sourceModificationText = sourceModificationText;
        _targetModificationText = targetModificationText;
    }

    ConversionEditorActivityState() {
        this(MutableIntArraySet.empty(), MutableSortedMap.empty(conversionKeySortFunction), MutableHashSet.empty(), false, null, null);
    }

    IntSet getRemoved() {
        return _removed;
    }

    Map<String, String> getAdded() {
        return _added;
    }

    Set<String> getDisabled() {
        return _disabled;
    }

    void toggleRemoved(int position) {
        if (_removed.contains(position)) {
            _removed.remove(position);
        }
        else {
            _removed.add(position);
        }
    }

    void toggleEnabled(String key) {
        if (!_disabled.remove(key)) {
            if (!_added.containsKey(key)) {
                throw new IllegalArgumentException();
            }

            if (!_disabled.add(key)) {
                throw new AssertionError();
            }
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

    boolean applyModification(Conversion conversion) {
        if (!_modifying) {
            throw new UnsupportedOperationException();
        }

        final boolean changed;
        final String source = _sourceModificationText;
        final String target = _targetModificationText;
        if (TextUtils.isEmpty(source)) {
            changed = false;
        }
        else if (_added.containsKey(source)) {
            if (equal(_added.get(source), target)) {
                changed = false;
            }
            else {
                _added.put(source, target);
                changed = true;
            }
        }
        else {
            final Map<String, String> map = conversion.getMap();
            final int conversionSize = map.size();
            int conversionIndex;
            for (conversionIndex = 0; conversionIndex < conversionSize; conversionIndex++) {
                if (equal(map.keyAt(conversionIndex), source)) {
                    break;
                }
            }

            if (conversionIndex < conversionSize) {
                final String oldTarget = map.valueAt(conversionIndex);
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
            if ((i % 32) == 0) {
                int word = 0;
                for (int j = i; j < i + 32 && j < mapSize; j++) {
                    final String key = _added.keyAt(j);
                    if (_disabled.contains(key)) {
                        word |= 1 << (j % 32);
                    }
                }
                dest.writeInt(word);
            }

            dest.writeString(_added.keyAt(i));
            dest.writeString(_added.valueAt(i));
        }

        dest.writeInt(_modifying? 1 : 0);
        if (_modifying) {
            dest.writeString(_sourceModificationText);
            dest.writeString(_targetModificationText);
        }
    }

    Conversion getResultingConversion(Conversion originalConversion) {
        Map<String, String> origMap = originalConversion.getMap();
        final int originalSize = origMap.size();
        final ImmutableMap.Builder<String, String> nonRemovedMapBuilder = new ImmutableHashMap.Builder<>();
        for (int i = 0; i < originalSize; i++) {
            if (!_removed.contains(i)) {
                nonRemovedMapBuilder.put(origMap.keyAt(i), origMap.valueAt(i));
            }
        }
        final ImmutableMap<String, String> nonRemovedMap = nonRemovedMapBuilder.build();

        final Set<String> enabledAddedKeys = _added.keySet().filterNot(_disabled::contains);
        final ImmutableSet<String> keys = nonRemovedMap.keySet().addAll(enabledAddedKeys);

        final MutableHashMap<String, String> result = MutableHashMap.empty();
        for (String key : keys) {
            final Map<String, String> map = (enabledAddedKeys.contains(key))? _added : nonRemovedMap;
            result.put(key, map.get(key));
        }

        return new Conversion(originalConversion.getSourceAlphabet(), originalConversion.getTargetAlphabet(), result);
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
            final MutableHashSet<String> disabled = MutableHashSet.empty();
            final MutableSortedMap<String, String> added = MutableSortedMap.empty((a, b) -> SortUtils.compareCharSequenceByUnicode(b, a));

            int word = 0;
            int bitCount = 0;
            for (int mapIndex = 0; mapIndex < mapSize; mapIndex++) {
                if (bitCount == 0) {
                    word = in.readInt();
                    bitCount = 32;
                }

                final String key = in.readString();
                final String value = in.readString();
                added.put(key, value);

                if ((word & 1) != 0) {
                    disabled.add(key);
                }
                word >>>= 1;
                --bitCount;
            }

            final boolean modifying = in.readInt() != 0;
            final String sourceModificationText = modifying? in.readString() : null;
            final String targetModificationText = modifying? in.readString() : null;
            return new ConversionEditorActivityState(removed, added, disabled, modifying, sourceModificationText, targetModificationText);
        }

        @Override
        public ConversionEditorActivityState[] newArray(int size) {
            return new ConversionEditorActivityState[size];
        }
    };
}
