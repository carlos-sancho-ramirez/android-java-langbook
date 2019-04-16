package sword.langbook3.android;

import sword.collections.ImmutableList;

class SearchResult {

    interface Types {
        int ACCEPTATION = 0; //auxId is dynamicAcceptation
        int AGENT = 1; //auxId not used
    }

    private final String _str;
    private final String _mainStr;
    private final int _type;
    private final int _id;
    private final int _auxId;
    private final String _mainAccMainStr;
    private final ImmutableList<String> _appliedRules;

    SearchResult(String str, String mainStr, int type, int id, int auxId, String mainAccMainStr, ImmutableList<String> appliedRules) {
        if (type != Types.ACCEPTATION && type != Types.AGENT || str == null || mainStr == null) {
            throw new IllegalArgumentException();
        }

        _str = str;
        _mainStr = mainStr;
        _type = type;
        _id = id;
        _auxId = auxId;
        _mainAccMainStr = mainAccMainStr;
        _appliedRules = appliedRules;
    }

    SearchResult(String str, String mainStr, int type, int id, int auxId) {
        this(str, mainStr, type, id, auxId, null, ImmutableList.empty());
    }

    String getStr() {
        return _str;
    }

    String getMainStr() {
        return _mainStr;
    }

    int getType() {
        return _type;
    }

    int getId() {
        return _id;
    }

    int getAuxiliarId() {
        return _auxId;
    }

    boolean isDynamic() {
        return _type == Types.ACCEPTATION && _id != _auxId;
    }

    String getMainAccMainStr() {
        return _mainAccMainStr;
    }

    ImmutableList<String> getAppliedRules() {
        return _appliedRules;
    }

    public SearchResult withMainAccMainStr(String str) {
        return new SearchResult(_str, _mainStr, _type, _id, _auxId, str, _appliedRules);
    }

    public SearchResult withRules(ImmutableList<String> rules) {
        return new SearchResult(_str, _mainStr, _type, _id, _auxId, _mainAccMainStr, rules);
    }

    @Override
    public int hashCode() {
        return (((_type * 31 + _id) * 31 + _auxId) * 31 + _str.hashCode()) * 31 + _mainStr.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SearchResult)) {
            return false;
        }

        final SearchResult that = (SearchResult) other;
        return _type == that._type &&
                _id == that._id &&
                _auxId == that._auxId &&
                _str.equals(that._str) &&
                _mainStr.equals(that._mainStr);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + _str + ',' + _mainStr + ',' + _type + ',' + _id + ',' + _auxId + ')';
    }
}
