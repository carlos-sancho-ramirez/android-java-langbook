package sword.langbook3.android.models;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.SortFunction;
import sword.collections.SortUtils;

public final class Conversion<AlphabetId> implements ConversionProposal<AlphabetId> {

    public static final SortFunction<String> keySortFunction = (a, b) -> SortUtils
            .compareCharSequenceByUnicode(b, a);
    public static final SortFunction<ImmutablePair<String, String>> pairSortFunction = (a, b) ->
            SortUtils.compareCharSequenceByUnicode(b.left, a.left);

    private final AlphabetId _sourceAlphabet;
    private final AlphabetId _targetAlphabet;
    private final ImmutableMap<String, String> _map;

    public Conversion(AlphabetId sourceAlphabet, AlphabetId targetAlphabet, sword.collections.Map<String, String> map) {
        if (sourceAlphabet == null || sourceAlphabet.equals(targetAlphabet)) {
            throw new IllegalArgumentException();
        }

        _sourceAlphabet = sourceAlphabet;
        _targetAlphabet = targetAlphabet;
        _map = map.toImmutable().sort(keySortFunction);
    }

    @Override
    public AlphabetId getSourceAlphabet() {
        return _sourceAlphabet;
    }

    public AlphabetId getTargetAlphabet() {
        return _targetAlphabet;
    }

    public ImmutablePair<AlphabetId, AlphabetId> getAlphabets() {
        return new ImmutablePair<>(_sourceAlphabet, _targetAlphabet);
    }

    public ImmutableMap<String, String> getMap() {
        return _map;
    }

    @Override
    public String convert(String text) {
        final int mapSize = _map.size();
        String result = "";
        while (text.length() > 0) {
            boolean found = false;
            for (int i = 0; i < mapSize; i++) {
                final String source = _map.keyAt(i);
                if (text.startsWith(source)) {
                    result += _map.valueAt(i);
                    text = text.substring(source.length());
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return result;
    }

    /**
     * Apply the given conversion in the inverse order to find all original
     * strings that can be converted to the given text.
     *
     * @param text Converted text to be analyzed
     * @return A set with all source texts that results in the given text once the conversion is applied. This will be empty is none, but never null.
     */
    public ImmutableSet<String> findSourceTexts(String text) {
        final ImmutableSet.Builder<String> builder = new ImmutableHashSet.Builder<>();
        if (text == null) {
            return builder.build();
        }

        final int mapSize = _map.size();
        for (int i = 0; i < mapSize; i++) {
            final String source = _map.keyAt(i);
            final String target = _map.valueAt(i);
            if (target.equals(text)) {
                builder.add(source);
            }
            else if (text.startsWith(target)) {
                for (String result : findSourceTexts(text.substring(target.length()))) {
                    builder.add(source + result);
                }
            }
        }

        return builder.build();
    }
}
