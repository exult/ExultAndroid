package com.exult.android;
import java.util.Vector;

public abstract class Gump extends ShapeID {
	protected int x, y;			// Location on screen.
	protected Vector<GumpWidget> elems;	// ie, checkmarks.
	protected void addElem(GumpWidget w) {
		elems.add(w);
	}
	
	public Gump(int shnum) {
		super(shnum, 0, ShapeFiles.GUMPS_VGA);
		setPos();
	}
	public final int getX() {
		return x;
	}
	public final int getY() {
		return y;
	}
	// Set centered.
	public final void setPos() {
		ShapeFrame shape = getShape();
		x = (gwin.getWidth() - shape.getWidth())/2;
		y = (gwin.getHeight() - shape.getHeight())/2;
	}
	//	Get area covered by gump and its contents.
	public void getDirty(Rectangle rect) {
		ShapeFrame s = getShape();
		if (s == null) 
			rect.set(0,0,0,0);
		else
			rect.set(x - s.getXLeft(), 	y - s.getYAbove(),
				s.getWidth(), s.getHeight());
	}
	public void paint() {
		paintShape(x, y);
		gwin.setPainted();
		paintElems();		// Checkmark, buttons.
	}
	public void paintElems() {
		int cnt = elems.size();
		for (int i = 0; i < cnt; ++i)
			elems.elementAt(i).paint();
	}
	public boolean hasPoint(int sx, int sy) {
		ShapeFrame s = getShape();
		return s != null && s.hasPoint(sx - x, sy - y);
	}
	/*
	 *	Is a given screen point on the checkmark?
	 *
	 *	Output: ->button if so.
	 */
	public GumpWidget.Button onButton
		(
		int mx, int my			// Point in window.
		) {
		int cnt = elems.size();
		for (int i = 0; i < cnt; ++i) {
			GumpWidget w = elems.elementAt(i);
			GumpWidget.Button b = w.onButton(mx, my);
			if (b != null)
				return b;
		}
		return null;
	}
}
