package croi.bjp.interaction;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentManager;

public class MyAdapter extends FragmentPagerAdapter
{
    int totalTabs;

    public MyAdapter(Context context, FragmentManager fm, int totalTabs)
    {
        super(fm);
        this.totalTabs = totalTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                VideosFragment videosFragment = new VideosFragment();
                return videosFragment;
            case 1:
                ImagesFragment imagesFragment = new ImagesFragment();
                return imagesFragment;
            case 2:
                LinksFragment linksFragment = new LinksFragment();
                return linksFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return totalTabs;
    }
}
