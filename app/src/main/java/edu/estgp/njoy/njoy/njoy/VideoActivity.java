package edu.estgp.njoy.njoy.njoy;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import edu.estgp.njoy.njoy.R;

public class VideoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        VideoView view = (VideoView)findViewById(R.id.videoView);

        int id = getIntent().getIntExtra("VIDEO", 0);
        if (id == 0) {
            finish();
        }

        String path = "android.resource://" + getPackageName() + "/" + id;
        view.setVideoURI(Uri.parse(path));
        view.start();

        view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                finish();
            }
        });
    }
}
