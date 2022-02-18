package sword.langbook3.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.models.CharacterCompositionDetailsModel;

import static sword.langbook3.android.models.CharacterCompositionDetailsModel.Part.INVALID_CHARACTER;

public final class CharacterCompositionDetailsAdapter extends BaseAdapter {

    private CharacterCompositionDetailsModel<CharacterId, AcceptationId> _model;
    private LayoutInflater _inflater;

    public void setModel(CharacterCompositionDetailsModel<CharacterId, AcceptationId> model) {
        _model = model;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return (_model == null)? 0 :
                _model.acceptationsWhereIncluded.isEmpty()? 1 :
                _model.acceptationsWhereIncluded.size() + 2;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return Math.min(position, 2);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return position >= 2;
    }

    @Override
    public Object getItem(int position) {
        return _model;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private View inflate(int layoutId, ViewGroup parent) {
        if (_inflater == null) {
            _inflater = LayoutInflater.from(parent.getContext());
        }

        return _inflater.inflate(layoutId, parent, false);
    }

    private String representChar(char ch) {
        return (ch == INVALID_CHARACTER)? "?" : "" + ch;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        if (position == 0) {
            if (convertView == null) {
                convertView = inflate(R.layout.character_details_activity_header, parent);
            }

            String mainText = representChar(_model.character);
            convertView.<TextView>findViewById(R.id.charBigScale).setText(mainText);

            final TextView firstTextView = convertView.findViewById(R.id.first);
            firstTextView.setText(representChar(_model.first.character));

            if (_model.first.isComposition) {
                final CharacterId firstId = _model.first.id;
                firstTextView.setOnClickListener(v -> CharacterCompositionDetailsActivity.open(context, firstId));
            }

            final TextView secondTextView = convertView.findViewById(R.id.second);
            secondTextView.setText(representChar(_model.second.character));

            if (_model.second.isComposition) {
                final CharacterId secondId = _model.second.id;
                secondTextView.setOnClickListener(v -> CharacterCompositionDetailsActivity.open(context, secondId));
            }

            convertView.<TextView>findViewById(R.id.compositionTypeInfo).setText(context.getString(R.string.characterCompositionType, Integer.toString(_model.compositionType)));
        }
        else if (position == 1) {
            if (convertView == null) {
                convertView = inflate(R.layout.acceptation_details_header, parent);
            }

            convertView.<TextView>findViewById(R.id.itemTextView).setText(R.string.characterCompositionAcceptationsHeader);
        }
        else {
            if (convertView == null) {
                convertView = inflate(R.layout.acceptation_details_item, parent);
            }

            final int mapIndex = position - 2;
            final CharacterCompositionDetailsModel.AcceptationInfo info = _model.acceptationsWhereIncluded.valueAt(mapIndex);
            final TextView textView = convertView.findViewById(R.id.itemTextView);
            textView.setText(info.text);
            final int textColor = info.isDynamic? R.color.agentDynamicTextColor : R.color.agentStaticTextColor;
            textView.setTextColor(context.getResources().getColor(textColor));
        }

        return convertView;
    }
}
