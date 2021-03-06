package com.zlping.demo.page;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.zlping.demo.R;


public class PageWidget extends View {
    private static final String TAG = "PageWidget";
    private int mWidth = 480;//屏幕宽度
    private int mHeight = 854;//屏幕高度
    private int mCornerX = 0; //拖曳点对应的页脚X轴坐
    private int mCornerY = 0;//拖曳点对应的页脚Y轴坐
    private Path mPath0;
    private Path mPath1;
    Bitmap mCurPageBitmap = null; //当前
    Bitmap mNextPageBitmap = null;//下一

    PointF mTouch = new PointF(); //拖曳
    PointF mBezierStart1 = new PointF(); //贝塞尔曲线起始点
    PointF mBezierControl1 = new PointF(); //贝塞尔曲线控制点
    PointF mBeziervertex1 = new PointF(); //贝塞尔曲线顶
    PointF mBezierEnd1 = new PointF(); //贝塞尔曲线结束点

    PointF mBezierStart2 = new PointF(); //另一个贝塞尔曲线
    PointF mBezierControl2 = new PointF();//另一个贝塞尔曲线控制
    PointF mBeziervertex2 = new PointF();//另一个贝塞尔曲线顶点
    PointF mBezierEnd2 = new PointF();//另一个贝塞尔曲线结束

    float mMiddleX;//直线中点X轴坐
    float mMiddleY;//直线中点Y轴坐
    float mDegrees;//卷起角度
    float mTouchToCornerDis;//直角三角形斜边长
    ColorMatrixColorFilter mColorMatrixFilter;
    Matrix mMatrix;
    float[] mMatrixArray = { 0, 0, 0, 0, 0, 0, 0, 0, 1.0f };

    boolean mIsRTandLB; //是否属于右上左下
    float mMaxLength = (float) Math.hypot(mWidth, mHeight);//斜边长度
    int[] mBackShadowColors;
    int[] mFrontShadowColors;
    GradientDrawable mBackShadowDrawableLR;
    GradientDrawable mBackShadowDrawableRL;
    GradientDrawable mFolderShadowDrawableLR;
    GradientDrawable mFolderShadowDrawableRL;

    GradientDrawable mFrontShadowDrawableHBT;
    GradientDrawable mFrontShadowDrawableHTB;
    GradientDrawable mFrontShadowDrawableVLR;
    GradientDrawable mFrontShadowDrawableVRL;

    Paint mPaint;

