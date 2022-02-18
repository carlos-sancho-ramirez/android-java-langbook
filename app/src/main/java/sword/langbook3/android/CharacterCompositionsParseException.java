package sword.langbook3.android;

public final class CharacterCompositionsParseException extends Exception {

    public CharacterCompositionsParseException(int line, int column, String message) {
        super("at (" + line + ", " + column + "). " + message);
    }
}
