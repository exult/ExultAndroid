package com.exult.android;
import java.util.Vector;

public abstract class Gump extends ShapeID {
	protected int x, y;			// Location on screen.
	protected Vector<GumpWidget> elems;	// ie, checkmarks.
	protected boolean handlesKbd;
	protected void addElem(GumpWidget w) {
		elems.add(w);
	}
	public Gump(int initx, int inity, int shnum) {
		super(shnum, 0, ShapeFiles.GUMPS_VGA);
		elems = new Vector<GumpWidget>();
		x = initx; y = inity;
		gumpman.addGump(this);
	}
	// Create centered.
	public Gump(int shnum) {
		super(shnum, 0, ShapeFiles.GUMPS_VGA);
		elems = new Vector<GumpWidget>();
		setPos();
		gumpman.addGump(this);
	}
	public void close() {
		gumpman.closeGump(this);
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
	public final void paintElems() {
		int cnt = elems.size();
		for (int i = 0; i < cnt; ++i)
			elems.elementAt(i).paint();
	}
	public final boolean canHandleKbd() {
		return handlesKbd;
	}
	public GameObject getContainer() {
		return null;
	}
	public void getShapeRect(Rectangle r, GameObject obj) {
		r.set(0,0,0,0);	// Overridden for containers.
	}
	public GameObject findObject(int mx, int my) {
		return null;
	}
	public boolean isPersistent() {
		return false;
	}
	public boolean hasPoint(int sx, int sy) {
		ShapeFrame s = getShape();
		return s != null && s.hasPoint(sx - x, sy - y);
	}
	/*
	 *	Is a given screen point on the checkmark?
	 *
	 *	Output: .button if so.
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
	/*
	 *	A generic gump used by generic containers:
	 */
	public static class Container extends Gump {
		private ContainerGameObject container;
		private Rectangle objectArea = new Rectangle();
		private Rectangle paintBox = new Rectangle();
		private void initialize(int shnum) {		// Initialize objectArea.
			//+++++FINISH	
			setObjectArea(48, 20, 66, 44, 8, 66);//++++TESTING: BAG
		}
		public Container(ContainerGameObject cont, int initx, int inity, 
								int shnum) {
			super(initx, inity, shnum);
			container = cont;
			initialize(shnum);
		}
						// Create centered.
		public Container(ContainerGameObject cont, int shnum) {
			super(shnum);
			container = cont;
			initialize(shnum);
		}
		private void setObjectArea(int x, int y, int w, int h, 
									int checkx, int checky) {
			objectArea.set(x, y, w, h);
			checkx += 16; checky -= 12;
			elems.add(new GumpWidget.Checkmark(this, checkx, checky));
		}
		public GameObject getContainer() {
			return container;
		}
		public void getShapeRect(Rectangle r, GameObject obj) {
			ShapeFrame s = obj.getShape();
			if (s == null)
				r.set(0, 0, 0, 0);
			r.set(x + objectArea.x + obj.getTx() - s.getXLeft(), 
					 y + objectArea.y + obj.getTy() - s.getYAbove(), 
						 s.getWidth(), s.getHeight());
		}
		public GameObject findObject(int mx, int my) {
			int cnt = 0;
			if (container == null)
				return null;
			ObjectList.ObjectIterator iter = container.getIterator();
			GameObject obj, found = null;
			Rectangle box = new Rectangle();
			while ((obj = iter.next()) != null) {
				getShapeRect(box, obj);
				if (box.hasPoint(mx, my)) {
					ShapeFrame s = obj.getShape();
					int ox = x + objectArea.x + obj.getTx(),
						oy = y + objectArea.y + obj.getTy();
					if (s.hasPoint(mx-ox, my-oy))
						found = obj;
				}
			}
							// ++++++Return top item.
			return found;
		}
		public void paint() {
			super.paint();
			if (container == null)
				return;
			// Set box to screen location.
			paintBox.set(objectArea.x + x, objectArea.y + y, objectArea.w,
															objectArea.h);
			ObjectList.ObjectIterator iter = container.getIterator();
			int cury = 0, curx = 0;
			int endy = paintBox.h, endx = paintBox.w;
			int loop = 0;			// # of times covering container.
			GameObject obj;
			while ((obj = iter.next()) != null) {
				ShapeFrame shape = obj.getShape();
				if (shape == null)
					continue;
				int objx = obj.getTx() - shape.getXLeft() + 
									1 + objectArea.x;
				int objy = obj.getTy() - shape.getYAbove() + 
									1 + objectArea.y;
							// Does obj. appear to be placed?
				if (!objectArea.hasPoint(objx, objy) ||
				    !objectArea.hasPoint(objx + shape.getXRight() - 1,
							objy + shape.getYBelow() - 1))
				{		// No.
					int px = curx + shape.getWidth(),
					    py = cury + shape.getHeight();
					if (px > endx)
						px = endx;
					if (py > endy)
						py = endy;
					obj.setShapePos(px - shape.getXRight(),
							py - shape.getYBelow());
							// Mostly avoid overlap.
					curx += shape.getWidth() - 1;
					if (curx >= endx)
					{
						cury += 8;
						curx = 0;
						if (cury >= endy)
							cury = 2*(++loop);
					}
				}
				obj.paintShape(paintBox.x + obj.getTx(),
								paintBox.y + obj.getTy());
			}
		}
	}
}
