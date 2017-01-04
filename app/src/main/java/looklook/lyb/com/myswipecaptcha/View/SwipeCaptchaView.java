package looklook.lyb.com.myswipecaptcha.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import java.util.Random;

import looklook.lyb.com.myswipecaptcha.R;

import static android.content.ContentValues.TAG;

/**
 * Created by 10400 on 2017/1/4.
 */

public class SwipeCaptchaView extends ImageView {
    //验证码的左上角(起点)的x y
    private int mCaptchaX;
    private int mCaptchaY;
    //验证码滑块的宽高
    private int mCaptchaWidth;
    private int mCaptchaHeight;
    //验证的误差允许值
    private float mMatchDeviation;
    private Random mRandom;
    private Paint mPaint;

    //控件的宽高
    protected int mWidth;
    protected int mHeight;

    //验证码 阴影、抠图的Path
    private Path mCaptchaPath;
    //用于绘制阴影的Paint
    private Paint mMaskShadowPaint;
    private Bitmap mMaskShadowBitmap;
    private PorterDuffXfermode mPorterDuffXfermode;
    //滑块Bitmap
    private Bitmap mMaskBitmap;
    private Paint mMaskPaint;
    private int mDragerOffset;

    private ValueAnimator mSuccessAnim;
    //验证失败的闪烁动画
    private ValueAnimator mFailAnim;

    private int mSuccessAnimOffset;//动画的offset
    private boolean isShowSuccessAnim;

    private Paint mSuccessPaint;//画笔
    private Path mSuccessPath;//成功动画 平行四边形Path
    private boolean isDrawMask;
    //是否处于验证模式，在验证成功后 为false，其余情况为true
    private boolean isMatchMode;


    public SwipeCaptchaView(Context context) {
        this(context, null);
    }