    Scroller mScroller;
    boolean mIsFinished = true;
    public PageWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        init(width,height);
    }
    public PageWidget(Context context,int w,int h) {
        super(context);
        init(w,h);
    }
    
    public void init(int w,int h){
        mWidth = w;
        mHeight = h;
        mPath0 = new Path();
        mPath1 = new Path();
        createDrawable();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);

        ColorMatrix cm = new ColorMatrix();
        float array[] = { 0.55f, 0, 0, 0, 80.0f, 0, 0.55f, 0, 0, 80.0f, 0, 0,
                0.55f, 0, 80.0f, 0, 0, 0, 0.2f, 0 };
        cm.set(array);
        mColorMatrixFilter = new ColorMatrixColorFilter(cm);
        mMatrix = new Matrix();
        mScroller = new Scroller(getContext());

        mTouch.x = 0.01f;//不让x,y,否则在点计算时会有问
        mTouch.y = 0.01f;
    }

    /**
     * 计算拖拽点对应的拖拽
     */
    public void calcCornerXY(float x, float y) {
        System.gc();
        if (x <= mWidth / 2)
            mCornerX = 0;
        else
            mCornerX = mWidth;
        if (y <= mHeight / 2)
            mCornerY = 0;
        else
            mCornerY = mHeight;
        if ((mCornerX == 0 && mCornerY == mHeight)
                || (mCornerX == mWidth && mCornerY == 0))
            mIsRTandLB = true;
        else
            mIsRTandLB = false;
    }

    public interface OnOpenMenu{
        public void openMenu();
    }
    private OnOpenMenu onOpenMenu;

    public void setOnOpenMenu(OnOpenMenu onOpenMenu) {
        this.onOpenMenu = onOpenMenu;
    }

    float down_x = 0;
    float down_y = 0;
    public boolean doTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mTouch.x = event.getX();
            mTouch.y = event.getY();
            this.postInvalidate();
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mTouch.x = event.getX();
            mTouch.y = event.getY();
            down_x = event.getX();
            down_y = event.getY();
            calcCornerXY(mTouch.x, mTouch.y);
            mTouchToCornerDis = (float) Math.hypot((mTouch.x - mCornerX),(mTouch.y - mCornerY));
            this.postInvalidate();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.i("ACTION_UP", "event.getX()==down_x:"+event.getX()+"|"+down_x+"||"+(event.getX()<=down_x+1&&event.getX()>=down_x-1));
            Log.i("ACTION_UP", "event.getY()==down_y:"+event.getY()+"|"+down_y+"||"+(event.getY()<=down_y+1&&event.getY()>=down_y-1));
            if(event.getX()<=down_x+3&&event.getX()>=down_x-3&&event.getY()<=down_y+3&&event.getY()>=down_y-3){
                if((event.getX() > mWidth / 3 && event.getX() < mWidth * 2 / 3 && event.getY() > mHeight / 3
                    && event.getY() < mHeight * 2 / 3)){//当点击中间时不翻页，调用回调方法
//                  )||event.getY() > mHeight / 2){//当点击中间时不翻页，调用回调方法
                    if(onOpenMenu!=null){
                        onOpenMenu.openMenu();
                    }
                    mTouch.x = 0.01f;
                    mTouch.y = 0.01f;
                    calcCornerXY(mTouch.x, mTouch.y);
                    mTouchToCornerDis = 0;
                    return true;
                }
            }
            if (canDragOver()) {
                startAnimation(5200);
            } else {
                mTouch.x = mCornerX - 0.09f;
                mTouch.y = mCornerY - 0.09f;
            }
            this.postInvalidate();
        }
        return true;
    }

    /**
     * 求解直线P1P2和直线P3P4的交点坐
     */
    public PointF getCross(PointF P1, PointF P2, PointF P3, PointF P4) {
        PointF CrossP = new PointF();
        // 二元函数通式y=ax+b
        float a1 = (P2.y - P1.y) / (P2.x - P1.x);
        float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);

        float a2 = (P4.y - P3.y) / (P4.x - P3.x);
        float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
        CrossP.x = (b2 - b1) / (a1 - a2);
        CrossP.y = a1 * CrossP.x + b1;
        return CrossP;
    }

    private void calcPoints() {
        mMiddleX = (mTouch.x + mCornerX) / 2;
        mMiddleY = (mTouch.y + mCornerY) / 2;
        mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
                * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
        mBezierControl1.y = mCornerY;
        mBezierControl2.x = mCornerX;
        mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
                * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);

         /*Log.i(TAG, "mTouchX  " + mTouch.x + "  mTouchY  " + mTouch.y);
         Log.i(TAG, "mBezierControl1.x  " + mBezierControl1.x
         + "  mBezierControl1.y  " + mBezierControl1.y);
         Log.i(TAG, "mBezierControl2.x  " + mBezierControl2.x
         + "  mBezierControl2.y  " + mBezierControl2.y);*/

        mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x)
                / 2;
        mBezierStart1.y = mCornerY;

        // 当mBezierStart1.x < 0或mBezierStart1.x > 480
        // 如果继续翻页，会出现BUG故在此限
        if (mTouch.x > 0 && mTouch.x < mWidth) {
            if (mBezierStart1.x < 0 || mBezierStart1.x > mWidth) {
                if (mBezierStart1.x < 0)
                    mBezierStart1.x = mWidth - mBezierStart1.x;

                float f1 = Math.abs(mCornerX - mTouch.x);
                float f2 = mWidth * f1 / mBezierStart1.x;
                mTouch.x = Math.abs(mCornerX - f2);

                float f3 = Math.abs(mCornerX - mTouch.x)
                        * Math.abs(mCornerY - mTouch.y) / f1;
                mTouch.y = Math.abs(mCornerY - f3);

                mMiddleX = (mTouch.x + mCornerX) / 2;
                mMiddleY = (mTouch.y + mCornerY) / 2;

                mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY)
                        * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
                mBezierControl1.y = mCornerY;

                mBezierControl2.x = mCornerX;
                mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX)
                        * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
                /* Log.i(TAG, "mTouchX --> " + mTouch.x + "  mTouchY-->  "
                 + mTouch.y);
                 Log.i(TAG, "mBezierControl1.x--  " + mBezierControl1.x
                 + "  mBezierControl1.y -- " + mBezierControl1.y);
                 Log.i(TAG, "mBezierControl2.x -- " + mBezierControl2.x
                 + "  mBezierControl2.y -- " + mBezierControl2.y);*/
                mBezierStart1.x = mBezierControl1.x
                        - (mCornerX - mBezierControl1.x) / 2;
            }
        }
        mBezierStart2.x = mCornerX;
        mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y)
                / 2;

        mTouchToCornerDis = (float) Math.hypot((mTouch.x - mCornerX),(mTouch.y - mCornerY));
        mBezierEnd1 = getCross(mTouch, mBezierControl1, mBezierStart1,
                mBezierStart2);
        mBezierEnd2 = getCross(mTouch, mBezierControl2, mBezierStart1,
                mBezierStart2);
