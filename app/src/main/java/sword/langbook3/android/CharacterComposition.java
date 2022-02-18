package sword.langbook3.android;

public final class CharacterComposition {
    public final int firstCharacter;
    public final int secondCharacter;
    public final int compositionType;

    CharacterComposition(int firstCharacter, int secondCharacter, int compositionType) {
        this.firstCharacter = firstCharacter;
        this.secondCharacter = secondCharacter;
        this.compositionType = compositionType;
    }
}