    public SwipeCaptchaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeCaptchaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }


    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        //px->sp
        int defaultSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        //默认宽高
        mCaptchaHeight = defaultSize;
        mCaptchaWidth = defaultSize;
        mMatchDeviation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                3, getResources().getDisplayMetrics());
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SwipeCaptchaView, defStyleAttr, 0);
        int n = ta.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = ta.getIndex(i);
            if (attr == R.styleable.SwipeCaptchaView_captchaHeight) {
                mCaptchaHeight = (int) ta.getDimension(attr, defaultSize);
            } else if (attr == R.styleable.SwipeCaptchaView_captchaWidth) {
                mCaptchaWidth = (int) ta.getDimension(attr, defaultSize);
            } else if (attr == R.styleable.SwipeCaptchaView_matchDeviation) {
                //误差值
                mMatchDeviation = ta.getDimension(attr, mMatchDeviation);
            }
        }
        ta.recycle();


        //抗锯齿
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(0x77000000);
        mPaint.setMaskFilter(new BlurMaskFilter(20, BlurMaskFilter.Blur.SOLID));

        mRandom = new Random(System.nanoTime());

        //滑块区域
        mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        mMaskPaint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);


        mMaskShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mMaskShadowPaint.setColor(Color.BLACK);
        mMaskShadowPaint.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.SOLID));


        mCaptchaPath = new Path();

        System.out.println("SwipeCaptchaView.init");
    }

    /**
     * 校验
     */
    public void matchCaptcha(){
        if(onCaptchaMatchCallback!=null){
            if(Math.abs(mDragerOffset-mCaptchaX)<mMatchDeviation){
//                onCaptchaMatchCallback.matchSuccess();
                mSuccessAnim.start();
            }else {
                //失败
//                onCaptchaMatchCallback.matchFailed();
                mFailAnim.start();
            }
            Log.e(TAG, "matchCaptcha: "+mDragerOffset+"  |"+mCaptchaX+" "+mMatchDeviation);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;

        //动画区域 会用到宽高
        createMatchAnim();

        post(new Runnable() {
            @Override
            public void run() {
                createCaptcha();
            }
        });

        System.out.println("SwipeCaptchaView.onSizeChanged");
    }

    private void createMatchAnim() {
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        mSuccessAnim=ValueAnimator.ofInt(mWidth+width,0);//从大到小
        mSuccessAnim.setDuration(500);
        mSuccessAnim.setInterpolator(new FastOutLinearInInterpolator());
        mSuccessAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mSuccessAnimOffset= (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        mSuccessAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isShowSuccessAnim=true;
                isDrawMask=false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isShowSuccessAnim=false;
                onCaptchaMatchCallback.matchSuccess();
                isMatchMode=false;
            }
        });
        mSuccessPaint = new Paint();
        mSuccessPaint.setShader(new LinearGradient(0,0,width/2*3,
                mHeight,new int[]{0x00ffffff, 0x88ffffff}
        ,new float[]{0,0.5f}, Shader.TileMode.MIRROR));
        //模仿斗鱼 是一个平行四边形滚动过去
        mSuccessPath=new Path();
        mSuccessPath.moveTo(0,0);
        mSuccessPath.rLineTo(width,0);
        mSuccessPath.rLineTo(width/2,mHeight);
        mSuccessPath.rLineTo(-width,0);
        mSuccessPath.close();

        mFailAnim=ValueAnimator.ofFloat(0,1);
        mFailAnim.setDuration(100)
                .setRepeatCount(4);
        mFailAnim.setRepeatMode(ValueAnimator.REVERSE);
        mFailAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                onCaptchaMatchCallback.matchFailed();
            }
        });
        mFailAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue= (float) animation.getAnimatedValue();
                Log.e(TAG, "onAnimationUpdate: "+animatedValue );
                if(animatedValue<0.5f){
                    isDrawMask=false;
                }else{
                    isDrawMask=true;
                }
                invalidate();

            }
        });

    }

    private void createCaptcha() {
        if(getDrawable()!=null){
            isMatchMode = true;
            createCaptchaPath();
            craeteMask();
            invalidate();
        }
    }

    private void craeteMask() {
        mMaskBitmap=getMaskBitmap(((BitmapDrawable)getDrawable()).getBitmap(),mCaptchaPath);
        //滑块阴影
        mMaskShadowBitmap=mMaskBitmap.extractAlpha();
        //拖动的位移重置
        mDragerOffset = 0;

        isDrawMask=true;

    }

    /**
     *
     * @param bitmap 原图
     * @param captchaPath 画笔
     * @return
     */
    //抠图
    private Bitmap getMaskBitmap(Bitmap bitmap, Path captchaPath) {
        Bitmap tempBitmap=Bitmap.createBitmap(mWidth,mHeight, Bitmap.Config.ARGB_8888);
//
        Canvas mCanvas=new Canvas(tempBitmap);
        ////绘制用于遮罩的圆形
        mCanvas.drawPath(captchaPath,mMaskPaint);
        //设置遮罩模式(图像混合模式)
        mMaskPaint.setXfermode(mPorterDuffXfermode);
        mCanvas.drawBitmap(bitmap,getImageMatrix(),mMaskPaint);
        mMaskPaint.setXfermode(null);
        return tempBitmap;
    }

    private void createCaptchaPath() {
        //圆的直径
        int gap=mCaptchaWidth/3;
        mCaptchaX = mRandom.nextInt(mWidth - mCaptchaWidth);
        mCaptchaY = mRandom.nextInt(mHeight - mCaptchaHeight);

        mCaptchaPath.reset();
        mCaptchaPath.moveTo(mCaptchaX, mCaptchaY);//左上角
        mCaptchaPath.lineTo(mCaptchaX+gap,mCaptchaY);
        //draw一个随机凹凸的圆
        DrawHelperUtils.drawPartCircle(new PointF(mCaptchaX+gap,mCaptchaY),
                new PointF(mCaptchaX+gap*2,mCaptchaY),mCaptchaPath,mRandom.nextBoolean());


        mCaptchaPath.lineTo(mCaptchaX + mCaptchaWidth, mCaptchaY);//右上角
        mCaptchaPath.lineTo(mCaptchaX+ mCaptchaWidth,mCaptchaY+gap);
        //draw一个随机凹凸的圆
        DrawHelperUtils.drawPartCircle(new PointF(mCaptchaX+mCaptchaWidth,mCaptchaY+gap),
                new PointF(mCaptchaX+mCaptchaWidth,mCaptchaY+gap*2),mCaptchaPath,mRandom.nextBoolean());



        mCaptchaPath.lineTo(mCaptchaX + mCaptchaWidth, mCaptchaY + mCaptchaHeight);//右下角
        mCaptchaPath.lineTo(mCaptchaX+ mCaptchaWidth-gap,mCaptchaY + mCaptchaHeight);
        //draw一个随机凹凸的圆
        DrawHelperUtils.drawPartCircle(new PointF(mCaptchaX+ mCaptchaWidth-gap,mCaptchaY + mCaptchaHeight),
                new PointF(mCaptchaX+mCaptchaWidth-gap*2,mCaptchaY + mCaptchaHeight),mCaptchaPath,mRandom.nextBoolean());





        mCaptchaPath.lineTo(mCaptchaX, mCaptchaY + mCaptchaHeight);//左下角
        mCaptchaPath.lineTo(mCaptchaX,mCaptchaY + mCaptchaHeight-gap);
        //draw一个随机凹凸的圆
        DrawHelperUtils.drawPartCircle(new PointF(mCaptchaX,mCaptchaY + mCaptchaHeight-gap),
                new PointF(mCaptchaX,mCaptchaY + mCaptchaHeight-gap*2),mCaptchaPath,mRandom.nextBoolean());



        mCaptchaPath.close();

        System.out.println("SwipeCaptchaView.createCaptchaPath");

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isMatchMode){
            if (mCaptchaPath != null) {
                canvas.drawPath(mCaptchaPath, mPaint);
            }
            //验证成功，白光扫过的动画，这一块动画感觉不完美，有提高空间
            if(isShowSuccessAnim){
                canvas.translate(mSuccessAnimOffset,0);
                canvas.drawPath(mSuccessPath,mSuccessPaint);
            }
            if(mMaskBitmap!=null && mMaskShadowBitmap!=null &&
                    isDrawMask){
                //绘制阴影
                canvas.drawBitmap(mMaskShadowBitmap,-mCaptchaX+mDragerOffset,0
                        ,mMaskShadowPaint);
                //绘制图片
                canvas.drawBitmap(mMaskBitmap,-mCaptchaX+mDragerOffset,
                        0,null);
            }
        }
    }

    public void setCurrentSwipteVaule(int value){
        mDragerOffset=value;
        invalidate();
    }

    //最大可滑动值
    public int getMaxSwipeValue(){
        return mWidth-mCaptchaWidth;
    }

    //重置验证码
    public void refresh(){
        createCaptcha();
    }

    /**
     * 重置验证码滑动距离,(一般用于验证失败)
     */
    public void resetCaptcha() {
        mDragerOffset = 0;
        invalidate();
    }

    /**
     * 验证码验证的回调
     */
    private OnCaptchaMatchCallback onCaptchaMatchCallback;

    public void setOnCaptchaMatchCallback(OnCaptchaMatchCallback onCaptchaMatchCallback) {
        this.onCaptchaMatchCallback = onCaptchaMatchCallback;
    }

    public interface OnCaptchaMatchCallback{
        void matchSuccess();
        void matchFailed();
    }

}