/*
         Log.i(TAG, "mBezierEnd1.x  " + mBezierEnd1.x + "  mBezierEnd1.y  "
         + mBezierEnd1.y);
         Log.i(TAG, "mBezierEnd2.x  " + mBezierEnd2.x + "  mBezierEnd2.y  "
         + mBezierEnd2.y);
*/
        /*
         * mBeziervertex1.x 推导
         * ((mBezierStart1.x+mBezierEnd1.x)/2+mBezierControl1.x)/2 化简等价
         * (mBezierStart1.x+ 2*mBezierControl1.x+mBezierEnd1.x) / 4
         */
        mBeziervertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
        mBeziervertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
        mBeziervertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
        mBeziervertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
    }

    private void drawCurrentPageArea(Canvas canvas, Bitmap bitmap, Path path) {
        mPath0.reset();
        mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
        mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x,
                mBezierEnd1.y);
        mPath0.lineTo(mTouch.x, mTouch.y);
        mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x,
                mBezierStart2.y);
        mPath0.lineTo(mCornerX, mCornerY);
        mPath0.close();

        canvas.save();
        canvas.clipPath(path, Region.Op.XOR);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.restore();
    }

    private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {
        mPath1.reset();
        mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
        mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
        mPath1.lineTo(mBeziervertex2.x, mBeziervertex2.y);
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
        mPath1.lineTo(mCornerX, mCornerY);
        mPath1.close();

        mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x
                - mCornerX, mBezierControl2.y - mCornerY));
        int leftx;
        int rightx;
        GradientDrawable mBackShadowDrawable;
        if (mIsRTandLB) {
            leftx = (int) (mBezierStart1.x);
            rightx = (int) (mBezierStart1.x + mTouchToCornerDis / 4);
            mBackShadowDrawable = mBackShadowDrawableLR;
        } else {
            leftx = (int) (mBezierStart1.x - mTouchToCornerDis / 4);
            rightx = (int) mBezierStart1.x;
            mBackShadowDrawable = mBackShadowDrawableRL;
        }
        canvas.save();
        canvas.clipPath(mPath0);
        canvas.clipPath(mPath1, Region.Op.INTERSECT);
        if(bitmap!=null){
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
        mBackShadowDrawable.setBounds(leftx, (int) mBezierStart1.y, rightx,
                (int) (mMaxLength + mBezierStart1.y));
        mBackShadowDrawable.draw(canvas);
        canvas.restore();
    }

    public void setBitmaps(Bitmap bm1, Bitmap bm2) {
        mCurPageBitmap = bm1;
        mNextPageBitmap = bm2;
    }
    
    public void setScreen(int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawColor(0xFFAAAAAA);
        if(mCurPageBitmap!=null&&mNextPageBitmap==null){
            canvas.drawBitmap(mCurPageBitmap, 0, 0, null);
        }else if(mCurPageBitmap!=null){
            calcPoints();
            drawCurrentPageArea(canvas, mCurPageBitmap, mPath0);
            drawNextPageAreaAndShadow(canvas, mNextPageBitmap);
            drawCurrentPageShadow(canvas);
            drawCurrentBackArea(canvas, mCurPageBitmap);
        }
        
    }

    /**
     * 创建阴影的GradientDrawable
     */
    private void createDrawable() {
        int[] color = { 0x333333, 0xb0333333 };
        mFolderShadowDrawableRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, color);
        mFolderShadowDrawableRL
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFolderShadowDrawableLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, color);
        mFolderShadowDrawableLR
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mBackShadowColors = new int[] { 0xff111111, 0x111111 };
        mBackShadowDrawableRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors);
        mBackShadowDrawableRL.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mBackShadowDrawableLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors);
        mBackShadowDrawableLR.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFrontShadowColors = new int[] { 0x80111111, 0x111111 };
        mFrontShadowDrawableVLR = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors);
        mFrontShadowDrawableVLR
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mFrontShadowDrawableVRL = new GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors);
        mFrontShadowDrawableVRL
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFrontShadowDrawableHTB = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors);
        mFrontShadowDrawableHTB
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);

        mFrontShadowDrawableHBT = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors);
        mFrontShadowDrawableHBT
                .setGradientType(GradientDrawable.LINEAR_GRADIENT);
    }

    /**
     * 绘制翻起页的阴影
     */
    public void drawCurrentPageShadow(Canvas canvas) {
        double degree;
        if (mIsRTandLB) {
            degree = Math.PI
                    / 4
                    - Math.atan2(mBezierControl1.y - mTouch.y, mTouch.x
                            - mBezierControl1.x);
        } else {
            degree = Math.PI
                    / 4
                    - Math.atan2(mTouch.y - mBezierControl1.y, mTouch.x
                            - mBezierControl1.x);
        }
        //翻起页阴影顶点与touch点的距离
        double d1 = (float) 25 * 1.414 * Math.cos(degree);
        double d2 = (float) 25 * 1.414 * Math.sin(degree);
        float x = (float) (mTouch.x + d1);
        float y;
        if (mIsRTandLB) {
            y = (float) (mTouch.y + d2);
        } else {
            y = (float) (mTouch.y - d2);
        }
        mPath1.reset();
        mPath1.moveTo(x, y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
        mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
        mPath1.close();
        float rotateDegrees;
        canvas.save();

        canvas.clipPath(mPath0, Region.Op.XOR);
        canvas.clipPath(mPath1, Region.Op.INTERSECT);
        int leftx;
        int rightx;
        GradientDrawable mCurrentPageShadow;
        if (mIsRTandLB) {
            leftx = (int) (mBezierControl1.x);
            rightx = (int) mBezierControl1.x + 25;
            mCurrentPageShadow = mFrontShadowDrawableVLR;
        } else {
            leftx = (int) (mBezierControl1.x - 25);
            rightx = (int) mBezierControl1.x + 1;
            mCurrentPageShadow = mFrontShadowDrawableVRL;
        }

        rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x
                - mBezierControl1.x, mBezierControl1.y - mTouch.y));
        canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
        mCurrentPageShadow.setBounds(leftx,
                (int) (mBezierControl1.y - mMaxLength), rightx,
                (int) (mBezierControl1.y));
        mCurrentPageShadow.draw(canvas);
        canvas.restore();

        mPath1.reset();
        mPath1.moveTo(x, y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
        mPath1.close();
        canvas.save();
        canvas.clipPath(mPath0, Region.Op.XOR);
        canvas.clipPath(mPath1, Region.Op.INTERSECT);
        if (mIsRTandLB) {
            leftx = (int) (mBezierControl2.y);
            rightx = (int) (mBezierControl2.y + 25);
            mCurrentPageShadow = mFrontShadowDrawableHTB;
        } else {
            leftx = (int) (mBezierControl2.y - 25);
            rightx = (int) (mBezierControl2.y + 1);
            mCurrentPageShadow = mFrontShadowDrawableHBT;
        }
        rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y
                - mTouch.y, mBezierControl2.x - mTouch.x));
        canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
        float temp;
        if (mBezierControl2.y < 0)
            temp = mBezierControl2.y - mHeight;
        else
            temp = mBezierControl2.y;

        int hmg = (int) Math.hypot(mBezierControl2.x, temp);
        if (hmg > mMaxLength)
            mCurrentPageShadow
                    .setBounds((int) (mBezierControl2.x - 25) - hmg, leftx,
                            (int) (mBezierControl2.x + mMaxLength) - hmg,
                            rightx);
        else
            mCurrentPageShadow.setBounds(
                    (int) (mBezierControl2.x - mMaxLength), leftx,
                    (int) (mBezierControl2.x), rightx);
