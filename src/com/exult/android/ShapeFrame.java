package com.exult.android;

public class ShapeFrame {
	private byte data[];			// The actual data.
	private int datalen;
	private short xleft;			// Extent to left of origin.
	private short xright;			// Extent to right.
	private short yabove;			// Extent above origin.
	private short ybelow;			// Extent below origin.
	private boolean rle;			// Run-length encoded.
	
	public byte[] getData() {
		return data;
	}
	public int getSize() {
		return datalen;
	}
	/*
	 *	Create a new frame by reflecting across a line running NW to SE.
	 *
	 *	May return 0.
	 */
	public ShapeFrame reflect() {
		if (data == null)
			return null;
		int w = getWidth(), h = getHeight();
		if (w < h)
			w = h;
		else
			h = w;			// Use max. dim.
		ShapeFrame reflected = new ShapeFrame();
		reflected.rle = true;		// Set data.
		reflected.xleft = yabove;
		reflected.yabove = xleft;
		reflected.xright = ybelow;
		reflected.ybelow = xright;
						// Create drawing area.
		ImageBuf ibuf = new ImageBuf(h, w);
		ibuf.fill8((byte)255);		// Fill with 'transparent' pixel.
						// Figure origin.
		int xoff = reflected.xleft, yoff = reflected.yabove;
		int in = 0; 	// Point to data, and draw.
		int scanlen;
		while ((scanlen = EUtil.Read2(data, in)) != 0) {
			in += 2;
						// Get length of scan line.
			boolean encoded = (scanlen&1) != 0;// Is it encoded?
			scanlen = scanlen>>1;
			int scanx = (short)EUtil.Read2(data, in);
			int scany = (short)EUtil.Read2(data, in + 2);
			in += 4;
			if (!encoded) {		// Raw data?
				ibuf.copy8(data, in, 1, scanlen,
						xoff + scany, yoff + scanx);
				in += scanlen;
				continue;
				}
			for (int b = 0; b < scanlen; )
				{
				byte bcnt = data[in++];
						// Repeat next char. if odd.
				boolean repeat = (bcnt&1) != 0;
				bcnt = (byte)(bcnt>>1); // Get count.
				if (repeat) {
					byte pix = data[in++];
					ibuf.fill8(pix, 1, bcnt,
						xoff + scany, yoff + scanx + b);
				} else {	// Get that # of bytes.
					ibuf.copy8(data, in, 1, bcnt,
						xoff + scany, yoff + scanx + b);
					in += bcnt;
				}
				b += bcnt;
				}
			}
		reflected.createRle(ibuf.getPixels(), w, h);
		return (reflected);
		}
	public ShapeFrame() {
		data = null;
		datalen = 0;
	}
									// Create frame from data.
	public ShapeFrame(byte pixels[], int w, int h, int xoff, int yoff, boolean setrle) {
		xleft = (short) xoff;
		xright = (short) (w - xoff - 1); 
		yabove = (short) yoff;
		ybelow = (short)(h - yoff - 1);
		rle = setrle;
		if (!rle) {
			datalen = EConst.c_num_tile_bytes;
			data = new byte[EConst.c_num_tile_bytes];
			data = pixels;
		}
		else {
			//data = encodeRle(pixels, w, h, xleft, yabove, datalen);
		}
	}
	/*
	 *	Read in a desired shape.
	 *
	 *	Output:	# of frames.
	 */
	public int read
		(
		byte shapes[],		// Data for this entire shape.
		int shapelen,		// Length expected for detecting RLE.
		int frnum			// Frame #.
		) {
		int framenum = frnum;
		rle = false;
		if (shapelen == 0) 
			return 0;
		int dlen = EUtil.Read4(shapes, 0);
		int hdrlen = EUtil.Read4(shapes, 4);
		if (dlen == shapelen) {
			rle = true;		// It's run-length-encoded.
							// Figure # frames.
			int nframes = (hdrlen - 4)/4;
			if (framenum >= nframes)// Bug out if bad frame #.
				return (nframes);
					// Get frame offset, lengeth.
			int frameoff, framelen;
			if (framenum == 0) {
				frameoff = hdrlen;
				framelen = nframes > 1 ? EUtil.Read4(shapes, 8) - frameoff :
					dlen - frameoff;
			} else {
				int from = 8 + (framenum - 1) * 4;
				frameoff = EUtil.Read4(shapes, from);
					// Last frame?
				if (framenum == nframes - 1)
					framelen = dlen - frameoff;
				else
					framelen = EUtil.Read4(shapes, from + 4) - frameoff;
			}
					// Get compressed data.
			getRleShape(shapes, frameoff, framelen);
					// Return # frames.
			return (nframes);
		}
		framenum &= 31;			// !!!Guessing here.
		xleft = yabove = EConst.c_tilesize;		// Just an 8x8 bitmap.
		xright= ybelow = -1;
		data = new byte[EConst.c_num_tile_bytes];	// Read in 8x8 pixels.
		datalen = EConst.c_num_tile_bytes;
		System.arraycopy(shapes, framenum*EConst.c_num_tile_bytes, data, 0, datalen);
		return (shapelen/EConst.c_num_tile_bytes);		// That's how many frames.
}

