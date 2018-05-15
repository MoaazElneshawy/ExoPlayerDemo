package com.moaazfathy.exoplayerdemo;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {

    PlayerView playerView;
    SimpleExoPlayer player;
    MediaSource mediaSource;
    boolean playWhenReady;
    long playbackPosition, currentWindow;
    private final static String CURRENT_POSITION = "current_position";
    private static final DefaultBandwidthMeter BANDWIDTH_METER =
            new DefaultBandwidthMeter();
    RadioButton default_rb, multi_rb, adaptive_rb;
    RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);

        radioGroup = findViewById(R.id.radio_group);
        default_rb = findViewById(R.id.default_rb);
        multi_rb = findViewById(R.id.multi_rb);
        adaptive_rb = findViewById(R.id.adaptive_rb);

        playbackPosition = C.TIME_UNSET;
        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(CURRENT_POSITION);
            Log.e("positionInSave", playbackPosition + "");
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.default_rb:
                        playerInitialization(false, buildMediaSource());
                        break;
                    case R.id.multi_rb:
                        playerInitialization(false, concatenatingBuildMediaSource());
                        break;
                    case R.id.adaptive_rb:
                        playerInitialization(true, adaptiveBuildMediaSource());
                        break;
                }
            }
        });


    }

    private void playerInitialization(boolean adaptive, MediaSource mediaSource) {
        if (player != null) {
            player.release();
            player = null;
        }
        ////////////////// ADAPTIVE STREAMING //////////////////
        if (adaptive) {
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

            player = ExoPlayerFactory.newSimpleInstance(
                    new DefaultRenderersFactory(this),
                    new DefaultTrackSelector(adaptiveTrackSelectionFactory),
                    new DefaultLoadControl());

        } else {
            ////////////////// DEFAULT AND MULTIMEDIA SOURCES //////////////////

            // for default media source or concatenation media sources
            player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this),
                    new DefaultTrackSelector(), new DefaultLoadControl());

            //for default media source

        }

        this.mediaSource = mediaSource;

        playerView.setPlayer(player);
        Log.e("positionInInit", playbackPosition + "");

        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
        player.seekTo(playbackPosition);

    }

    // building adaptive media source
    private MediaSource adaptiveBuildMediaSource() {
        DataSource.Factory manifestDataSourceFactory =
                new DefaultHttpDataSourceFactory("ua");
        DashChunkSource.Factory dashChunkSourceFactory =
                new DefaultDashChunkSource.Factory(
                        new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER));
        return new DashMediaSource.Factory(dashChunkSourceFactory,
                manifestDataSourceFactory).createMediaSource(Uri.parse(getString(R.string.media_url_dash)));
    }


    // building default media source
    private MediaSource buildMediaSource() {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                createMediaSource(Uri.parse(getString(R.string.media_url_mp3)));
    }


    // concatenation between many media sources
    private MediaSource concatenatingBuildMediaSource() {

        ExtractorMediaSource videoSource =
                new ExtractorMediaSource.Factory(
                        new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                        createMediaSource(Uri.parse(getString(R.string.video_url)));


        ExtractorMediaSource audioSource =
                new ExtractorMediaSource.Factory(
                        new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                        createMediaSource(Uri.parse(getString(R.string.media_url_mp3)));

        return new ConcatenatingMediaSource(videoSource,audioSource );
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            playerInitialization(false, buildMediaSource());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        hideSystemUi();
        if ((Util.SDK_INT <= 23 || player == null)) {
            playerInitialization(false, buildMediaSource());
        }
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        // hiding system controls
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            Log.e("positionInRelease", playbackPosition + "");
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CURRENT_POSITION, playbackPosition);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        playbackPosition = savedInstanceState.getLong(CURRENT_POSITION);
    }
}
