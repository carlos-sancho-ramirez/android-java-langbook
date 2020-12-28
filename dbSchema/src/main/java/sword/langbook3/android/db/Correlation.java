package sword.langbook3.android.db;

import sword.collections.ArrayLengthFunction;
import sword.collections.IntResultFunction;
import sword.collections.IntValueMap;
import sword.collections.Map;
import sword.collections.MutableList;
import sword.collections.Predicate;
import sword.collections.Set;

public interface Correlation<AlphabetId> extends Map<AlphabetId, String> {

    @Override
    Correlation<AlphabetId> filter(Predicate<? super String> predicate);

    @Override
    Correlation<AlphabetId> filterNot(Predicate<? super String> predicate);

    IntValueMap<AlphabetId> mapToInt(IntResultFunction<? super String> func);

    ImmutableCorrelation<AlphabetId> toImmutable();

    MutableCorrelation<AlphabetId> mutate();

    MutableCorrelation<AlphabetId> mutate(ArrayLengthFunction arrayLengthFunction);

    default boolean equalCorrelation(Correlation<AlphabetId> that) {
        if (that == null) {
            return false;
        }

        final Set<AlphabetId> keySet = keySet();
        if (!keySet.equalSet(that.keySet())) {
            return false;
        }

        for (AlphabetId key : keySet) {
            final String a = get(key);
            final String b = that.get(key);
            if (a != b && (a == null || !a.equals(b))) {
                return false;
            }
        }

        return true;
    }

    default MutableList<Entry<AlphabetId>> toCorrelationEntryList() {
        final MutableList<Entry<AlphabetId>> result = MutableList.empty();
        for (Map.Entry<AlphabetId, String> entry : entries()) {
            result.append(new Entry<>(entry.key(), entry.value()));
        }

        return result;
    }

    final class Entry<AlphabetId> {
        public AlphabetId alphabet;
        public String text;

        public Entry(AlphabetId alphabet, String text) {
            if (alphabet == null) {
                throw new IllegalArgumentException();
            }

            this.alphabet = alphabet;
            this.text = text;
        }

        @Override
        public String toString() {
            return "Correlation.Entry("+ alphabet + ", " + text + ')';
        }

        @Override
        public int hashCode() {
            return alphabet.hashCode() * 37 + ((text == null)? 0 : text.hashCode());
        }

        private boolean equal(Object a, Object b) {
            return a == b || a != null && a.equals(b);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof Entry)) {
                return false;
            }

            final Entry that = (Entry) other;
            return alphabet == that.alphabet && equal(text, that.text);
        }
    }
}
