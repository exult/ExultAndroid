package com.exult.android;
import android.graphics.Point;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.Color;
import java.util.Arrays;

public class ImageBuf {
	private int width, height;
	private int clipx, clipy, clipw, cliph;
	private byte pixels[];		// The RGB data.
	private int rgba[];			// Buffer for transfering to canvas.
	private int pal[];			// Palette.
	private int clipbuf[] = new int[3];		// srcx, srcw, destx
	private Rectangle tempClipSrc = new Rectangle();
	private Point tempClipDest = new Point();
	private Rectangle tempShowRect = new Rectangle();
	private Bitmap toScale;
	private int scalew, scaleh;
	private Rect scaleSrc, scaleDest;
	
	private boolean clipInternal(int clips, int clipl) {
		if (clipbuf[2] < clips) {
			if ((clipbuf[1] += (clipbuf[2] - clips)) <= 0)
				return (false);
			clipbuf[0] -= (clipbuf[2] - clips);
			clipbuf[2] = clips;
		}
		if (clipbuf[2] + clipbuf[1] > (clips + clipl))
			if ((clipbuf[1] = ((clips + clipl) - clipbuf[2])) <= 0)
				return (false);
		return (true);
	}
	public ImageBuf(int w, int h) {
		width = w; height = h;
		clipx = clipy = 0;
		clipw = w; cliph = h;
		rgba = new int[w*h];
		pixels = new byte[w*h];
		pal = new int[256];
	}
	public void setToScale(int w, int h) {
		scalew = w; scaleh = h;
		toScale = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		scaleSrc = new Rect();
		scaleDest = new Rect();
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public void setClip(int x, int y, int w, int h) {
		clipx = x; clipy = y; clipw = w; cliph = h;
	}
	public void clearClip() {
		clipx = clipy = 0; clipw = width; cliph = height;
	}
	/*
	 * Utility to create each palette color.
	 */
	private int getColor8 (
		byte val,
		int maxval,
		int brightness) {		// 100=normal.
		int c = (((int)val&0xff)*brightness*255)/
							(100*maxval);
		c = c & 0xff;
		return (c <= 255 ? c : 255);
	}
	//	Set palette from 256 3-byte entries.
	public void setPalette(
		byte rgbs[],			// 256 3-byte entries.
		int maxval,				// Highest val. for each color.
		int brightness			// Brightness control (100 = normal).
		) {
		for (int i = 0; i < 256; i++) {
			int r = getColor8(rgbs[3*i], maxval, brightness);
			int g  = getColor8(rgbs[3*i + 1], maxval, brightness);
			int b  = getColor8(rgbs[3*i + 2], maxval, brightness);
			pal[i] = (0xff<<24)|(r<<16) | (g<<8) | b;	// Include alpha = 0xff.
		}
	}
	public byte [] getPixels() {
		return pixels;
	}
	// Clip in one dim.  Sets clipbuf.
	private boolean clipX(int srcx, int srcw, int destx, int desty) {
		if (desty < clipy || desty >= clipy + cliph)
			return false;
		clipbuf[0] = srcx; clipbuf[1] = srcw; clipbuf[2] = destx;
		return clipInternal(clipx, clipw); 
	}
	public boolean clip(Rectangle src, Point dest) {
	// Start with x-dim.
		clipbuf[0] = src.x; clipbuf[1] = src.w; clipbuf[2] = dest.x;
		if (clipInternal(clipx, clipw)) {
			src.x = clipbuf[0]; src.w = clipbuf[1]; dest.x = clipbuf[2];
		} else
			return false;
		clipbuf[0] = src.y; clipbuf[1] = src.h; clipbuf[2] = dest.y;
		if (clipInternal(clipy, cliph)) {
			src.y = clipbuf[0]; src.h = clipbuf[1]; dest.y = clipbuf[2];
		} else
			return false;
		return true;
	}
	/*
	 * 	Fill with a single byte.
	 */
	public void fill8(byte v) {
		int i, cnt = width*height;
		for (i = 0; i < cnt; ++i)
			pixels[i] = v;
	}
	/*
	 *	Fill a rectangle with an 8-bit value.
	 */
	public void fill8(byte pix, int srcw, int srch, int destx, int desty) {
		int srcx = 0, srcy = 0;
		tempClipSrc.set(srcx, srcy, srcw, srch);
		tempClipDest.set(destx, desty);
		if (!clip(tempClipSrc, tempClipDest))
			return;
		int ind = tempClipDest.y*width + tempClipDest.x;
		int to_next = width - tempClipSrc.w;	// # pixels to next line.
		while (tempClipSrc.h-- > 0) {			// Do each line.
			for (int cnt = tempClipSrc.w; cnt > 0; cnt--)
				pixels[ind++] = pix;
			ind += to_next;	// Get to start of next line.
		}
	}
	public void fillLine8(byte pix, int srcw, int destx, int desty) {
		int srcx = 0;
		// Constrain to window's space.
		if (!clipX(srcx, srcw, destx, desty))
			return;
		srcx = clipbuf[0]; srcw = clipbuf[1]; destx = clipbuf[2];
		int start = desty*width + destx;
		Arrays.fill(pixels, start, start + srcw, pix);
	}
	// Is rect. visible within clip?
	public boolean isVisible(int x, int y, int w, int h)
		{ return (!(x >= clipx + clipw || y >= clipy + cliph ||
			x + w <= clipx || y + h <= clipy)); }
	public synchronized void show(Canvas c, int x, int y, int w, int h) {
		Rectangle rect = tempShowRect;
		rect.set(x, y, w, h);
		rect.enlarge(4, 4, 4, 4, width, height);	// Increase area by 4.
		x = rect.x; y = rect.y; w = rect.w; h = rect.h;
		// Make it 4 pixel aligned too
		int dx = x&3;
		int dy = y&3;
		x -= dx;
		w += dx;
		y -= dy;
		h += dy;
		if ((w&3) != 0) w += 4-(w&3);
		if ((h&3) != 0) h += 4-(h&3);
		if (w + x > width) w = width - x;
		if (h + y > height) h = height - y;
		w &= ~3;
		h &= ~3;
		// Fill RGBA using palette.
		int to = 0, from = y*width + x, hcnt = h;
		while (hcnt-- > 0) {
			for (int i = 0; i < w; ++i) {
				rgba[to++] = pal[(int)pixels[from++]&0xff];
			}
			from += width - w;
		}
		if (toScale == null)
			c.drawBitmap(rgba, 0, w, x, y, w, h, false, null);
		else 
			blit(c);// +++++++++FOR NOW.
	}
	public void blit(Canvas c) {
		if (toScale != null) {
			toScale.setPixels(rgba, 0, width, 0, 0, width, height);
			scaleSrc.set(0, 0, width - 1, height - 1);
			scaleDest.set(0, 0, scalew - 1, scaleh - 1);
			c.drawBitmap(toScale, scaleSrc, scaleDest, null);
		} else
			c.drawBitmap(rgba, 0, width, 0, 0, width, height, false, null);
	}
	public void show(Canvas c) {
		show(c, 0, 0, width, height);
	}
	/*
	 *	Copy an area of the image within itself.
	 */

	public void copy
		(
		int srcx, int srcy,		// Where to start.
		int srcw, int srch,		// Dimensions to copy.
		int destx, int desty		// Where to copy to.
		) {
		int ynext, yfrom, yto;		// Figure y stuff.
		if (srcy >= desty) {		// Moving up?
			ynext = width;
			yfrom = srcy;
			yto = desty;
		} else {			// Moving down.
			ynext = -width;
			yfrom = srcy + srch - 1;
			yto = desty + srch - 1;
		}
		int to = yto*width + destx;
		int from = yfrom*width + srcx;
						// Go through lines.
		while (srch-- > 0) {
			System.arraycopy(pixels, from, pixels, to, srcw);
			to += ynext;
			from += ynext;
		}
	}
	/*
	 *	Copy another rectangle into this one.
	 */
	public void copy8
		(
		byte src_pixels[],		// Source rectangle pixels.
		int start,
		int srcw, int srch,		// Dimensions of source.
		int destx, int desty
		) {
		int srcx = 0, srcy = 0;
		int src_width = srcw;		// Save full source width.
									// Constrain to window's space.
		tempClipSrc.set(srcx, srcy, srcw, srch);
		tempClipDest.set(destx, desty);
		if (!clip(tempClipSrc, tempClipDest))
			return;
		int to = tempClipDest.y*width + tempClipDest.x;
		int from = start + tempClipSrc.y*src_width + tempClipSrc.x;
		// int dlen = pixels.length; int slen = src_pixels.length; //++++DEBUGGING
		while (tempClipSrc.h-- > 0) {
			for (int i = 0; i < tempClipSrc.w; ++i) {
				pixels[to + i] = src_pixels[from + i];
			}
			from += src_width;
			to += width; 
		}
	}
	// Slightly Optimized RLE Painter
	public void paintRle (int xoff, int yoff, byte inptr[])
	{
		int in = 0;
		int scanlen;
		int right = clipx+clipw;
		int bottom = clipy+cliph;

		while ((scanlen = EUtil.Read2(inptr, in)) != 0)
		{
			in += 2;
						// Get length of scan line.
			boolean encoded = (scanlen&1) != 0;// Is it encoded?
			scanlen = scanlen>>1;
			int scanx = xoff + (short)EUtil.Read2(inptr, in);
			in += 2;
			int scany = yoff + (short)EUtil.Read2(inptr, in);
			in += 2;
			// Is there somthing on screen?
			boolean on_screen = true;
			if (scanx >= right || scany >= bottom || scany < clipy || scanx+scanlen < clipx)
				on_screen = false;

			if (!encoded) {	// Raw data?
				// Only do the complex calcs if we think it could be on screen
				if (on_screen) {
					// Do we need to skip pixels at the start?
					if (scanx < clipx) {
						int delta = clipx-scanx;
						in += delta;
						scanlen -= delta;
						scanx = clipx;
					}
					// Do we need to skip pixels at the end?
					int skip = scanx+scanlen - right;
					if (skip < 0) skip = 0;
					// Is there anything to put on the screen?
					if (skip < scanlen) {
						int dest = scany*width + scanx;
						int end = in+scanlen-skip;
 						while (in < end) pixels[dest++] = inptr[in++];
						in += skip;
						continue;
					}
				}
				in += scanlen;
				continue;
			} else {	// Encoded
				int dest = scany*width + scanx;
				while (scanlen > 0) {
					int bcnt = (int)inptr[in++]&0xff;
							// Repeat next char. if odd.
					int repeat = bcnt&1;
					bcnt = bcnt>>1; // Get count.

					// Only do the complex calcs if we think it could be on screen
					if (on_screen && scanx < right && scanx+bcnt > clipx) {
						if (repeat != 0)	{ // Const Colour
							// Do we need to skip pixels at the start?
							if (scanx < clipx) {
								int delta = clipx-scanx;
								dest += delta;
								bcnt -= delta;
								scanlen -= delta;
								scanx = clipx;
							}

							// Do we need to skip pixels at the end?
							int skip = scanx+bcnt - right;
							if (skip < 0) skip = 0;
							// Is there anything to put on the screen?
							if (skip < bcnt) {
								byte col = inptr[in++];
								int end = dest + bcnt - skip;
								while (dest < end) 
									pixels[dest++] = col;
								scanx += bcnt;
								scanlen -= bcnt;
								continue;
							}

							// Make sure all the required values get
							// properly updated
							scanx += bcnt;
							scanlen -= bcnt;
							++in;
							continue;
						} else {
							// Do we need to skip pixels at the start?
							if (scanx < clipx) {
								int delta = clipx-scanx;
								dest += delta;
								in += delta;
								bcnt -= delta;
								scanlen -= delta;
								scanx = clipx;
							}
							// Do we need to skip pixels at the end?
							int skip = scanx+bcnt - right;
							if (skip < 0) skip = 0;
							// Is there anything to put on the screen?
							if (skip < bcnt) {
								int end = dest + bcnt - skip;
								while (dest < end) 
									pixels[dest++] = inptr[in++];
								in += skip;
								scanx += bcnt;
								scanlen -= bcnt;
								continue;
							}
							// Make sure all the required values get
							// properly updated
							scanx += bcnt;
							scanlen -= bcnt;
							in += bcnt;
							continue;
						}
					}

					// Make sure all the required values get
					// properly updated

					dest += bcnt;
					scanx += bcnt;
					scanlen -= bcnt;
					if (repeat == 0) 
						in += bcnt;
					else 
						++in;
					continue;
				}
			}
		}
	}
	/*
	 *	This class represents a single transparent color by providing a
	 *	palette for its effect on all the other colors.
	 */
	public class XformPalette {
		public byte colors[] = new byte[256];	// For transforming 8-bit colors.
		public byte get(int i)
			{ return colors[i]; }
		};
	/*
	 *	Copy a line into this buffer where some of the colors are translucent.
	 */
	public void copyLineTranslucent8
		(
		byte src_pixels[],	// Source rectangle pixels.
		int start,
		int srcw,			// Width to copy.
		int destx, int desty,
		int first_translucent,		// Palette index of 1st trans. color.
		int last_translucent,		// Index of last trans. color.
		XformPalette xforms[]		// Transformers.  Need same # as
							//   (last_translucent - 
							//    first_translucent + 1).
		) {
		int srcx = 0;
						// Constrain to window's space.
		if (!clipX(srcx, srcw, destx, desty))
			return;
		srcx = clipbuf[0]; srcw = clipbuf[1]; destx = clipbuf[2];
		int to = desty*width + destx;
		int from = start + srcx;
		byte newc;
		for (int i = srcw; i > 0; i--) {
						// Get char., and transform.
			int c = src_pixels[from++]&0xff;
			if (c >= first_translucent && c <= last_translucent)
						// Use table to shift existing pixel.
				newc = xforms[c - first_translucent].colors[(int)pixels[to]&0xff];
			else
				newc = (byte) c;
			pixels[to++] = newc;
		}
	}
	/*
	 *	Apply a translucency table to a line.
	 */
	public void fillLineTranslucent8
		(
		int srcw,
		int destx, int desty,
		XformPalette xform		// Transform table.
		)
		{
		int srcx = 0;
						// Constrain to window's space.
		if (!clipX(srcx, srcw, destx, desty))
			return;
		srcx = clipbuf[0]; srcw = clipbuf[1]; destx = clipbuf[2];
		int ind = desty*width + destx;
		while (srcw-- > 0){
			pixels[ind] = xform.colors[(int)pixels[ind]&0xff];
			ind++;
		}
	}
}
