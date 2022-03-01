package sword.langbook3.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.models.CharacterDetailsModel;
import sword.langbook3.android.models.CharacterDetailsModel.ForeignComposition;

import static sword.langbook3.android.models.CharacterDetailsModel.Part.INVALID_CHARACTER;
import static sword.langbook3.android.models.CharacterDetailsModel.UNKNOWN_COMPOSITION_TYPE;

public final class CharacterDetailsAdapter extends BaseAdapter {

    private interface ViewTypes {
        int CHARACTER = 0;
        int COMPOSITION = 1;
        int SECTION_HEADER = 2;
        int NAVIGABLE = 3;
    }

    private CharacterDetailsModel<CharacterId, AcceptationId> _model;
    private LayoutInflater _inflater;

    private int _asFirstHeaderPosition;
    private int _asSecondHeaderPosition;
    private int _acceptationsWhereIncludedHeaderPosition;
    private int _count;

    public void setModel(CharacterDetailsModel<CharacterId, AcceptationId> model) {
        _model = model;

        if (model != null) {
            final int asFirstCount = model.asFirst.size();
            final int asSecondCount = model.asSecond.size();
            final int acceptationsWhereIncludedCount = model.acceptationsWhereIncluded.size();

            _asFirstHeaderPosition = (model.compositionType != UNKNOWN_COMPOSITION_TYPE)? 2 : 1;
            _asSecondHeaderPosition = (asFirstCount > 0) ? _asFirstHeaderPosition + asFirstCount + 1 : _asFirstHeaderPosition;
            _acceptationsWhereIncludedHeaderPosition = (asSecondCount > 0) ? _asSecondHeaderPosition + asSecondCount + 1 : _asSecondHeaderPosition;
            _count = (acceptationsWhereIncludedCount > 0) ? _acceptationsWhereIncludedHeaderPosition + acceptationsWhereIncludedCount + 1 : _acceptationsWhereIncludedHeaderPosition;
        }
        else {
            _count = 0;
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return _count;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0)? ViewTypes.CHARACTER :
                (position < _asFirstHeaderPosition)? ViewTypes.COMPOSITION :
                (position == _asFirstHeaderPosition || position == _asSecondHeaderPosition || position == _acceptationsWhereIncludedHeaderPosition)? ViewTypes.SECTION_HEADER : ViewTypes.NAVIGABLE;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return position >= 2 && position != _asFirstHeaderPosition && position != _asSecondHeaderPosition && position != _acceptationsWhereIncludedHeaderPosition;
    }

    @Override
    public Object getItem(int position) {
        final int viewType = getItemViewType(position);
        if (viewType == ViewTypes.CHARACTER || viewType == ViewTypes.COMPOSITION) {
            return _model;
        }
        else if (viewType == ViewTypes.SECTION_HEADER) {
            return null;
        }
        else {
            if (position > _acceptationsWhereIncludedHeaderPosition) {
                return _model.acceptationsWhereIncluded.keyAt(position - _acceptationsWhereIncludedHeaderPosition - 1);
            }
            else if (position > _asSecondHeaderPosition) {
                return _model.asSecond.valueAt(position - _asSecondHeaderPosition - 1).id;
            }
            else {
                return _model.asFirst.valueAt(position - _asFirstHeaderPosition - 1).id;
            }
        }
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
        final int viewType = getItemViewType(position);
        if (viewType == ViewTypes.CHARACTER) {
            if (convertView == null) {
                convertView = inflate(R.layout.character_details_activity_header, parent);
            }

            String mainText = representChar(_model.character);
            convertView.<TextView>findViewById(R.id.charBigScale).setText(mainText);
        }
        else if (viewType == ViewTypes.COMPOSITION) {
            if (convertView == null) {
                convertView = inflate(R.layout.character_details_activity_composition, parent);
            }

            final TextView firstTextView = convertView.findViewById(R.id.first);
            firstTextView.setText(representChar(_model.first.character));

            final CharacterId firstId = _model.first.id;
            firstTextView.setOnClickListener(v -> CharacterDetailsActivity.open(context, firstId));

            final TextView secondTextView = convertView.findViewById(R.id.second);
            secondTextView.setText(representChar(_model.second.character));

            final CharacterId secondId = _model.second.id;
            secondTextView.setOnClickListener(v -> CharacterDetailsActivity.open(context, secondId));

            convertView.<TextView>findViewById(R.id.compositionTypeInfo).setText(context.getString(R.string.characterCompositionType, Integer.toString(_model.compositionType)));
        }
        else if (viewType == ViewTypes.SECTION_HEADER) {
            if (convertView == null) {
                convertView = inflate(R.layout.acceptation_details_header, parent);
            }

            final int strRes = (position == _acceptationsWhereIncludedHeaderPosition)? R.string.characterCompositionAcceptationsHeader :
                    (position == _asSecondHeaderPosition)? R.string.characterCompositionAsSecondHeader :
                    R.string.characterCompositionAsFirstHeader;

            convertView.<TextView>findViewById(R.id.itemTextView).setText(strRes);
        }
        else {
            if (convertView == null) {
                convertView = inflate(R.layout.acceptation_details_item, parent);
            }

            final TextView textView = convertView.findViewById(R.id.itemTextView);
            if (position > _acceptationsWhereIncludedHeaderPosition) {
                final int mapIndex = position - _acceptationsWhereIncludedHeaderPosition - 1;
                final CharacterDetailsModel.AcceptationInfo info = _model.acceptationsWhereIncluded.valueAt(mapIndex);
                textView.setText(info.text);
                final int textColor = info.isDynamic ? R.color.agentDynamicTextColor : R.color.agentStaticTextColor;
                textView.setTextColor(context.getResources().getColor(textColor));
            }
            else {
                final ForeignComposition<CharacterId> composition = (position > _asSecondHeaderPosition)?
                        _model.asSecond.valueAt(position - _asSecondHeaderPosition - 1) :
                        _model.asFirst.valueAt(position - _asFirstHeaderPosition - 1);

                textView.setText(representChar(composition.character));
                textView.setTextColor(context.getResources().getColor(R.color.agentStaticTextColor));
            }
        }

        return convertView;
    }
}
