package sword.langbook3.android;

import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableIntValueMap;

public final class CharacterCompositionsParser {
    final MutableIntValueMap<Character> _assignedChars = MutableIntValueHashMap.empty();
    final MutableIntValueMap<String> _assignedTokens = MutableIntValueHashMap.empty();
    final MutableIntKeyMap<CharacterComposition> _compositions = MutableIntKeyMap.empty();

    int _lastAssignedChar;
    int _line = 1;
    int _column;
    StringBuilder _tokenNameBuilder;

    int _currentId;
    int _currentFirst;
    int _currentSecond;
    int _currentCompositionType;
    boolean _lastWasCarriageReturn;
    CharacterCompositionsParserResult _result;

    private void storeAndClear() {
        _compositions.put(_currentId, new CharacterComposition(_currentFirst, _currentSecond, _currentCompositionType));
        _currentId = 0;
        _currentFirst = 0;
        _currentSecond = 0;
        _currentCompositionType = 0;
        _lastWasCarriageReturn = false;

        ++_line;
        _column = 0;
    }

    private void error(String message) throws CharacterCompositionsParseException {
        throw new CharacterCompositionsParseException(_line, _column, message);
    }

    public void next(char ch) throws CharacterCompositionsParseException {
        if (_result != null) {
            error("Parser already finalized");
        }

        ++_column;
        if (_lastWasCarriageReturn) {
            if (ch != '\n') {
                error("Expected line break just after carriage return");
            }

            storeAndClear();
            return;
        }

        if (_currentSecond != 0) {
            if (_currentCompositionType == 0) {
                if (ch == '\r' || ch == '\n') {
                    error("Composition type is missing");
                }

                if (ch < '1' || ch > '9') {
                    error("Composition type must start from a digit from 1 to 9");
                }
            }
            else if (ch < '0' && ch != '\r' && ch != '\n' || ch > '9') {
                error("Composition type is expected to be a number, so only values from 0 to 9 are allowed");
            }
        }

        if (ch == '{') {
            if (_tokenNameBuilder != null) {
                error("Unexpected '{' in the token name");
            }

            _tokenNameBuilder = new StringBuilder();
            return;
        }

        if (_tokenNameBuilder != null) {
            if (ch == '\r' || ch == '\n') {
                error("Unexpected line break while defining a token");
            }

            if (ch == '}') {
                final String token = _tokenNameBuilder.toString();
                _tokenNameBuilder = null;

                int assigned = _assignedTokens.get(token, 0);
                if (assigned == 0) {
                    assigned = ++_lastAssignedChar;
                    _assignedTokens.put(token, assigned);
                }
                else if (_currentId == 0) {
                    error("Composed character already defined");
                }

                if (_currentId == 0) {
                    _currentId = assigned;
                }
                else if (_currentFirst == 0) {
                    _currentFirst = assigned;
                }
                else {
                    _currentSecond = assigned;
                }
            }
            else {
                _tokenNameBuilder.append(ch);
            }
        }
        else {
            if (_currentSecond == 0) {
                int assigned = _assignedChars.get(ch, 0);
                if (assigned == 0) {
                    assigned = ++_lastAssignedChar;
                    _assignedChars.put(ch, assigned);
                }
                else if (_currentId == 0) {
                    error("Composed character already defined");
                }

                if (_currentId == 0) {
                    _currentId = assigned;
                }
                else if (_currentFirst == 0) {
                    _currentFirst = assigned;
                }
                else {
                    _currentSecond = assigned;
                }
            }
            else {
                if (ch == '\r') {
                    _lastWasCarriageReturn = true;
                }
                else if (ch == '\n') {
                    storeAndClear();
                }
                else {
                    _currentCompositionType = _currentCompositionType * 10 + (ch - '0');
                }
            }
        }
    }

    public void finalReached() throws CharacterCompositionsParseException {
        if (_tokenNameBuilder != null) {
            error("End of file reached while defining a token");
        }

        if (_currentId != 0) {
            if (_currentCompositionType == 0) {
                error("End of file reached while defining a composition");
            }
            else {
                storeAndClear();
            }
        }

        if (_result == null) {
            _result = new CharacterCompositionsParserResult(_compositions.toImmutable(), _assignedChars.toImmutable().invert());
            _compositions.clear();
            _assignedChars.clear();
            _assignedTokens.clear();
        }
    }

    public CharacterCompositionsParserResult getCharacterCompositions() {
        if (_result == null) {
            throw new UnsupportedOperationException("Parser not finalised");
        }

        return _result;
    }
}
