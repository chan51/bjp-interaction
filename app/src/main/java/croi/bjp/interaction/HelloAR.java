package croi.bjp.interaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import cn.easyar.CameraCalibration;
import cn.easyar.CameraDevice;
import cn.easyar.CameraDeviceFocusMode;
import cn.easyar.CameraDeviceType;
import cn.easyar.CameraFrameStreamer;
import cn.easyar.Frame;
import cn.easyar.FunctorOfVoidFromPointerOfTargetAndBool;
import cn.easyar.ImageTarget;
import cn.easyar.ImageTracker;
import cn.easyar.ImageTrackerMode;
import cn.easyar.Renderer;
import cn.easyar.StorageType;
import cn.easyar.Target;
import cn.easyar.TargetInstance;
import cn.easyar.TargetStatus;
import cn.easyar.Vec2I;
import cn.easyar.Vec4I;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HelloAR extends AppCompatActivity
{
    private Context ARContext;
    private CameraDevice camera;
    private CameraFrameStreamer streamer;
    private ArrayList<ImageTracker> trackers;
    private Renderer videobg_renderer;
    private ArrayList<VideoRenderer> video_renderers;
    private VideoRenderer current_video_renderer;
    private int tracked_target = 0;
    private int active_target = 0;
    private ARVideo video = null;
    private boolean viewport_changed = false;
    private Vec2I view_size = new Vec2I(0, 0);
    private int rotation = 0;
    private Vec4I viewport = new Vec4I(0, 0, 1280, 720);

    private GLView glView;
    private String target_name;

    List<String> indexes = null;
    HashMap<String, String> videosList = new HashMap<>();
    public static String basePath = "https://www.croimedia.com/sites/bjp-app/";

    TabLayout tabLayout;
    ViewPager viewPager;
    ConstraintLayout layout;
    Boolean isAnimated = false;


    public HelloAR() { }

    public HelloAR(Context context)
    {
        ARContext = context;
        trackers = new ArrayList<ImageTracker>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_view);

        glView = new GLView(this);
        ((ViewGroup) findViewById(R.id.preview)).addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bottomSheet();
        tabViewLayout();
    }

    private void bottomSheet()
    {
        final Button listButton = findViewById(R.id.listButton);
        layout = (ConstraintLayout) findViewById(R.id.tabViewLayout);

        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isAnimated) {
                    isAnimated = true;
                    layout.setVisibility(View.VISIBLE);
                    layout.startAnimation(AnimationUtils.loadAnimation(HelloAR.this, R.anim.translate_animation_top));
                    listButton.setText("Back");
                } else {
                    isAnimated = false;
                    VideosFragment.stopVideo();
                    layout.setVisibility(View.GONE);
                    layout.startAnimation(AnimationUtils.loadAnimation(HelloAR.this, R.anim.translate_animation_bottom));
                    listButton.setText("Related Content");
                }
            }
        });
        listButton.bringToFront();
    }

    private void tabViewLayout()
    {
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        tabLayout.addTab(tabLayout.newTab().setText("Videos"));
        tabLayout.addTab(tabLayout.newTab().setText("Images"));
        tabLayout.addTab(tabLayout.newTab().setText("Links"));

        final MyAdapter adapter = new MyAdapter(this,getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                VideosFragment.stopVideo();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (glView != null) { glView.onResume(); }
    }

    @Override
    protected void onPause()
    {
        if (glView != null) { glView.onPause(); }
        super.onPause();
    }

    @Override
    public void onStop() { super.onStop(); }

    public boolean initialize()
    {
        camera = new CameraDevice();
        streamer = new CameraFrameStreamer();
        streamer.attachCamera(camera);

        boolean status = true;
        camera.setSize(new Vec2I(1280, 720));
        status &= camera.open(CameraDeviceType.Back);

        if (!status) { return status; }
        ImageTracker tracker = new ImageTracker();
        tracker.createWithMode(ImageTrackerMode.PreferPerformance);
        tracker.setSimultaneousNum(2);
        tracker.attachStreamer(streamer);
        loadAllFromJsonFile(tracker, "targets.json");
        trackers.add(tracker);

        return status;
    }

    public void setCameraZoom(Float zoomScale) {
        camera.setZoomScale(zoomScale);
    }

    private void loadAllFromJsonFile(ImageTracker tracker, String path)
    {
        for (ImageTarget target : ImageTarget.setupAll(path, StorageType.Assets)) {
            tracker.loadTarget(target, new FunctorOfVoidFromPointerOfTargetAndBool() {
                @Override
                public void invoke(Target target, boolean status) {
                    try {
                        Log.i("HelloAR", String.format("load target (%b): %s (%d)", status, target.name(), target.runtimeID()));
                    } catch (Throwable ex) {
                    }
                }
            });
        }
    }

    public void dispose()
    {
        if (video != null) {
            video.dispose();
            video = null;
        }
        tracked_target = 0;
        active_target = 0;

        for (ImageTracker tracker : trackers) {
            tracker.dispose();
        }
        trackers.clear();
        video_renderers.clear();
        current_video_renderer = null;
        if (videobg_renderer != null) {
            videobg_renderer.dispose();
            videobg_renderer = null;
        }
        if (streamer != null) {
            streamer.dispose();
            streamer = null;
        }
        if (camera != null) {
            camera.dispose();
            camera = null;
        }
    }

    public boolean start()
    {
        boolean status = true;
        status &= (camera != null) && camera.start();
        status &= (streamer != null) && streamer.start();
        camera.setFocusMode(CameraDeviceFocusMode.Continousauto);
        for (ImageTracker tracker : trackers) {
            status &= tracker.start();
        }
        return status;
    }

    public boolean stop()
    {
        boolean status = true;
        for (ImageTracker tracker : trackers) {
            status &= tracker.stop();
        }
        status &= (streamer != null) && streamer.stop();
        status &= (camera != null) && camera.stop();
        return status;
    }

    public void initGL()
    {
        imageTargetList();
        if (active_target != 0) {
            video.onLost();
            video.dispose();
            video = null;
            tracked_target = 0;
            active_target = 0;
        }
        if (videobg_renderer != null) {
            videobg_renderer.dispose();
        }
        videobg_renderer = new Renderer();
        video_renderers = new ArrayList<VideoRenderer>();
        for (int k = 0; k < videosList.size(); k += 1) {
            VideoRenderer video_renderer = new VideoRenderer();
            video_renderer.init();
            video_renderers.add(video_renderer);
        }
        current_video_renderer = null;
    }

    public void resizeGL(int width, int height)
    {
        view_size = new Vec2I(width, height);
        viewport_changed = true;
    }

    private void updateViewport()
    {
        CameraCalibration calib = camera != null ? camera.cameraCalibration() : null;
        int rotation = calib != null ? calib.rotation() : 0;
        if (rotation != this.rotation) {
            this.rotation = rotation;
            viewport_changed = true;
        }
        if (viewport_changed) {
            Vec2I size = new Vec2I(1, 1);
            if ((camera != null) && camera.isOpened()) {
                size = camera.size();
            }
            if (rotation == 90 || rotation == 270) {
                size = new Vec2I(size.data[1], size.data[0]);
            }
            float scaleRatio = Math.max((float) view_size.data[0] / (float) size.data[0], (float) view_size.data[1] / (float) size.data[1]);
            Vec2I viewport_size = new Vec2I(Math.round(size.data[0] * scaleRatio), Math.round(size.data[1] * scaleRatio));
            viewport = new Vec4I((view_size.data[0] - viewport_size.data[0]) / 2, (view_size.data[1] - viewport_size.data[1]) / 2, viewport_size.data[0], viewport_size.data[1]);

            if ((camera != null) && camera.isOpened())
                viewport_changed = false;
        }
    }

    public void render()
    {
        GLES20.glClearColor(1.f, 1.f, 1.f, 1.f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (videobg_renderer != null) {
            Vec4I default_viewport = new Vec4I(0, 0, view_size.data[0], view_size.data[1]);
            GLES20.glViewport(default_viewport.data[0], default_viewport.data[1], default_viewport.data[2], default_viewport.data[3]);
            if (videobg_renderer.renderErrorMessage(default_viewport)) {
                return;
            }
        }

        if (streamer == null) { return; }
        Frame frame = streamer.peek();
        try {
            updateViewport();
            GLES20.glViewport(viewport.data[0], viewport.data[1], viewport.data[2], viewport.data[3]);

            if (videobg_renderer != null) {
                videobg_renderer.render(frame, viewport);
            }

            ArrayList<TargetInstance> targetInstances = frame.targetInstances();
            if (targetInstances.size() > 0) {
                TargetInstance targetInstance = targetInstances.get(0);
                Target target = targetInstance.target();
                int status = targetInstance.status();
                if (status == TargetStatus.Tracked) {
                    int id = target.runtimeID();
                    if (active_target != 0 && active_target != id) {
                        video.onLost();
                        video.dispose();
                        video = null;
                        tracked_target = 0;
                        active_target = 0;
                        target_name = null;
                    }
                    if (tracked_target == 0) {
                        if (video == null && video_renderers.size() > 0) {
                            video = new ARVideo();
                            target_name = target.name();
                            renderVideo();
                        }
                        if (video != null) {
                            video.onFound();
                            tracked_target = id;
                            active_target = id;
                            target_name = target.name();
                        }
                    }
                    ImageTarget imagetarget = target instanceof ImageTarget ? (ImageTarget)(target) : null;
                    if (imagetarget != null) {
                        if (current_video_renderer != null) {
                            video.update();
                            if (video.isRenderTextureAvailable()) {
                                current_video_renderer.render(camera.projectionGL(0.2f, 500.f), targetInstance.poseGL(), imagetarget.size());
                            }
                        }
                    }
                }
            } else {
                if (tracked_target != 0) {
                    video.onLost();
                    tracked_target = 0;
                    target_name = null;
                }
            }
        }
        finally {
            frame.dispose();
        }
    }

    private void imageTargetList()
    {
        videosList = new HashMap<>();
        videosList.put("iskcon", "iskcon.mp4");
        videosList.put("shivaji", "shivaji.mp4");
        videosList.put("elephanta", "elephanta.mp4");
        videosList.put("all_in_one_1", "all_scheme.mp4");
        videosList.put("modi_main_bhi_chowkidar", "modi_main_bhi_chowkidar.mp4");

        videosList.put("ppc_record", "ppc_record.mp4");
        videosList.put("ppc_namaskar", "ppc_namaskar.mp4");
        videosList.put("ppc_bjp_work", "ppc_bjp_work.mp4");
        videosList.put("ppc_modi_work", "ppc_modi_work.mp4");
        videosList.put("ppc_about_rail", "ppc_about_rail.mp4");
        videosList.put("ppc_about_pali", "ppc_about_pali.mp4");
        videosList.put("ppc_pali_vikas", "ppc_pali_vikas.mp4");
        videosList.put("ppc_with_public", "ppc_with_public.mp4");
        videosList.put("ppc_modi_kushal", "ppc_modi_kushal.mp4");
        videosList.put("ppc_about_family", "ppc_about_family.mp4");
        videosList.put("ppc_swachh_bharat", "ppc_swachh_bharat.mp4");
        videosList.put("ppc_health_center", "ppc_health_center.mp4");
        videosList.put("ppc_digital_kendra", "ppc_digital_kendra.mp4");
        videosList.put("ppc_electricity_01", "ppc_electricity_01.mp4");
        videosList.put("ppc_electricity_02", "ppc_electricity_02.mp4");
        videosList.put("ppc_gas_connection", "ppc_gas_connection.mp4");
        videosList.put("ppc_with_amit_shah", "ppc_with_amit_shah.mp4");
        videosList.put("ppc_with_modi_ji_01", "ppc_with_modi_ji_01.mp4");
        videosList.put("ppc_with_modi_ji_02", "ppc_with_modi_ji_02.mp4");
        videosList.put("ppc_with_vasundhara", "ppc_with_vasundhara.mp4");
        videosList.put("ppc_parliament_award", "ppc_parliament_award.mp4");
        videosList.put("ppc_done_for_rajmarg", "ppc_done_for_rajmarg.mp4");
        videosList.put("ppc_in_conference_01", "ppc_in_conference_01.mp4");
        videosList.put("ppc_in_conference_02", "ppc_in_conference_02.mp4");
        videosList.put("ppc_in_conference_03", "ppc_in_conference_03.mp4");
        videosList.put("ppc_with_modi_statue", "ppc_with_modi_statue.mp4");
        videosList.put("ppc_about_clean_water", "ppc_about_clean_water.mp4");
        videosList.put("ppc_about_agriculture", "ppc_about_agriculture.mp4");
        videosList.put("ppc_together_with_pali", "ppc_together_with_pali.mp4");

        videosList.put("new_poster_03", "pp_choudhary_laws_bill2019.mp4");
        videosList.put("new_poster_04", "mos_p.p_chaudhary_on_discussion.mp4");
        videosList.put("new_poster_02", "pp_chaudhary_speech_at_parliament.mp4");
        videosList.put("new_poster_01", "shri_pp_choudhary_at_utkarsh_jodhpur.mp4");
        videosList.put("gajendra_singh_agriculture_01", "gajendra_singh_agriculture_1.mp4");

        videosList.put("poster_04", "w.mp4");
        videosList.put("poster_02", "samagra_shiksha_short_film.mp4");
        videosList.put("poster_01", "fasal_bima_yojna_has_benifted.mp4");
        videosList.put("poster_03", "pradhan_mantri_ujjwala_yojana.mp4");

        videosList.put("pc_mohan_speech_01", "pc_mohan_speech_01.mp4");
        videosList.put("rakesh_chippa_card_02", "ppc_with_amit_shah.mp4");
        videosList.put("rakesh_chippa_card_01", "ppc_with_modi_ji_01.mp4");
        videosList.put("pc_mohan_swatchh_bharath", "pc_mohan_swatchh_bharath.mp4");
        videosList.put("kerala_bjp_parivarthana_yatra", "kerala_bjp_parivarthana_yatra.mp4");

        videosList.put("girish_bapat_poster_01", "girish_bapat_poster_01.mp4");

        indexes = new ArrayList<String>(videosList.keySet());
    }

    private void renderVideo()
    {
        String videoName = null;
        Integer videoIndex = null;

        if (videosList.containsKey(target_name)) {
            videoName = videosList.get(target_name);
            videoIndex = indexes.indexOf(target_name);
        }
        if(videoName == null || videoIndex == null) {
            return;
        }

        if (video_renderers.get(videoIndex).texId() != 0) {
            video.openStreamingVideo(basePath + "videos/" + videoName, video_renderers.get(videoIndex).texId());
            current_video_renderer = video_renderers.get(videoIndex);
            trackRecord(target_name);
        } else {
        }
    }

    public void trackRecord(String poster_name)
    {
        try {
            runOnUiThread(new Runnable() {
                public void run() {
                    final Toast toast = Toast.makeText(ARContext, "Experience top up reality ", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("url", basePath + "app.php");
            jsonObject.put("poster_name", poster_name);
            postRequest(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void postRequest(JSONObject jsonObject)
    {
        try {
            JSONObject json = new JSONObject();
            json.put("location", MainActivity.getLocation());
            json.put("package_name", "croi.bjp.interaction");
            json.put("poster_name", jsonObject.getString("poster_name"));
            json.put("device", Build.MANUFACTURER + " " + Build.MODEL + ", V-" + Build.VERSION.RELEASE);
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString());

            Request request = new Request.Builder()
                    .url(jsonObject.getString("url"))
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .build();

            Response response = client.newCall(request).execute();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
