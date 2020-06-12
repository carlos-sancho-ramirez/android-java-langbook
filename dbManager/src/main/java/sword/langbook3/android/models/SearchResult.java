package sword.langbook3.android.models;

import sword.collections.ImmutableIntList;

import static sword.langbook3.android.collections.EqualUtils.equal;

public final class SearchResult {

    public interface Types {
        int ACCEPTATION = 0;
        int AGENT = 1;
    }

    private final String _str;
    private final String _mainStr;
    private final int _type;
    private final int _id;
    private final boolean _isDynamic;
    private final String _mainAccMainStr;
    private final ImmutableIntList _appliedRules;

    public SearchResult(String str, String mainStr, int type, int id, boolean isDynamic, String mainAccMainStr, ImmutableIntList appliedRules) {
        if (type != Types.ACCEPTATION && type != Types.AGENT || str == null || mainStr == null) {
            throw new IllegalArgumentException();
        }

        if (type != Types.ACCEPTATION && isDynamic) {
            throw new IllegalArgumentException();
        }

        _str = str;
        _mainStr = mainStr;
        _type = type;
        _id = id;
        _isDynamic = isDynamic;
        _mainAccMainStr = mainAccMainStr;
        _appliedRules = appliedRules;
    }

    public SearchResult(String str, String mainStr, int type, int id, boolean isDynamic) {
        this(str, mainStr, type, id, isDynamic, null, ImmutableIntList.empty());
    }

    public String getStr() {
        return _str;
    }

    public String getMainStr() {
        return _mainStr;
    }

    public int getType() {
        return _type;
    }

    public int getId() {
        return _id;
    }

    public boolean isDynamic() {
        return _isDynamic;
    }

    public String getMainAccMainStr() {
        return _mainAccMainStr;
    }

    public ImmutableIntList getAppliedRules() {
        return _appliedRules;
    }

    @Override
    public int hashCode() {
        return ((_type * 31 + _id) * 31 + _str.hashCode()) * 31 + _mainStr.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SearchResult)) {
            return false;
        }

        final SearchResult that = (SearchResult) other;
        return _type == that._type &&
                _id == that._id &&
                _isDynamic == that._isDynamic &&
                _str.equals(that._str) &&
                _mainStr.equals(that._mainStr) &&
                equal(_mainAccMainStr, that._mainAccMainStr) &&
                equal(_appliedRules, that._appliedRules);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + _str + ',' + _mainStr + ',' + _type + ',' + _id + ',' + _isDynamic + ',' + _mainAccMainStr + ',' + _appliedRules + ')';
    }
}
