package sword.langbook3.android.models;

public final class CharacterPickerItem<CharacterId> {
    public final CharacterId id;
    public final String text;
    public final boolean isComposed;
    public final boolean isCompositionPart;

    public CharacterPickerItem(CharacterId id, String text, boolean isComposed, boolean isCompositionPart) {
        this.id = id;
        this.text = text;
        this.isComposed = isComposed;
        this.isCompositionPart = isCompositionPart;
    }
}
