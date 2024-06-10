package com.example.slideshow.newslideshow;


import static com.example.slideshow.newslideshow.glstuff.GlUtil.bindTexAndBitmap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Choreographer;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.slideshow.R;
import com.example.slideshow.VideoPreviewActivity;
import com.example.slideshow.adapters.TransAdapter;
import com.example.slideshow.databinding.ActivitySlideShowBinding;
import com.example.slideshow.newslideshow.glstuff.EglCore;
import com.example.slideshow.newslideshow.glstuff.FilterGLProgram;
import com.example.slideshow.newslideshow.glstuff.FilterGLProgram.Filters;
import com.example.slideshow.newslideshow.glstuff.GlUtil;
import com.example.slideshow.newslideshow.glstuff.TextureMovieEncoder2;
import com.example.slideshow.newslideshow.glstuff.VideoEncoderCore;
import com.example.slideshow.newslideshow.glstuff.WindowSurface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class SlideShowActivity extends AppCompatActivity implements SurfaceHolder.Callback, Choreographer.FrameCallback {
    private static final String TAG = "SlideShowActivity";

    public static final String KEY_IMG_LIST = "IMG_LIST";
    public static final String KEY_IS_SQUARE = "IS_SQUARE";
    private static final String ASSET_PREFIX = "file:///android_asset/";
    private static final int RESULT_FROM_photos = 6060;
    private static final int RESULT_FROM_MUSIC = 7070;

    //1712039123863
    //1712039168126

    private ProgressDialog loadingDialog;
    private boolean isSquare;
    private int durationPerTransition = 3;
    private final AtomicBoolean haltPreview = new AtomicBoolean(false);
    private List<String> pathList;
    private RenderThread mRenderThread;
    @Nullable
    private TransAdapter transAdapter;
    private ActivitySlideShowBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySlideShowBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        pathList = getIntent().getStringArrayListExtra(KEY_IMG_LIST);
        isSquare = getIntent().getBooleanExtra(KEY_IS_SQUARE, false);

        if (isSquare) {//layout params for square

            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) binding.surfaceView.getLayoutParams();
            layoutParams.topToBottom = binding.layoutToolbar.getId();
            layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
            layoutParams.dimensionRatio = "1:1";
            binding.surfaceView.setLayoutParams(layoutParams);
            binding.surfaceView.requestLayout();

            ConstraintLayout.LayoutParams scrollLayoutParams = (ConstraintLayout.LayoutParams) binding.scroll.getLayoutParams();
            scrollLayoutParams.topToBottom = binding.surfaceView.getId();
            scrollLayoutParams.bottomToBottom = ConstraintSet.PARENT_ID;
            scrollLayoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            scrollLayoutParams.height = 0;
            binding.scroll.setLayoutParams(scrollLayoutParams);
            binding.scroll.requestLayout();
        }

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setTitle("Please Wait");

        binding.surfaceView.getHolder().addCallback(this);


        List<Filters> filtersList = new ArrayList<>();

        for (Filters value : Filters.values()) {
            if (!value.name().contains("Mix")) {
                filtersList.add(value);
            }
        }


        transAdapter = new TransAdapter(this, filtersList.toArray(new Filters[0]), (filters) -> {
            if (mRenderThread != null) {
                RenderHandler rh = mRenderThread.getHandler();
                if (rh != null) {
                    Log.i(TAG, "onCreate:changeFilter " + filters);
                    Choreographer.getInstance().removeFrameCallback(this);
                    rh.changeFilter(filters);
                    loadingDialog.show();
                }

            }
        });

        binding.themeRecycle.setAdapter(transAdapter);


        initClickListeners();

    }


    private void initClickListeners() {
        binding.ivBtnBack.setOnClickListener(v -> onBackPressed());

        binding.done.setOnClickListener(v -> {
            if (mRenderThread != null) {
                RenderHandler handler = mRenderThread.getHandler();
                if (handler != null) {
                    handler.saveRecording();
                }
            }
        });


        binding.surfaceView.setOnClickListener(v -> {
            if (mRenderThread != null) {
                RenderHandler renderHandler = mRenderThread.getHandler();
                if (renderHandler != null) {
                    renderHandler.togglePlayback();
                }
            }
        });


        binding.sbVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mRenderThread != null) {
                    RenderHandler handler = mRenderThread.getHandler();
                    if (handler != null) {
                        handler.seekTo(seekBar.getProgress());
                    }
                }
            }
        });

    }


    @Nullable
    private Bitmap getBitmapFromAsset(String assetFileName) {
        try (InputStream inputStream = getAssets().open(assetFileName)) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private String[] getAssetFolderData(String folder) {
        try {
            String[] paths = getAssets().list(folder);
            if (paths != null) {
                for (int i = 0; i < paths.length; i++) {
                    paths[i] = folder + "/" + paths[i];
                }
            }
            return paths;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRenderThread != null) {
            Log.d(TAG, "onResume re-hooking choreographer");
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    protected void onDestroy() {
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.handleDestroy();
            }
        }

        super.onDestroy();
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated: ");
        initSetup();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged: ");
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendSurfaceChanged(format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendShutdown();
            try {
                mRenderThread.join();
            } catch (InterruptedException ie) {
                // not expected
                throw new RuntimeException("join was interrupted", ie);
            }
        }
        mRenderThread = null;

        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {

        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            Log.i(TAG, "doFrame: halt " + haltPreview.get());
            if (!haltPreview.get()) {
                Choreographer.getInstance().postFrameCallback(this);
                rh.sendDoFrame(frameTimeNanos);
            }
        }
    }

    private void handleRecOutput(@Nullable File file) {
        Choreographer.getInstance().removeFrameCallback(this);
        if (file != null) {
            startActivity(new Intent(this, VideoPreviewActivity.class)
                    .putExtra(VideoPreviewActivity.KEY_PATH, file.getAbsolutePath()));
        }
    }

    private void handleToastInfo(@StringRes int msg) {
        if (!isFinishing()) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePlayState(boolean state) {


        if (state) {
            Choreographer.getInstance().postFrameCallback(this);
            binding.ivBtnPreview.setVisibility(View.GONE);

        } else {
            Choreographer.getInstance().removeFrameCallback(this);
            binding.ivBtnPreview.setVisibility(View.VISIBLE);

        }
    }

    private void handleHideDialog() {
        haltPreview.set(false);
        if (loadingDialog.isShowing() && !isFinishing()) {
            loadingDialog.dismiss();
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    private void initSetup() {
        if (pathList == null || pathList.isEmpty() || transAdapter == null) {
            new Handler().postDelayed(this::initSetup, 200);
            return;
        }

        Log.i(TAG, "initSetup:pathList-> " + pathList);

        assert transAdapter.getCurrentFilter() != null;
        mRenderThread = new RenderThread(binding.surfaceView.getHolder(), new ActivityHandle(this),
                getFilesDir(), pathList.stream().map(Uri::parse).collect(Collectors.toList()), transAdapter.getCurrentFilter(),
                getDisplayRefreshNsec(this), durationPerTransition, progressPercent -> {
            Log.i(TAG, "initSetupProgressPercent: " + progressPercent);

            int totalSec = (pathList.size() * durationPerTransition) - durationPerTransition;
            int currentSec = Math.round(progressPercent / 100f * totalSec);

            runOnUiThread(() -> {
                if (!isFinishing()) {
                    binding.tvStartVideo.setText(DateUtils.formatElapsedTime(currentSec));
                    binding.tvEndVideo.setText(DateUtils.formatElapsedTime(totalSec));
                    binding.sbVideo.setProgress((int) progressPercent);
                }
            });

        });

        mRenderThread.setName("Record GL render");
        mRenderThread.start();
        mRenderThread.waitUntilReady();

        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendSurfaceCreated();
        }

        Choreographer.getInstance().postFrameCallback(this);
    }

    private long getDisplayRefreshNsec(@NonNull Activity activity) {
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        double displayFps = display.getRefreshRate();
        long refreshNs = Math.round(1000000000L / displayFps);
        Log.d(TAG, "refresh rate is " + displayFps + " fps --> " + refreshNs + " ns");
        return refreshNs;
    }

    static class ActivityHandle extends Handler {
        private static final int MSG_REC_OUT = 0;
        private static final int MSG_TOAST_INFO = 1;
        private static final int MSG_PLAY_STATE = 2;
        private static final int MSG_HIDE_DIALOG = 3;

        private final WeakReference<SlideShowActivity> activityWeakReference;

        ActivityHandle(SlideShowActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        public void sendRecOutput(@Nullable File file) {
            sendMessage(obtainMessage(MSG_REC_OUT, file));
        }

        public void sendToastMsg(int msg) {
            sendMessage(obtainMessage(MSG_TOAST_INFO, msg));
        }

        public void sendPlayState(boolean state) {
            sendMessage(obtainMessage(MSG_PLAY_STATE, state));
        }

        public void sendHideDialog() {
            sendMessage(obtainMessage(MSG_HIDE_DIALOG));
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            SlideShowActivity activity = activityWeakReference.get();
            if (activity == null) return;

            switch (msg.what) {
                case MSG_REC_OUT:
                    activity.handleRecOutput((File) msg.obj);
                    break;
                case MSG_TOAST_INFO:
                    activity.handleToastInfo((int) msg.obj);
                    break;
                case MSG_PLAY_STATE:
                    activity.handlePlayState((boolean) msg.obj);
                    break;
                case MSG_HIDE_DIALOG:
                    activity.handleHideDialog();
                    break;
            }

        }


    }

    /**
     * A Thread Responsible for all Graphic operation. <br></br>
     * <p>
     * Instance of this should not be used to interact with graphic operation considering
     * GPU's are async in nature accessing directly can cause concurrency or raise condition <br></br>
     * Use {@link  RenderHandler} to interact with Graphic thread
     * </p>
     */
    public static class RenderThread extends Thread {

        private volatile SurfaceHolder surfaceHolder;
        private final float[] mIdentityMatrix;
        private RenderHandler handler;
        private EglCore mEglCore;
        private WindowSurface mWindowSurface;

        private final ActivityHandle activityHandle;

        //------
        private boolean mRecordingEnabled;
        @Nullable
        private File mOutputFile;
        private final File parentFile;
        private WindowSurface mInputWindowSurface;
        private TextureMovieEncoder2 mVideoEncoder;

        private Object mStartLock = new Object();
        private boolean mReady = false;
        private Bitmap textureBitmap1, textureBitmap2;

        private final int[] textureIds = new int[2];
        private int textureId1, textureId2;
        private FilterGLProgram triangle;
        private Filters currentFilter;

        private boolean isFirstDrawCall = true;
        private long startTime;
        private float ratio;
        private final float[] mViewMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mMVPMatrix = new float[16];
        private int width, height;
        private float durationPerImg;
        private final int imgCount;
        private int currentImgPos;
        private float currentProgressPer;
        private long pauseInterval = 0L, pauseStart = 0L;
        private final List<Uri> fileList;
        private State recordingState;
        private final long refreshPeriodNs;
        private final ProgressListener progressListener;
        private final AtomicBoolean playingState = new AtomicBoolean(true);

        private enum State {
            RecOn, RecOff, RecResume, Preview
        }

        private RenderThread(@NonNull SurfaceHolder surfaceHolder, @NonNull ActivityHandle activityHandle,
                             @NonNull File parentFile, @NonNull List<Uri> imgList,
                             @NonNull Filters currentFilter, long refreshPeriodNs, float durationPerImg,
                             @NonNull ProgressListener progressListener) {

            this.surfaceHolder = surfaceHolder;
            this.activityHandle = activityHandle;
            this.parentFile = parentFile;
            this.fileList = imgList;
            this.currentFilter = currentFilter;
            this.refreshPeriodNs = refreshPeriodNs;
            this.durationPerImg = durationPerImg;
            this.progressListener = progressListener;

            imgCount = imgList.size();

            mIdentityMatrix = new float[16];
            Matrix.setIdentityM(mIdentityMatrix, 0);

            textureBitmap1 = uriToBitmap(fileList.get(0));
            textureBitmap2 = uriToBitmap(fileList.get(1));
            currentImgPos = 2;
            recordingState = State.RecOn;

        }

        private Bitmap uriToBitmap(Uri uri) {
            try {
                // Use BitmapFactory to decode the Uri into a Bitmap
                return BitmapFactory.decodeStream(activityHandle.activityWeakReference.get().getContentResolver().openInputStream(uri));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void run() {
            Looper.prepare();
            handler = new RenderHandler(this);
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

            synchronized (mStartLock) {
                mReady = true;
                mStartLock.notify();    // signal waitUntilReady()
            }

            Looper.loop();
            releaseGL();
            mEglCore.release();

            synchronized (mStartLock) {
                mReady = false;
            }
        }

        public void waitUntilReady() {
            synchronized (mStartLock) {
                while (!mReady) {
                    try {
                        mStartLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        @Nullable
        public RenderHandler getHandler() {
            return handler;
        }

        private void releaseGL() {
            GlUtil.checkGlError("releaseGl start");

            int[] values = new int[1];

            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }
            if (triangle != null) {
                triangle.release();
                triangle = null;
            }
            if (mOffscreenTexture > 0) {
                values[0] = mOffscreenTexture;
                GLES20.glDeleteTextures(1, values, 0);
                mOffscreenTexture = -1;
            }
            if (mFramebuffer > 0) {
                values[0] = mFramebuffer;
                GLES20.glDeleteFramebuffers(1, values, 0);
                mFramebuffer = -1;
            }
            if (mDepthBuffer > 0) {
                values[0] = mDepthBuffer;
                GLES20.glDeleteRenderbuffers(1, values, 0);
                mDepthBuffer = -1;
            }


            GlUtil.checkGlError("releaseGl done");

            mEglCore.makeNothingCurrent();
        }

        public void surfaceCreated() {
            Surface surface = surfaceHolder.getSurface();
            prepareGL(surface);
           /* if (BuildConfig.DEBUG) {
                Log.i(TAG, " RenderThread surfaceCreated: start update");
            }*/
            startTime = System.currentTimeMillis();
            updatePlayingState(true);
        }

        private void prepareGL(Surface surface) {
            if (fileList.size() < 2) return;

            mWindowSurface = new WindowSurface(mEglCore, surface, false);
            mWindowSurface.makeCurrent();

            triangle = new FilterGLProgram(currentFilter);

            // Generate texture
            GLES20.glGenTextures(textureIds.length, textureIds, 0);
            textureId1 = textureIds[0];
            textureId2 = textureIds[1];

            if (textureBitmap1 == null) {
                textureBitmap1 = uriToBitmap(fileList.get(0));
            }

            if (textureBitmap2 == null) {
                textureBitmap2 = uriToBitmap(fileList.get(1));
                currentImgPos = 2;
            }

            Log.i(TAG, "prepareGL: " + fileList.get(0));
            if (textureBitmap1 == null || textureBitmap2 == null) return;

            // Bind the texture
            bindTexAndBitmap(textureId1, textureBitmap1);
            bindTexAndBitmap(textureId2, textureBitmap2);

        }

        public void surfaceChanged(int width, int height) {
            this.width = width;
            this.height = height;

            prepareFramebuffer(width, height);
            GLES20.glViewport(0, 0, width, height);

            ratio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 7);

            updatePlayingState(playingState.get());

            Log.i(TAG, "surfaceChanged: width " + width + " height " + height + " ratio " + ratio);

        }

        private int mOffscreenTexture;
        private int mFramebuffer;
        private int mDepthBuffer;

        private void prepareFramebuffer(int width, int height) {
            GlUtil.checkGlError("prepareFramebuffer start");

            int[] values = new int[1];

            // Create a texture object and bind it.  This will be the color buffer.
            GLES20.glGenTextures(1, values, 0);
            GlUtil.checkGlError("glGenTextures");
            mOffscreenTexture = values[0];   // expected > 0
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture);
            GlUtil.checkGlError("glBindTexture " + mOffscreenTexture);

            // Create texture storage.
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            // Set parameters.  We're probably using non-power-of-two dimensions, so
            // some values may not be available for use.
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GlUtil.checkGlError("glTexParameter");

            // Create framebuffer object and bind it.
            GLES20.glGenFramebuffers(1, values, 0);
            GlUtil.checkGlError("glGenFramebuffers");
            mFramebuffer = values[0];    // expected > 0
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
            GlUtil.checkGlError("glBindFramebuffer " + mFramebuffer);

            // Create a depth buffer and bind it.
            GLES20.glGenRenderbuffers(1, values, 0);
            GlUtil.checkGlError("glGenRenderbuffers");
            mDepthBuffer = values[0];    // expected > 0
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer);
            GlUtil.checkGlError("glBindRenderbuffer " + mDepthBuffer);

            // Allocate storage for the depth buffer.
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
            GlUtil.checkGlError("glRenderbufferStorage");

            // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBuffer);
            GlUtil.checkGlError("glFramebufferRenderbuffer");
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0);
            GlUtil.checkGlError("glFramebufferTexture2D");

            // See if GLES is happy with all this.
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Framebuffer not complete, status=" + status);
            }

            // Switch back to the default framebuffer.
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            GlUtil.checkGlError("prepareFramebuffer done");
        }

        private void startEncoder() {
            Log.d(TAG, "starting to record");
            // This is very Important ~ Gaurav
            // Record at 1280x720, regardless of the window dimensions.  The encoder may
            // explode if given "strange" dimensions, e.g. a width that is not a multiple
            // of 16.  We can box it as needed to preserve dimensions.
            final int BIT_RATE = 4000000;   // 4Mbps
            final int VIDEO_WIDTH = width;
            final int VIDEO_HEIGHT = height;

            mOutputFile = new File(parentFile, System.currentTimeMillis() + ".mp4");

            VideoEncoderCore encoderCore;
            try {
                encoderCore = new VideoEncoderCore(VIDEO_WIDTH, VIDEO_HEIGHT, BIT_RATE, mOutputFile);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            mInputWindowSurface = new WindowSurface(mEglCore, encoderCore.getInputSurface(), true);
            mVideoEncoder = new TextureMovieEncoder2(encoderCore);
        }

        private void stopEncoder(TextureMovieEncoder2.Callback callback) {
            if (mVideoEncoder != null) {
                Log.d(TAG, "stopping recorder, mVideoEncoder=" + mVideoEncoder);
                mVideoEncoder.stopRecording(callback);
                // TODO: wait (briefly) until it finishes shutting down so we know file is
                //       complete, or have a callback that updates the UI
                mVideoEncoder = null;
            }
            if (mInputWindowSurface != null) {
                mInputWindowSurface.release();
                mInputWindowSurface = null;
            }
        }

        private boolean mRecordedPrevious;
        private boolean mPreviousWasDropped;

        public void doFrame(long timeStampNanos) {

            Log.i(TAG, "doFrame: drawing " + timeStampNanos);

            if (width <= 0 || height <= 0) return;

            long diff = System.nanoTime() - timeStampNanos;
            long max = refreshPeriodNs - 2000000;
            if (diff > max) {
                // too much, drop a frame
                Log.d(TAG, "diff is " + (diff / 1000000.0) + " ms, max " + (max / 1000000.0) + ", skipping render");
                mRecordedPrevious = false;
                mPreviousWasDropped = true;
                return;
            }

            if (playingState.get()) {

                boolean swapResult;

                if (recordingState.equals(State.Preview) || mRecordedPrevious) {
                    mRecordedPrevious = false;
                    draw(false, pauseInterval);
                    swapResult = mWindowSurface.swapBuffers();
                } else {
                    mRecordedPrevious = true;

                    if (showLog) {
                        Log.i(TAG, "changeFilter: RS " + recordingState);
                    }

                    if (recordingState.equals(State.RecOn)) {
                        setRecordingEnabled(true, null);
                    }

                    // Draw for display, swap.
                    draw(false, 0);
                    swapResult = mWindowSurface.swapBuffers();

                    if (showLog) {
                        Log.i(TAG, "changeFilter: RE "
                                + mRecordingEnabled
                                + " ," + mVideoEncoder
                                + " ," + mInputWindowSurface);

                        showLog = false;
                    }

                    if (mVideoEncoder == null || mInputWindowSurface == null) return;
                    // Draw for recording.
                    mVideoEncoder.frameAvailableSoon();
                    mInputWindowSurface.makeCurrent();

                    draw(true, 0);
                    mInputWindowSurface.setPresentationTime(timeStampNanos);
                    mInputWindowSurface.swapBuffers();
                    mWindowSurface.makeCurrent();
                }

                mPreviousWasDropped = false;

                if (!swapResult) {
                    Log.w(TAG, "swapBuffers failed, killing renderer thread");
                    shutdown();
                }

                if (recordingState.equals(State.RecOff)) {
                    setRecordingEnabled(false, null);
                    recordingState = State.Preview;
                    updatePlayingState(false);
                    currentImgPos = 0;
                    Log.i(TAG, "changeFilter: RE " + mRecordingEnabled);
                }
            }

        }

        boolean showLog = false;

        private void draw(boolean forRec, long pauseInterval) {

            long currentTime = System.currentTimeMillis();
            float elapsedTime = (currentTime - (startTime + pauseInterval)) / 1000.0f;
            elapsedTime = Math.min(elapsedTime, durationPerImg);
//            float progress = easeInOut(elapsedTime, 0.0f, 1.0f, durationPerImg);
            float progress = linear(elapsedTime, durationPerImg);

            Log.i(TAG, "draw: ct = " + currentTime +
                    " , st = " + startTime +
                    " , et = " + elapsedTime +
                    " , p = " + progress +
                    " , pi = " + pauseInterval +
                    " , ic = " + currentImgPos);

            int current = currentImgPos - 1;

            currentProgressPer = ((current + progress) / (float) imgCount) * 100f;
            Log.i(TAG, "draw: perc " + currentProgressPer);

            progressListener.onProgress(currentProgressPer);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GlUtil.checkGlError("clear buffers");

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);//decide color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);//draw color
            GlUtil.checkGlError("set color bg");

            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            triangle.draw(mMVPMatrix, textureId1, textureId2, progress);

            if (progress >= 1f) {
                this.pauseInterval = 0;
                pauseStart = 0;
                if (imgCount > currentImgPos) {
                    if (textureBitmap1 == null || textureBitmap2 == null) return;

                    GLES20.glDeleteTextures(textureIds.length, textureIds, 0);
                    textureBitmap1.recycle();
                    textureBitmap1 = textureBitmap2;

                    textureBitmap2 = uriToBitmap(fileList.get(currentImgPos));

                    if (textureBitmap1 == null || textureBitmap2 == null) return;

                    GLES20.glGenTextures(textureIds.length, textureIds, 0);
                    textureId1 = textureIds[0];
                    textureId2 = textureIds[1];

                    bindTexAndBitmap(textureId1, textureBitmap1);
                    bindTexAndBitmap(textureId2, textureBitmap2);

                    startTime = System.currentTimeMillis();
                    currentImgPos++;
                } else {
                    if (forRec) {
                        recordingState = State.RecOff;
                        Log.i(TAG, "changeFilter: " + recordingState);
                    } else {
                        if (recordingState.equals(State.Preview)) {
                            updatePlayingState(false);
                            currentImgPos = 0;
                        }
                    }
                }
            }
        }

        private float easeInOut(float t, float startTime, float max, float dur) {
            t /= dur / 2;
            if (t < 1) return max / 2 * t * t * t + startTime;
            t -= 2;
            return max / 2 * (t * t * t + 2) + startTime;
        }

        private float linear(float t, float dur) {
            return t / dur;
        }

        public void setRecordingEnabled(boolean enable, @Nullable TextureMovieEncoder2.Callback callback) {
            if (enable == mRecordingEnabled) {
                return;
            }
            mRecordingEnabled = enable;
            if (enable) {
                Log.i(TAG, "setRecordingEnabled: starting encoding");
                startEncoder();
                Log.i(TAG, "setRecordingEnabled: started encoding");
            } else {
                Log.i(TAG, "setRecordingEnabled: stop encoding");
                stopEncoder(callback);
                Log.i(TAG, "setRecordingEnabled: stopped encoding");
            }
        }

        public void shutdown() {
            updatePlayingState(false);
            stopEncoder(null);
            Looper.myLooper().quit();
        }

        public void changeFilter(Filters filters) {
            Log.i(TAG, "changeFilter: called");
            if (currentFilter != filters) {
                updatePlayingState(false);
                setRecordingEnabled(false, outputFile -> {
                    if (outputFile != null) {
                        outputFile.delete();
                    }
                });

                if (triangle != null) {
                    triangle.release();
                    triangle = null;
                }

                if (textureBitmap1 != null) textureBitmap1.recycle();
                if (textureBitmap2 != null) textureBitmap2.recycle();

                textureBitmap1 = uriToBitmap(fileList.get(0));
                textureBitmap2 = uriToBitmap(fileList.get(1));
                currentImgPos = 2;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }

                triangle = new FilterGLProgram(filters);

                bindTexAndBitmap(textureId1, textureBitmap1);
                bindTexAndBitmap(textureId2, textureBitmap2);

                mOutputFile = null;
                currentFilter = filters;
                recordingState = State.RecOn;
                showLog = true;
                /*if (BuildConfig.DEBUG) {
                    Log.i(TAG, " RenderThread surfaceCreated3: start update");
                }*/
                startTime = System.currentTimeMillis();
                updatePlayingState(true);

                activityHandle.sendHideDialog();
            }
        }

        private void updatePlayingState(boolean newState) {
            playingState.set(newState);
            activityHandle.sendPlayState(playingState.get());
        }

        public void saveVideo() {
            if (recordingState.equals(State.Preview)) {
                activityHandle.sendRecOutput(mOutputFile);
            } else {
                if (currentProgressPer >= 100f) {
                    activityHandle.sendRecOutput(mOutputFile);
                } else {
                    activityHandle.sendToastMsg(R.string.watch_the_entire_video_to_save);
                }
            }
        }

        public void changeTime(int durationPerImg) {
            if (this.durationPerImg != durationPerImg) {
                updatePlayingState(false);
                setRecordingEnabled(false, outputFile -> {
                    if (outputFile != null) {
                        outputFile.delete();
                    }
                });

                if (textureBitmap1 != null) textureBitmap1.recycle();
                if (textureBitmap2 != null) textureBitmap2.recycle();

                textureBitmap1 = uriToBitmap(fileList.get(0));
                textureBitmap2 = uriToBitmap(fileList.get(1));
                currentImgPos = 2;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }

                mOutputFile = null;
                this.durationPerImg = durationPerImg;
                recordingState = State.RecOn;
                showLog = true;
               /* if (BuildConfig.DEBUG) {
                    Log.i(TAG, " RenderThread surfaceCreated4: start update");
                }*/
                startTime = System.currentTimeMillis();
                updatePlayingState(true);
            }
        }

        public void toggleVideo() {
            if (recordingState.equals(State.Preview)) {
                updatePlayingState(!playingState.get());

                if (!playingState.get()) {
                    pauseStart = System.currentTimeMillis();
                } else if (pauseStart != 0) {
                    pauseInterval = System.currentTimeMillis() - pauseStart;
                }

                Log.i(TAG, "toggleVideo: ps " + pauseStart + " , pi " + pauseInterval);

            } else {
                activityHandle.sendToastMsg(R.string.playback_can_be_toggled_once_you_played_the_video);
            }
        }

        public void seekTo(int percent) {
            if (recordingState.equals(State.Preview)) {
                currentImgPos = Math.min(Math.round((percent / 100f) * imgCount), imgCount);
            } else {
                activityHandle.sendToastMsg(R.string.you_can_seek_after_you_completely_watched_the_video);
            }
        }

        public interface ProgressListener {
            void onProgress(float progress);
        }

    }

    /**
     * A Handler which queue task for Graphic Thread {@link  RenderThread}<br></br>
     * This class ensure every event is queued properly thus offering thread safety somewhat 😅
     */
    private static class RenderHandler extends Handler {

        private static final int MSG_SURFACE_CREATED = 0;
        private static final int MSG_SURFACE_CHANGED = 1;
        private static final int MSG_DO_FRAME = 2;
        private static final int MSG_RECORDING_ENABLED = 3;
        private static final int MSG_SHUTDOWN = 5;
        private static final int MSG_CHANGE_FILTER = 6;
        private static final int MSG_SAVE_REC = 7;
        private static final int MSG_TOGGLE_VID_PLAYBACK = 8;
        private static final int MSG_CHANGE_TIME = 9;
        private static final int MSG_SEEK_TO = 10;

        private final WeakReference<RenderThread> mWeakRenderThread;

        private RenderHandler(RenderThread renderThread) {
            this.mWeakRenderThread = new WeakReference<>(renderThread);
        }

        public void sendSurfaceCreated() {
            sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CREATED));
        }

        public void sendSurfaceChanged(@SuppressWarnings("unused") int format, int width, int height) {
            // ignore format
            sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CHANGED, width, height));
        }

        public void sendDoFrame(long frameTimeNanos) {
            sendMessage(obtainMessage(RenderHandler.MSG_DO_FRAME, (int) (frameTimeNanos >> 32), (int) frameTimeNanos));
        }

        public void setRecordingEnabled(boolean enabled) {
            sendMessage(obtainMessage(MSG_RECORDING_ENABLED, enabled ? 1 : 0, 0));
        }

        public void togglePlayback() {
            sendMessage(obtainMessage(MSG_TOGGLE_VID_PLAYBACK));
        }

        public void handleDestroy() {
            sendShutdown();
        }

        public void changeFilter(Filters filter) {
            sendMessage(obtainMessage(RenderHandler.MSG_CHANGE_FILTER, filter));
        }

        public void sendShutdown() {
            sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
        }

        public void saveRecording() {
            sendMessage(obtainMessage(MSG_SAVE_REC));
        }

        public void changeTime(int durationPerImg) {
            sendMessage(obtainMessage(MSG_CHANGE_TIME, durationPerImg));
        }

        public void seekTo(int percent) {
            sendMessage(obtainMessage(MSG_SEEK_TO, percent));
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            RenderThread renderThread = mWeakRenderThread.get();
            if (renderThread == null) {
                Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
                return;
            }

            switch (msg.what) {
                case MSG_SURFACE_CREATED:
                    renderThread.surfaceCreated();
                    break;
                case MSG_SURFACE_CHANGED:
                    renderThread.surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_DO_FRAME:
                    long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
                    renderThread.doFrame(timestamp);
                    break;
                case MSG_RECORDING_ENABLED:
                    renderThread.setRecordingEnabled(msg.arg1 != 0, null);
                    break;
                case MSG_SHUTDOWN:
                    renderThread.shutdown();
                    break;
                case MSG_CHANGE_FILTER:
                    renderThread.changeFilter((Filters) msg.obj);
                    break;
                case MSG_SAVE_REC:
                    renderThread.saveVideo();
                    break;
                case MSG_TOGGLE_VID_PLAYBACK:
                    renderThread.toggleVideo();
                    break;
                case MSG_CHANGE_TIME:
                    renderThread.changeTime((int) msg.obj);
                    break;
                case MSG_SEEK_TO:
                    renderThread.seekTo((int) msg.obj);
                    break;
                default:
                    throw new RuntimeException("unknown message " + msg.what);

            }
        }


    }

}
