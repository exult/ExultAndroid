package com.exult.android;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ExultActivity extends Activity {
	private static Point clickPoint;	// Non-null if getClick() is active.
	private static Point clickIgnore;	// For waiting for a click, but don't care where.
	private static ClickTracker clickTrack;
	private static final Semaphore clickWait = new Semaphore(1, true);
	private static boolean targeting, tracking, trackingMouse;
	private static ExultActivity instance;
	private static GameWindow gwin;
	public static boolean restartFlag;
	public static Vibrator vibrator; 
	private static SharedPreferences prefs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// start tracing to "/sdcard/calc.trace"
        //Debug.startMethodTracing("calc");
    	instance = this;
    	EUtil.initSystemPaths();
    	new Game.BGGame();	// Stores itself in GameSingletons.
    	ShapeFiles.load();
        super.onCreate(savedInstanceState);
        //setContentView(new MySurfaceView(this));
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.main);
        setButtonHandlers();
        startupGame();
    }
    @Override
    public void onDestroy() {
    	// stop tracing
        //Debug.stopMethodTracing();
    	if (GameSingletons.audio != null)
    		GameSingletons.audio.stop();
    	super.onDestroy();
    }
    public static Activity instanceOf() {
    	return instance;
    }
    //	Show main menu and wait until done.
    private void showMainMenu() {
    	GameSingletons.game.topMenu();
    }
    private void startupGame() {
    	
		boolean skipIntro = prefs.getBoolean("skipIntroPref", false);
		System.out.println("skipIntro:  " + skipIntro);
		Thread t = new Thread() {	// Run this way so plasma will display.
			@Override
			public void run() {
				boolean skipMenu = prefs.getBoolean("skipGameMenu", false);
				if (!skipMenu)
					showMainMenu();
				gwin.initFiles(true);
				gwin.readGwin();
				gwin.setupGame();
				gwin.setAllDirty();
			}
		};
		if (!skipIntro) {
			GameSingletons.game.playIntro(t);
		} else
			t.start();
	}
    public void vibrate() {
    	if (vibrator == null)
    		vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
    	vibrator.vibrate(100);
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
    public static class YesNoDialog implements Runnable {
    	String msg;
    	Observer client;
    	Reporter reporter;
    	YesNoDialog(Observer c, String m) {
    		client = c;
    		msg = m;
    		reporter = new Reporter();
    		reporter.addObserver(c);
    	}
    	private static class Reporter extends Observable {
    		public void setChanged() { super.setChanged(); }
    	}
    	public void run() {
    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {		
    				Boolean answer = (which == DialogInterface.BUTTON_POSITIVE);
    				reporter.setChanged();
    				reporter.notifyObservers(answer);
    				dialog.dismiss();
    			}
    		};
    		AlertDialog.Builder builder = new AlertDialog.Builder(instance);
    		builder.setMessage(msg).setPositiveButton("Yes", dialogClickListener)
    	    	.setNegativeButton("No", dialogClickListener).show();
    	}
    };
    public static void askYesNo(Observer c, String txt) {
    	instance.runOnUiThread(new YesNoDialog(c, txt));
    }
    public static interface ClickTracker {
    	public abstract void onMotion(int x, int y);
    	public void onDown(int x, int y);
    }
    public static GameObject waitForClick(Point p, ClickTracker track, int mouseShape) {
    	if (p == null) {
    		if (clickIgnore == null)
    			clickIgnore = new Point();
    		p = clickIgnore;
    	}
    	p.x = -1;
    	GameWindow.targetObj = null;
    	//System.out.println("clickWait: wait for first acquire");
    	clickWait.drainPermits();
    	//System.out.println("clickWait: got first acquire");
    	GameSingletons.tqueue.pause();
    	tracking = (p != null);
    	trackingMouse = tracking && mouseShape >= 0;
    	if (mouseShape >= 0)
    		GameSingletons.mouse.setShape(mouseShape);
    	Point save = clickPoint;	// Don't expect this to happen.
    	ClickTracker trackSave = clickTrack;
    	clickPoint = p;
    	clickTrack = track;
    	//System.out.println("clickTrack before: " + clickTrack);
    	// Wait for the click.
    	try { clickWait.acquire(); } catch (InterruptedException e) {
    		p.x = -1;
    	}
    	clickPoint = save;
    	clickTrack = trackSave;
    	clickWait.release();
    	//System.out.println("clickTrack after: " + clickTrack);
    	GameObject ret = GameWindow.targetObj;
    	if (ret != null)
    		gwin.addDirty(ret);
    	GameWindow.targetObj = null;
    	tracking = targeting = trackingMouse = false;
    	GameSingletons.mouse.hide();
    	GameSingletons.tqueue.resume();
    	return ret;
    }
    public static void getClick(Point p) {
    	waitForClick(p, null, p == null ? -1 : Mouse.greenselect);
    }
    public static void getClick(Point p, ClickTracker t, int mouseShape) {
    	waitForClick(p, t, mouseShape);
    }
    public static GameObject getTarget(Point p, int mouseShape) {
    	if (p.x < 0 || p.y < 0) {
    		GameSingletons.mouse.setLocation(gwin.getWidth()/2, gwin.getHeight()/2);
    		instance.vibrate();
    	}
    	targeting = true;
    	return waitForClick(p, null, mouseShape);
    }
    public static void setInCombat() {
    	instance.runOnUiThread(new Runnable() {
    		public void run() {
    			View btns = (View) instance.findViewById(R.id.buttons_list);
    			btns.setBackgroundColor(gwin.inCombat() ? 0xff8f0000 : 0xff8f8f40);
    		}
    	});
    }
    @Override
	public void onBackPressed() {
		// Keeps program from exiting.
    	if (AndroidSave.instance != null)
    		AndroidSave.instance.close();
    	else if (GameSingletons.gumpman.gumpMode()) {
			GameSingletons.gumpman.closeAllGumps(false);
			clickWait.release();
    	} else if (!VideoPlayer.closeIfPlaying())
    		askToQuit();
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
    }
    public static void quit() {
    	instance.finish();
    }
    public static void askToQuit() {
		Observer o = new Observer() {
			public void update(Observable o, Object arg) {
				if ((Boolean)arg)
					ExultActivity.quit();
			}
		};
		ExultActivity.askYesNo(o, "Do you really want to quit?");
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Cheat cheat = GameSingletons.cheat;
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.save_restore_button:
        	Shortcuts.save(instance);
            return true;
        case R.id.Settings_button:
        	startActivity(new Intent(this, Preferences.class));
        	return true;
        case R.id.quit_button:
        	Shortcuts.quit();
            return true;
        case R.id.teleport_map_button:
        	cheat.mapTeleport();
        	return true;
        case R.id.hack_mover_button:
        	cheat.toggleHackMover();
        	item.setChecked(cheat.inHackMover());
        	return true;
        case R.id.god_mode_button:
            cheat.toggleGodMode();
            item.setChecked(cheat.inGodMode());
            return true;
        case R.id.wizard_mode_button:
        	cheat.toggleWizardMode();
        	item.setChecked(cheat.inWizardMode());
        	return true;
        case R.id.infra_mode_button:
        	cheat.toggleInfravision();
        	item.setChecked(cheat.inInfravision());
        	return true;
        case R.id.pickpocket_button:
        	cheat.togglePickpocket();
        	item.setChecked(cheat.inPickpocket());
        	return true;
        case R.id.intro_video_button:
        	GameSingletons.game.playIntro(null);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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
    	public static long GameTime;
    	public long nextTickTime;
    	public static int stdDelay = 200;	// Frame delay in msecs.
    	private int showItemsX = -1, showItemsY = -1;
    	private long showItemsTime = 0, lastB1Click;
    	// downMouse saves pos. on 'down' event, trackMouse has current pos.
    	private MousePos downMouse = new MousePos(), trackMouse = new MousePos();
    	private final static int clickDist = 5;		// Max. distance in movement to consider a click.
    	private boolean dragging = false, dragged = false;
    	private boolean movingAvatar = false;
    	private int avatarStartX, avatarStartY;
    	private float oldZoomDist = -1, zoomX = -1, zoomY = -1, oldZoomFactor;
    	private boolean wasZooming;
    	private Point movePoint = new Point();	// A temp.
    	@Override
    	protected void  onSizeChanged(int w, int h, int oldw, int oldh) {
    		System.out.printf("Size changed to %1$d, %2$d\n", w, h);
    		gwin.getWin().setToScale(w, h);
    	}
    	@Override
    	protected void onDraw(Canvas canvas){
    		Mouse mouse = GameSingletons.mouse;
    		GameTime = System.currentTimeMillis();
    		if (GameTime > nextTickTime) {
                nextTickTime = GameTime + stdDelay;
                TimeQueue.ticks +=1;
                mouse.hide();
                if (restartFlag) {
                	restartFlag = false;
                	gwin.read();
                }
                if (!dragging && UsecodeMachine.running == 0) {
                	synchronized (gwin.getTqueue()) {
                		gwin.getTqueue().activate(TimeQueue.ticks);
                	}
                }
                // If mouse still down, keep moving.
                if (movingAvatar && !gwin.isMoving() && gwin.mainActorCanAct()) {
                	int x = (int)gwin.getWin().screenToGameX(avatarMotion.getX()), 
    					y = (int)gwin.getWin().screenToGameY(avatarMotion.getY());
                	//System.out.println("Keep moving");
                	gwin.startActor(avatarStartX, avatarStartY, x, y, mouse.avatarSpeed);
                } else if (!dragging && !movingAvatar && downMouse.isLongPress(trackMouse.x, trackMouse.y)) {
                	// Long press.
                	if (!tracking) {	// Initial longpress:  target.
                		trackMouse.set(downMouse.x, downMouse.y);
                		downMouse.longPress = true;
                		Shortcuts.target();
                	} else if (targeting && !downMouse.longPress && GameWindow.targetObj != null) {
                		instance.vibrate();
                		downMouse.longPress = true;
                		dragging = DraggingInfo.startDragging(mouse.getX(), mouse.getY());
    					dragged = false;
                	}
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
                	if (dragging || movingAvatar || trackingMouse) {
                		if (mouse.show())
                			gwin.setPainted();
                	}
                	if (TimeQueue.ticks%3 == 0)
                		rotatePalette();
                	if (!gwin.show(canvas, false)) {	
                		gwin.getWin().blit(canvas);
                	}
                }
    		} else synchronized (gwin.getWin()) {
    			// This makes for much smoother mouse tracking:
    			if (trackingMouse && mouse.show())
    				gwin.show(canvas, true);
    			else
    				gwin.getWin().blit(canvas);
    		}
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
    		gwin = new GameWindow(EConst.c_game_w, EConst.c_game_h);	// Standard U7 dims.
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
    	private void moveAvatarMouse(int x, int y) {
    		gwin.getShapeLocation(movePoint, gwin.getMainActor());
    		GameSingletons.mouse.move((x + movePoint.x)/2, (y + movePoint.y)/2);
    	}
    	/*
         * 	Store info about a mouse event.
         */
        private static final class MousePos {
        	boolean down, longPress, maybeLong;
        	int x, y;					// Location on screen.
        	long setTime;				// When mouse 'pressed'.
        	public void set(int mx, int my) {
        		x = mx; y = my;
        	}
        	public void setDown(int mx, int my, long time) {
        		x = mx; y = my;
        		setTime = time;
        		down = maybeLong = true;
        		longPress = false;
        	}
        	public void up() {
        		down = longPress = false;
        	}
        	public boolean pointNear(int px, int py) {
        		return 	x - clickDist <= px && px <= x + clickDist &&
    					y - clickDist <= py && py <= y + clickDist; 
        	}
        	public boolean pointNear(int px, int py, int dist) {
        		return 	x - dist <= px && px <= x + dist &&
						y - dist <= py && py <= y + dist; 
        	}
        	public boolean isLongPress(int px, int py) {
        		if (down && maybeLong && GameTime > setTime + 800) {
        			if (GameTime < setTime + 1500 && pointNear(px, py, clickDist + 2))
        				return true;
        			maybeLong = false;	// Move too far or waited too long.
        		}
        		return false;
        	}
        };
    	private OnTouchListener touchListener = new OnTouchListener() {
    		public boolean onTouch(View v, MotionEvent event) {
    			if (gwin.busyMessage != null)
    				return false;
    			synchronized (gwin.getTqueue()) {
    			float sx = event.getX(), sy = event.getY();
    			int x = (int)gwin.getWin().screenToGameX(sx), 
    				y = (int)gwin.getWin().screenToGameY(sy);
    			Mouse mouse = GameSingletons.mouse;
    			boolean canAct = gwin.mainActorCanAct();
    			// int state = event.getMetaState();
    			switch (event.getAction() & MotionEvent.ACTION_MASK) {
    			case MotionEvent.ACTION_DOWN:
    				//System.out.println("action_down: " + x + ", " + y);
    				if (tracking)
    					trackMouse.set(x, y);
    				if (gwin.wizardEye) {
    					gwin.shiftWizardEye(x, y);
    					return true;
    				}
    				if (clickTrack == null && UsecodeMachine.running <= 0) {
    					if (tracking) {	
    						dragging = DraggingInfo.startDragging(mouse.getX(), mouse.getY());
    						if (!DraggingInfo.onButton()) {	// Only looking for button clicks here.
    							DraggingInfo.abort();
    							dragging = false;
    						}
    					}
    					dragged = false;
    					GameObject obj = dragging?DraggingInfo.getObject():null;
    					if (obj == gwin.getMainActor() && canAct) {
    						DraggingInfo.abort();
    						dragging = false;
    						//System.out.println("Starting motion");
        					avatarMotion = MotionEvent.obtain(event);
        					avatarStartX = x; avatarStartY = y;
    					} else if (!dragging || (obj != null && !obj.isDragable() &&
    											 !GameSingletons.cheat.inHackMover())) {
    						DraggingInfo.abort();
    						dragging = false;
    						if (canAct) {
    							avatarMotion = MotionEvent.obtain(event);
    							gwin.getShapeLocation(movePoint, gwin.getMainActor());
    							avatarStartX = movePoint.x; avatarStartY = movePoint.y;
    						}
    					}
    				} else if (clickPoint != null && clickTrack != null) {
    					clickTrack.onDown(mouse.getX(), mouse.getY());
    					clickTrack.onMotion(mouse.getX(), mouse.getY());
    				}
    				downMouse.setDown(x, y, GameTime);
    				return true;
    			case MotionEvent.ACTION_UP:
    				boolean clickHandled = false;
    				//System.out.println("action_up: " + x + ", " + y);
    				if (!tracking)
    					mouse.hide();
    				gwin.stopActor();
    				avatarMotion = null;
    				movingAvatar = false;
    				boolean wasLongPress = downMouse.longPress;
    				downMouse.up();
    				trackMouse.up();
    				if (zoomX >= 0 || wasZooming) {	// Don't want to cancel targetting if we were zooming.
    					zoomX = -1;
    					wasZooming = false;
    					return true;
    				}
    				//System.out.println("action_up: " + x + ", " + y + ", mouse = " + mouse.getX() + ", " + mouse.getY());
    				if (dragging) {
    					clickHandled = GameSingletons.drag.drop(mouse.getX(), mouse.getY(), dragged);
    					GameWindow.targetObj = null;
    				}
    				if (clickPoint != null) {
    					if (dragging || (!wasLongPress && downMouse.pointNear(x, y))) {	// Dropped, or clicked.
    						clickPoint.set(mouse.getX(), mouse.getY());
    						clickWait.release();
    					} else if (targeting && GameWindow.targetObj != null)
    						gwin.showObjName(GameWindow.targetObj);
    					clickHandled = true;
    				} else if (UsecodeMachine.running <= 0 && GameTime < lastB1Click + 500 && downMouse.pointNear(x, y)) {
    					gwin.doubleClicked(x, y);
    					showItemsX = -1000;
    					clickHandled = true;
    				} else if (!dragging || !dragged)
    					lastB1Click = GameTime;
    				if (!clickHandled && canAct && downMouse.pointNear(x, y)) {
    					showItemsX = x; showItemsY = y;
    					showItemsTime = GameTime + 500;
    				}
    				dragging = dragged = false;
    				return true;
    			case MotionEvent.ACTION_MOVE:
    				//System.out.println("action_move: " + x + ", " + y);
    				if (zoomX >= 0) {
    					float newDist = spacing(event);
    					if (newDist > 10f) {
    				         float scale = newDist / oldZoomDist;
    				         //System.out.printf("Zoom: new scale is %1$f, center is (%2$f,%3$f)\n", scale, zoomX, zoomY);
    				         Shortcuts.zoom(scale*oldZoomFactor);
    				         float newx = ((event.getX(0) + event.getX(1))/2);
    				         float newy = ((event.getY(0) + event.getY(1))/2);
    				         float diffx =  zoomX- newx,
  						  	  	   diffy =  zoomY - newy;
    				         if (Math.abs(diffx) > 5f || Math.abs(diffy) > 5f) {
    							Shortcuts.pan(diffx, diffy);
    							zoomX = newx;
    							zoomY = newy;
    				         }
    					}
    					return true;
    				}
    				if (gwin.wizardEye) {
    					gwin.shiftWizardEye(x, y);
    					return true;
    				}
    				if (avatarMotion != null && clickPoint == null) {
    					if (movingAvatar || !downMouse.pointNear(x, y, clickDist + 2)) {
    						GameSingletons.mouse.setSpeedCursor(avatarStartX, avatarStartY);
    						//System.out.printf("Mouse moved from %1$d,%2$d to %3$d, %4$d\n",
    						//		leftDownX, leftDownY, x, y);
    						movingAvatar = true;
    						avatarMotion.setLocation(sx, sy);
    						gwin.startActor(avatarStartX, avatarStartY, x, y, 
    							GameSingletons.mouse.avatarSpeed);
    						moveAvatarMouse(x, y);
    					}
    				} else if (tracking) {
    					// Move the mouse to follow the touch.
    					int deltax = x - trackMouse.x, deltay = y - trackMouse.y;
    					int mx = mouse.getX() + deltax, my = mouse.getY() + deltay;
    					mouse.move(mx, my);
    					if (clickTrack != null)
    						clickTrack.onMotion(mx, my);
    					if (dragging) {	// From longpress
    						dragged = GameSingletons.drag.moved(mx, my);
    					} else if (targeting) {
    						GameObject obj;
    						Gump gump = GameSingletons.gumpman.findGump(mx, my);
    						if (gump != null)
    							obj = gump.findObject(mx, my);
    						else
    							obj = gwin.findObject(mx, my);
    						if (obj != GameWindow.targetObj) {
    							if (GameWindow.targetObj != null)
    								gwin.addDirty(GameWindow.targetObj);
    							if (obj != null)
    								gwin.addDirty(obj);
    							GameWindow.targetObj = obj;
    						}
    					}
    				} else if (dragging) {
    					mouse.move(x, y);
    					dragged = GameSingletons.drag.moved(x, y);
    				}
    				trackMouse.set(x, y);
    				return true;
    			case MotionEvent.ACTION_POINTER_DOWN:
    				//System.out.println("action_pointer_down: " + x + ", " + y);
    				oldZoomDist = spacing(event);
    				System.out.printf("oldZoomDist = %1$f\n", oldZoomDist);
    				if (!dragging && oldZoomDist > 10f) {
    					zoomX = (event.getX(0) + event.getX(1))/2;
    					zoomY = (event.getY(0) + event.getY(1))/2;
    					oldZoomFactor = Shortcuts.getZoomFactor();
    					gwin.stopActor();
        				avatarMotion = null;
        				movingAvatar = false;
        				wasZooming = true;
    				}
    				return true;    			
    			case MotionEvent.ACTION_POINTER_UP:
    				//System.out.println("ACTION_POINTER_UP");
    				zoomX = -1;
    				return true;
    			case MotionEvent.ACTION_CANCEL:
    				return true;
    			}
    			return false;
    		}
    		}
    	};
    	private float spacing(MotionEvent event) {
    		float x = event.getX(0) - event.getX(1);
    		float y = event.getY(0) - event.getY(1);
    		return (float) Math.sqrt(x * x + y * y);
    	}
    	private OnKeyListener keyListener = new OnKeyListener() {
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
    			System.out.println("onKey: " + keyCode);
    			if (UsecodeMachine.running > 0 || gwin.busyMessage != null)
    				return false;
    			Gump.Modal modal = GameSingletons.gumpman.getModal();
		        if (event.getAction() == KeyEvent.ACTION_DOWN) {
		        	if (modal != null) {
		        		modal.keyDown(keyCode);
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
		        	case KeyEvent.KEYCODE_C:	
			        	if (event.isAltPressed()) {
			        		GameSingletons.clock.fakeNextPeriod();
			        		return true;
			        	} else {
			        		Shortcuts.combat();
			        	}
			        	return true;
			        case KeyEvent.KEYCODE_G:
				        if (event.isAltPressed()) {
				        	GameSingletons.cheat.toggleGodMode();
				        	return true;
				        } else
				        	return false;
		        	case KeyEvent.KEYCODE_H:
			        	if (event.isAltPressed()) {
			        		GameSingletons.cheat.toggleHackMover();
			        		return true;
			        	} else
			        		return false;
			        case KeyEvent.KEYCODE_I:
			        	if (!event.isAltPressed()) {
			        		Shortcuts.inv();
					    } else {
					    	GameSingletons.cheat.toggleInfravision();
					    }
					    return true;
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
		        	case KeyEvent.KEYCODE_P:
			        	if (event.isAltPressed()) {
			        		GameSingletons.cheat.togglePickpocket();
			        		return true;
			        	} else
			        		return false;
		        	case KeyEvent.KEYCODE_S:
		        		if (event.isAltPressed()) 
		        			//GameSingletons.audio.startSpeech(EUtil.rand()%4);
		        			GameSingletons.ucmachine.doSpeech(EUtil.rand()%8);
		        			//GameSingletons.audio.playFile(
		        			//		EUtil.getSystemPath(EFile.INTROSND), false);
		        		else if (!event.isShiftPressed()) {
		        			Shortcuts.save(instance);
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
		        	case KeyEvent.KEYCODE_W:
		        		if (event.isAltPressed()) {
		        			GameSingletons.cheat.toggleWizardMode();
		        			return true;
		        		} else
		        			return false;
		        	case KeyEvent.KEYCODE_X:
		        		if (event.isAltPressed()) {
		        			ExultActivity.instance.finish();
		        			return true;
		        		} else
		        			return false;
		        	case KeyEvent.KEYCODE_Z:
		        		if (!event.isAltPressed()) {
		        			Shortcuts.stats();
		        			return true;
		        		} else {
		        			return false;
		        		}
		        	case KeyEvent.KEYCODE_EQUALS:
		        		if (event.isShiftPressed()) { // +
		        			Shortcuts.zoom();
		        			return true;
		        		} else
		        			return true;
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
    		if (thread == null)
    			thread = new MySurfaceThread(getHolder(), this);
   			thread.start();
    		thread.setRunning(true);
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
    		thread = null;
    	}
    } // End of MySurfaceView
   
}