	/*
	 * Read RLE shape.
	 */
	private void getRleShape
		(
		byte shapes[],		// Data read from entire shape.
		int framePos,		// Position in data.
		int len			// Length of entire frame data.
		) {
		xright = (short)EUtil.Read2(shapes, framePos);
		xleft = (short)EUtil.Read2(shapes, framePos+2);
		yabove = (short)EUtil.Read2(shapes, framePos+4);
		ybelow = (short)EUtil.Read2(shapes, framePos+6);
		len -= 8;			// Subtract what we just read.
		data = new byte[len + 2];	// Allocate and read data.
		datalen = len+2;
		System.arraycopy(shapes, framePos+8, data, 0, len);
		data[len] = 0;			// 0-delimit.
		data[len + 1] = 0;
		rle = true;
	}
	/*
	 *	Skip transparent pixels.
	 *
	 *	Output: Ind of 1st non-transparent pixel, or w-x if not more non-transparent
	 */
	private static int SkipTransparent
		(
		byte pixels[],		// 8-bit pixel scan line.
		int ind,
		int x,				// X-coord. of pixel to start with.
		int w				// Remaining width of pixels.
		) {
		while (x < w && pixels[ind] == -1) {
			x++;
			ind++;
		}
		return (x);
	}
	/*
	 *	Split a line of pixels into runs, where a run
	 *	consists of different pixels, or a repeated pixel.
	 *
	 *	Output:	Index of end of scan line.
	 */

	private static int Find_runs(
			short runs[],	// Each run's length is returned.
							// For each byte, bit0==repeat.
							// List ends with a 0.
			byte pixels[],		// Scan line (8-bit color).
			int ind,
			int x,					// X-coord. of pixel to start with.
			int w					// Remaining width of pixels.
		) {
		int runcnt = 0;			// Counts runs.
		while (x < w && pixels[ind] != -1) {	// Stop at first transparent pixel.
			int run = 0;		// Look for repeat.
			while (x < w - 1 && pixels[ind] == pixels[ind + 1]) {
				x++;
				ind++;
				run++;
			}
			if (run > 0) {		// Repeated?  Count 1st, shift, flag.
				run = ((run + 1)<<1)|1;
				x++;			// Also pass the last one.
				ind++;
			} else do {			// Pass non-repeated run of any length.
				x++;
				ind++;
				run += 2;	// So we don't have to shift.
			}
			while (x < w && pixels[ind] != -1 &&
					(x == w - 1 || pixels[ind] != pixels[ind + 1]));
					// Store run length.
			runs[runcnt++] = (short) run;
		}
		runs[runcnt] = 0;		// 0-delimit list.
		return (x);
	}
	/*
	 *	Encode an 8-bit image into an RLE frame.
	 *
	 *	Output:	.allocated RLE data.
	 */

