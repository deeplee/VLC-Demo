
package com.example.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;
import org.videolan.libvlc.Media;
import org.videolan.vlc.util.VLCInstance;
import org.videolan.vlc.util.WeakHandler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements IVideoPlayer {

    String TAG = "Lee";
    private MediaPlayer mPlayer;
    private MediaPlayer mPlayer2;
    private MediaPlayer mPlayer3;
    private MediaPlayer mPlayer4;
    private SurfaceView surfaceView;
    private SurfaceView surfaceView2;
    private SurfaceView surfaceView3;
    private SurfaceView surfaceView4;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder surfaceHolder2;
    private SurfaceHolder surfaceHolder3;
    private SurfaceHolder surfaceHolder4;

    private String mLocation;
    private LibVLC mLibVLC;
    private int savedIndexPosition = -1;
    private int mLastAudioTrack = -1;
    private int mLastSpuTrack = -2;
    private AudioManager mAudioManager;
    private int mAudioMax;
    private SharedPreferences mSettings;
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;
    // private EventManager mEventManger;
    private boolean mIsPlaying;
    private int mVideoHeight;
    private int mVideoWidth;
    private int mSarNum;
    private int mSarDen;
    private int mSurfaceAlign;
    private static final int OVERLAY_TIMEOUT = 4000;
    private static final int OVERLAY_INFINITE = 3600000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int SURFACE_SIZE = 3;
    private static final int AUDIO_SERVICE_CONNECTION_SUCCESS = 5;
    private static final int AUDIO_SERVICE_CONNECTION_FAILED = 6;
    private static final int FADE_OUT_INFO = 4;
    private EventHandler eventandler;
    Media rtspmedia;

    private Socket clientSocket = null;
    private OutputStream outStream = null;
    int x = 0;
    int y = 0;
    int action = -1;
    int hostw = 0;
    int hosth = 0;
    int surfx = 0;
    int surfy = 0;
    byte[] msgBuffer = null;
    String out = null;
    boolean change = false;
    boolean connected = false;
    DisplayMetrics dm = null;
    public static Context instance;
    private SendThread mSendThread = null;

    private String ip_adress;

    private EditText ip;
    private Button con;
    private Button btn_BACK;
    private Button btn_MENU;
    private Button btn_HOME;
    InputMethodManager inputmanger;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub

        if (connected) {
            ip.setFocusable(false);
            ip.setEnabled(false);
        }

        x = (int) ev.getX() - surfx;
        y = (int) ev.getY() - surfy;
        action = ev.getAction();
        if (x < 0 || y < 0 || x > hostw || y > hosth) {
            change = false;
        } else
            change = true;
        String out = Integer.toString(hostw) + ";" + Integer.toString(hosth) + ";"
                + Integer.toString(action) + ";" + Integer.toString(x) + ";" + Integer.toString(y)
                + ";";
        out = "touch;" + out;
        try {
            msgBuffer = out.getBytes("utf-8");
            if (change && connected) {
                if (!clientSocket.isClosed() && clientSocket.isConnected()) {
                    outStream = clientSocket.getOutputStream();
                    outStream.write(msgBuffer);
                    Log.e("Send", "Send msg:" + out);
                } else {
                    Log.e("Send", "连接断开，重连...");
                    reconnect();
                }
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            reconnect();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            reconnect();
            e.printStackTrace();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        EventHandler em = EventHandler.getInstance();
        em.removeHandler(mHandler);
        mLibVLC.eventVideoPlayerActivityCreated(false);
        mLibVLC.stop();
        mLibVLC.clearBuffer();
        mLibVLC.destroy();
        mLibVLC.closeAout();
        mLibVLC.detachSurface();
        mLibVLC.stopDebugBuffer();
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        inputmanger = (InputMethodManager) getSystemService(instance.INPUT_METHOD_SERVICE);
        ip = (EditText) findViewById(R.id.ip);
        con = (Button) findViewById(R.id.btn_CON);
        btn_BACK = (Button) findViewById(R.id.btn_BACK);
        btn_HOME = (Button) findViewById(R.id.btn_HOME);
        btn_MENU = (Button) findViewById(R.id.btn_MENU);
        dm = new DisplayMetrics();

        // getWindowManager().getDefaultDisplay().getMetrics(dm);
        // hostw = dm.widthPixels;
        // hosth = dm.heightPixels;

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(mSurfaceCallback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // surfaceView2 = (SurfaceView) findViewById(R.id.surfaceView2);
        // surfaceHolder2 = surfaceView2.getHolder();
        // surfaceHolder2.addCallback(Surfaceview2Callback);
        // surfaceHolder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //
        // surfaceView3 = (SurfaceView) findViewById(R.id.surfaceView3);
        // surfaceHolder3 = surfaceView3.getHolder();
        // surfaceHolder3.addCallback(Surfaceview3Callback);
        // surfaceHolder3.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //
        // surfaceView4 = (SurfaceView) findViewById(R.id.surfaceView4);
        // surfaceHolder4 = surfaceView4.getHolder();
        // surfaceHolder4.addCallback(Surfaceview4Callback);
        // surfaceHolder4.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        surfaceView.setKeepScreenOn(true);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int pitch;
        String chroma = pref.getString("chroma_format", "");
        if (LibVlcUtil.isGingerbreadOrLater() && chroma.equals("YV12")) {
            surfaceHolder.setFormat(ImageFormat.YV12);
            pitch = ImageFormat.getBitsPerPixel(ImageFormat.YV12) / 8;
        } else if (chroma.equals("RV16")) {
            surfaceHolder.setFormat(PixelFormat.RGB_565);
            PixelFormat info = new PixelFormat();
            PixelFormat.getPixelFormatInfo(PixelFormat.RGB_565, info);
            pitch = info.bytesPerPixel;
        } else {
            surfaceHolder.setFormat(PixelFormat.RGBX_8888);
            PixelFormat info = new PixelFormat();
            PixelFormat.getPixelFormatInfo(PixelFormat.RGBX_8888, info);
            pitch = info.bytesPerPixel;
        }
        mSurfaceAlign = 16 / pitch - 1;
        // LibVLC.useIOMX(getApplicationContext());
        try {

            mLibVLC = VLCInstance.getLibVlcInstance();
        } catch (LibVlcException e) {
            Log.i(TAG, "LibVLC.getInstance() error:" + e.toString());
            e.printStackTrace();
            return;
        }

        mLibVLC.eventVideoPlayerActivityCreated(true);

        eventandler = EventHandler.getInstance();
        eventandler.addHandler(mEventHandler);

        Buttonclick btnclick = new Buttonclick();

        btn_HOME.setOnClickListener(btnclick);
        btn_BACK.setOnClickListener(btnclick);
        btn_MENU.setOnClickListener(btnclick);
        con.setOnClickListener(btnclick);

        ip.setOnEditorActionListener(new EditText.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // TODO Auto-generated method stub
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    inputmanger.hideSoftInputFromWindow(ip.getWindowToken(), 0);
                    ip_adress = ip.getText().toString();
                    mLocation = "rtsp://" + ip_adress + ":8554/h263ESVideoTest";
                    mSendThread = new SendThread();
                    mSendThread.start();

                    return true;
                }
                return false;
            }

        });

    }

    class Buttonclick implements Button.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

            switch (v.getId()) {
                case R.id.btn_CON:
                    ip_adress = ip.getText().toString();
                    mSendThread = new SendThread();
                    mSendThread.start();
                    inputmanger.hideSoftInputFromWindow(ip.getWindowToken(), 0);
                    break;
                case R.id.btn_BACK:
                    if (connected) {
                        String out = "key;" + "BACK";
                        try {
                            msgBuffer = out.getBytes("utf-8");

                            outStream = clientSocket.getOutputStream();
                            outStream.write(msgBuffer);
                            Log.e("Send", "Send msg:" + out);

                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            reconnect();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            reconnect();
                            e.printStackTrace();
                        }
                    }
                    break;
                case R.id.btn_HOME:
                    if (connected) {
                        String out = "key;" + "HOME";
                        try {
                            msgBuffer = out.getBytes("utf-8");

                            outStream = clientSocket.getOutputStream();
                            outStream.write(msgBuffer);
                            Log.e("Send", "Send msg:" + out);

                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            reconnect();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            reconnect();
                            e.printStackTrace();
                        }
                    }
                    break;
                case R.id.btn_MENU:
                    if (connected) {
                        String out = "key;" + "MENU";
                        try {
                            msgBuffer = out.getBytes("utf-8");

                            outStream = clientSocket.getOutputStream();
                            outStream.write(msgBuffer);
                            Log.e("Send", "Send msg:" + out);

                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            reconnect();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            reconnect();
                            e.printStackTrace();
                        }
                    }
                    break;

                default:
                    break;
            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class SendThread extends Thread {
        @Override
        public void run() {
            try {
                clientSocket = new Socket(ip_adress, 8888);
                Log.e(TAG, "连接成功");
                connected = true;
                con.setClickable(false);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                connected = false;
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                connected = false;
                e.printStackTrace();
            }

            String path = "rtsp://172.16.32.14:8554/h263ESVideoTest";
            String rtspurl = "rtsp://" + ip_adress + ":8554/h263ESVideoTest";
            // pathUri = LibVLC.getInstance().nativeToURI(path);
            // mLocation = LibVLC.PathToURI("/sdcard/boot.mp4");
            // Log.e(TAG, "start rtsp");
            if (connected) {
                mLibVLC.setMediaList();
                rtspmedia = new Media( mLibVLC, rtspurl);
                mLibVLC.getMediaList().add(rtspmedia);
                savedIndexPosition = mLibVLC.getMediaList().size() - 1;
                mLibVLC.playIndex(savedIndexPosition);
            }
        };
    }

    public void reconnect() {
        try {

            clientSocket = new Socket(ip_adress, 8888);
            Log.e("Send", "重新连接成功");
            connected = true;
            con.setClickable(false);

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            connected = false;
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            connected = false;
            e.printStackTrace();
        }
    }

    private final VideoEventHandler mEventHandler = new VideoEventHandler(this);
    private final Handler mHandler = new VideoPlayerHandler(this);

    private static class VideoPlayerHandler extends WeakHandler<MainActivity> {
        public VideoPlayerHandler(MainActivity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = getOwner();
            if (activity == null) // WeakReference could be GC'ed early
                return;

            switch (msg.what) {
                case SURFACE_SIZE:
                    activity.changeSurfaceSize();
                    break;
            }
        }
    };

    @Override
    public void setSurfaceSize(int width, int height, int visible_width, int visible_height,
            int sar_num, int sar_den) {
        // TODO Auto-generated method stub
        if (width * height == 0)
            return;
        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mSarNum = sar_num;
        mSarDen = sar_den;
        Message msg = mHandler.obtainMessage(SURFACE_SIZE);
        mHandler.sendMessage(msg);

    }

    private final SurfaceHolder.Callback mSurfaceCallback = new Callback() {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (format == PixelFormat.RGBX_8888)
                Log.d(TAG, "Pixel format is RGBX_8888");
            else if (format == PixelFormat.RGB_565)
                Log.d(TAG, "Pixel format is RGB_565");
            else if (format == ImageFormat.YV12)
                Log.d(TAG, "Pixel format is YV12");
            else
                Log.d(TAG, "Pixel format is other/unknown");
            if (mLibVLC != null) {
                mLibVLC.attachSurface(surfaceHolder.getSurface(), MainActivity.this);
            }
            Log.e(TAG, "Surface changed");
            int xy[] = new int[2];
            surfaceView.getLocationOnScreen(xy);
            Log.e(TAG, "x: " + xy[0] + ", Y: " + xy[1]);
            surfx = xy[0];
            surfy = xy[1];
            hosth = surfaceView.getHeight();
            hostw = surfaceView.getWidth();

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mLibVLC != null)
                mLibVLC.detachSurface();
        }
    };

    //
    // private final SurfaceHolder.Callback Surfaceview2Callback = new
    // Callback() {
    // @Override
    // public void surfaceChanged(SurfaceHolder holder, int format, int width,
    // int height) {
    // String url2 = "/sdcard/boot.mp4";
    // try {
    // mPlayer2 = new MediaPlayer();
    // mPlayer2.setDisplay(surfaceHolder2);
    // mPlayer2.setDataSource(url2);
    // mPlayer2.prepare();
    // mPlayer2.setLooping(true);
    // mPlayer2.start();
    // } catch (IllegalArgumentException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (SecurityException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IllegalStateException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    //
    // @Override
    // public void surfaceCreated(SurfaceHolder holder) {
    // }
    //
    // @Override
    // public void surfaceDestroyed(SurfaceHolder holder) {
    // if(mLibVLC != null)
    // mLibVLC.detachSurface();
    // }
    // };
    //
    //
    // private final SurfaceHolder.Callback Surfaceview3Callback = new
    // Callback() {
    // @Override
    // public void surfaceChanged(SurfaceHolder holder, int format, int width,
    // int height) {
    // String url3 = "/sdcard/boot.mp4";
    // try {
    // mPlayer3 = new MediaPlayer();
    // mPlayer3.setDataSource(url3);
    // mPlayer3.prepare();
    // mPlayer3.setLooping(true);
    // mPlayer3.setDisplay(surfaceHolder3);
    // mPlayer3.start();
    // } catch (IllegalArgumentException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (SecurityException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IllegalStateException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    //
    // @Override
    // public void surfaceCreated(SurfaceHolder holder) {
    // }
    //
    // @Override
    // public void surfaceDestroyed(SurfaceHolder holder) {
    // if(mLibVLC != null)
    // mLibVLC.detachSurface();
    // }
    // };
    //
    // private final SurfaceHolder.Callback Surfaceview4Callback = new
    // Callback() {
    // @Override
    // public void surfaceChanged(SurfaceHolder holder, int format, int width,
    // int height) {
    // String url4 = "/sdcard/boot.mp4";
    // try {
    // mPlayer4 = new MediaPlayer();
    // mPlayer4.setDataSource(url4);
    // mPlayer4.prepare();
    // mPlayer4.setLooping(true);
    // mPlayer4.setDisplay(surfaceHolder4);
    // mPlayer4.start();
    // } catch (IllegalArgumentException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (SecurityException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IllegalStateException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    //
    // @Override
    // public void surfaceCreated(SurfaceHolder holder) {
    // }
    //
    // @Override
    // public void surfaceDestroyed(SurfaceHolder holder) {
    // if(mLibVLC != null)
    // mLibVLC.detachSurface();
    // }
    // };

    public static Context getMainContex() {
        return instance;
    }

    class VideoEventHandler extends WeakHandler<MainActivity> {
        public VideoEventHandler(MainActivity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = getOwner();
            if (activity == null)
                return;
            switch (msg.getData().getInt("event")) {
                case EventHandler.MediaPlayerPlaying:
                    Log.i(TAG, "MediaPlayerPlaying");
                    mIsPlaying = true;
                    break;
                case EventHandler.MediaPlayerPaused:
                    Log.i(TAG, "MediaPlayerPaused");
                    mIsPlaying = false;
                    break;
                case EventHandler.MediaPlayerStopped:
                    Log.i(TAG, "MediaPlayerStopped");
                    mIsPlaying = false;
                    break;
                case EventHandler.MediaPlayerEndReached:
                    Log.i(TAG, "MediaPlayerEndReached");
                    break;
                case EventHandler.MediaPlayerVout:
                    break;
                default:
                    // Log.e(TAG, String.format("Event not handled (0x%x)",
                    // msg.getData().getInt("event")));
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private void changeSurfaceSize() {
        // get screen size
        int dw = surfaceView.getWidth();
        int dh = surfaceView.getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (dw > dh && isPortrait || dw < dh && !isPortrait) {
            int d = dw;
            dw = dh;
            dh = d;
        }

        // sanity check
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar, vw;
        double density = (double) mSarNum / (double) mSarDen;
        if (density == 1.0) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoWidth;
            ar = (double) mVideoWidth / (double) mVideoHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoWidth * density;
            ar = vw / mVideoHeight;
        }

        // compute the display aspect ratio
        double dar = (double) dw / (double) dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_FIT_HORIZONTAL:
                dh = (int) (dw / ar);
                break;
            case SURFACE_FIT_VERTICAL:
                dw = (int) (dh * ar);
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoHeight;
                dw = (int) vw;
                break;
        }

        // align width on 16bytes
        int alignedWidth = (mVideoWidth + mSurfaceAlign) & ~mSurfaceAlign;

        // force surface buffer size
        surfaceHolder.setFixedSize(alignedWidth, mVideoHeight);

        // set display size
        LayoutParams lp = surfaceView.getLayoutParams();
        lp.width = dw * alignedWidth / mVideoWidth;
        lp.height = dh;
        surfaceView.setLayoutParams(lp);
        surfaceView.invalidate();
    }

}
