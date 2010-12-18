package com.exult.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.content.Context;
import android.graphics.Point;

public class ExultActivity extends Activity {
	public long GameTime;
	public long nextTickTime;
	public static int stdDelay = 200;	// Frame delay in msecs.
	public GameWindow gwin;
	public ImageBuf ibuf;
	private static Point clickPoint;	// Non-null if getClick() is active.
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// start tracing to "/sdcard/calc.trace"
        //Debug.startMethodTracing("calc");
    	new Game.BGGame();	// Stores itself in GameSingletons.
    	EUtil.initSystemPaths();
    	ShapeFiles.load();
        super.onCreate(savedInstanceState);
        setContentView(new MySurfaceView(this));
    }
    @Override
    public void onDestroy() {
    	// stop tracing
        //Debug.stopMethodTracing();
    	super.onDestroy();
    }
    public static void getClick(Point p) {
    	Point save = clickPoint;	// Don't expect this to happen.
    	p.x = -1;
    	clickPoint = p;
    	while (p.x < 0) {
    		try {
    			Thread.sleep(200);
    		} catch (InterruptedException e) {
    			p.x = -1; break;
    		}
    	}
    	clickPoint = save;
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
    	private MotionEvent avatarMotion;	// When moving Avatar.
    	private int showItemsX = -1, showItemsY = -1;
    	public long showItemsTime = 0;
    	private long lastB1Click = 0;
    	private int leftDownX = -1, leftDownY = -1;
    	private boolean dragging = false, dragged = false;
    	private boolean movingAvatar = false;
    	private int avatarStartX, avatarStartY;
    	@Override
    	protected void  onSizeChanged(int w, int h, int oldw, int oldh) {
    		System.out.println("Size changed.  Old w was " + oldw +
    				", neww is " + w);
    		gwin.getWin().setToScale(w, h);
    	}
    	@Override
    	protected void onDraw(Canvas canvas){
    		if (GameTime > nextTickTime) {
                nextTickTime = GameTime + stdDelay;
                TimeQueue.ticks +=1;
                
                if (!dragging) {
                	synchronized (gwin.getTqueue()) {
                		gwin.getTqueue().activate(TimeQueue.ticks);
                	}
                }
                // If mouse still down, keep moving.
                if (movingAvatar && !gwin.isMoving()) {
                	int x = (int)gwin.getWin().screenToGameX(avatarMotion.getX()), 
    					y = (int)gwin.getWin().screenToGameY(avatarMotion.getY());
                	System.out.println("Keep moving");
                	gwin.startActor(avatarStartY, avatarStartY, x, y, 1);
                }
                // Handle delayed showing of items clicked on.
                if (showItemsX >= 0 && GameTime > showItemsTime) {
                	gwin.showItems(showItemsX, showItemsY);
                	showItemsX = showItemsY = -1000;
                }
                if (gwin.isDirty()) {
                	gwin.paintDirty();
                }
                synchronized (gwin.getWin()) {
                	if (TimeQueue.ticks%3 == 0)
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
    		// 'Touch' handler
    		setOnTouchListener(touchListener);
    		setFocusable(true);
    		setFocusableInTouchMode(true);
    		requestFocus();
    		ItemNames.init(false, false);
    		android.view.Display display = ((android.view.WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    		int width = display.getWidth(), height = display.getHeight();
    		gwin = new GameWindow(320, 200);	// Standard U7 dims.
    		gwin.getWin().setToScale(width, height);
    		gwin.initFiles(false);
    		gwin.setupGame();
    		gwin.setAllDirty();
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
    	private OnTouchListener touchListener = new OnTouchListener() {
    		public boolean onTouch(View v, MotionEvent event) {
    			synchronized (gwin.getTqueue()) {
    			float sx = event.getX(), sy = event.getY();
    			int x = (int)gwin.getWin().screenToGameX(sx), 
    				y = (int)gwin.getWin().screenToGameY(sy);
    			// int state = event.getMetaState();
    			switch (event.getAction()) {
    			case MotionEvent.ACTION_DOWN:
    				if (clickPoint == null && UsecodeMachine.running <= 0) {
    					dragging = DraggingInfo.startDragging(x, y);
    					dragged = false;
    					if (dragging && DraggingInfo.getObject() ==
    											gwin.getMainActor()) {
    						DraggingInfo.abort();
    						dragging = false;
    						System.out.println("Starting motion");
        					avatarMotion = MotionEvent.obtain(event);
        					avatarStartX = x; avatarStartY = y;
    					}
    				}
    				leftDownX = x; leftDownY = y;
    				return true;
    			case MotionEvent.ACTION_UP:
    				boolean clickHandled = false;
    				gwin.stopActor();
    				avatarMotion = null;
    				movingAvatar = false;
    				if (clickPoint != null) {
    					if (leftDownX - 1 <= x && x <= leftDownX + 1 &&
    						leftDownY - 1 <= y && y <= leftDownY + 1) {
    						clickPoint.set(x, y);
    					}
    					return true;
    				}
    				if (dragging) {
    					clickHandled = GameSingletons.drag.drop(x, y, dragged);
    				}
    				if (GameTime - lastB1Click < 500 &&
    						UsecodeMachine.running <= 0 &&
    						leftDownX - 1 <= x && x <= leftDownX + 1 &&
    						leftDownY - 1 <= y && y <= leftDownY + 1) {
    					dragging = dragged = false;
    					// This function handles the trouble of deciding what to
    					// do when the avatar cannot act.
    					gwin.doubleClicked(x, y);
    					// +++++ Mouse::mouse->set_speed_cursor();
    					showItemsX = -1000;
    					return true;
    				}	
    				if (!dragging || !dragged)
    					lastB1Click = GameTime;
    				if (!clickHandled && 
    						/* ++++ gwin.getMainActor().canAct()*/ true &&
    						leftDownX - 1 <= x && x <= leftDownX + 1 &&
    						leftDownY - 1 <= y && y <= leftDownY + 1) {
    					showItemsX = x; showItemsY = y;
    					showItemsTime = GameTime + 500;
    				}
    				dragging = dragged = false;
    				return true;
    			case MotionEvent.ACTION_MOVE:
    				if (avatarMotion != null && clickPoint == null) {
    					movingAvatar = true;
    					avatarMotion.setLocation(sx, sy);
    					gwin.startActor(avatarStartX, avatarStartY, x, y, 1);
    				} else if (dragging)
    					dragged = GameSingletons.drag.moved(x, y);
    				return true;
    			case MotionEvent.ACTION_CANCEL:
    				return true;
    			}
    			return false;
    		}
    		}
    	};
    	private OnKeyListener keyListener = new OnKeyListener() {
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
    		if (UsecodeMachine.running > 0 || clickPoint != null)
    			return false;
		        if (event.getAction() == KeyEvent.ACTION_DOWN) {
		        	if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT ||
		        		keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
		        		return false;		// Weed these out for performance.
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
		        		if (event.isAltPressed()) {
		        			if (gwin.skipLift == 16)
		        				gwin.skipLift = 11;
		        			else
		        				gwin.skipLift--;
		        			if (gwin.skipLift < 0)	// 0 means 'terrain-editing'.
		        				gwin.skipLift = 16;
		        			System.out.println("Setting skipLift to " + gwin.skipLift);
		        			gwin.setAllDirty();
		        			return true;
		        		} else
		        			return false;
		        	case KeyEvent.KEYCODE_X:
		        		if (event.isAltPressed()) {
		        			finish();
		        			return true;
		        		} else
		        			return false;
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
   
}