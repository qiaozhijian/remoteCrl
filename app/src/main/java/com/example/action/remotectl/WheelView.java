package com.example.action.remotectl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author _Andy
 * @version 1.0.0
 * @date 2017年6月8日 下午1:55:59
 * @类说明 自定义全角度方向盘控制控件
 */
@SuppressLint("ClickableViewAccessibility")
public class WheelView extends View implements View.OnTouchListener {

    public WheelView(Context context) {
        super(context);
        initWheelView();
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWheelView();
    }

    public WheelView(Context context, AttributeSet attrs, int paramInt) {
        super(context, attrs, paramInt);
        initWheelView();
    }

    /**
     * 控件中心点的X坐标
     */
    public double centerX = 0.0D;
    /**
     * 控件中心点的Y坐标
     */
    public double centerY = 0.0D;
    /**
     * 控件的半径
     */
    private int joystickRadius;

    /**
     * 整个控件的大圆的画笔
     */
    private Paint mainCircle;
    /**
     * 第二个内圆的画笔
     */
    private Paint secondaryCircle;
    /**
     * 垂直线的画笔
     */
    private Paint verticalLine;
    /**
     * 水平线的画笔
     */
    private Paint horizontalLine;


    /**
     * 初始化画笔的基本样式
     */
    private void initWheelView() {
        this.mainCircle = new Paint(1);
        this.mainCircle.setColor(Color.BLUE);
        this.mainCircle.setStrokeWidth(3.0f);
        this.mainCircle.setStyle(Paint.Style.STROKE);

        this.secondaryCircle = new Paint();
        this.secondaryCircle.setColor(Color.GREEN);
        this.secondaryCircle.setStrokeWidth(3.0f);
        this.secondaryCircle.setStyle(Paint.Style.STROKE);

        this.verticalLine = new Paint();
        this.verticalLine.setStrokeWidth(3.0F);
        this.verticalLine.setColor(Color.RED);

        this.horizontalLine = new Paint();
        this.horizontalLine.setStrokeWidth(3.0F);
        this.horizontalLine.setColor(Color.BLACK);

    }

    /**
     * 重写View中的测量方法
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //取宽高中的小值，作为宽高值
        int i = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
        //将宽高值传给父容器，告诉父容器我需要占用多少宽高
        setMeasuredDimension(i, i);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 测量宽高的方法
     */
    private int measure(int paramInt) {
        int i = View.MeasureSpec.getMode(paramInt);
        int j = View.MeasureSpec.getSize(paramInt);
        if (i == 0)//当模式=0时，表示 android:layout_width="wrap_content"
            return 200;//此时设置默认宽高为200
        return j;
    }

    /**
     * 重写View中的绘制方法
     */
    @Override
    protected void onDraw(Canvas canvas) {
        //获取控件的宽高并取一半作为中心点坐标
        this.centerX = (getWidth() / 2);
        this.centerY = (getHeight() / 2);
        this.joystickRadius = Math.min((int) this.centerX, (int) this.centerY) - 3;

//        canvas.drawColor(Color.YELLOW);//画背景色
//        paint.setUnderlineText(true);//设置文字带下划线

        //用画布画大圆。参数顺序：圆心X坐标，圆心Y坐标，圆半径，画笔样式
        canvas.drawCircle(
                (int) this.centerX,
                (int) this.centerY,
                this.joystickRadius,
                this.mainCircle);// paint
        //用画布画里面的小圆。
        canvas.drawCircle(
                (int) this.centerX,
                (int) this.centerY,
                this.joystickRadius / 2,
                this.secondaryCircle);//secondaryCircle
        //画竖直线
        canvas.drawLine(
                (float) this.centerX,
                (float) (this.centerY - this.joystickRadius + 1),
                (float) this.centerX,
                (float) (this.centerY + this.joystickRadius - 1),
                this.verticalLine);
        //画水平线
        canvas.drawLine(
                (float) (this.centerX - this.joystickRadius + 1),
                (float) this.centerY,
                (float) (this.centerX + this.joystickRadius - 1),
                (float) this.centerY,
                this.horizontalLine);


    }

//****************************分割线**************************************//

    /**
     * 自定义的接口用于监听处理控件的触摸事件
     */
    private OnWheelViewMoveListener onWheelViewMoveListener;

    /**
     * 为接口设置监听器
     *
     * @param listener  回调
     * @param paramLong 监听时间间隔
     */
    public void setOnWheelViewMoveListener(
            OnWheelViewMoveListener listener, long paramLong) {
        this.onWheelViewMoveListener = listener;
        WheelView.loopInterval = paramLong;
    }

    /**
     * 为接口设置监听器,默认时间间隔
     *
     * @param listener 回调
     */
    public void setOnWheelViewMoveListener(
            OnWheelViewMoveListener listener) {
        this.onWheelViewMoveListener = listener;
    }

    /**
     * 手指点击的位置X坐标
     */
    private int position_X = 0;
    /**
     * 手指点击的位置Y坐标
     */
    private int position_Y = 0;
    /**
     * 当前手指位置所在的角度
     */
    private int lastAngle = 0;
    /**
     * 当前手指位置与中心的距离
     */
    private int lastDistance = 0;