	public static byte[] encodeRle(
		byte pixels[],			// 8-bit uncompressed data.
		int w, int h,			// Width, height.
		int xoff, int yoff		// Origin (xleft, yabove).
		)
		{
						// Create an oversized buffer.
		byte buf[] = new byte[w*h*2 + 16*h];
		int out = 0;	// Index into buf.
		int ind = 0;
		int newx;			// Gets new x at end of a scan line.
		short runs[] = new short[200];
		for (int y = 0; y < h; y++) {	// Go through rows.
			int x, oldx = 0;
			ind = y*w;
			for (x = 0; (x = SkipTransparent(pixels, ind, x, w)) < w; x = oldx = newx) {
				ind += x - oldx;
				newx = Find_runs(runs, pixels, ind, x, w);
						// Just 1 non-repeated run?
				if (runs[1] == 0 && (runs[0]&1) == 0)
					{
					int len = runs[0] >> 1;
					out = EUtil.Write2(buf, out, runs[0]);
						// Write position.
					out = EUtil.Write2(buf, out, x - xoff);
					out = EUtil.Write2(buf, out, y - yoff);
					EUtil.Memcpy(buf, out, pixels, ind, len);
					ind += len;
					out += len;
					continue;
					}
						// Encoded, so write it with bit0==1.
				out = EUtil.Write2(buf, out, ((newx - x)<<1)|1);
						// Write position.
				out = EUtil.Write2(buf, out, x - xoff);
				out = EUtil.Write2(buf, out, y - yoff);
						// Go through runs.
				for (int i = 0; runs[i] != 0; i++)
					{
					int len = runs[i]>>1;
						// Check for repeated run.
					if ((runs[i]&1) != 0) {
						while (len > 0) {
							int c = len > 127
								? 127 : len;
							buf[out++] = (byte)((c<<1)|1);
							buf[out++] = pixels[ind];
							ind += c;
							len -= c;
						}
					} else while (len > 0) {
						int c = len > 127 ? 127 : len;
						buf[out++] = (byte)(c<<1);
						System.arraycopy(pixels, ind, buf, out, c);
						out += c;
						ind += c;
						len -= c;
					}
				}
			}
		}
		out = EUtil.Write2(buf, out, 0);			// End with 0 length.
		int len = out;		// Create buffer of correct size.
		byte data[] = new byte[len];
		EUtil.Memcpy(data, 0, buf, 0, len);
		return data;
		}
	/*
	 *	Encode an 8-bit image into an RLE frame.
	 *
	 *	Output:	Data is set to compressed image.
	 */

