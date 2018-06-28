package sword.langbook3.android;

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

    SearchResult(String str, String mainStr, int type, int id, int auxId) {
        if (type != Types.ACCEPTATION && type != Types.AGENT || str == null || mainStr == null) {
            throw new IllegalArgumentException();
        }

        _str = str;
        _mainStr = mainStr;
        _type = type;
        _id = id;
        _auxId = auxId;
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

    @Override
    public int hashCode() {
        return (((_type * 31 + _id) * 31 + _auxId) * 31 + _str.hashCode()) * 31 + _mainStr.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof SearchResult)) {
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
