package com.exult.android;
import android.view.KeyEvent;
import java.util.Observable;
import java.util.Observer;

public class SliderGump extends Gump.Modal {
	private Reporter reporter;
	protected int diamondx;			// Rel. pos. where diamond is shown.
	protected static final short diamondy = 6;
	protected int min_val, max_val;		// Max., min. values to choose from.
	protected int step_val;			// Amount to step by.
	protected int val;			// Current value.
	protected boolean dragging;		// 1 if dragging the diamond.
	protected int prev_dragx;			// Prev. x-coord. of mouse.
	protected int pushed_newval = -1;	// For pressing on the slider.
	protected void setVal(int newval) {	// Set to new value.
		val = newval;
		int xdist = xmax - xmin;
		if(max_val-min_val==0) {
			val=0;
			diamondx=xmin;
		} else
			diamondx = xmin + ((val - min_val)*xdist)/(max_val - min_val);
	}
						// Coords:
	protected static final short leftbtnx = 31, rightbtnx = 103, btny = 14;
	protected static final short xmin = 35, xmax = 93;

	protected ShapeID	diamond;		// Diamond

	public SliderGump(int mival, int mxval, int step, int defval, Observer c) {
		super(game.getShape("gumps/slider"));
		min_val = mival; max_val = mxval;
		step_val = step;
		val = defval;
		diamond = new ShapeID(game.getShape("gumps/slider_diamond"), 0, ShapeFiles.GUMPS_VGA);
		addCheckMark(6, 30);
		addElem(new SliderButton(this, leftbtnx, btny, 
						game.getShape("gumps/slider_left"), true));
		addElem(new SliderButton(this, rightbtnx, btny, 
						game.getShape("gumps/slider_right"), false));
							// Init. to middle value.
		if (defval < min_val)
		  defval = min_val;
		else if (defval > max_val)
		  defval = max_val;
		setVal(defval);
		reporter = new Reporter();
		reporter.addObserver(c);
	}
	public void setDone() {		// Done from 'yes'/'no' button.
		reporter.setChanged();
		reporter.notifyObservers(this);
	}
	private static class Reporter extends Observable {
		public void setChanged() { super.setChanged(); }
	}
	public int getVal()			// Get last value set.
		{ return val; }
						// An arrow was clicked on.
	public void clickedLeftArrow() {
		moveDiamond(-step_val);
	}
	public void clickedRightArrow() {
		moveDiamond(step_val);
	}
	public void moveDiamond(int dir) {
		int newval = val;
		newval += dir;
		if (newval < min_val)
			newval = min_val;
		if (newval > max_val)
			newval = max_val;

		setVal(newval);
		paint();
		gwin.setPainted();
	}
						// Paint it and its contents.
	public void paint() {
		final int textx = 128, texty = 7;
		// Paint the gump itself.
		super.paint();
		// Paint red "checkmark".
		paintElems();
		// Paint slider diamond.
		diamond.paintShape(x + diamondx, y + diamondy);
		// Print value.
		gumpman.paintNum(val, x + textx, y + texty);
		gwin.setPainted();
	}
	@Override
	public void close() {
		super.close();
		setDone();
		done = true; 
	}
	//	Clicktracker
	@Override
	public void onDown(int mx, int my) {
		dragging = false;
		GumpWidget.Button btn = super.onButton(mx, my);
		if (btn != null)
			pushed = btn;
		else
			pushed = null;
		if (pushed != null) {
			if (!pushed.push(true)) 
				pushed = null;
			return;
		}
						// See if on diamond.
		ShapeFrame d_shape = diamond.getShape();
		if (d_shape.hasPoint(mx - (x + diamondx), my - (y + diamondy))) {
			// Start to drag it.
			dragging = true;
			prev_dragx = mx;
		} else {
			if(my-getY()<diamondy || my-getY()>diamondy+d_shape.getHeight())
				return;
			diamondx = mx-getX();
			if(diamondx<xmin)
				diamondx = xmin;
			if(diamondx>xmax)
				diamondx = xmax;
			final int xdist = xmax - xmin;
			int delta = (diamondx - xmin)*(max_val - min_val)/xdist;
						// Round down to nearest step.
			delta -= delta%step_val;
			pushed_newval = min_val + delta;
		}
	}
	@Override
	public void onUp(int mx, int my) {
		int newval = pushed_newval;
		pushed_newval = -1;
		if (dragging) {			// Done dragging?
			setVal(val);		// Set diamond in correct pos.
			paint();
			gwin.setPainted();
			dragging = false;
		}
		if (pushed == null) {
			if (newval >= 0 && newval != val) {
				val = newval;
				paint();
			}
			return;
		}
		pushed.unpush(true);
		if (pushed.onButton(mx, my) != null)
			pushed.activate(true);
		pushed = null;
	}
	@Override
	public void keyDown(int chr) { // Character typed.
		switch(chr) {		
		case KeyEvent.KEYCODE_ENTER:
			close();
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			clickedLeftArrow();
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			clickedRightArrow();
			break;
		} 
	}
	
	/*
	 *	One of the two arrow button on the slider:
	 */
	public static class SliderButton extends GumpWidget.Button {
		boolean is_left;
		public
		SliderButton(Gump par, int px, int py, int shapenum, boolean left) {
			super(par, shapenum, px, py);
			is_left = left;
		}
						// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) return false;
			if (is_left)
				((SliderGump) parent).clickedLeftArrow();
			else
				((SliderGump) parent).clickedRightArrow();
			return true;
		}
	}
}
