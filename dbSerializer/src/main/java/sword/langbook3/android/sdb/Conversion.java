package sword.langbook3.android.sdb;

import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.Map;
import sword.collections.SortFunction;
import sword.collections.SortUtils;
import sword.langbook3.android.collections.ImmutableIntPair;

public final class Conversion {

    public static final SortFunction<String> keySortFunction = (a, b) -> SortUtils
            .compareCharSequenceByUnicode(b, a);
    public static final SortFunction<ImmutablePair<String, String>> pairSortFunction = (a, b) ->
            SortUtils.compareCharSequenceByUnicode(b.left, a.left);

    private final int _sourceAlphabet;
    private final int _targetAlphabet;
    private final ImmutableMap<String, String> _map;

    public Conversion(int sourceAlphabet, int targetAlphabet, Map<String, String> map) {
        if (sourceAlphabet == targetAlphabet) {
            throw new IllegalArgumentException();
        }

        _sourceAlphabet = sourceAlphabet;
        _targetAlphabet = targetAlphabet;
        _map = map.toImmutable().sort(keySortFunction);
    }

    public int getSourceAlphabet() {
        return _sourceAlphabet;
    }

    public int getTargetAlphabet() {
        return _targetAlphabet;
    }

    public ImmutableIntPair getAlphabets() {
        return new ImmutableIntPair(_sourceAlphabet, _targetAlphabet);
    }

    public ImmutableMap<String, String> getMap() {
        return _map;
    }

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
}
