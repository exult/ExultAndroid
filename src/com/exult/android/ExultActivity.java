package com.exult.android;

import java.io.IOException;
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
import android.app.AlertDialog;
import android.widget.Toast;
import android.widget.Button;
import android.util.AttributeSet;
import android.content.DialogInterface;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.lang.InterruptedException;

public class ExultActivity extends Activity {
	private static Point clickPoint;	// Non-null if getClick() is active.
	private static final Semaphore clickWait = new Semaphore(1, true);
	private static boolean targeting;
	private static ExultActivity instance;
	private static GameWindow gwin;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// start tracing to "/sdcard/calc.trace"
        //Debug.startMethodTracing("calc");
    	instance = this;
    	new Game.BGGame();	// Stores itself in GameSingletons.
    	EUtil.initSystemPaths();
    	ShapeFiles.load();
        super.onCreate(savedInstanceState);
        //setContentView(new MySurfaceView(this));
        setContentView(R.layout.main);
        setButtonHandlers();
    }
    @Override
    public void onDestroy() {
    	// stop tracing
        //Debug.stopMethodTracing();
    	if (GameSingletons.audio != null)
    		GameSingletons.audio.stop();
    	super.onDestroy();
    }
    static class MessageDisplayer implements Runnable {
    	String msg;
    	boolean toast;
    	boolean fatal;
    	MessageDisplayer(String m, boolean t, boolean f) {
    		msg = m; toast = t; fatal = f;
    	}
    	public void run() {
    		if (toast) {
    			Toast.makeText(instance, msg, Toast.LENGTH_LONG).show();
    		} else {
    			AlertDialog alertDialog = new AlertDialog.Builder(instance).create();
    			alertDialog.setTitle("Exult");
    			alertDialog.setMessage(msg);
    			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int which) {
    					dialog.dismiss();
    					if (fatal)
    						instance.finish();
    				}
    			});
    			alertDialog.setIcon(R.drawable.icon);
    			alertDialog.show();
        	}
    	}
    }
    public static void showToast(String s) {
    	instance.runOnUiThread(new MessageDisplayer(s, true, false));
    }
    public static void alert(String msg) {
    	instance.runOnUiThread(new MessageDisplayer(msg, false, false));
    }
    public static void fileFatal(String nm) {
    	fatal("Error reading '" + EUtil.getSystemPath(nm) + "'");
    }
    public static void fatal(String msg) {
    	instance.runOnUiThread(new MessageDisplayer(msg, false, true));
    }
    public static GameObject waitForClick(Point p, Boolean target) {
    	p.x = -1;
    	
    	GameWindow.targetObj = null;
    	try {clickWait.acquire();} catch (InterruptedException e) {
    		return null;	// Failed.
    	}
    	targeting = target;
    	GameSingletons.mouse.setShape(Mouse.greenselect);
    	Point save = clickPoint;	// Don't expect this to happen.
    	clickPoint = p;
    	// Wait for the click.
    	try { clickWait.acquire(); } catch (InterruptedException e) {
    		p.x = -1;
    	}
    	clickPoint = save;
    	clickWait.release();
    	GameObject ret = GameWindow.targetObj;
    	if (ret != null)
    		gwin.addDirty(ret);
    	GameWindow.targetObj = null;
    	targeting = false;
    	return ret;
    }
    public static void getClick(Point p) {
    	waitForClick(p, false);
    }
    public static GameObject getTarget(Point p) {
    	GameSingletons.mouse.setLocation(gwin.getWidth()/2, gwin.getHeight()/2);
    	return waitForClick(p, true);
    }
    /*
     * Button handlers:
     */
    private void setButtonHandlers() {
    	Button button;
    	button = (Button) findViewById(R.id.target_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.target();}
        });
        button = (Button) findViewById(R.id.combat_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.combat();}
        });
        button = (Button) findViewById(R.id.inventory_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.inv();}
        });
        button = (Button) findViewById(R.id.stats_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.stats();}
        });
        button = (Button) findViewById(R.id.feed_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.feed();}
        });
        button = (Button) findViewById(R.id.zoom_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.zoom();}
        });
        button = (Button) findViewById(R.id.save_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.save();}
        });
        button = (Button) findViewById(R.id.quit_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Shortcuts.quit();}
        });
    }
    public static void quit() {
    	instance.finish();
    }
    /*
     * Subclasses.
     */
    public static class MySurfaceThread extends Thread {
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
    
    public static class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    	private MySurfaceThread thread;
    	private MotionEvent avatarMotion;	// When moving Avatar.
    	public ImageBuf ibuf;
    	public long GameTime;
    	public long nextTickTime;
    	public static int stdDelay = 200;	// Frame delay in msecs.
    	private int showItemsX = -1, showItemsY = -1;
    	private long showItemsTime = 0;
    	private long lastB1Click = 0;
    	private int leftDownX = -1, leftDownY = -1;
    	private boolean dragging = false, dragged = false;
    	private boolean movingAvatar = false;
    	private int avatarStartX, avatarStartY;
    	private Point movePoint = new Point();	// A temp.
    	@Override
    	protected void  onSizeChanged(int w, int h, int oldw, int oldh) {
    		System.out.printf("Size changed to %1$d, %2$d\n", w, h);
    		gwin.getWin().setToScale(w, h);
    	}
    	@Override
    	protected void onDraw(Canvas canvas){
    		GameTime = System.currentTimeMillis();
    		if (GameTime > nextTickTime) {
                nextTickTime = GameTime + stdDelay;
                TimeQueue.ticks +=1;
                GameSingletons.mouse.hide();
                if (!dragging && clickPoint == null && 
                						GameSingletons.ucmachine.running == 0) {
                	synchronized (gwin.getTqueue()) {
                		gwin.getTqueue().activate(TimeQueue.ticks);
                	}
                }
                // If mouse still down, keep moving.
                if (movingAvatar && !gwin.isMoving()) {
                	int x = (int)gwin.getWin().screenToGameX(avatarMotion.getX()), 
    					y = (int)gwin.getWin().screenToGameY(avatarMotion.getY());
                	System.out.println("Keep moving");
                	gwin.startActor(avatarStartY, avatarStartY, x, y, 
                			GameSingletons.mouse.avatarSpeed);
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
                	if (dragging || movingAvatar || targeting)
                		GameSingletons.mouse.show();
                	if (TimeQueue.ticks%3 == 0)
                		rotatePalette();
                	if (!gwin.show(canvas, false)) {	
                		if (Mouse.mouseUpdate)
                			GameSingletons.mouse.blitDirty(canvas);
                		Mouse.mouseUpdate = false;
                		gwin.getWin().blit(canvas);
                	}
                }
    		} else
    			gwin.getWin().blit(canvas);
    	}
    	// For direct instantiation.
    	public MySurfaceView(Context ctx){
    		super(ctx);
    		init();
    	}
    	// For instantiation from XML
    	public MySurfaceView(Context context, AttributeSet attrs) {
    		super(context, attrs);
    		init();
        } 
    	@Override
        protected void onMeasure(int widthSpec, int heightSpec) {
    		int w = figureDim(widthSpec), h = figureDim(heightSpec);
    		System.out.printf("onMeasure: %1$d, %2$d\n", w, h);
            setMeasuredDimension(w, h);
        }
    	private int figureDim(int spec) {
    		//int result = 0;
            //int specMode = MeasureSpec.getMode(spec);
            int specSize = MeasureSpec.getSize(spec);
            return specSize;
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
    		TimeQueue.tickMsecs = stdDelay;
    		/* +++++++++NEEDED
    		android.view.Display display = ((android.view.WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    		int width = display.getWidth(), height = display.getHeight();
    		
    		gwin.getWin().setToScale(width, height);
    		*/
    		gwin = new GameWindow(320, 200);	// Standard U7 dims.
    		gwin.initFiles(false);
    		gwin.readGwin();
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
    			if (gwin.busyMessage != null)
    				return false;
    			synchronized (gwin.getTqueue()) {
    			float sx = event.getX(), sy = event.getY();
    			int x = (int)gwin.getWin().screenToGameX(sx), 
    				y = (int)gwin.getWin().screenToGameY(sy);
    			Gump.Modal modal = GameSingletons.gumpman.getModal();
    			// int state = event.getMetaState();
    			switch (event.getAction()) {
    			case MotionEvent.ACTION_DOWN:
    				GameSingletons.mouse.move(x, y);
    				if (clickPoint == null && UsecodeMachine.running <= 0) {
    					if (modal != null) {
    						modal.mouseDown(x, y, 1);	// FOR NOW, button = 1.
    						return true;
    					}
    					dragging = DraggingInfo.startDragging(x, y);
    					dragged = false;
    					GameObject obj = dragging?DraggingInfo.getObject():null;
    					if (obj == gwin.getMainActor()) {
    						DraggingInfo.abort();
    						dragging = false;
    						System.out.println("Starting motion");
        					avatarMotion = MotionEvent.obtain(event);
        					avatarStartX = x; avatarStartY = y;
    					} else if (!dragging || (obj != null && !obj.isDragable() &&
    											 !GameSingletons.cheat.inHackMover())) {
    						DraggingInfo.abort();
    						dragging = false;
    						avatarMotion = MotionEvent.obtain(event);
    						gwin.getShapeLocation(movePoint, gwin.getMainActor());
    						avatarStartX = movePoint.x; avatarStartY = movePoint.y;
    					}
    				}
    				leftDownX = x; leftDownY = y;
    				return true;
    			case MotionEvent.ACTION_UP:
    				boolean clickHandled = false;
    				if (!targeting)
    					GameSingletons.mouse.hide();
    				gwin.stopActor();
    				avatarMotion = null;
    				movingAvatar = false;
    				if (clickPoint != null) {
    					if (targeting ||
    					   (leftDownX - 1 <= x && x <= leftDownX + 1 &&
    						leftDownY - 1 <= y && y <= leftDownY + 1)) {
    						clickPoint.set(x, y);
    						clickWait.release();
    					}
    					return true;
    				}
    				if (modal != null) {
    					modal.mouseUp(x, y, 1);	// FOR NOW, button = 1.
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
    				GameSingletons.mouse.move(x, y);
    				Mouse.mouseUpdate = true;
    				if (avatarMotion != null && clickPoint == null) {
    					if (modal != null) {
    						modal.mouseDrag(x, y);
    						return true;
    					}
    					GameSingletons.mouse.setSpeedCursor(avatarStartX,
    							avatarStartY);
    					movingAvatar = true;
    					avatarMotion.setLocation(sx, sy);
    					gwin.startActor(avatarStartX, avatarStartY, x, y, 
    							GameSingletons.mouse.avatarSpeed);
    				} else if (dragging) {
    					dragged = GameSingletons.drag.moved(x, y);
    				} else if (targeting) {
    					GameObject obj;
    					Gump gump = GameSingletons.gumpman.findGump(x, y);
    					if (gump != null)
    						obj = gump.findObject(x, y);
    					else
    						obj = gwin.findObject(x, y);
    					if (obj != GameWindow.targetObj) {
    						if (GameWindow.targetObj != null)
    							gwin.addDirty(GameWindow.targetObj);
    						if (obj != null)
    							gwin.addDirty(obj);
    						GameWindow.targetObj = obj;
    					}
    				}
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
    			if (UsecodeMachine.running > 0 || clickPoint != null || 
    										gwin.busyMessage != null)
    				return false;
    			Gump.Modal modal = GameSingletons.gumpman.getModal();
		        if (event.getAction() == KeyEvent.ACTION_DOWN) {
		        	if (modal != null) {
		        		modal.textInput(keyCode, event.getUnicodeChar());
		        		return true;
		        	}
		        	if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT ||
		        		keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
		        		return false;		// Weed these out for performance.
		        	switch (keyCode) {
		        	case KeyEvent.KEYCODE_DPAD_RIGHT:
		        		for (int i = 0; i < 4; ++i)
		        			gwin.shiftViewHoriz(false, true); 
		        		return true;
		        	case KeyEvent.KEYCODE_DPAD_LEFT:		        		
		        		for (int i = 0; i < 4; ++i)
		        			gwin.shiftViewHoriz(true, true); 
		        		return true;
		        	case KeyEvent.KEYCODE_DPAD_DOWN:		        		
		        		for (int i = 0; i < 4; ++i)
		        			gwin.shiftViewVertical(false, true); 
		        		return true;
		        	case KeyEvent.KEYCODE_DPAD_UP:		        		
		        		for (int i = 0; i < 4; ++i)
		        			gwin.shiftViewVertical(true, true); 
		        		return true;
		        	case KeyEvent.KEYCODE_H:
			        		if (event.isAltPressed()) {
			        			GameSingletons.cheat.toggleHackMover();
			        			return true;
			        		} else
			        			return false;
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
		        		
		        	case KeyEvent.KEYCODE_C:	
		        		if (event.isAltPressed()) {
		        			GameSingletons.clock.fakeNextPeriod();
		        			return true;
		        		} else
		        			return false;
		        	case KeyEvent.KEYCODE_R:
		        		if (event.isAltPressed()) {
		        			gwin.read(1);	// +++++++TESTING
		        		}
		        		return true;
		        	case KeyEvent.KEYCODE_S:
		        		if (event.isAltPressed()) 
		        			gwin.write(1, "Test save to zip");	//++++++++++TESTING.
		        		else if (!event.isShiftPressed()) {
		        			Shortcuts.save();
		        		}
		        		return true;
		        		
		        	case KeyEvent.KEYCODE_T:
		        		if (event.isAltPressed()) {
		        			GameSingletons.cheat.mapTeleport();
		        		} else if (!event.isShiftPressed()) {
		        			Shortcuts.target();
		        		}
		        		return true;
		        	case KeyEvent.KEYCODE_U:
			        	if (event.isAltPressed())
			        		GameSingletons.ucmachine.debug = !
			        					GameSingletons.ucmachine.debug;
			        	return true;
		        	case KeyEvent.KEYCODE_X:
		        		if (event.isAltPressed()) {
		        			ExultActivity.instance.finish();
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