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
}
