package com.exult.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.KeyEvent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;

public class ExultActivity extends Activity {
	public VgaFile vgaFile;
	public long GameTime;
	public long nextTickTime;
	public static int stdDelay = 200;	// Frame delay in msecs.
	public static int ticks = 0;
	public GameWindow gwin;
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
    		if (GameTime > nextTickTime ) {
                nextTickTime = GameTime + stdDelay;
                ticks +=1;
                // I think we would execute timed activities here using new ticks.
          
                
                if (gwin.isDirty()) {
                	gwin.paintDirty();
                	// +++++++TESTING draw avatar
                	ShapeFiles.SHAPES_VGA.getShape(721, 1).paint(gwin.getWin(), 184, 92);
                }
                synchronized (gwin.getWin()) {
                	if (ticks%3 == 0)
                		rotatePalette();
                	if (!gwin.show(canvas, false)) {	
                		// Blit mouse++++
                		gwin.getWin().blit(canvas);
                	}
                }
    		} else
    			gwin.getWin().blit(canvas);
    	}
    	public MySurfaceView(Context context){
    		super(context);
    		init();
    	}
    	private void init(){
    		getHolder().addCallback(this);
    		thread = new MySurfaceThread(getHolder(), this);
    		// Keystroke handler.
    		setOnKeyListener(keyListener);
    		setFocusable(true);
    		setFocusableInTouchMode(true);
    		requestFocus();
    		//create a graphic
    		testSprite1 = new AnimationSprite();
    		android.view.Display display = ((android.view.WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    		int width = display.getWidth(), height = display.getHeight();
    		gwin = new GameWindow(320, 200);	// Standard U7 dims.
    		gwin.getWin().setToScale(width, height);
    		gwin.initFiles(false);
    		gwin.setupGame();
    		gwin.centerView(1035,2181);//+++++FOR NOW testing.
    		gwin.setAllDirty();
    		/*
    		ibuf = new ImageBuf(width, height);
    		pal0 = new Palette(ibuf);
    		pal0.set(Palette.PALETTE_DAY, -1, null);	
    		testSprite1.Init(721, 6);	// Avatar.
    		*/
    	}
    	private final void rotatePalette() {
    		ImageBuf win = gwin.getWin();
    		//System.out.println("rotatePalette: ticks = " + ticks);
    			win.rotateColors(0xfc, 3);
    			win.rotateColors(0xf8, 4);
    			win.rotateColors(0xf4, 4);
    			win.rotateColors(0xf0, 4);
    			win.rotateColors(0xe8, 8);
    			win.rotateColors(0xe0, 8);
    			gwin.setPainted();
    	}
    	private OnKeyListener keyListener = new OnKeyListener() {
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN) {
		        	switch (keyCode) {
		        	case KeyEvent.KEYCODE_DPAD_RIGHT:
		        		gwin.shiftViewHoriz(false); break;
		        	case KeyEvent.KEYCODE_DPAD_LEFT:
		        		gwin.shiftViewHoriz(true); break;
		        	case KeyEvent.KEYCODE_DPAD_DOWN:
		        		gwin.shiftViewVertical(false); break;
		        	case KeyEvent.KEYCODE_DPAD_UP:
		        		gwin.shiftViewVertical(true); break;
		        	case KeyEvent.KEYCODE_L:
		        		if (/*event.isAltPressed()*/ true) {
		        			if (gwin.skipLift == 16)
		        				gwin.skipLift = 11;
		        			else
		        				gwin.skipLift--;
		        			if (gwin.skipLift < 0)	// 0 means 'terrain-editing'.
		        				gwin.skipLift = 16;
		        			System.out.println("Setting skipLift to " + gwin.skipLift);
		        			gwin.setAllDirty();
		        		}
		        	}
		        }
    			return false;		// Didn't handle it here.
    	    }
    	};
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