    /**
     * 实现事件监听
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        /* 处理(消费掉)这个控件的事件监听 */
        return true;
    }

    /**
     * 处理监听事件
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //获取手触碰的坐标位置
        this.position_X = (int) event.getX();
        this.position_Y = (int) event.getY();
        /** 手机点击的位置与控件中心的距离 */
        double d = Math.sqrt(//X坐标的平方+Y坐标的平方再开平方
                Math.pow(this.position_X - this.centerX, 2) +
                        Math.pow(this.position_Y - this.centerY, 2));

        if (d > this.joystickRadius) {
            this.position_X = (int) ((this.position_X - this.centerX)
                    * this.joystickRadius / d + this.centerX);
            this.position_Y = (int) ((this.position_Y - this.centerY)
                    * this.joystickRadius / d + this.centerY);
        }
        invalidate();//再重新绘制
        if ((this.onWheelViewMoveListener != null) &&
                (event.getAction() == MotionEvent.ACTION_UP)) {
            this.position_X = (int) this.centerX;
            this.position_Y = (int) this.centerY;
            this.thread.interrupt();//手指抬起时中断，并做一次位置和方向的监听
            if (this.onWheelViewMoveListener != null)
                this.onWheelViewMoveListener.onValueChanged(getAngle(), getDistance());
        }
        if ((this.onWheelViewMoveListener != null) &&
                (event.getAction() == MotionEvent.ACTION_DOWN)) {
            if ((this.thread != null) && (this.thread.isAlive()))
                this.thread.interrupt();
            this.thread = new Thread(runnable);
            this.thread.start();//手指按下开始
            if (this.onWheelViewMoveListener != null)
                //自定义接口处理触摸事件
                this.onWheelViewMoveListener.onValueChanged(getAngle(), getDistance());
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 根据所处的位置计算得到角度
     */
    private int getAngle() {
        if (this.position_X > this.centerX) {
            if (this.position_Y < this.centerY) {//右上角，第一象限
                int m = (int) (90.0D + 57.295779500000002D * Math
                        .atan((this.position_Y - this.centerY)
                                / (this.position_X - this.centerX)));
                this.lastAngle = m;
                return m;
            }
            if (this.position_Y > this.centerY) {//右下角，第二象限
                int k = 90 + (int) (57.295779500000002D * Math
                        .atan((this.position_Y - this.centerY)
                                / (this.position_X - this.centerX)));
                this.lastAngle = k;
                return k;
            }
            this.lastAngle = 90;
            return 90;
        }
        if (this.position_X < this.centerX) {
            if (this.position_Y < this.centerY) {//左上角，第三象限
                int j = (int) (57.295779500000002D * Math
                        .atan((this.position_Y - this.centerY)
                                / (this.position_X - this.centerX)) - 90.0D);
                this.lastAngle = j;
                return j;
            }
            if (this.position_Y > this.centerY) {//左下角，第四象限
                int i = -90 + (int) (57.295779500000002D * Math
                        .atan((this.position_Y - this.centerY)
                                / (this.position_X - this.centerX)));
                this.lastAngle = i;
                return i;
            }
            this.lastAngle = -90;
            return -90;
        }
        if (this.position_Y <= this.centerY) {
            this.lastAngle = 0;
            return 0;
        }
        if (this.lastAngle < 0) {
            this.lastAngle = -180;
            return -180;
        }
        this.lastAngle = 180;
        return 180;
    }

    /**
     * 得到所处位置与中心的距离(百分比)
     */
    private int getDistance() {
        this.lastDistance = (int) (100.0D * Math.sqrt(
                Math.pow(this.position_X - this.centerX, 2) +
                        Math.pow(this.position_Y - this.centerY, 2)
        ) / this.joystickRadius);
        return lastDistance;
    }

    /**
     * 监听时间间隔，默认为100毫秒
     */
    private static long loopInterval = 100L;
    /**
     * 手指按下之后的循环监听线程
     */
    private Thread thread;
    /**
     * 事件监听线程的实现
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (Thread.interrupted())
                    return;
                post(new Runnable() {
                    public void run() {
                        if (WheelView.this.onWheelViewMoveListener != null)
                            WheelView.this.onWheelViewMoveListener.onValueChanged(
                                    WheelView.this.getAngle(),
                                    WheelView.this.getDistance());
                    }
                });
                try {
                    Thread.sleep(WheelView.loopInterval);
                } catch (InterruptedException localInterruptedException) {
                }
            }
        }
    };

    /**
     * @author _Andy
     * @version 1.0.0
     * @date 2017年6月9日 上午10:57:49
     * @类说明 自定义事件监听的接口
     */
    public abstract interface OnWheelViewMoveListener {
        /**
         * 接口回调函数
         *
         * @param angle    角度
         * @param distance 距离
         */
        public abstract void onValueChanged(int angle, int distance);
    }

}