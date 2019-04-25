package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableList;
import sword.langbook3.android.models.SearchResult;

class SearchResultAdapter extends BaseAdapter {

    private final ImmutableList<SearchResult> _items;
    private final ImmutableIntKeyMap<String> _ruleTexts;
    private LayoutInflater _inflater;

    SearchResultAdapter(ImmutableList<SearchResult> items, ImmutableIntKeyMap<String> ruleTexts) {
        if (items == null) {
            throw new IllegalArgumentException();
        }

        _items = items;
        _ruleTexts = ruleTexts;
    }

    @Override
    public int getCount() {
        return _items.size();
    }

    @Override
    public SearchResult getItem(int i) {
        return _items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        final View view;
        if (convertView == null) {
            if (_inflater == null) {
                _inflater = LayoutInflater.from(viewGroup.getContext());
            }

            view = _inflater.inflate(R.layout.search_result, viewGroup, false);
        }
        else {
            view = convertView;
        }

        final SearchResult item = _items.get(i);
        final TextView tv = view.findViewById(R.id.searchResultTextView);
        final int textColor = item.isDynamic()? R.color.agentDynamicTextColor : R.color.agentStaticTextColor;
        tv.setTextColor(tv.getContext().getResources().getColor(textColor));

        final String str = item.getStr();
        final String mainStr = item.getMainStr();

        final String text = str.equals(mainStr)? str : mainStr + " (" + str + ')';
        tv.setText(text);

        final TextView auxTv = view.findViewById(R.id.searchResultAdditionalInfo);
        final ImmutableIntList rules = item.getAppliedRules();
        if (rules.isEmpty()) {
            auxTv.setVisibility(View.GONE);
        }
        else {
            auxTv.setText(item.getMainAccMainStr() + " + " + rules.map(_ruleTexts::get).reduce((a, b) -> a + " + " + b));
            auxTv.setVisibility(View.VISIBLE);
        }

        return view;
    }
}
