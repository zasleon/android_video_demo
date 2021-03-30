package com.example.xielm.myapplication;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class video_player extends ActionBarActivity implements
        android.media.MediaPlayer.OnBufferingUpdateListener,
        android.media.MediaPlayer.OnCompletionListener,
        android.media.MediaPlayer.OnPreparedListener,
        android.widget.SeekBar.OnSeekBarChangeListener,
        android.view.SurfaceHolder.Callback {
    private int videoWidth;//视频宽
    private int videoHeight;//视频高
    private int mobileWidth;//屏幕宽
    private int mobileHeight;//屏幕高
    private static boolean nature_refresh=true;//自然更新进度条
    private static android.os.Handler handler=new android.os.Handler();//处理变更UI的线程操作
    public static android.app.Activity activity;

    static android.media.MediaPlayer mediaPlayer;
    static android.view.SurfaceView surfaceview;
    static android.view.SurfaceHolder surfaceHolder;
    static android.widget.TextView current_time;//滑动进度条时同步更新时间栏，而不是滑动完了才更新（如果不加static程序会崩溃）
    static android.widget.TextView max_duration;
    static android.widget.TextView jump_to_this_position;
    static com.example.xielm.myapplication.VerticalProgressBar volume_progressbar;
    static com.example.xielm.myapplication.VerticalProgressBar brightness_progressbar;
    static android.widget.TextView volume_signal;
    static android.widget.TextView brightness_signal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();//隐藏标题栏
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
        setContentView(R.layout.activity_video_player);
        activity=this;
        video_player.this.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//横向播放

        android.view.WindowManager wm = (android.view.WindowManager) this.getSystemService(android.content.Context.WINDOW_SERVICE);//获取屏幕大小，做兼容性调整
        mobileWidth=wm.getDefaultDisplay().getWidth();
        mobileHeight=wm.getDefaultDisplay().getHeight();

        current_time = (android.widget.TextView) findViewById(R.id.current_time);//滑动进度条时同步更新时间栏，而不是滑动完了才更新（如果不加static程序会崩溃）
        max_duration= (android.widget.TextView) findViewById(R.id.max_duration);//总时长
        jump_to_this_position= (android.widget.TextView) findViewById(R.id.jump_to_this_position);//快进/后退到xx：xx
        //========================================自定义进度条部分========================================
        volume_progressbar=(com.example.xielm.myapplication.VerticalProgressBar)findViewById(R.id.volumeProgressBar);//音量变化进度条（该进度条为自定义设置）
        brightness_progressbar=(com.example.xielm.myapplication.VerticalProgressBar)findViewById(R.id.brightnessProgressBar);//亮度变化进度条（该进度条为自定义设置）
        volume_progressbar.setVisibility(android.view.View.INVISIBLE);//音量进度条设置为不可见（该进度条为自定义设置）
        brightness_progressbar.setVisibility(android.view.View.INVISIBLE);//亮度进度条设置为不可见（该进度条为自定义设置）
        volume_signal=(android.widget.TextView) findViewById(R.id.volume_signal);//进度条上名字不可见（该进度条为自定义设置）
        volume_signal.setVisibility(android.view.View.INVISIBLE);
        brightness_signal=(android.widget.TextView) findViewById(R.id.brightness_signal);//进度条上名字不可见（该进度条为自定义设置）
        brightness_signal.setVisibility(android.view.View.INVISIBLE);
        //========================================自定义进度条部分结束====================================
        skbProgress=(android.widget.SeekBar) video_player.activity.findViewById(R.id.seekBar);//获取进度条
        skbProgress.setOnSeekBarChangeListener(new video_player());//监视进度条是否被拖动

        mTimer.schedule(mTimerTask, 0, 100);

        surfaceview=(android.view.SurfaceView) video_player.activity.findViewById(R.id.surfaceView);//播放画面容器surfaceview初始化
        video_player.surfaceHolder=video_player.surfaceview.getHolder();
        video_player.surfaceHolder.addCallback(this);
    }

    public static void play_this_video(String address) {//使用该函数进入播放影片
        video_player.mediaPlayer = new android.media.MediaPlayer();
        telecom.tsleep(500);
        try{video_player.mediaPlayer.setDataSource(address);}catch(java.io.IOException ex1)
        {video_player.add_log("mediaPlayer创建失败！"); return;}
        video_player.mediaPlayer.setDisplay(video_player.surfaceHolder);
        video_player.mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
        video_player.mediaPlayer.prepareAsync();
        video_player.mediaPlayer.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(android.media.MediaPlayer mp) {                // 装载完毕回调
                video_player.mediaPlayer.start();
            }
        });
    }


    @Override//销毁该页面时，在activity结束的时候回收资源
    protected void onDestroy() {
        mediaPlayer.stop();//stop自带释放资源功能    if(mediaPlayer != null) {mediaPlayer.release();mediaPlayer = null;}
        super.onDestroy();
        }
    //-------------------------------调整亮度与音量与快进/后退到指定位置----------------------------------------------------

    private static float first_point_X=0;//滑动起始点x值
    private static float first_point_Y=0;//滑动起始点y值
    private static float calculate_point_X=0;//计算起点的x值（某种契机下计算起点不是滑动起始点）
    private static float calculate_point_Y=0;//计算起点的y值（某种契机下计算起点不是滑动起始点）
    private static float first_state=0;//初始值
    private static int now_change_what=0;//现在正在修改什么
    private final int brightness_changing=1;//改亮度
    private final int volume_changing=2;//该音量
    private final int movie_forward_backward_changing=3;//画面快进或倒退
	private final int changing_nothing=4;//画面快进或倒退
    private void setBrightness(android.view.MotionEvent e1,android.view.MotionEvent e2) {//手指滑动调节亮度
        android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness= (float)((calculate_point_Y - e2.getY())/mobileHeight + first_state);//计算calculate_point_Y与当前滑动位置在y轴上的距离，设置系统亮度对应值

        if(lp.screenBrightness>=0&&lp.screenBrightness<=1)//当系统亮度对应值在系统亮度范围内
            getWindow().setAttributes(lp);//修改系统亮度

        if(lp.screenBrightness<0)lp.screenBrightness=0;if(lp.screenBrightness>1)lp.screenBrightness=1;
        if(lp.screenBrightness==0||lp.screenBrightness==1)//当亮度达到上下限，滑动最初点设置为手指达到亮度上/下限的点
        {
            calculate_point_Y=e2.getY();//根据手指达到亮度上/下限的点的y值进行亮度变化计算
            first_state=lp.screenBrightness;//亮度变化起始亮度变为上/下限
        }
        brightness_progressbar.setProgress((int)(100*lp.screenBrightness));
    }

    private void setVolume(android.view.MotionEvent e1,android.view.MotionEvent e2){//手指滑动调节音量
        android.media.AudioManager mAudioManager = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
        float max=mAudioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);//获取系统最大音量
        double set_Volume= (float)((calculate_point_Y - e2.getY())*max/mobileHeight + first_state);//计算calculate_point_Y与当前滑动位置在y轴上的距离，设置系统音量对应值

        if(set_Volume>=0&&set_Volume<=max)//当系统音量对应值在系统可播放音量的范围内
            mAudioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (int) set_Volume, android.media.AudioManager.FLAG_PLAY_SOUND);//修改系统音量

        if(set_Volume<0)set_Volume=0;if(set_Volume>max)set_Volume=max;
        if(set_Volume==0||set_Volume==max)//当音量达到上/下限，滑动最初点设置为手指达到音量上/下限的点
        {
            calculate_point_Y=e2.getY();//根据手指达到音量上/下限的点的y值进行音量变化计算
            first_state=mAudioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);//音量变化起始音量变为上/下限
        }
        volume_progressbar.setProgress((int)(100*set_Volume/max));
    }
    private void setMovie(android.view.MotionEvent e1,android.view.MotionEvent e2) {//横向划动，快进或倒退到指定时间位置处
        if (((int) first_state + (int) (e2.getX() - first_point_X) * 10) > 0)//如果往后退没有超过起点
        {
            move_position = (int) first_state + (int) (e2.getX() - first_point_X) * 10;
            if (move_position > mediaPlayer.getDuration())
                move_position = mediaPlayer.getDuration();//如果快进超过了最大长度
        }
        else
            move_position=0;//如果往后退超过了起点，将“即将移动到的指定时间位置”改为0起始处
        current_time.setText(time_switch_int_to_string(move_position));//停止current_time自动对齐mediaplayer播放的更新时间，变为显示划动后即将到的指定时间位置
        jump_to_this_position.setTextSize(mobileHeight/50);
        if(e2.getX() - first_point_X>0)
            jump_to_this_position.setText("快进到 "+time_switch_int_to_string(move_position));
        else
            jump_to_this_position.setText("后退到 "+time_switch_int_to_string(move_position));
    }
    private void test_info(android.view.MotionEvent e1, android.view.MotionEvent e2,float distanceX, float distanceY) {
        tt=(android.widget.TextView)findViewById(R.id.ttt);
        tt.setVisibility(android.view.View.VISIBLE);
        switch(now_change_what)
        {
            case brightness_changing:tt.setText("brightness_changing");break;
            case volume_changing:tt.setText("volume_changing");break;
            case movie_forward_backward_changing:if((e2.getX() - e1.getX())>0)tt.setText("move forward");else tt.setText("move backward");break;
            case changing_nothing:tt.setText("changing_nothing");break;
        }
        tt.setText(tt.getText() +
                "\ny: " + (e1.getY() - e2.getY()) +
                "\nx: " + (e2.getX() - e1.getX()) +
                "\n 本次滑动 y轴方向速度   " + Math.abs(distanceY) +
                "\n 本次滑动 x轴方向速度   " + Math.abs(distanceX));
    }
    private static final double FLING_MIN_DISTANCE = 1;
    private static final double FLING_MIN_VELOCITY = 1;
    private static android.widget.TextView tt;
    private static int move_position=0;//定位快进/后退到指定位置的时间

    private android.view.GestureDetector mGestureDetector;
    @Override
    protected void onResume() {
        mGestureDetector = new android.view.GestureDetector(
                new android.view.GestureDetector.OnGestureListener() {
                    public boolean onFling(android.view.MotionEvent e1,android.view.MotionEvent e2, float velocityX, float velocityY) {
                        return true;
                    }
                    public boolean onScroll(android.view.MotionEvent e1, android.view.MotionEvent e2,float distanceX, float distanceY) {
                        if(e1.getY()<mobileHeight/6)return true;//触碰点过高，容易和下拉头部菜单冲突，驳回功能请求

                        if((e1.getY()!=first_point_Y||e1.getX()!=first_point_X)||now_change_what==changing_nothing)//以滑动最初点为起点进行调整，只有当手指初次划动触碰点变化了才会执行其中内容
                        {
                            now_change_what=changing_nothing;
							if(Math.abs(distanceY) > FLING_MIN_VELOCITY||Math.abs(distanceX) > FLING_MIN_VELOCITY)//划动幅度够大
							{
                                first_point_X=e1.getX();//记录初始点x值
                                first_point_Y=e1.getY();//记录初始点y值
                                calculate_point_Y=first_point_Y;//根据最初点的y值进行变化计算
								//if(e1.getX() - e2.getX() > FLING_MIN_DISTANCE||e2.getX() - e1.getX() > FLING_MIN_DISTANCE)
                                if(Math.abs(e1.getX() - e2.getX()) > Math.abs(e1.getY() - e2.getY()))//横着划动幅度大于竖直划动幅度，则为“快进/后退到指定位置”
                                {
                                    jump_to_this_position.setVisibility(android.view.View.VISIBLE);
                                    now_change_what = movie_forward_backward_changing;//更新“正在修改状态”为“快进/后退到指定位置”
                                    nature_refresh=false;
                                    first_state=mediaPlayer.getCurrentPosition();
                                }
								else//竖直划动幅度大于横向划动幅度
									if(e1.getX() > mobileWidth/2)//如果是在右半边屏幕划动手指
                                    {
                                        volume_signal.setVisibility(android.view.View.VISIBLE);
                                        volume_progressbar.setVisibility(android.view.View.VISIBLE);
                                        now_change_what = volume_changing;//设置当前在修改音量
                                        android.media.AudioManager mAudioManager = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
                                        first_state=mAudioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);//设置音量变化起始音量
                                    }
									else//左半边屏幕划动手指
                                    {
                                        brightness_signal.setVisibility(android.view.View.VISIBLE);
                                        brightness_progressbar.setVisibility(android.view.View.VISIBLE);
                                        now_change_what = brightness_changing;//设置当前在修改亮度
                                        first_state=getWindow().getAttributes().screenBrightness;//获取当前亮度，并将该亮度设置为起始亮度
                                    }
							}
                        }
                        //test_info(e1,e2,distanceX,distanceY);//显示测试信息
                        if(Math.abs(distanceY) > FLING_MIN_VELOCITY||Math.abs(distanceX) > FLING_MIN_VELOCITY)//划动幅度够大
							switch(now_change_what)
							{
								case brightness_changing:
                                    setBrightness(e1,e2);
                                    break;
                                case volume_changing:
                                    setVolume(e1, e2);
                                    break;
                                case movie_forward_backward_changing:
                                    setMovie(e1, e2);
                                    break;
                                case changing_nothing:break;
							}
                        return true;
                    }

                    public boolean onSingleTapUp(android.view.MotionEvent e) {
                        return false;
                    }
                    public boolean onDown(android.view.MotionEvent e) {
                        return false;
                    }
                    public void onLongPress(android.view.MotionEvent e) {                    }
                    public void onShowPress(android.view.MotionEvent e) {                        // TODO Auto-generated method stub
                    }
                });
        super.onResume();
    }

    //-------------------------------调整亮度与音量与快进/后退到指定位置----------------------------------------------------

    //--------------------------------显示、隐藏进度条，暂停继续播放------------------------------
    private long firstClick_ACTION_UP = 0;//记录首次/最近一次点击时间
    private long secondClick_ACTION_UP = 0;//记录本次点击时间
    private final int totalTime = 300;//两次点击时间间隔，单位毫秒
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        boolean result = mGestureDetector.onTouchEvent(event);
        if (result)//如果被判定为在屏幕上划动
        {
            if(event.getAction()== android.view.MotionEvent.ACTION_UP)//如果是手指抬起了，隐藏部分UI
            {
                if(!nature_refresh) {
                    jump_to_this_position.setVisibility(android.view.View.INVISIBLE);
                    mediaPlayer.seekTo(move_position);//定位到视频某时间位置
                    if (move_position != mediaPlayer.getDuration() && !mediaPlayer.isPlaying())//如果定位到的地方不是视频末尾且当前没在播放
                        mediaPlayer.start();
                    nature_refresh = true;//允许mediaplayer播放自动更新
                }
                brightness_progressbar.setVisibility(android.view.View.INVISIBLE);
                brightness_signal.setVisibility(android.view.View.INVISIBLE);
                volume_progressbar.setVisibility(android.view.View.INVISIBLE);
                volume_signal.setVisibility(android.view.View.INVISIBLE);
            }
            return result;
        }
        //如果是上划下滑事件,越过下面步骤。如果不加这句话，上下滑在 某种契机 下会导致锁栏
        //某种契机：在有进度条的情况下下滑导致进度条不再显示，单点也没用，只有再进行一次反方向上下滑才能重新让进度条显示
        //锁栏：手指单击屏幕却不显示进度条

        if(event.getAction()== android.view.MotionEvent.ACTION_UP)//如果是“手指抬起了”的动作
        {
            //硬修复scroll结束时可能接收不到ACTION_UP动作的bug，即手指划动后手指离开屏幕时，可能这个抬手的动作手势会没与划动衔接，来到这里
            if(!nature_refresh) {
                jump_to_this_position.setVisibility(android.view.View.INVISIBLE);
                mediaPlayer.seekTo(move_position);//定位到视频某时间位置
                if (move_position != mediaPlayer.getDuration() && !mediaPlayer.isPlaying())//如果定位到的地方不是视频末尾且当前没在播放
                    mediaPlayer.start();
                nature_refresh = true;//允许mediaplayer播放自动更新
                return super.onTouchEvent(event);
            }
            if(brightness_progressbar.getVisibility()==android.view.View.VISIBLE||volume_progressbar.getVisibility()==android.view.View.VISIBLE) {
                brightness_progressbar.setVisibility(android.view.View.INVISIBLE);
                brightness_signal.setVisibility(android.view.View.INVISIBLE);
                volume_progressbar.setVisibility(android.view.View.INVISIBLE);
                volume_signal.setVisibility(android.view.View.INVISIBLE);
                return super.onTouchEvent(event);
            }
            //修复结束-===============================================================================================================

            if(firstClick_ACTION_UP==0)//如果没进行首次点击
                firstClick_ACTION_UP = System.currentTimeMillis();//获取当前时间，赋予首次点击点击时间
            else{
                secondClick_ACTION_UP = System.currentTimeMillis();//获取当前时间，赋予最近一次
                if (secondClick_ACTION_UP-firstClick_ACTION_UP<totalTime/2) {//如果两次点击屏幕时间过短
                    if (mediaPlayer.isPlaying())//判断当前是否正在播放
                        mediaPlayer.pause();//暂停播放
                    else
                        mediaPlayer.start();//当前没在播放，开始继续播放
                    firstClick_ACTION_UP = secondClick_ACTION_UP;//将当前点击时间设为最近一次点击时间
                    return super.onTouchEvent(event);
                }
                firstClick_ACTION_UP = secondClick_ACTION_UP;//将当前点击时间设为最近一次点击时间
            }
            if(skbProgress.getVisibility()==android.view.View.VISIBLE)//获取当前进度条状态
            {
                android.widget.TextView pointer=(android.widget.TextView) findViewById(R.id.max_duration);
                pointer.setVisibility(android.view.View.INVISIBLE);
                pointer=(android.widget.TextView) findViewById(R.id.current_time);
                pointer.setVisibility(android.view.View.INVISIBLE);
                android.widget.Button pointer2=(android.widget.Button)findViewById(R.id.video_rate);//调整速率按钮
                pointer2.setVisibility(android.view.View.INVISIBLE);
                skbProgress.setVisibility(android.view.View.INVISIBLE);//隐藏进度条
            }
            else {
                android.widget.TextView pointer=(android.widget.TextView) findViewById(R.id.max_duration);
                pointer.setVisibility(android.view.View.VISIBLE);
                pointer=(android.widget.TextView) findViewById(R.id.current_time);
                pointer.setVisibility(android.view.View.VISIBLE);
                android.widget.Button pointer2=(android.widget.Button)findViewById(R.id.video_rate);//调整速率按钮
                pointer2.setVisibility(android.view.View.VISIBLE);
                skbProgress.setVisibility(android.view.View.VISIBLE);//显示进度条
            }
        }
        return super.onTouchEvent(event);
    }
    //--------------------------------显示、隐藏进度条，暂停继续播放------------------------------
    //----------------------进度条随时间变动----------------------
    static android.widget.SeekBar skbProgress;
    private java.util.Timer mTimer=new java.util.Timer();
    java.util.TimerTask mTimerTask = new java.util.TimerTask() {
        @Override
        public void run() {
            if(mediaPlayer==null)
                return;
            if (mediaPlayer.isPlaying() && skbProgress.isPressed() == false&&nature_refresh) {//如果当前媒体正在播放，且进度条没被手指拖动，且没有手指位移划动快进/后退动作
                handleProgress.sendEmptyMessage(0);
            }
        }
    };
    android.os.Handler handleProgress = new android.os.Handler() {//如果handleProgress前加了static，current_time不会动态及时更新
        public void handleMessage(android.os.Message msg) {
            int position = mediaPlayer.getCurrentPosition();
            int duration = mediaPlayer.getDuration();
            String durationstring=time_switch_int_to_string(duration);// duration / 1000 = "s"
            String currentpositionstring=time_switch_int_to_string(position);

            if (duration > 0) {
                long pos = skbProgress.getMax() * position / duration;
                skbProgress.setProgress((int) pos);//更新进度条上点的位置
                max_duration.setText(durationstring);//更新mediaplayer播放内容总时长
                current_time.setText(currentpositionstring);//实时更新mediaplayer播放的当前时间点
            }
        };
    };

    private static String time_switch_int_to_string(int time)//将数字转换为“00:00:00”字符串形式表示
    {
        String result="";
        if(time / 1000<3600) {
            int minute=time / 1000 / 60;
            int second=(time / 1000) % 60;
            if(minute>=10)
                result = minute+":";
            else
                result ="0"+minute+":";
            if(second>=10)
                result=result+second;
            else
                result=result+"0"+second;
        }
        else {
            int hour    =time / 1000 / 60 / 60;
            int minute  =(time / 1000 - 3600 * (time / 1000 / 60 / 60)) / 60;
            int second  =(time / 1000) % 60;
            if(hour>=10)
                result=hour+":";
            else
                result="0"+hour+":";
            if(minute>=10)
                result=result+minute+":";
            else
                result=result+"0"+minute+":";
            if(second>=10)
                result=result+second;
            else
                result=result+"0"+second;
        }
        return result;

    }
    //----------------------进度条随时间变动----------------------
    //--------------------------------------------进度条seek bar触碰-------------------------------
    private static int progress;
    @Override
    public void onProgressChanged(android.widget.SeekBar seekBar, int progress,boolean fromUser) {
        // 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
        this.progress = progress * mediaPlayer.getDuration()/ seekBar.getMax();
        current_time.setText(time_switch_int_to_string(video_player.progress));//手指拖动进度条时，进度条上的点所移动到的时间点数据同步更新到textview current_time上
    }
    @Override
    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {

    }
    @Override
    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {//进度条拖动结束时
        // seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
        mediaPlayer.seekTo(progress);//定位到视频某时间位置
        if(mediaPlayer.isPlaying()!=true)mediaPlayer.start();
    }
    //--------------------------------------------进度条seek bar触碰-------------------------------



    //----------------------杂项-----------------------------------------------------------------------------------------------
    @Override//通过onPrepared播放
    public void onPrepared(android.media.MediaPlayer arg0) {
        videoWidth = mediaPlayer.getVideoWidth();
        videoHeight = mediaPlayer.getVideoHeight();
        if (videoHeight != 0 && videoWidth != 0) {
            arg0.start();
        }
        add_log("mediaPlayer onPrepared");
    }
    @Override//播放完成时调用,由于stop()会彻底释放资源，所以调用pause()
    public void onCompletion(android.media.MediaPlayer arg0) {
        mediaPlayer.pause(); // TODO Auto-generated method stub
    }
    @Override
    public void onBufferingUpdate(android.media.MediaPlayer arg0, int bufferingProgress) {//更新进度条点所在位置
        skbProgress.setSecondaryProgress(bufferingProgress);
        int currentProgress=skbProgress.getMax()*mediaPlayer.getCurrentPosition()/mediaPlayer.getDuration();
        android.util.Log.e(currentProgress + "% play", bufferingProgress + "% buffer");
    }
    @Override
    public void surfaceChanged(android.view.SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        add_log("mediaPlayer surface changed");
    }
    @Override
    public void surfaceCreated(android.view.SurfaceHolder arg0) {
        try {
            mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnPreparedListener(this);
        } catch (Exception e) {
            add_log("mediaPlayer error");
        }
        add_log("mediaPlayer surface created");
    }
    @Override//销毁surface
    public void surfaceDestroyed(android.view.SurfaceHolder arg0) {
        add_log("mediaPlayer surface destroyed");
    }
    //----------------------杂项-----------------------------------------------------------------------------------------------






    public static String record;
    public static void add_log(String log)//加日志
    {
        /*
        record = log;
        handler.post(new Runnable() {
            @Override
            public void run() {
                android.view.WindowManager wm = (android.view.WindowManager) activity.getSystemService(android.content.Context.WINDOW_SERVICE);

                android.widget.TableLayout table = (android.widget.TableLayout) activity.findViewById(R.id.videolog);
                String history_log = record;
                android.widget.TableRow row = new android.widget.TableRow(activity);
                row.setGravity(android.view.Gravity.CENTER);
                android.widget.TextView log = new android.widget.TextView(activity);
                log.setGravity(android.view.Gravity.CENTER);
                log.setTextSize(22);
                log.setWidth(wm.getDefaultDisplay().getWidth() * 4 / 5);
                log.setText(history_log);
                log.setTextColor(android.graphics.Color.rgb(0, 0, 255));
                row.addView(log);
                table.addView(row, new android.widget.TableLayout.LayoutParams(android.widget.TableLayout.LayoutParams.MATCH_PARENT,android.widget.TableLayout.LayoutParams.WRAP_CONTENT));
                telecom.tsleep(200);
            }
        });

        android.widget.ScrollView history_scroll = (android.widget.ScrollView) activity.findViewById(R.id.scrollView3);
        history_scroll.post(new Runnable() {
            public void run() {
                android.widget.ScrollView history_scroll = (android.widget.ScrollView) activity.findViewById(R.id.scrollView3);
                history_scroll.scrollBy(0, 10000);
            }
        });
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_video_player, menu);
        //return true;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return true;/*
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);*/
    }
}