/*
         Log.i(TAG, "mBezierControl2.x   " + mBezierControl2.x
         + "  mBezierControl2.y  " + mBezierControl2.y);*/
        mCurrentPageShadow.draw(canvas);
        canvas.restore();
    }

    /**
     *  绘制翻起页背
     */
    private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
        int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
        float f1 = Math.abs(i - mBezierControl1.x);
        int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
        float f2 = Math.abs(i1 - mBezierControl2.y);
        float f3 = Math.min(f1, f2);
        mPath1.reset();
        mPath1.moveTo(mBeziervertex2.x, mBeziervertex2.y);
        mPath1.lineTo(mBeziervertex1.x, mBeziervertex1.y);
        mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mPath1.close();
        GradientDrawable mFolderShadowDrawable;
        int left;
        int right;
        if (mIsRTandLB) {
            left = (int) (mBezierStart1.x - 1);
            right = (int) (mBezierStart1.x + f3 + 1);
            mFolderShadowDrawable = mFolderShadowDrawableLR;
        } else {
            left = (int) (mBezierStart1.x - f3 - 1);
            right = (int) (mBezierStart1.x + 1);
            mFolderShadowDrawable = mFolderShadowDrawableRL;
        }
        canvas.save();
        canvas.clipPath(mPath0);
        canvas.clipPath(mPath1, Region.Op.INTERSECT);

        mPaint.setColorFilter(mColorMatrixFilter);

        float dis = (float) Math.hypot(mCornerX - mBezierControl1.x,
                mBezierControl2.y - mCornerY);
        float f8 = (mCornerX - mBezierControl1.x) / dis;
        float f9 = (mBezierControl2.y - mCornerY) / dis;
        mMatrixArray[0] = 1 - 2 * f9 * f9;
        mMatrixArray[1] = 2 * f8 * f9;
        mMatrixArray[3] = mMatrixArray[1];
        mMatrixArray[4] = 1 - 2 * f8 * f8;
        mMatrix.reset();
        mMatrix.setValues(mMatrixArray);
        mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
        mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
        canvas.drawColor(getResources().getColor(R.color.white));
