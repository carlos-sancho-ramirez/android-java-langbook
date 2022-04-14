package sword.langbook3.android.models;

public final class IdentifiableCharacterCompositionResult<Id> {
    public final Id id;
    public final String text;
    public CharacterCompositionDefinitionRegister register;

    public IdentifiableCharacterCompositionResult(Id id, String text, CharacterCompositionDefinitionRegister register) {
        this.id = id;
        this.text = text;
        this.register = register;
    }
}
