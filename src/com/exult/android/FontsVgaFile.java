package com.exult.android;


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
		sfonts.close();
		if (pfonts != null)
			pfonts.close();
	}
	public FontsVgaFile() {
		init();
	}
	// Text rendering:
	public int paintTextBox(ImageBuf win, int fontnum, 
			String text, int x, int y, int w, int h, 
			int vert_lead, boolean pbreak, boolean center)
		{ return fonts[fontnum].paintTextBox(win, text, x, y, w, h,
				vert_lead, pbreak, center); }
	public int paintTextBox(ImageBuf win, int fontnum, 
			String text, int start, int textlen, int x, int y, int w, int h, 
			int vert_lead, boolean pbreak, boolean center)
		{ return fonts[fontnum].paintTextBox(win, text, start, textlen, 
				x, y, w, h, vert_lead, pbreak, center); }
	public int paintText(int fontnum, 
							String text, int xoff, int yoff)
			{ return fonts[fontnum].paintText(win, text, xoff, yoff); }
	public int getTextWidth(int fontnum, String text)
		{ return fonts[fontnum].getTextWidth(text); }
	public int getTextWidth(int fontnum, String text, int textlen)
		{ return fonts[fontnum].getTextWidth(text, 0, textlen); }
	public int getTextHeight(int fontnum)
		{ return fonts[fontnum].getTextHeight(); }
	
	public static class Font {
		private int horLead;
		private int verLead;
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
		private final static boolean isSpace(int c)
			{ return c == ' ' || c == '\n' || c == '\t'; }
		private final static int passWhitespace(String text, int ind) {
			while (isSpace(text.charAt(ind)))
				ind++;
			return (ind);
		}
		// Just spaces and tabs:
		private final static int passSpace(String text, int textlen, int ind) {
			int c;
			while (ind < textlen && 
					((c = text.charAt(ind)) == ' ' || c == '\t'))
				ind++;
			return ind;
		}
		private final static int passWord(String text, int textlen, int ind) {
			int c;
			while (ind < textlen && 
					(c = text.charAt(ind)) != 0 && (!isSpace(c) || 
											(c == '\f') || (c == 0xb)))
				ind++;
			return (ind);
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
				horLead = 0;
				verLead = 0;
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
				horLead = hlead;
				verLead = vlead;
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
		public int paintText(ImageBuf win, String text, int xoff, int yoff) {
			return paintText(win, text, text.length(), xoff, yoff);
		}
		public int paintText(ImageBuf win,  
						String text, int textlen, int xoff, int yoff) {
			int x = xoff, ind = 0;
			yoff += getTextBaseline();
			if (fontShapes != null)
				while (textlen-- > 0) {
					int c = text.charAt(ind++);
					ShapeFrame shape= fontShapes.getFrame(c);
					if (shape == null)
						continue;
					shape.paintRle(win, x, yoff);
					x += shape.getWidth() + horLead;
					}
			return (x - xoff);
		}
						// Text rendering:
		/*
		 *	Draw text within a rectangular area.
		 *	Special characters handled are:
		 *		\n	New line.
		 *		space	Word break.
		 *		tab	Treated like a space for now.
		 *		*	Page break.
		 *		^	Uppercase next letter. 
		 *
		 *	Output:	If out of room, -offset of end of text painted.
		 *		Else height of text painted.
		 */
		int paintTextBox(ImageBuf win, String text, int x, int y, int w, 
					int h, int vertLead, boolean pbreak, boolean center) {
			int textlen = text.length();
			return paintTextBox(win, text, 0, textlen, x, y, w, h,
									vertLead, pbreak, center);
		}
		int paintTextBox(ImageBuf win, String text, int start, int textlen, 
				int x, int y, int w, 
				int h, int vertLead, boolean pbreak, boolean center) {
			int chr, ind = start;
			win.setClip(x, y, w, h);
			int endx = x + w;		// Figure where to stop.
			int curx = x, cury = y;
			int height = getTextHeight() + vertLead + verLead;
			int space_width = getCharWidth(' ');
			int max_lines = h/height;	// # lines that can be shown.
			StringBuffer lines[] = new StringBuffer[max_lines + 1];
			lines[0] = new StringBuffer();
			int cur_line = 0;
			int last_punct_end = -1;	// ->last period, qmark, etc.
											// Last punct in 'lines':
			int last_punct_line = -1, last_punct_offset = -1;
			while (ind < textlen && (chr = text.charAt(ind)) != 0) {
				switch (chr) {		// Special cases.
				case '\n':		// Next line.
					curx = x;
					ind++;
					cur_line++;
					cury += height;
					
					if (cur_line >= max_lines)
						break;	// No more room.
					lines[cur_line] = new StringBuffer();
					continue;
				case '\r':		//??
					ind++;
					continue;
				case ' ':		// Space.
				case '\t':
					{		// Pass space.
					int wrd = passSpace(text, textlen, ind);
					if (wrd != ind)
						{
						int sw = getTextWidth(text, ind, wrd);
						if (sw <= 0)
							sw = space_width;
						int nsp = sw/space_width;
						curx += nsp*space_width;
						while (nsp-- > 0)
							lines[cur_line].append(' ');
						
					}
					ind = wrd;
					break;
					}
				}
				if (cur_line >= max_lines || ind >= textlen)
					break;
				chr = text.charAt(ind);
				if (chr == '*') {
					chr = text.charAt(++ind);
					if (cur_line > 0)
						break;
				}
				boolean ucase_next = chr == '^';
				if (ucase_next)	// Skip it.
					++ind;
							// Pass word & get its width.
				int ewrd = passWord(text, textlen, ind);
				int width;
				if (ucase_next) {
					int c = Character.toUpperCase(text.charAt(ind));
					width = getCharWidth(c) + getTextWidth(text, ind+1, ewrd);
					}
				else
					width = getTextWidth(text, ind, ewrd);
				if (curx + width - horLead > endx) {		// Word-wrap.
					if (ucase_next)
						ind--;	// Put the '^' back.
					curx = x;
					cur_line++;
					cury += height;
					if (cur_line >= max_lines)
						break;	// No more room.
					lines[cur_line] = new StringBuffer();
				}
							// Store word.
				if (ucase_next) {
					lines[cur_line].append(Character.toUpperCase(text.charAt(ind)));
					++ind;
				}
				lines[cur_line].append(text.substring(ind, ewrd));
				curx += width;
				ind = ewrd;		// Continue past the word.
				// Keep loc. of punct. endings.
				chr = text.charAt(ind -1);
				if (chr == '.' || chr == '?' || chr == '!' || chr == ',' || chr == '"'){
					last_punct_end = ind;
					last_punct_line = cur_line;
					last_punct_offset = lines[cur_line].length();
				}
			}
			if (ind < textlen &&			// Out of room?
							// Break off at end of punct.
					pbreak && last_punct_end != 0)
				ind = passWhitespace(text, last_punct_end);
			else {
				last_punct_line = -1;
			}
			cury = y;			// Render text.
			for (int i = 0; i <= cur_line; i++) {
				String str = new String(lines[i]);
				int len = str.length();
				if (i == last_punct_line)
					len = last_punct_offset;
				if (center)
					centerText(win, x + w/2, cury, str);
				else
					paintText(win, str, len, x, cury);
				cury += height;
				if (i == last_punct_line)
					break;
				}
			win.clearClip();
			if (ind < textlen)			// Out of room?
				return -(ind - start);		// Return -offset of end.
			else						// Else return height.
				return (cury - y);
		}
		/*
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
		public final int getCharWidth(int chr) {
			ShapeFrame shape = fontShapes.getFrame(chr);
			if (shape != null)
				return shape.getWidth() + horLead;
			else
				return 0;
		}
		public final int getTextWidth(String text) {
			return getTextWidth(text, 0, text.length());
		}
		public final int getTextWidth(String text, int start, int stop) {
			int width = 0, ind;
			int chr;
			if (fontShapes != null) {
				for (ind = start; ind < stop; ++ind) {
					chr = text.charAt(ind);
					if (chr == 0)
						break;
					ShapeFrame shape = fontShapes.getFrame(chr);
					if (shape != null)
						width += shape.getWidth() + horLead;
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
		*/
		int drawText(ImageBuf win, int x, int y, String s)
			{ return paintText(win, s, x, y); }
		/*
		int draw_text_box(Image_buffer8 *win, 
					int x, int y, int w, int h, const char *s)
			{ return paint_text_box(win, s, x, y, w, h, 0, 0); }
		*/
		int centerText(ImageBuf win, int x, int y, String s) {
			return drawText(win, x - getTextWidth(s)/2, y, s);
		}
	}
	
}
