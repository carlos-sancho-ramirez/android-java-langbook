package sword.langbook3.android;

import static sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate.bindCompositionType;
import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.models.CharacterCompositionPart;
import sword.langbook3.android.models.CharacterCompositionRepresentation;
import sword.langbook3.android.models.CharacterDetailsModel;
import sword.langbook3.android.util.ContextExtensionsAdapter;

public final class CharacterDetailsAdapter extends BaseAdapter {

    private interface ViewTypes {
        int CHARACTER = 0;
        int COMPOSITION = 1;
        int UNICODE_NUMBER = 2;
        int SECTION_HEADER = 3;
        int COMPOSITION_TYPE = 4;
        int NAVIGABLE = 5;
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
            final boolean isUnicodeNumberPresent = model.representation.character != INVALID_CHARACTER;
            final int asFirstCount = model.asFirst.size();
            final int asSecondCount = model.asSecond.size();
            final int acceptationsWhereIncludedCount = model.acceptationsWhereIncluded.size();

            _asFirstHeaderPosition = (model.compositionType != null)? 4 : 1;
            if (isUnicodeNumberPresent) {
                _asFirstHeaderPosition++;
            }

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
        return 6;
    }

    @Override
    public int getItemViewType(int position) {
        final boolean isUnicodeNumberPresent = _model.representation.character != INVALID_CHARACTER;
        return (position == 0)? ViewTypes.CHARACTER :
                (position == 1 && _model.compositionType != null)? ViewTypes.COMPOSITION :
                (isUnicodeNumberPresent && (position == 1 || position == 2 && _model.compositionType != null))? ViewTypes.UNICODE_NUMBER :
                (_model.compositionType != null && (position == 2 || position == 3 && isUnicodeNumberPresent))? ViewTypes.SECTION_HEADER :
                (_model.compositionType != null && (position == 3 || position == 4 && isUnicodeNumberPresent))? ViewTypes.COMPOSITION_TYPE :
                (position == _asFirstHeaderPosition || position == _asSecondHeaderPosition || position == _acceptationsWhereIncludedHeaderPosition)? ViewTypes.SECTION_HEADER : ViewTypes.NAVIGABLE;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        final boolean isUnicodeNumberPresent = _model.representation.character != INVALID_CHARACTER;
        return (!isUnicodeNumberPresent && (position > 2 || position == 2 && _model.compositionType == null) ||
                (isUnicodeNumberPresent && (position > 3 || position == 3 && _model.compositionType == null))) &&
                position != _asFirstHeaderPosition && position != _asSecondHeaderPosition && position != _acceptationsWhereIncludedHeaderPosition;
    }

    @Override
    public Object getItem(int position) {
        final int viewType = getItemViewType(position);
        if (viewType == ViewTypes.CHARACTER || viewType == ViewTypes.COMPOSITION || viewType == ViewTypes.UNICODE_NUMBER) {
            return _model;
        }
        else if (viewType == ViewTypes.SECTION_HEADER) {
            return null;
        }
        else if (viewType == ViewTypes.COMPOSITION_TYPE) {
            return _model.compositionType.id;
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

    static String representChar(char ch, String token) {
        return (ch != INVALID_CHARACTER)? "" + ch :
                (token != null)? "{" + token + '}' : "?";
    }

    public static String representChar(CharacterCompositionRepresentation representation) {
        return representChar(representation.character, representation.token);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        final ContextExtensionsAdapter contextAdapter = new ContextExtensionsAdapter(context);
        final int viewType = getItemViewType(position);
        if (viewType == ViewTypes.CHARACTER) {
            if (convertView == null) {
                convertView = inflate(R.layout.character_details_activity_header, parent);
            }

            String mainText = representChar(_model.representation);

            final TextView textView = convertView.findViewById(R.id.charBigScale);
            textView.setText(mainText);

            final int textSizeRes = (_model.representation.character != INVALID_CHARACTER)?
                    R.dimen.characterDetailsCharacterTextSize :
                    R.dimen.characterDetailsTokenTextSize;
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(textSizeRes));
        }
        else if (viewType == ViewTypes.COMPOSITION) {
            if (convertView == null) {
                convertView = inflate(R.layout.character_details_activity_composition, parent);
            }

            final TextView firstTextView = convertView.findViewById(R.id.first);
            firstTextView.setText(representChar(_model.first.representation));

            final int firstTextSizeRes = (_model.first.representation.character != INVALID_CHARACTER)?
                    R.dimen.characterDetailsCompositionCharacterTextSize :
                    R.dimen.characterDetailsCompositionTokenTextSize;
            firstTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(firstTextSizeRes));

            final CharacterId firstId = _model.first.id;
            firstTextView.setOnClickListener(v -> CharacterDetailsActivity.open(contextAdapter, firstId));

            final TextView secondTextView = convertView.findViewById(R.id.second);
            secondTextView.setText(representChar(_model.second.representation));

            final int secondTextSizeRes = (_model.second.representation.character != INVALID_CHARACTER)?
                    R.dimen.characterDetailsCompositionCharacterTextSize :
                    R.dimen.characterDetailsCompositionTokenTextSize;
            secondTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(secondTextSizeRes));

            final CharacterId secondId = _model.second.id;
            secondTextView.setOnClickListener(v -> CharacterDetailsActivity.open(contextAdapter, secondId));
        }
        else if (viewType == ViewTypes.COMPOSITION_TYPE) {
            if (convertView == null) {
                convertView = inflate(R.layout.character_composition_definition_entry, parent);
            }

            bindCompositionType(convertView, _model.compositionType);
        }
        else if (viewType == ViewTypes.UNICODE_NUMBER) {
            if (convertView == null) {
                convertView = inflate(R.layout.acceptation_details_header, parent);
            }

            final int unicodeNumber = _model.representation.character;
            convertView.<TextView>findViewById(R.id.itemTextView).setText(context.getString(R.string.characterDetailsUnicodeNumberEntry, Integer.toString(unicodeNumber) + " (0x" + Integer.toHexString(unicodeNumber) + ")"));
        }
        else if (viewType == ViewTypes.SECTION_HEADER) {
            if (convertView == null) {
                convertView = inflate(R.layout.acceptation_details_header, parent);
            }

            final int strRes = (position == 2 && _model.compositionType != null)? R.string.characterCompositionTypeHeader :
                    (position == _acceptationsWhereIncludedHeaderPosition)? R.string.characterCompositionAcceptationsHeader :
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
                final CharacterCompositionPart<CharacterId> composition = (position > _asSecondHeaderPosition)?
                        _model.asSecond.valueAt(position - _asSecondHeaderPosition - 1) :
                        _model.asFirst.valueAt(position - _asFirstHeaderPosition - 1);

                textView.setText(representChar(composition.representation));
                textView.setTextColor(context.getResources().getColor(R.color.agentStaticTextColor));
            }
        }

        return convertView;
    }
}
