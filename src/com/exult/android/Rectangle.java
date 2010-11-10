package com.exult.android;

public class Rectangle {
	public int x, y, w, h;
	public Rectangle(int xin, int yin, int win, int hin) {
		x = xin; y = yin; w = win; h = hin;
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
	public void enlarge(int left, int right, int top, int bottom, int maxw, int maxh) {
		x -= left; w += left+right;
		y -= top; h += top+bottom;

		if (x < 0) { w += x; x = 0; }
		if (y < 0) { h += y; y = 0; }

		if (x + w > maxw) w = maxw - x;
		if (y + h > maxh) h = maxh - y;
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
}
