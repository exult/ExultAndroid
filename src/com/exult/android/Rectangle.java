package com.exult.android;

public class Rectangle {
	public int x, y, w, h;
	public Rectangle(int xin, int yin, int win, int hin) {
		x = xin; y = yin; w = win; h = hin;
	}
	public void enlarge(int left, int right, int top, int bottom, int maxw, int maxh) {
		x -= left; w += left+right;
		y -= top; h += top+bottom;

		if (x < 0) { w += x; x = 0; }
		if (y < 0) { h += y; y = 0; }

		if (x + w > maxw) w = maxw - x;
		if (y + h > maxh) h = maxh - y;
	}
}