//        canvas.drawBitmap(bitmap, mMatrix, mPaint);
        // canvas.drawBitmap(bitmap, mMatrix, null);
        mPaint.setColorFilter(null);
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
        mFolderShadowDrawable.setBounds(left, (int) mBezierStart1.y, right,
                (int) (mBezierStart1.y + mMaxLength));
        mFolderShadowDrawable.draw(canvas);
        canvas.restore();
    }

    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            float x = mScroller.getCurrX();
            float y = mScroller.getCurrY();
            int finalY = this.mScroller.getFinalY();
            int finalX = mScroller.getFinalX();
            Log.i(TAG, "computeScroll x="+x+"|y="+y+"|finalX="+finalX+"|finalY="+finalY);
            if(x==finalX&&y==finalY){
                Log.i(TAG, "onAnimationEnd");
                setVisibility(View.INVISIBLE);
                mIsFinished = true;
            }
            mTouch.x = x;
            mTouch.y = y;
            postInvalidate();
        }else{
            if(mIsFinished==false){
                Log.i(TAG, "onAnimationEnd mIsFinished="+mIsFinished);
                setVisibility(View.INVISIBLE);
                mIsFinished = true;
            }
            
        }
    }

    private void startAnimation(int delayMillis) {
        Log.i(TAG, "startAnimation ");
        setVisibility(View.VISIBLE);
        mIsFinished = false;
        int dx, dy;
        // dx 水平方向滑动的距离，负会使滚动向左滚动
        // dy 垂直方向滑动的距离，负会使滚动向上滚动
        if (mCornerX > 0) {
            dx = -(int) (mWidth + mTouch.x);
        } else {
            dx = (int) (mWidth - mTouch.x + mWidth);
        }
        if (mCornerY > 0) {
            dy = (int) (mHeight - mTouch.y);
        } else {
            dy = (int) (1 - mTouch.y); //防止mTouch.y变为0
        }
        mScroller.startScroll((int) mTouch.x, (int) mTouch.y, dx, dy,
                delayMillis);
    }
    

    public void abortAnimation() {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
    }

    public boolean canDragOver() {
        if (mTouchToCornerDis > mWidth / 10)
            return true;
        return false;
    }

    /**
     * 是否从左边翻向右
     */
    public boolean DragToRight() {
        
//      if (mCornerX > 0)
        if (down_x >= mWidth/2)
            return false;
        return true;
    }

}
