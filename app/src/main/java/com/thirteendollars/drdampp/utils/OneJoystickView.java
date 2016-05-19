package com.thirteendollars.drdampp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Damian Nowakowski on 17.05.16.
 */
public class OneJoystickView extends View implements Runnable {


    public final static long DEFAULT_LOOP_INTERVAL = 200; // 500 ms


    protected OnJoystickMoveListener onJoystickMoveListener;
    protected Thread thread = new Thread(this);
    protected long loopInterval = DEFAULT_LOOP_INTERVAL;

    private int xPosition = 0; // Touch x position
    private int yPosition = 0; // Touch y position
    private float centerX = 0; // Center view x position
    private float centerY = 0; // Center view y position
    private Paint mainCircle;
    private Paint button;
    private Paint horizontalLine;
    private Paint verticalLine;
    private int joystickRadius;
    private int buttonRadius;



    public OneJoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJoystickViews();
    }

    public OneJoystickView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initJoystickViews();
    }

    protected void initJoystickViews() {
        mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainCircle.setColor(Color.WHITE);
        mainCircle.setStyle(Paint.Style.FILL_AND_STROKE);

        verticalLine = new Paint();
        verticalLine.setStrokeWidth(15);
        verticalLine.setColor(Color.BLACK);

        horizontalLine = new Paint();
        horizontalLine.setStrokeWidth(15);
        horizontalLine.setColor(Color.BLACK);

        button = new Paint(Paint.ANTI_ALIAS_FLAG);
        button.setColor(Color.GRAY);
        button.setStyle(Paint.Style.FILL_AND_STROKE);
    }


    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        // before measure, get the center of views
        xPosition = getWidth() / 2;
        yPosition = getWidth() / 2;
        int d = Math.min(xNew, yNew);
        buttonRadius = (int) (d / 2 * 0.20);
        joystickRadius = (int) (d / 2 * 0.75);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // setting the measured values to resize the view to a certain width and
        // height
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
        setMeasuredDimension(d, d);
    }

    protected int measure(int measureSpec) {
        int result = 0;

        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        centerX = (getWidth()) / 2;
        centerY = (getHeight()) / 2;

        // painting the main circle
        canvas.drawCircle(centerX, centerY, joystickRadius,
                mainCircle);
        // paint lines
        canvas.drawLine( centerX,  centerY,  centerX,
                (centerY - joystickRadius), verticalLine);
        canvas.drawLine( (centerX - joystickRadius),  centerY,
                (centerX + joystickRadius),  centerY,
                horizontalLine);
        canvas.drawLine( centerX,  (centerY + joystickRadius),
                centerX,  centerY, horizontalLine);

        // painting the move button
        canvas.drawCircle(xPosition, yPosition, buttonRadius, button);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        invalidate();

        xPosition = (int) event.getX();
        yPosition = (int) event.getY();

        double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        if (abs > joystickRadius) {
            xPosition = (int) ((xPosition - centerX) * joystickRadius / abs + centerX);
            yPosition = (int) ((yPosition - centerY) * joystickRadius / abs + centerY);
        }


        if (event.getAction() == MotionEvent.ACTION_UP) {
            xPosition = (int) centerX;
            yPosition = (int) centerY;
            thread.interrupt();
            if (onJoystickMoveListener != null)
                sendCurrentControllerPosition();
        }

        if (onJoystickMoveListener != null && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread = new Thread(this);
            thread.start();
            if (onJoystickMoveListener != null)
                sendCurrentControllerPosition();
        }

        return true;
    }


    protected void sendCurrentControllerPosition() {
        int speedValue = (int) ( ( (yPosition-centerY)/joystickRadius ) *100);
        int turnValue = (int) ( ( (xPosition-centerX)/joystickRadius ) *100);
        onJoystickMoveListener.onValueChanged( -speedValue,turnValue );
    }


    public void setOnJoystickMoveListener(OnJoystickMoveListener listener,long repeatInterval) {
        this.onJoystickMoveListener = listener;
        this.loopInterval = repeatInterval;
    }

    public interface OnJoystickMoveListener {
        void onValueChanged(int speed, int turn);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                public void run() {

                    if (onJoystickMoveListener != null){
                        sendCurrentControllerPosition();
                    }
                }
            });
            try {
                Thread.sleep(loopInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}