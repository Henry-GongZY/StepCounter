package com.example.stepcounter;

import androidx.appcompat.app.AppCompatActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

public class Main2Activity extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private Sensor acc_sensor;
    private Sensor mag_sensor;

    private float[] accValues = new float[3];
    private float[] magValues = new float[3];
    private float r[] = new float[9]; // 旋转矩阵保存磁场和加速度的数据
    private float values[] = new float[3];// 模拟方向传感器的数据
    private LevelView levelView;
    private TextView tvHorz;
    private TextView tvVert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        levelView = (LevelView)findViewById(R.id.gv_hv);

        tvVert = (TextView)findViewById(R.id.tvv_vertical);
        tvHorz = (TextView)findViewById(R.id.tvv_horz);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Button bt=(Button)findViewById(R.id.back);
        bt.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                //Intent i=new Intent(Main2Activity.this,MainActivity.calss);
                //startActivity(i);
                Main2Activity.this.finish();
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();

        acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener((SensorEventListener)this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener((SensorEventListener)this, mag_sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                accValues = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magValues = event.values.clone();
                break;
        }

        SensorManager.getRotationMatrix(r, null, accValues, magValues);
        SensorManager.getOrientation(r, values);

        float azimuth = values[0];
        float pitchAngle = values[1];
        float rollAngle = - values[2];
        onAngleChanged(rollAngle, pitchAngle, azimuth);

    }

    private void onAngleChanged(float rollAngle, float pitchAngle, float azimuth){
        //设置显示角度
        levelView.setAngle(rollAngle, pitchAngle);
        tvHorz.setText(String.valueOf((int)Math.toDegrees(rollAngle)) + "°");
        tvVert.setText(String.valueOf((int)Math.toDegrees(pitchAngle)) + "°");
    }
}
