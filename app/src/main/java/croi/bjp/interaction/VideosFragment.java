package croi.bjp.interaction;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.support.v4.app.Fragment;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideosFragment extends Fragment
{
    private Uri videoUri;
    LinearLayout videoLayout;
    private static VideoView videoView;
    String[] listviewTitle = new String[] {
    };
    int[] listviewImage = new int[] {
    };
    String[] listviewVideo = new String[] {
    };
    String[] listviewShortDescription = new String[] {
    };

    public VideosFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        videoLayout = (LinearLayout) inflater.inflate(R.layout.fragment_videos, container, false);
        videoView = videoLayout.findViewById(R.id.videoView);
        updateList();
        return videoLayout;
    }

    private void updateList()
    {
        ListView androidListView = (ListView) videoLayout.findViewById(R.id.list_view);
        List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();
        SimpleAdapter simpleAdapter = new SimpleAdapter(getActivity().getBaseContext(), aList, R.layout.listview, null, null);

        for (int i = 0; i < listviewTitle.length; i++) {
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("listview_title", listviewTitle[i]);
            hm.put("listview_description", listviewShortDescription[i]);
            hm.put("listview_image", Integer.toString(listviewImage[i]));
            aList.add(hm);
        }

        String[] from = {"listview_image", "listview_title", "listview_description"};
        int[] to = {R.id.listview_image, R.id.listview_item_title, R.id.listview_item_short_description};

        simpleAdapter = new SimpleAdapter(getActivity().getBaseContext(), aList, R.layout.listview, from, to);
        androidListView.setAdapter(simpleAdapter);
        androidListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                videoUri = Uri.parse(HelloAR.basePath + "videos/" + listviewVideo[position]);
                videoView.setVideoURI(videoUri);
                videoView.setKeepScreenOn(true);
                videoView.setZOrderOnTop(true);
                videoView.setVisibility(View.VISIBLE);
                videoView.start();
            }
        });
    }

    public static void stopVideo()
    {
        if(videoView.isPlaying()){
            videoView.stopPlayback();
            videoView.setVisibility(View.GONE);
            videoView.setZOrderOnTop(false);
            videoView.stopPlayback();
        }
    }
}
