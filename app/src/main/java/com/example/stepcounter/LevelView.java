package com.example.stepcounter;

import android.content.Context;
import android.view.View;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Vibrator;
import android.util.AttributeSet;
import org.xml.sax.Attributes;
import com.example.stepcounter.R;
import com.example.stepcounter.LevelView;
import com.example.stepcounter.MainActivity;
public class LevelView extends View {
    private float mLimitRadius = 0;//最大圆的半径
    private float mBubbleRadius;//气泡的半径
    private int mLimitColor;//最大限制圆的颜色
    private float mLimitCircleWidth;//限制圆的宽度
    private int mBubbleRuleColor;//气泡中心圆的颜色
    private float mBubbleRuleWidth;//气泡中心圆的宽
    private float mBubbleRuleRadius;//气泡中心圆的半径
    private int mHorizontalColor;//检测水平后的颜色
    private int mBubbleColor;//气泡的颜色
    private Paint mBubblePaint;
    private Paint mLimitPaint;
    private Paint mBubbleRulePaint;
    private PointF centerPnt = new PointF();//中心点的坐标
    private PointF bubblePoint;//计算后的气泡坐标点
    private double pitchAngle = -90;
    private double rollAngle = -90;
    private Vibrator vibrator;
    long lasttime;//上一个时间点

    public LevelView(Context context) {
        super(context);
        init(null, 0);
    }
    public LevelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LevelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    //初始化
    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.LevelView , defStyle, 0); //R.styleable.LevelView
        mBubbleRuleColor = a.getColor(R.styleable.LevelView_bubbleRuleColor, mBubbleRuleColor);
        mBubbleColor = a.getColor(R.styleable.LevelView_bubbleColor, mBubbleColor);
        mLimitColor = a.getColor(R.styleable.LevelView_limitColor, mLimitColor);
        mHorizontalColor = a.getColor(R.styleable.LevelView_horizontalColor, mHorizontalColor);

        mLimitRadius = a.getDimension(R.styleable.LevelView_limitRadius, mLimitRadius);
        mBubbleRadius = a.getDimension(R.styleable.LevelView_bubbleRadius, mBubbleRadius);
        mLimitCircleWidth = a.getDimension(R.styleable.LevelView_limitCircleWidth, mLimitCircleWidth);

        mBubbleRuleWidth = a.getDimension(R.styleable.LevelView_bubbleRuleWidth, mBubbleRuleWidth);

        mBubbleRuleRadius = a.getDimension(R.styleable.LevelView_bubbleRuleRadius, mBubbleRuleRadius);

        a.recycle();

        mBubblePaint = new Paint();

        mBubblePaint.setColor(mBubbleColor);
        mBubblePaint.setStyle(Paint.Style.FILL);
        mBubblePaint.setAntiAlias(true);

        mLimitPaint = new Paint();

        mLimitPaint.setStyle(Paint.Style.STROKE);
        mLimitPaint.setColor(mLimitColor);
        mLimitPaint.setStrokeWidth(mLimitCircleWidth);

        mLimitPaint.setAntiAlias(true);//抗锯齿

        mBubbleRulePaint = new Paint();
        mBubbleRulePaint.setColor(mBubbleRuleColor);
        mBubbleRulePaint.setStyle(Paint.Style.STROKE);
        mBubbleRulePaint.setStrokeWidth(mBubbleRuleWidth);
        mBubbleRulePaint.setAntiAlias(true);

        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        calculateCenter(widthMeasureSpec, heightMeasureSpec);//计算出中心点
    }

    private void calculateCenter(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.UNSPECIFIED);//宽度

        int height = MeasureSpec.makeMeasureSpec(heightMeasureSpec, MeasureSpec.UNSPECIFIED);//高度

        int center = Math.min(width, height) / 2;//中心点

        centerPnt.set(center, center);//设置中心点
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean isCenter = isCenter(bubblePoint);//是否是中心点的判断
        int limitCircleColor = isCenter ? mHorizontalColor : mLimitColor;//设置大圈的颜色
        int bubbleColor = isCenter ? mHorizontalColor : mBubbleColor;//设置气泡的颜色

        long currentTime = System.currentTimeMillis();//得到现在的系统时间
        long timeInterval = currentTime - lasttime;//计算得出时间间隔
        /*if (timeInterval < 100) return;
        lasttime = currentTime;
        //水平时振动
        if(isCenter){
            vibrator.vibrate(500);
        }*/

        //经过一个时间间隔再进行判断
        if(timeInterval>=200)
        {
            lasttime = currentTime;
            if(isCenter){
                vibrator.vibrate(500);
            }
        }

        mBubblePaint.setColor(bubbleColor);
        mLimitPaint.setColor(limitCircleColor);

        canvas.drawCircle(centerPnt.x, centerPnt.y, mBubbleRuleRadius, mBubbleRulePaint);//画出中心圈
        canvas.drawCircle(centerPnt.x, centerPnt.y, mLimitRadius, mLimitPaint);//画出大限制圈
        drawBubble(canvas);
    }

    //判断是否在中心
    private boolean isCenter(PointF bubblePoint){
        if(bubblePoint == null){
            return false;
        }
        return Math.abs(bubblePoint.x - centerPnt.x) < 2.5 && Math.abs(bubblePoint.y - centerPnt.y) < 2.5;
    }

    private void drawBubble(Canvas canvas) {
        if(bubblePoint != null){
            canvas.drawCircle(bubblePoint.x, bubblePoint.y, mBubbleRadius, mBubblePaint);
        }
    }

    private PointF convertCoordinate(double rollAngle, double pitchAngle, double radius){
        double scale = radius / Math.toRadians(90);

        //x0 y0是以圆心为原点，使用弧度表示的坐标
        double x0 = -(rollAngle * scale);
        double y0 = -(pitchAngle * scale);

        //使用屏幕坐标表示气泡点
        double x = centerPnt.x - x0;
        double y = centerPnt.y - y0;

        return new PointF((float)x, (float)y);
    }

    public void setAngle(double rollAngle, double pitchAngle) {

        this.pitchAngle = pitchAngle;
        this.rollAngle = rollAngle;

        float limitRadius = mLimitRadius - mBubbleRadius;

        bubblePoint = convertCoordinate(rollAngle, pitchAngle, mLimitRadius);
        outLimit(bubblePoint, limitRadius);

        //当坐标超出了最大圆时，取最大圆上的点
        if(outLimit(bubblePoint, limitRadius)){
            onCirclePoint(bubblePoint, limitRadius);
        }
        invalidate();
    }


    private boolean outLimit(PointF bubblePnt, float limitRadius){
        float cSqrt = (bubblePnt.x - centerPnt.x)*(bubblePnt.x - centerPnt.x) + (centerPnt.y - bubblePnt.y) * + (centerPnt.y - bubblePnt.y);

        if(cSqrt - limitRadius * limitRadius > 0){
            return true;
        }
        return false;
    }


    private PointF onCirclePoint(PointF bubblePnt, double limitRadius) {
        double azimuth = Math.atan2((bubblePnt.y - centerPnt.y), (bubblePnt.x - centerPnt.x));
        azimuth = azimuth < 0 ? 2 * Math.PI + azimuth : azimuth;

        double x1 = centerPnt.x + limitRadius * Math.cos(azimuth);
        double y1 = centerPnt.y + limitRadius * Math.sin(azimuth);

        bubblePnt.set((float) x1, (float) y1);

        return bubblePnt;
    }

    public double getPitchAngle(){
        return this.pitchAngle;
    }

    public double getRollAngle(){
        return this.rollAngle;
    }
}
