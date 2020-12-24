package sword.langbook3.android.db;

import java.util.Iterator;

import sword.collections.ArrayLengthFunction;
import sword.collections.Function;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntResultFunction;
import sword.collections.Map;
import sword.collections.Predicate;
import sword.collections.SortFunction;
import sword.collections.TransformerWithKey;
import sword.collections.UnmappedKeyException;

public final class ImmutableCorrelation implements Correlation, ImmutableMap<AlphabetId, String> {

    private static final ImmutableCorrelation EMPTY = new ImmutableCorrelation(ImmutableHashMap.empty());
    private final ImmutableMap<AlphabetId, String> map;

    ImmutableCorrelation(ImmutableMap<AlphabetId, String> map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        this.map = map;
    }

    public static ImmutableCorrelation empty() {
        return EMPTY;
    }

    @Override
    public int indexOfKey(AlphabetId key) {
        return map.indexOfKey(key);
    }

    @Override
    public String get(AlphabetId key) throws UnmappedKeyException {
        return map.get(key);
    }

    @Override
    public String get(AlphabetId key, String defaultValue) {
        return map.get(key, defaultValue);
    }

    @Override
    public TransformerWithKey<AlphabetId, String> iterator() {
        return map.iterator();
    }

    @Override
    public AlphabetId keyAt(int index) {
        return map.keyAt(index);
    }

    @Override
    public ImmutableSet<AlphabetId> keySet() {
        return map.keySet();
    }

    @Override
    public ImmutableSet<Map.Entry<AlphabetId, String>> entries() {
        return map.entries();
    }

    public ImmutableCorrelation put(AlphabetId key, String value) {
        final ImmutableMap<AlphabetId, String> newMap = map.put(key, value);
        return (newMap == map)? this : new ImmutableCorrelation(newMap);
    }

    @Override
    public ImmutableList<String> toList() {
        return map.toList();
    }

    @Override
    public ImmutableSet<String> toSet() {
        return map.toSet();
    }

    @Override
    public ImmutableIntSet indexes() {
        return map.indexes();
    }

    @Override
    public ImmutableIntValueMap<String> count() {
        return map.count();
    }

    @Override
    public ImmutableCorrelation filter(Predicate<? super String> predicate) {
        final ImmutableMap<AlphabetId, String> newMap = map.filter(predicate);
        return (newMap == map)? this : new ImmutableCorrelation(newMap);
    }

    @Override
    public ImmutableCorrelation filterNot(Predicate<? super String> predicate) {
        final ImmutableMap<AlphabetId, String> newMap = map.filterNot(predicate);
        return (newMap == map)? this : new ImmutableCorrelation(newMap);
    }

    @Override
    public ImmutableIntValueMap<AlphabetId> mapToInt(IntResultFunction<? super String> mapFunc) {
        return map.mapToInt(mapFunc);
    }

    @Override
    public ImmutableCorrelation toImmutable() {
        return this;
    }

    @Override
    public MutableCorrelation mutate() {
        return new MutableCorrelation(map.mutate());
    }

    @Override
    public MutableCorrelation mutate(ArrayLengthFunction arrayLengthFunction) {
        return new MutableCorrelation(map.mutate(arrayLengthFunction));
    }

    @Override
    public <U> ImmutableMap<AlphabetId, U> map(Function<? super String, ? extends U> mapFunc) {
        return map.map(mapFunc);
    }

    @Override
    public ImmutableMap<AlphabetId, String> sort(SortFunction<? super AlphabetId> function) {
        return map.sort(function);
    }

    public ImmutableCorrelation removeAt(int index) {
        return new ImmutableCorrelation(map.removeAt(index));
    }

    private static boolean entryLessThan(ImmutableList<ImmutableCorrelation> a, ImmutableList<ImmutableCorrelation> b) {
        final Iterator<ImmutableCorrelation> itA = a.iterator();
        final Iterator<ImmutableCorrelation> itB = b.iterator();

        while (itA.hasNext() && itB.hasNext()) {
            ImmutableCorrelation headA = itA.next();
            ImmutableCorrelation headB = itB.next();

            for (int i = 0; i < headA.size(); i++) {
                final AlphabetId alphabet = headA.keyAt(i);
                if (headB.size() == i) {
                    return false;
                }

                final AlphabetId alphabetB = headB.keyAt(i);

                if (alphabet.key < alphabetB.key) {
                    return true;
                }
                else if (alphabet.key > alphabetB.key) {
                    return false;
                }

                final String textA = headA.valueAt(i);
                final String textB = headB.valueAt(i);
                if (textA.length() < textB.length()) {
                    return true;
                }
                else if (textA.length() > textB.length()) {
                    return false;
                }
            }
        }

        return itB.hasNext();
    }

    private void checkPossibleCorrelationArraysRecursive(
            ImmutableSet.Builder<ImmutableList<ImmutableCorrelation>> builder,
            ImmutableCorrelation left,
            ImmutableCorrelation right) {
        final int remainingSize = size();
        if (remainingSize == 0) {
            for (ImmutableList<ImmutableCorrelation> array : right.checkPossibleCorrelationArrays()) {
                builder.add(array.prepend(left));
            }
        }
        else {
            final AlphabetId firstAlphabet = keyAt(0);
            final String firstText = valueAt(0);

            // TODO: Change this to global.skip(1) when available
            final ImmutableCorrelation.Builder tailBuilder = new ImmutableCorrelation.Builder();
            for (int i = 1; i < remainingSize; i++) {
                tailBuilder.put(keyAt(i), valueAt(i));
            }
            final ImmutableCorrelation tail = tailBuilder.build();

            final int firstTextSize = firstText.length();
            for (int i = 1; i < firstTextSize; i++) {
                final ImmutableCorrelation newLeft = left.put(firstAlphabet, firstText.substring(0, i));
                final ImmutableCorrelation newRight = right.put(firstAlphabet, firstText.substring(i));
                tail.checkPossibleCorrelationArraysRecursive(builder, newLeft, newRight);
            }
        }
    }

    public ImmutableSet<ImmutableList<ImmutableCorrelation>> checkPossibleCorrelationArrays() {
        final int globalSize = size();
        final IntResultFunction<String> lengthFunc = text -> (text == null)? 0 : text.length();
        final ImmutableIntValueMap<AlphabetId> lengths = mapToInt(lengthFunc);
        if (globalSize == 0 || lengths.anyMatch(length -> length <= 0)) {
            return ImmutableHashSet.empty();
        }

        final ImmutableSet.Builder<ImmutableList<ImmutableCorrelation>> builder = new ImmutableHashSet.Builder<>();
        builder.add(new ImmutableList.Builder<ImmutableCorrelation>().add(this).build());

        if (globalSize > 1) {
            checkPossibleCorrelationArraysRecursive(builder, ImmutableCorrelation.empty(), ImmutableCorrelation.empty());
        }
        return builder.build().sort(ImmutableCorrelation::entryLessThan);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ImmutableCorrelation)) {
            return false;
        }

        final ImmutableCorrelation that = (ImmutableCorrelation) other;
        return map.equals(that.map);
    }

    public static final class Builder {
        private final MutableCorrelation correlation = MutableCorrelation.empty();

        public Builder put(AlphabetId alphabet, String text) {
            correlation.put(alphabet, text);
            return this;
        }

        public ImmutableCorrelation build() {
            return correlation.toImmutable();
        }
    }
}
