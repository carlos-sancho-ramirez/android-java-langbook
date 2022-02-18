package sword.langbook3.android.db;

import sword.langbook3.android.models.CharacterCompositionDetailsModel;

public interface CharacterCompositionsChecker<CharacterId> {
    CharacterCompositionDetailsModel<CharacterId> getCharacterCompositionDetails(CharacterId characterId);
}
