package com.exult.android;
import java.io.InputStream;

public class FontsVgaFile extends GameSingletons {
	private static Font fonts[];
	/*
	 *	Horizontal leads, by fontnum:
	 *
	 *	This must include the Endgame fonts (currently 32-35)!!
	 *      And the MAINSHP font (36)
	 *	However, their values are set elsewhere
	 */
	private static int hlead[] = {-2, -1, 0, -1, 0, 0, -1, -2, -1, -1};
	public void init() {
		int cnt = hlead.length;
		EFile sfonts = fman.getFileObject(EFile.FONTS_VGA);
		EFile pfonts = fman.getFileObject(EFile.PATCH_FONTS);
		int sn = sfonts.numberOfObjects();
		int pn = pfonts != null ? pfonts.numberOfObjects() : 0;
		int numfonts = pn > sn ? pn : sn;
		fonts = new Font[numfonts];

		for (int i = 0; i < numfonts; i++) {
			fonts[i] = new Font();
			fonts[i].load(sfonts, pfonts, i, i < cnt ? hlead[i] : 0, 0);
		}
	}
	public FontsVgaFile() {
		init();
	}
	public int paintText(int fontnum, 
							String text, int xoff, int yoff)
			{ return fonts[fontnum].paintText(win, text, xoff, yoff); }
	public int getTextWidth(int fontnum, String text)
		{ return fonts[fontnum].getTextWidth(text); }
	public int getTextHeight(int fontnum)
		{ return fonts[fontnum].getTextHeight(); }
	public static class Font {
		private int hor_lead;
		private int ver_lead;
		private VgaFile.ShapeFile fontShapes;
		private int  highest, lowest;

		private void calc_highlow() {
			boolean unset = true;
			int cnt = fontShapes.getNumFrames();
			for (int i = 0; i < cnt; i++) {
				ShapeFrame f = fontShapes.getFrame(i);
				if (f == null) continue;
				if (unset) {
					unset = false;
					highest = f.getYAbove();
					lowest = f.getYBelow();
					continue;
				}
				
				if (f.getYAbove() > highest) highest = f.getYAbove();
				if (f.getYBelow() > lowest) lowest = f.getYBelow();
			}
		}
		/*
		Font(const File_spec& fname0, int index, int hlead=0, int vlead=1);
		Font(const File_spec& fname0, const File_spec& fname1, int index,
				int hlead=0, int vlead=1);
		int load(const File_spec& fname0, int index, int hlead=0, int vlead=1);
*/
		public void load(EFile file, EFile patchfile, int index,
										int hlead, int vlead) {
			byte data[] = patchfile != null ? patchfile.retrieve(index) : null;
			if (data == null || data.length == 0)
				data = file.retrieve(index);
			if (data == null || data.length == 0) {
				fontShapes = null;
				hor_lead = 0;
				ver_lead = 0;
			} else {
				int start = 0;
				// Is it an IFF archive?
				if (data[0] == 'f' && data[1] == 'o' && data[2] == 'n' &&
														data[3] == 't') {
					start = 8;		// Yes, skip first 8 bytes.
					byte fontdata[] = new byte[data.length - start];
					System.arraycopy(data, start, fontdata, 0, fontdata.length);
					data = fontdata;
				}
				fontShapes = new VgaFile.ShapeFile(data);
				hor_lead = hlead;
				ver_lead = vlead;
				calc_highlow();
			}
		}
		/*
		 *	Get font baseline as the distance from the top.
		 */
		public int getTextBaseline() {
			return highest;
		}
		/*
		 *	Paint text using font from "fonts.vga".
		 *
		 *	Output:	Width in pixels of what was painted.
		 */
		public int paintText(ImageBuf win,  
						String text, int xoff, int yoff) {
			int x = xoff, ind = 0, textlen = text.length();
			yoff += getTextBaseline();
			if (fontShapes != null)
				while (textlen-- > 0) {
					int c = text.charAt(ind++);
					ShapeFrame shape= fontShapes.getFrame(c);
					if (shape == null)
						continue;
					shape.paintRle(win, x, yoff);
					x += shape.getWidth() + hor_lead;
					}
			return (x - xoff);
		}
		/*
						// Text rendering:
		int paint_text_box(Image_buffer8 *win,  
			const char *text, int x, int y, int w, 
			int h, int vert_lead = 0, bool pbreak = false,
			bool center = false, Cursor_info *cursor = 0);
		int paint_text(Image_buffer8 *win,  
			const char *text, int xoff, int yoff,
			unsigned char *trans = 0);
		
		int paint_text_box_fixedwidth(Image_buffer8 *win,  
			const char *text, int x, int y, int w, 
			int h, int char_width, int vert_lead = 0, int pbreak = 0);
		int paint_text_fixedwidth(Image_buffer8 *win,  
			const char *text, int xoff, int yoff, int width);
		int paint_text_fixedwidth(Image_buffer8 *win,  
			const char *text, int textlen, int xoff, int yoff, int width);
						// Get text width.
		*/
		public int getTextWidth(String text) {
			return getTextWidth(text, text.length());
		}
		public int getTextWidth(String text, int textlen) {
			int width = 0, ind;
			int chr;
			if (fontShapes != null) {
				for (ind = 0; ind < textlen; ++ind) {
					chr = text.charAt(ind);
					if (chr == 0)
						break;
					ShapeFrame shape = fontShapes.getFrame(chr);
					if (shape != null)
						width += shape.getWidth() + hor_lead;
				}
			}
			return (width);
		}
						// Get text height, baseline.
		public int getTextHeight() {
			return highest + lowest + 1;
		}
		public int get_text_baseline() {
			return highest;
		}
		/*
		int find_cursor(const char *text, int x, int y, int w, int h,
						int cx, int cy, int vert_lead);
		int find_xcursor(const char *text, int textlen, int cx);

		int draw_text(Image_buffer8 *win, int x, int y, const char *s,
			unsigned char *trans = 0)
			{ return paint_text(win, s, x, y, trans); }
		int draw_text_box(Image_buffer8 *win, 
					int x, int y, int w, int h, const char *s)
			{ return paint_text_box(win, s, x, y, w, h, 0, 0); }
		int center_text(Image_buffer8 *iwin, int x, int y, const char *s);
		*/
	}
}
