package com.pekka.guardmyrear;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.VideoView;
import android.widget.MediaController;

public class StreamActivity extends AppCompatActivity implements SensorIndicatorFragment.OnFragmentInteractionListener {

    protected MediaController m_controller;
    protected boolean m_stream_started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        VideoView view = (VideoView)findViewById(R.id.videoView);

        if(!m_stream_started)
        {
            /* TODO: Avoid reloading the stream on rotation */
            m_controller = new MediaController(this);

            Uri m = Uri.parse(getIntent().getStringExtra(getResources().getString(R.string.stream_resource)));
            view.setVideoURI(m);
            view.start();
            m_stream_started = true;
        }
        m_controller.setAnchorView(view);
        view.setMediaController(m_controller);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        System.out.println(uri);
    }
}
