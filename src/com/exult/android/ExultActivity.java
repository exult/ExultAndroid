package com.exult.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;

public class ExultActivity extends Activity {
	public VgaFile vgaFile;
	public long GameTime;
	public AnimationSprite testSprite1;
	public ImageBuf ibuf;
	public Palette pal0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	EUtil.initSystemPaths();
    	ShapeFiles.load();
    	vgaFile = ShapeFiles.SHAPES_VGA.getFile();
        super.onCreate(savedInstanceState);
        setContentView(new MySurfaceView(this));
    }
    
    /*
     * Subclasses.
     */
    public class MySurfaceThread extends Thread {
    	private SurfaceHolder myThreadSurfaceHolder;
    	private MySurfaceView myThreadSurfaceView;
    	private boolean myThreadRun = false;
    	public MySurfaceThread(SurfaceHolder surfaceHolder, MySurfaceView surfaceView) {
    		myThreadSurfaceHolder = surfaceHolder;
    		myThreadSurfaceView = surfaceView;
    	}
    	public void setRunning(boolean b) {
    		myThreadRun = b;
    	}
    	@Override
    	public void run() {
    		while (myThreadRun) {
    			Canvas c = null;
    			try {
    				GameTime = System.currentTimeMillis();
    				c = myThreadSurfaceHolder.lockCanvas(null);
    				synchronized (myThreadSurfaceHolder) {
    					myThreadSurfaceView.onDraw(c);
    				}
    			} finally {
    				if (c != null) {
    					myThreadSurfaceHolder.unlockCanvasAndPost(c);
    				}
    			}
    		}
    	}
    }	// End of MySurfaceThread
    
    public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    	private MySurfaceThread thread;
    	@Override
    	protected void onDraw(Canvas canvas){
    		canvas.drawColor(Color.BLACK);
    		testSprite1.Update(GameTime);
    		testSprite1.draw(canvas);
    	}
    	public MySurfaceView(Context context){
    		super(context);
    		init();
    	}
    	private void init(){
    		getHolder().addCallback(this);
    		thread = new MySurfaceThread(getHolder(), this);
    		//create a graphic
    		testSprite1 = new AnimationSprite();
    		/*
    		android.view.Display display = ((android.view.WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    		int width = display.getWidth(), height = display.getHeight();
    		*/
    		int width = 320, height = 200;	// Standard U7 screen.
    		ibuf = new ImageBuf(width, height);
    		pal0 = new Palette(ibuf);
    		pal0.set(Palette.PALETTE_DAY, -1, null);	
    		testSprite1.Init(721, 6);	// Avatar.
    	}
    	@Override
    	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    	}
    	@Override
    	public void surfaceCreated(SurfaceHolder holder){
    		thread.setRunning(true);
    		thread.start();
    	}
    	@Override
    	public void surfaceDestroyed(SurfaceHolder holder){
    		boolean retry = true;
    		thread.setRunning(false);
    		while (retry){
    			try{
    				thread.join();
    				retry = false;
    			} catch (InterruptedException e){}
    		}
    	}
    } // End of MySurfaceView
    public class AnimationSprite {
    	private ShapeFrame shape;
        private int mXPos;
        private int mYPos;
        private int mFPS;
        private int mNoOfFrames;
        private int mShapeNum;
        private int mCurrentFrame;
        private long mFrameTimer;
        private int mUpdateCnt, mDir;
        public AnimationSprite() {
            mFrameTimer =0;
            mCurrentFrame =0;
            mXPos = 80;
            mYPos = 200;
            mUpdateCnt = 0;
            mDir = 1;
        } public void Init(int shapeNum, int theFPS) {
        	mShapeNum = shapeNum;
            mFPS = 1000 /theFPS;
            shape = vgaFile.getShape(mShapeNum, 0);
            mNoOfFrames = vgaFile.getNumFrames(mShapeNum);
        }
        public void Update(long GameTime) {
            if(GameTime > mFrameTimer + mFPS ) {
                mFrameTimer = GameTime;
                mCurrentFrame +=1;
                //mYPos += 2*mDir;
                mXPos += 2*mDir;
                if (mUpdateCnt == 40) {
                	mDir = -mDir;
                	mUpdateCnt = 0;
                }
                mUpdateCnt += 1;
                if(mCurrentFrame >= mNoOfFrames) {
                    mCurrentFrame = 0;
                }
                shape = vgaFile.getShape(mShapeNum, mCurrentFrame);
            }
        }
        public void draw(Canvas canvas) {
        	ibuf.fill8((byte)0);
        	shape.paint(ibuf, mXPos, mYPos);
        	ibuf.show(canvas);
        }
    } // End of AnimationSprite
   
}