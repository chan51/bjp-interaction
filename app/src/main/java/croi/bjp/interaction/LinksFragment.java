package croi.bjp.interaction;

import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.support.v4.app.Fragment;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LinksFragment extends Fragment
{
    LinearLayout linksLayout;
    String[] listviewTitle = new String[] {
            "Official Website", "Official Facebook Account", "Official Twitter Account", "Official Youtube Channel",
    };
    Integer[] listviewIcon = new Integer[] {
            R.drawable.ic_openweb, R.drawable.ic_openweb, R.drawable.ic_openweb, R.drawable.ic_openweb,
    };
    String[] listviewLinks = new String[] {
            "http://bjp.org/", "https://www.facebook.com/BJP4India/", "https://twitter.com/bjp4india", "https://www.youtube.com/channel/UCrwE8kVqtIUVUzKui2WVpuQ",
    };

    public LinksFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        linksLayout = (LinearLayout) inflater.inflate(R.layout.fragment_links, container, false);
        updateList();
        return linksLayout;
    }

    private void updateList()
    {
        ListView androidListView = (ListView) linksLayout.findViewById(R.id.inc_view);
        List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();
        SimpleAdapter simpleAdapter = new SimpleAdapter(getActivity().getBaseContext(), aList, R.layout.listview, null, null);

        for (int i = 0; i < listviewTitle.length; i++) {
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("listview_title", listviewTitle[i]);
            hm.put("listview_image", Integer.toString(listviewIcon[i]));
            aList.add(hm);
        }

        String[] from = {"listview_image", "listview_title"};
        int[] to = {R.id.listview_image, R.id.listview_item_title};

        simpleAdapter = new SimpleAdapter(getActivity().getBaseContext(), aList, R.layout.listview, from, to);
        androidListView.setAdapter(simpleAdapter);
        androidListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mCustomURL = listviewLinks[position];
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(LinksFragment.this.getContext(), Uri.parse(mCustomURL));
            }
        });

        // TextView listview_item_short_description = (TextView) linksLayout.findViewById(R.id.listview_item_short_description);
        // listview_item_short_description.setVisibility(View.GONE);
    }
}
