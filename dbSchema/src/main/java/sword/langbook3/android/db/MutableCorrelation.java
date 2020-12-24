package sword.langbook3.android.db;

import sword.collections.ArrayLengthFunction;
import sword.collections.Function;
import sword.collections.IntResultFunction;
import sword.collections.IntSet;
import sword.collections.IntValueMap;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.MutableHashMap;
import sword.collections.MutableMap;
import sword.collections.Predicate;
import sword.collections.Set;
import sword.collections.SortFunction;
import sword.collections.TransformerWithKey;
import sword.collections.UnmappedKeyException;

public final class MutableCorrelation implements Correlation, MutableMap<AlphabetId, String> {

    private final MutableMap<AlphabetId, String> map;

    MutableCorrelation(MutableMap<AlphabetId, String> map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        this.map = map;
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
    public Set<AlphabetId> keySet() {
        return map.keySet();
    }

    @Override
    public Set<Map.Entry<AlphabetId, String>> entries() {
        return map.entries();
    }

    public boolean put(AlphabetId key, String value) {
        return map.put(key, value);
    }

    public boolean remove(AlphabetId key) {
        return map.remove(key);
    }

    @Override
    public List<String> toList() {
        return map.toList();
    }

    @Override
    public Set<String> toSet() {
        return map.toSet();
    }

    @Override
    public IntSet indexes() {
        return map.indexes();
    }

    @Override
    public IntValueMap<String> count() {
        return map.count();
    }

    @Override
    public Correlation filter(Predicate<? super String> predicate) {
        return new ImmutableCorrelation(map.toImmutable().filter(predicate));
    }

    @Override
    public Correlation filterNot(Predicate<? super String> predicate) {
        return new ImmutableCorrelation(map.toImmutable().filterNot(predicate));
    }

    @Override
    public IntValueMap<AlphabetId> mapToInt(IntResultFunction<? super String> mapFunc) {
        return map.mapToInt(mapFunc);
    }

    @Override
    public ImmutableCorrelation toImmutable() {
        return new ImmutableCorrelation(map.toImmutable());
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
    public Map<AlphabetId, String> sort(SortFunction<? super AlphabetId> function) {
        return map.sort(function);
    }

    @Override
    public <U> Map<AlphabetId, U> map(Function<? super String, ? extends U> mapFunc) {
        return map.map(mapFunc);
    }

    public void removeAt(int index) {
        map.removeAt(index);
    }

    public boolean clear() {
        return map.clear();
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

        if (!(other instanceof MutableCorrelation)) {
            return false;
        }

        final MutableCorrelation that = (MutableCorrelation) other;
        return map.equals(that.map);
    }

    public static MutableCorrelation empty() {
        return new MutableCorrelation(MutableHashMap.empty());
    }
}