	private void createRle
		(
		byte pixels[],			// 8-bit uncompressed data.
		int w, int h			// Width, height.
		) {
		data = encodeRle(pixels, w, h, xleft, yabove);
		datalen = data.length;
	}
	public void createRle(byte pixels[], int xl, int ya, int w, int h) {
		xleft = (short)xl; yabove = (short)ya; 
		xright = (short)(xleft + w - 1); ybelow = (short)(yabove + h - 1);
		data = encodeRle(pixels, w, h, xleft, yabove);
		datalen = data.length;
		rle = true;
	}
	public final int getWidth()		// Get dimensions.
		{ return xleft + xright + 1; 
		}
	public final int getHeight()
		{ return yabove + ybelow + 1; 
		}
	public final int getXRight() {
		return xright;
	}
	public final int getXLeft() {
		return xleft;
	}
	public final int getYAbove() {
		return yabove;
	}
	public final int getYBelow() {
		return ybelow;
	}
	public final boolean isRle() {
		return rle;
	}
	public final boolean isEmpty() {
		return data[0] == 0 && data[1] == 0;
	}
	/*
	 *	Paint either type of shape.
	 */
	public void paint
		(
		ImageBuf win,			// Buffer to paint in.
		int xoff, int yoff		// Where to show in iwin.
		) {
		if (rle)
			paintRle(win, xoff, yoff);
		else
			win.copy8(data, 0, EConst.c_tilesize, EConst.c_tilesize,
					xoff - EConst.c_tilesize, yoff - EConst.c_tilesize);
		}
	public void paintRle
		(
		ImageBuf win,			// Buffer to paint in.
		int xoff, int yoff		// Where to show in iwin.
		) {
		int w = getWidth(), h = getHeight();
		if (w >= EConst.c_tilesize || h >= EConst.c_tilesize) {		// Big enough to check?  Off screen?
			if (!win.isVisible(xoff - xleft, yoff - yabove, w, h))
				return;
		}
		win.paintRle (xoff, yoff, data);
	}
	/*
	 *	Show a Run-Length_Encoded shape with translucency.
	 */
	public void paintRleTranslucent
		(
		ImageBuf win,			// Buffer to paint in.
		int xoff, int yoff,		// Where to show in iwin.
		ImageBuf.XformPalette xforms[]		// Transforms translucent colors
		) {
		int xfcnt = xforms.length;
		assert(rle);

		int w = getWidth(), h = getHeight();
		if (w >= EConst.c_tilesize || h >= EConst.c_tilesize)	// Big enough to check?  Off screen?
			if (!win.isVisible(xoff - xleft, yoff - yabove, w, h))
				return;
						// First pix. value to transform.
		int xfstart = 0xff - xfcnt;
		int in = 0;
		int scanlen;
		while ((scanlen = EUtil.Read2(data, in)) != 0) {
			in += 2;
						// Get length of scan line.
			boolean encoded = (scanlen&1) != 0;// Is it encoded?
			scanlen = scanlen>>1;
			int scanx = (short)EUtil.Read2(data, in);
			in += 2;
			int scany = (short)EUtil.Read2(data, in);
			in += 2;
			if (!encoded) {		// Raw data?
				win.copyLineTranslucent8(data, in, scanlen,
						xoff + scanx, yoff + scany,
						xfstart, 0xfe, xforms);
				in += scanlen;
				continue;
			}
			for (int b = 0; b < scanlen; ) {
				int bcnt = data[in++]&0xff;
						// Repeat next char. if odd.
				boolean repeat = (bcnt&1) != 0;
				bcnt = bcnt>>1; // Get count.
				if (repeat) {
					int pix = data[in++]&0xff;
					if (pix >= xfstart && pix <= 0xfe)
						win.fillLineTranslucent8(bcnt,
							xoff + scanx + b, yoff + scany,
							xforms[pix - xfstart]);
					else
						win.fillLine8((byte)pix, bcnt,
						      xoff + scanx + b, yoff + scany);
				} else {		// Get that # of bytes.
					win.copyLineTranslucent8(data, in, bcnt,
						xoff + scanx + b, yoff + scany,
						xfstart, 0xfe, xforms);
					in += bcnt;
				}
				b += bcnt;
			}
		}
	}
	/*
	 *	Paint a shape purely by translating the pixels it occupies.  This is
	 *	used for invisible NPC's.
	 */
	public void paintRleTransformed
		(
		ImageBuf win,		// Buffer to paint in.
		int xoff, int yoff,		// Where to show in iwin.
		ImageBuf.XformPalette xform		// Use to transform pixels.
		) {
		assert(rle);

		int w = getWidth(), h = getHeight();
		if (w >= EConst.c_tilesize || h >= EConst.c_tilesize)// Big enough to check?  Off screen?
			if (!win.isVisible(xoff - xleft, 
							yoff - yabove, w, h))
				return;
		int in = 0;
		int scanlen;
		while ((scanlen = EUtil.Read2(data, in)) != 0)
			{
			in += 2;
						// Get length of scan line.
			int encoded = scanlen&1;// Is it encoded?
			scanlen = scanlen>>1;
			short scanx = (short)EUtil.Read2(data, in);
			short scany = (short)EUtil.Read2(data, in + 2);
			in += 4;
			if (encoded == 0) {		// Raw data?
						// (Note: 1st parm is ignored).
				win.fillLineTranslucent8(scanlen, xoff + scanx, yoff + scany, xform);
				in += scanlen;
				continue;
			}
			for (int b = 0; b < scanlen; ) {
				int bcnt = (int)data[in++]&0xff;
						// Repeat next char. if odd.
				int repeat = bcnt&1;
				bcnt = bcnt>>1; // Get count.
				in += repeat != 0 ? 1 : bcnt;
				win.fillLineTranslucent8(bcnt, xoff + scanx + b, yoff + scany, xform);
				b += bcnt;
			}
		}
	}
	/*
	 *	Paint outline around a shape.
	 */
	public void paintRleOutline
		(
		ImageBuf win,		// Buffer to paint in.
		int xoff, int yoff,		// Where to show in win.
		byte color		// Color to use.
		) {
		assert(rle);
		int w = getWidth(), h = getHeight();
		if (w >= EConst.c_tilesize || h >= EConst.c_tilesize) // Big enough to check?  Off screen?
			if (!win.isVisible(xoff - xleft, yoff - yabove, w, h))
				return;
		int firsty = -10000;		// Finds first line.
		int lasty = -10000;
		int ind = 0;
		int scanlen;
		while ((scanlen = EUtil.Read2(data, ind)) != 0) {
			ind += 2;
						// Get length of scan line.
			int encoded = scanlen&1;// Is it encoded?
			scanlen = scanlen>>1;
			short scanx = (short)EUtil.Read2(data, ind);
			short scany = (short)EUtil.Read2(data, ind + 2);
			ind += 4;
			int x = xoff + scanx;
			int y = yoff + scany;
			if (firsty == -10000) {
				firsty = y;
				lasty = y + h - 1;
			}
						// Put pixel at both ends.
			win.putPixel(color, x, y);
			win.putPixel(color, x + scanlen - 1, y);

			if (encoded == 0) {		// Raw data?
				if (y == firsty ||	// First line?
				    y == lasty)		// Last line?
					win.fillLine8(color, scanlen, x, y);
				ind += scanlen;
				continue;
			}
			for (int b = 0; b < scanlen; ) {
				int bcnt = (int)(data[ind++])&0xff;
						// Repeat next char. if odd.
				int repeat = bcnt&1;
				bcnt = bcnt>>1; // Get count.
				if (repeat != 0)	// Pass repetition byte.
					ind++;
				else		// Skip that # of bytes.
					ind += bcnt;
				if (y == firsty || 	// First line?
				    y == lasty)		// Last line?
					win.fillLine8(color, bcnt, x + b, y);
				b += bcnt;
			}
		}
	}
	/*
	 * Is a point, relative to the shape's 'origin', actually within the shape.
	 */
	public final boolean hasPoint
		(
		int x, int y			// Relative to origin of shape.
		) {
		if (!rle) {			// 8x8 flat?
			return x >= -xleft && x < xright && y >= -yabove && y < ybelow;
		}
		int in = 0;
		int scanlen;
		while ((scanlen = EUtil.Read2(data, in)) != 0) {
			in += 2;
					// Get length of scan line.
			boolean encoded = (scanlen&1) != 0;// Is it encoded?
			scanlen = scanlen>>1;
			int scanx = (short)EUtil.Read2(data, in);
			in += 2;
			int scany = (short)EUtil.Read2(data, in);
			in += 2;
					// Be liberal by 1 pixel.
			if (y == scany && x >= scanx - 1 && x <= scanx + scanlen)
				return (true);
			if (!encoded) {		// Raw data?
				in += scanlen;
				continue;
			}
			for (int b = 0; b < scanlen; ) {
				byte bcnt = data[in++];
					// Repeat next char. if odd.
				int repeat = bcnt&1;
				bcnt = (byte)((bcnt&0xff)>>1); // Get count.
				if (repeat != 0)
					in++;	// Skip pixel to repeat.
				else		// Skip that # of bytes.
					in += bcnt;
				b += bcnt;
			}
		}
		return false;			// Never found it.
	}
	/*
	 * Just check the rectangular area for the point.
	 */
	public final boolean boxHasPoint
	(
	int x, int y			// Relative to origin of shape.
	) {
	return x >= -xleft && x < xright && y >= -yabove && y < ybelow;
	}
}
