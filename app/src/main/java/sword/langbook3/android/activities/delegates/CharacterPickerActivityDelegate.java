package sword.langbook3.android.activities.delegates;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.collections.ImmutableList;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.CharacterDetailsActivity;
import sword.langbook3.android.CharacterPickerAdapter;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.IdentifiableResult;

public final class CharacterPickerActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> {
    public interface ArgKeys {
        String CHARACTER_STRING = BundleKeys.CHARACTER_STRING;
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        activity.setContentView(R.layout.character_picker_activity);

        final String characterString = activity.getIntent().getStringExtra(ArgKeys.CHARACTER_STRING);
        final ImmutableList<IdentifiableResult<CharacterId>> items = DbManager.getInstance().getManager().getCharacterPickerItems(characterString);
        final ListView listView = activity.findViewById(R.id.listView);
        listView.setAdapter(new CharacterPickerAdapter(items));
        listView.setOnItemClickListener((parent, view, position, id) ->
                CharacterDetailsActivity.open(activity, items.valueAt(position).id));
    }
}
