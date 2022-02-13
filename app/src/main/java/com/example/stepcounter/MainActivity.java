package com.example.stepcounter;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener,View.OnClickListener{
    //public static Class<?> calss;
    final int valueNum=5;
    float [] tempValue=new float[valueNum];//用于存放阈值的波峰波谷插值
    int tempCount=0;
    boolean isDirectionUp=false;//是否上升的标志位
    int continueUpCount=0;//持续上升的次数
    int continueUpFormerCount=0;//上一点的持续上升的次数，为了记录波峰的上升次数
    boolean lastStatus=false;//上一点的状态，上升还是下降
    float peak=0;//波峰值
    float valley=0;//波谷值
    long timeOfThisPeak=0;//本次波峰时间
    long timeOfLastPeak=0;//上次波峰的时间
    long timeOfNow=0;//当前时间
    float gravityOld=0;//上次传感器的值
    final float initialValue=(float)1.7;//初始阈值
    float ThreadValue=(float)2.0;//动态阈值需要动态的数据，这个值用于这些动态数据的阈值

    //初始范围
    float minValue=11f;
    float maxValue=19.6f;

    int CountTimeState=0;//0-准备计时，1-计时中，2-正常计步中
    int CurrentStep=0;//记录当前的步数
    int TempStep=0;//记录临时的步数
    int lastStep=-1;//记录上一次临时的步数
    float average=0;//用x,y,z轴三个维度计算平均值
    Timer timer;//倒计时3.5s，3.5s内不会显示计步，用于屏蔽细微波动
    long duration=3500;//计时器持续时间
    TimeCount timecount;

    float msteps=0;//计步传感器的步数
    SensorManager mSensorManager;
    Sensor stepCounter;
    TextView steps;
    TextView time;
    TextView steps2;
    TextView degrees;
    Button clear;
    Button mybutton0;
    Button bt;
    ImageView image;
    float currentDegree=0f;

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    public LocationClient mLocationClient;
    public BDLocationListener myListener = new MyLocationListener();
    private LatLng latLng;
    private boolean isFirstLoc = true; // 是否首次定位

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        List<String> permissionList=new ArrayList<>();//权限申请列表
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACTIVITY_RECOGNITION))//行为识别权限
        {
            permissionList.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION))//粗略位置信息权限
        {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE))//WIFI状态权限
        {
            permissionList.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE))//网络状态权限
        {
            permissionList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION))//精确位置权限
        {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE))//读取手机状态权限
        {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CHANGE_NETWORK_STATE))//改变网络状态权限
        {
            permissionList.add(Manifest.permission.CHANGE_NETWORK_STATE);
        }
        if (PackageManager.PERMISSION_GRANTED!= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE))//读写数据权限
        {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions =permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
        setContentView(R.layout.activity_main);
        mSensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        steps=(TextView)findViewById(R.id.step);
        steps2=(TextView)findViewById(R.id.step2);
        time=(TextView)findViewById(R.id.time);
        degrees=(TextView)findViewById(R.id.degree);
        image=(ImageView)findViewById(R.id.compass);
        clear=(Button)findViewById(R.id.clear);
        clear.setOnClickListener(this);
        mybutton0=(Button)findViewById(R.id.jump);
        mybutton0.setOnClickListener(this);
        new TimeThread().start();
        mMapView = (MapView) findViewById(R.id.bmapView);
        bt = (Button) findViewById(R.id.bt);
        bt.setOnClickListener(this);
        initMap();
    }

    private void initMap() {
        //获取地图控件引用
        mBaiduMap = mMapView.getMap();
        //普通地图
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setTrafficEnabled(true);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        mLocationClient = new LocationClient(getApplicationContext());     //实例化
        //进行参数配置
        initLocation();
        mLocationClient.registerLocationListener(myListener);    //注册监听函数
        //开启定位
        mLocationClient.start();
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                if (location.getLocType() == BDLocation.TypeGpsLocation) {
                    // GPS定位结果
                    Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                    // 网络定位结果
                    Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                    // 离线定位结果
                    Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    Toast.makeText(MainActivity.this, "服务器错误!", Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    Toast.makeText(MainActivity.this, "网络错误，请检查网络连接！", Toast.LENGTH_SHORT).show();
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    Toast.makeText(MainActivity.this, "手机模式错误，请检查是否处于飞行模式！", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); //高精度定位模式，同时使用gps和网络定位并返回更准确的版本
        option.setCoorType("bd09ll");    //设置返回定位坐标结果坐标系
        option.setScanSpan(1000);        //1s一次间隔的周期定位
        option.setIsNeedAddress(true);   //需要语义化结果
        option.setOpenGps(true);         //启动gps
        option.setLocationNotify(true);   //当gps有效时按照设定的周期频率输出GPS结果
        mLocationClient.setLocOption(option); //启用以上设置
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt:
                //定位点至于屏幕中央
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                break;
            case R.id.clear:
                tempValue=new float[valueNum];//用于存放阈值的波峰波谷插值
                tempCount=0;
                isDirectionUp=false;//是否上升的标志位
                continueUpCount=0;//持续上升的次数
                continueUpFormerCount=0;//上一点的持续上升的次数，为了记录波峰的上升次数
                lastStatus=false;//上一点的状态，上升还是下降
                peak=0;//波峰值
                valley=0;//波谷值
                timeOfThisPeak=0;//本次波峰时间
                timeOfLastPeak=0;//上次波峰的时间
                timeOfNow=0;//当前时间
                gravityOld=0;//上次传感器的值
                ThreadValue=(float)2.0;//动态阈值需要动态的数据，这个值用于这些动态数据的阈值
                CountTimeState=0;//0-准备计时，1-计时中，2-正常计步中
                CurrentStep=0;//记录当前的步数
                TempStep=0;//记录临时的步数
                lastStep=-1;//记录上一次临时的步数
                steps2.setText("你当前已经走了"+CurrentStep+"步");
                break;
            case R.id.jump:
                Intent intent=new Intent(MainActivity.this,Main2Activity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    protected  void onResume(){
        super.onResume();
        mMapView.onResume();
        stepCounter=mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //找到计步传感器
        if(stepCounter!=null) {
            mSensorManager.registerListener((SensorEventListener) this,stepCounter,SensorManager.SENSOR_DELAY_GAME);
        }
        else{
            steps.setText("该手机不支持计步传感器");
        }
        stepCounter=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //找到加速度传感器
        if(stepCounter!=null) {
            mSensorManager.registerListener((SensorEventListener) this,stepCounter,SensorManager.SENSOR_DELAY_GAME);
        }
        else{
            steps2.setText("该手机不支持加速度传感器");
        }
        stepCounter=mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this,stepCounter,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        mSensorManager.unregisterListener(this);
        mMapView.onPause();
        super.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensortype=event.sensor.getType();
        if(sensortype==Sensor.TYPE_STEP_COUNTER){
            msteps=event.values[0];
            steps.setText("你开机以来共走了"+String.valueOf((int)msteps)+"步");
        }
        if(sensortype==Sensor.TYPE_ORIENTATION){
            float degree = event.values[0]; //获取z转过的角度
            //旋转动画
            RotateAnimation ra = new RotateAnimation(currentDegree,-degree, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            ra.setDuration(100);//动画持续时间
            ra.setFillAfter(true);//动画结束后停留在动画的结束位置
            image.startAnimation(ra);
            currentDegree = -degree;
            if(event.values[0]>=355||event.values[0]<=5)
                degrees.setText("正北方向");
            else if(event.values[0]>=85&&event.values[0]<=95)
                degrees.setText("正东方向");
            else if(event.values[0]>=175&&event.values[0]<=185)
                degrees.setText("正南方向");
            else if(event.values[0]>=265&&event.values[0]<=275)
                degrees.setText("正西方向");
            else if(event.values[0]>5&&event.values[0]<85)
                degrees.setText("北偏东"+String.valueOf((int)event.values[0])+"°");
            else if(event.values[0]>95&&event.values[0]<175)
                degrees.setText("南偏东"+String.valueOf(180-(int)event.values[0])+"°");
            else if(event.values[0]>185&&event.values[0]<265)
                degrees.setText("南偏西"+String.valueOf((int)event.values[0]-180)+"°");
            else if(event.values[0]>275&&event.values[0]<355)
                degrees.setText("北偏西"+String.valueOf(360-(int)event.values[0])+"°");
        }
        synchronized (this){
            if(sensortype==Sensor.TYPE_ACCELEROMETER){
                calc_step(event);
                //steps2.setText("你当前已经走了"+average+"步");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class TimeThread extends Thread {

        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1;  //消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }

    };
    //在主线程里面处理消息并更新UI界面
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    long sysTime = System.currentTimeMillis();//获取系统时间
                    CharSequence sysTimeStr = DateFormat.format("hh:mm:ss", sysTime);//时间显示格式
                    time.setText("当前时间："+sysTimeStr); //更新时间
                    break;
                default:
                    break;

            }
        }

    };

    synchronized private void calc_step(SensorEvent event){
        //算出加速度传感器的x、y、z三轴的平均数值（为了平衡在某一个方向数值过大造成的数据误差）
        average=(float)Math.sqrt(Math.pow(event.values[0],2) +Math.pow(event.values[1],2)+Math.pow(event.values[2],2));
        detectorNewStep(average);
    }

    private void detectorNewStep(float values){
        if(gravityOld==0)//第一次进行探测
        {
            gravityOld=values;
        }
        else{
            if(DetectorPeak(values,gravityOld))//检测发现是波峰
            {
                timeOfLastPeak=timeOfThisPeak;
                timeOfNow=System.currentTimeMillis();

                if(timeOfNow-timeOfLastPeak>=200&&(peak-valley>=ThreadValue) &&(timeOfNow-timeOfLastPeak)<=2000)//两次的时间差在200到2000毫秒之间并且波峰与波谷的差大于阈值就判定为一步，更新这次波峰的时间
                {
                    timeOfThisPeak=timeOfNow;
                    preStep();
                }
                if(timeOfNow-timeOfLastPeak>=200 &&(peak-valley>=initialValue))//符合时间差条件，但波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
                {
                    timeOfThisPeak=timeOfNow;
                    ThreadValue=Peak_Valley_Thread(peak-valley);
                }
            }
        }
        gravityOld=values;
    }

    /**
     * 监测波峰
     * 以下四个条件判断为波峰
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于1.2g,小于2g
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰作对比
     */
    public boolean DetectorPeak(float newValue,float oldValue){
        lastStatus=isDirectionUp;
        if(newValue>=oldValue){
            isDirectionUp=true;
            continueUpCount++;
        }else{
            continueUpFormerCount=continueUpCount;
            continueUpCount=0;
            isDirectionUp=false;
        }
        if(!isDirectionUp&&lastStatus&&(continueUpFormerCount>=2&&(oldValue>=minValue&&oldValue<maxValue)))//满足上面波峰的四个条件，此时为波峰状态
        {
            peak=oldValue;
            return true;
        }
        else if(!lastStatus&&isDirectionUp)//满足波谷条件，此时为波谷状态
        {
            valley=oldValue;
            return false;
        }
        else{
            return false;
        }
    }

    private void preStep(){
        if(CountTimeState==0)//开启计时器(倒计时3.5秒,倒计时时间间隔为1秒)，在3.5秒内每1秒去监测一次。
        {
            timecount=new TimeCount(duration,1000);
            timecount.start();
            CountTimeState=1;  //计时中
        }
        else if(CountTimeState==1)//如果传感器测得的数据满足走一步的条件则步数加1
        {
            TempStep++;
        }
        else if(CountTimeState==2){
            CurrentStep++;
            steps2.setText("你当前已经走了"+CurrentStep+"步");
        }
    }

    /**
     * 动态生成阈值，阈值是为了跟波峰与波谷的差值进行比较，进而判断是否为1步。
     * 1.通过波峰波谷的差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中
     * 3.再将数组传入函数averageValue中计算阈值
     */
    public float Peak_Valley_Thread(float value){
        float tempThread=ThreadValue;
        if(tempCount<valueNum){
            tempValue[tempCount]=value;
            tempCount++;
        }
        else{
            //此时tempCount=valueNum=5
            tempThread=averageValue(tempValue,valueNum);
            for(int i=1;i<valueNum;i++){
                tempValue[i-1]=tempValue[i];
            }
            tempValue[valueNum-1]=value;
        }
        return tempThread;
    }

    /**
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     * 这些数据是通过大量的统计得到的
     */
    public float averageValue(float value[],int n){
        float ave=0;
        for(int i=0;i<n;i++){
            ave+=value[i];
        }
        ave=ave/valueNum;  //计算数组均值
        if(ave>=8){
            ave=(float)4.3;
        }
        else if(ave>=7&&ave<8){
            ave=(float)3.3;
        }
        else if(ave>=4&&ave<7){
            ave=(float)2.3;
        }
        else if(ave>=3&&ave<4){
            ave=(float)2.0;
        }
        else{
            ave=(float)1.7;
        }
        return ave;
    }

    class TimeCount extends CountDownTimer{

        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (lastStep == TempStep) {
                //一段时间内，TempStep没有步数增长，则计时停止，同时计步也停止
                timecount.cancel();
                CountTimeState = 0;
                lastStep = -1;
                TempStep = 0;
            }
            else {
                lastStep = TempStep;
            }
        }

        @Override
        public void onFinish() {
            //如果计时器正常结束，则开始计步
            timecount.cancel();
            CurrentStep+=TempStep;
            lastStep=-1;
            timer=new Timer(true);
            TimerTask task=new TimerTask(){
                public void run(){
                    //当步数不再增长的时候停止计步
                    if(lastStep==CurrentStep){
                        timer.cancel();
                        CountTimeState=0;
                        lastStep=-1;
                        TempStep=0;
                    }
                    else{
                        lastStep=CurrentStep;
                    }
                }
            };
            timer.schedule(task,2000,2000);   //每隔2s执行一次，不断监测是否已经停止运动了。
            CountTimeState=2;
        }
    }
}