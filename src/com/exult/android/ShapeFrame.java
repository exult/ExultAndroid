package com.exult.android;
import android.graphics.Canvas;
import java.io.RandomAccessFile;
import java.io.IOException;

public class ShapeFrame {
	private byte data[];			// The actual data.
	private int datalen;
	private short xleft;			// Extent to left of origin.
	private short xright;			// Extent to right.
	private short yabove;			// Extent above origin.
	private short ybelow;			// Extent below origin.
	private boolean rle;			// Run-length encoded.
	private static Canvas scrwin;	// Screen window to render to.
	
	public byte[] getData() {
		return data;
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
			int scanx = EUtil.Read2(data, in);
			int scany = EUtil.Read2(data, in + 2);
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
		RandomAccessFile shapes,	// Shapes data source to read.
		int shapeoff,		// Offset of shape in file.
		int shapelen,		// Length expected for detecting RLE.
		int frnum			// Frame #.
		) throws IOException
		{
		int framenum = frnum;
		rle = false;
		if (shapelen == 0 && shapeoff == 0) 
			return 0;
						// Get to actual shape.
		shapes.seek(shapeoff);
		int dlen = EUtil.Read4(shapes);
		int hdrlen = EUtil.Read4(shapes);
		if (dlen == shapelen)
			{
			rle = true;		// It's run-length-encoded.
						// Figure # frames.
			int nframes = (hdrlen - 4)/4;
			if (framenum >= nframes)// Bug out if bad frame #.
				return (nframes);
						// Get frame offset, lengeth.
			int frameoff, framelen;
			if (framenum == 0)
				{
				frameoff = hdrlen;
				framelen = nframes > 1 ? EUtil.Read4(shapes) - frameoff :
							dlen - frameoff;
				}
			else
				{
				shapes.skipBytes((framenum - 1) * 4);
				frameoff = EUtil.Read4(shapes);
						// Last frame?
				if (framenum == nframes - 1)
					framelen = dlen - frameoff;
				else
					framelen = EUtil.Read4(shapes) - frameoff;
				}
						// Get compressed data.
			getRleShape(shapes, shapeoff + frameoff, framelen);
						// Return # frames.
			return (nframes);
			}
		framenum &= 31;			// !!!Guessing here.
		xleft = yabove = EConst.c_tilesize;		// Just an 8x8 bitmap.
		xright= ybelow = -1;
		shapes.seek(shapeoff + framenum*EConst.c_num_tile_bytes);
		data = new byte[EConst.c_num_tile_bytes];	// Read in 8x8 pixels.
		datalen = EConst.c_num_tile_bytes;
		shapes.read(data);
		return (shapelen/EConst.c_num_tile_bytes);		// That's how many frames.
	}
	/*
	 * Read RLE shape.
	 */
	private void getRleShape
		(
		RandomAccessFile shapes,		// Shapes data source to read.
		int filepos,			// Position in file.
		int len			// Length of entire frame data.
		) throws IOException {
		shapes.seek(filepos);		// Get to extents.
		xright = (short)EUtil.Read2(shapes);
		xleft = (short)EUtil.Read2(shapes);
		yabove = (short)EUtil.Read2(shapes);
		ybelow = (short)EUtil.Read2(shapes);
		len -= 8;			// Subtract what we just read.
		data = new byte[len + 2];	// Allocate and read data.
		datalen = len+2;
		shapes.read(data, 0, len);
		data[len] = 0;			// 0-delimit.
		data[len + 1] = 0;
		rle = true;
	}
	/*
	 *	Skip transparent pixels.
	 *
	 *	Output:	Index of first non-transparent pixel (w if no more).
	 */
	private static int Skip_transparent
		(
		byte pixels[],		// 8-bit pixel scan line.
		int ind,
		int x,				// X-coord. of pixel to start with.
		int w				// Remaining width of pixels.
		) {
		while (x < w && pixels[ind + x] == 255)
			x++;
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
		while (x < w && pixels[x] != 255) {	// Stop at first transparent pixel.
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
			while (x < w && pixels[ind] != 255 &&
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
	 *	Output:	->allocated RLE data.
	 */

	public static byte[] encodeRle(
		byte pixels[],			// 8-bit uncompressed data.
		int w, int h,			// Width, height.
		int xoff, int yoff,		// Origin (xleft, yabove).
		int datalen				// Length of RLE data returned.
		)
		{
						// Create an oversized buffer.
		byte buf[] = new byte[w*h*2 + 16*h];
		int out = 0;	// Index into buf.
		int ind = 0;
		int newx;			// Gets new x at end of a scan line.
		for (int y = 0; y < h; y++)	// Go through rows.
			for (int x = Skip_transparent(pixels, ind, 0, w); x < w; x = Skip_transparent(pixels, ind, newx, w))
				{
				short runs[] = new short[200];// Get runs.
				ind += x;
				newx = Find_runs(runs, pixels, ind, x, w);
				ind += newx;
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
							buf[out++] = pixels[ind++];
							ind += c;
							len -= c;
						}
					} else while (len > 0) {
						int c = len > 127 ? 127 : len;
						buf[out++] = (byte)(c<<1);
						EUtil.Memcpy(buf, out, pixels, ind, c);
						out += c;
						ind += c;
						len -= c;
						}
					}
				}
		out = EUtil.Write2(buf, out, 0);			// End with 0 length.
		datalen = out;		// Create buffer of correct size.
		byte data[] = new byte[datalen];
		EUtil.Memcpy(data, 0, buf, 0, datalen);
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
		data = encodeRle(pixels, w, h, xleft, yabove, datalen);
	}
	public final int getWidth()		// Get dimensions.
		{ return xleft + xright + 1; 
		}
	public final int getHeight()
		{ return yabove + ybelow + 1; 
		}
	public final int getXLeft() {
		return xleft;
	}
	public final int getYAbove() {
		return yabove;
	}
	public final boolean isRle() {
		return rle;
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
			int scanx = EUtil.Read2(data, in);
			in += 2;
			int scany = EUtil.Read2(data, in);
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
}
