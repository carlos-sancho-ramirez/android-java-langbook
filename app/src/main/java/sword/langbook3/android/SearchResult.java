package sword.langbook3.android;

class SearchResult {

    private final String _str;
    private final String _mainStr;
    private final int _acceptation;
    private final int _dynamicAcceptation;

    SearchResult(String str, String mainStr, int acceptation, int dynamicAcceptation) {
        _str = str;
        _mainStr = mainStr;
        _acceptation = acceptation;
        _dynamicAcceptation = dynamicAcceptation;
    }

    String getStr() {
        return _str;
    }

    String getMainStr() {
        return _mainStr;
    }

    int getAcceptation() {
        return _acceptation;
    }

    int getDynamicAcceptation() {
        return _dynamicAcceptation;
    }
}
