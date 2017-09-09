package sword.langbook3.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class SearchResultAdapter extends BaseAdapter {

    private final SearchResult[] _items;
    private LayoutInflater _inflater;

    SearchResultAdapter(SearchResult[] items) {
        if (items == null) {
            throw new IllegalArgumentException();
        }

        _items = items;
    }

    @Override
    public int getCount() {
        return _items.length;
    }

    @Override
    public SearchResult getItem(int i) {
        return _items[i];
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

        final TextView tv = view.findViewById(R.id.searchResultTextView);
        tv.setText(_items[i].getStr());

        return view;
    }
}
