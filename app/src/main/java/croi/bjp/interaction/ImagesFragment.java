package croi.bjp.interaction;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.InputStream;

public class ImagesFragment extends Fragment
{
    LinearLayout imageLayout;
    String[] images = new String[] {
    };
    public ImagesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        imageLayout = (LinearLayout) inflater.inflate(R.layout.fragment_images, container, false);
        showImages();
        return imageLayout;
    }

    private void showImages()
    {
        LinearLayout imageListLayout = (LinearLayout) imageLayout.findViewById(R.id.imageListLayout);
        for(int i = 0; i < images.length; i++) {
            ImageView image = new ImageView(ImagesFragment.this.getContext());
            image.setLayoutParams(new android.view.ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600));
            new DownloadImageFromInternet(image).execute(HelloAR.basePath + images[i]);
            image.setPadding(0, 10, 0, 0);
            imageListLayout.addView(image);
        }
    }

    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap>
    {
        ImageView imageView;

        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];
            Bitmap bimage = null;
            try {
                InputStream in = new java.net.URL(imageURL).openStream();
                bimage = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bimage;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }
}
