package sword.langbook3.android.models;

import sword.collections.ImmutableIntList;
import sword.langbook3.android.db.AcceptationIdInterface;

import static sword.langbook3.android.collections.EqualUtils.equal;

public final class SearchResult<ID> {

    private final String _str;
    private final String _mainStr;
    private final ID _id;
    private final boolean _isDynamic;
    private final String _mainAccMainStr;
    private final ImmutableIntList _appliedRules;

    public SearchResult(String str, String mainStr, ID id, boolean isDynamic, String mainAccMainStr, ImmutableIntList appliedRules) {
        if (!(id instanceof AcceptationIdInterface) && !(id instanceof Integer) || str == null || mainStr == null) {
            throw new IllegalArgumentException();
        }

        if (!(id instanceof AcceptationIdInterface) && isDynamic) {
            throw new IllegalArgumentException();
        }

        _str = str;
        _mainStr = mainStr;
        _id = id;
        _isDynamic = isDynamic;
        _mainAccMainStr = mainAccMainStr;
        _appliedRules = appliedRules;
    }

    public SearchResult(String str, String mainStr, ID id, boolean isDynamic) {
        this(str, mainStr, id, isDynamic, null, ImmutableIntList.empty());
    }

    public String getStr() {
        return _str;
    }

    public String getMainStr() {
        return _mainStr;
    }

    public ID getId() {
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
        return (_id.hashCode() * 31 + _str.hashCode()) * 31 + _mainStr.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SearchResult)) {
            return false;
        }

        final SearchResult that = (SearchResult) other;
        return _id.equals(that._id) &&
                _isDynamic == that._isDynamic &&
                _str.equals(that._str) &&
                _mainStr.equals(that._mainStr) &&
                equal(_mainAccMainStr, that._mainAccMainStr) &&
                equal(_appliedRules, that._appliedRules);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + _str + ',' + _mainStr + ',' + _id + ',' + _isDynamic + ',' + _mainAccMainStr + ',' + _appliedRules + ')';
    }
}
