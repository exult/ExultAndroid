package com.exult.android;

public class Rectangle {
	public int x, y, w, h;
	public Rectangle(int xin, int yin, int win, int hin) {
		x = xin; y = yin; w = win; h = hin;
	}
	public Rectangle(Rectangle r) {
		x = r.x; y = r.y; w = r.w; h = r.h;
	}
	public Rectangle() {
		x = y = w = h = -1;
	}
	public final void set(int xx, int yy, int ww, int hh) {
		x = xx; y = yy; w = ww; h = hh;
	}
	public final void set(Rectangle r) {
		x = r.x; y = r.y; w = r.w; h = r.h;
	}
	@Override
	public String toString() {
		return "Rect("+ x + "," + y + "," + w + "," + h + ")";
	}
	public final boolean hasPoint(int px, int py) {
		return (px >= x && px < x + w && py >= y && py < y + h);
	}
	public final void enlarge(int left, int right, int top, int bottom, int maxw, int maxh) {
		x -= left; w += left+right;
		y -= top; h += top+bottom;

		if (x < 0) { w += x; x = 0; }
		if (y < 0) { h += y; y = 0; }

		if (x + w > maxw) w = maxw - x;
		if (y + h > maxh) h = maxh - y;
	}
	public void shift(int deltax, int deltay) {
		x += deltax;
		y += deltay;
	}		
	public final void enlarge(int delta) {
		x -= delta; y -= delta; w += 2*delta; h += 2*delta;
	}
	// Does it intersect another?
	public boolean intersects(Rectangle r2)	{
		return (x >= r2.x + r2.w ? false : r2.x >= x + w ? false :
		y >= r2.y + r2.h ? false : r2.y >= y + h ? false : true);
	}
	//	Intersect another with this.
	public final void intersect(Rectangle r2) {
		int xend = x + w, yend = y + h;
		int xend2 = r2.x + r2.w, yend2 = r2.y + r2.h;
		x = x >= r2.x ? x : r2.x;
		y = y >= r2.y ? y : r2.y;
		w = (xend <= xend2 ? xend : xend2) - x;
		h = (yend <= yend2 ? yend : yend2) - y;
	}
	public final void add(Rectangle r2) {
		int xend = x + w, yend = y + h;
		int xend2 = r2.x + r2.w, yend2 = r2.y + r2.h;
		x = x < r2.x ? x : r2.x;
		y = y < r2.y ? y : r2.y;
		w = (xend > xend2 ? xend : xend2) - x;
		h = (yend > yend2 ? yend : yend2) - y;
	}
	public final boolean equals(Rectangle r2) {
		return x == r2.x && y == r2.y && w == r2.w && h == r2.h;
	}
}
