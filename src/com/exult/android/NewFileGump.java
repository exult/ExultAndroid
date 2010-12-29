package com.exult.android;
import java.io.IOException;

public final class NewFileGump extends Gump.Modal {
	private static final int MAX_SAVEGAME_NAME_LEN = 0x50;
	static final String loadtext = "LOAD";
	static final String savetext = "SAVE";
	static final String deletetext = "DELETE";
	static final String canceltext = "CANCEL";
	GumpWidget.Button buttons[] = new GumpWidget.Button[8];	// 2 sets of 4 buttons
	static final int btn_cols[] = 	// x-coord of each button.
		{186, 2, 15, 158, 171};
	static final int btn_rows[] =	// y-coord of each button.
	 	{2, 46, 88, 150, 209};
	// Text field info
	static final short fieldx = 2;	// Start Y of each field
	static final short fieldy = 2;	// Start X of first
	static final short fieldw = 207;	// Width of each field
	static final short fieldh = 12;	// Height of each field
	static final short fieldgap = 1;	// Gap between fields
	static final short fieldcount = 14;	// Number of fields
	static final short textx = 12;	// X Offset in field
	static final short texty = 2;	// Y Offset in field
	static final short textw = 190;	// Maximum allowable width of text
	static final short iconx = 2;	// X Offset in field
	static final short icony = 2;	// Y Offset in field

	// Scrollbar and Slider Info
	static final short scrollx = 212;	// X Offset
	static final short scrolly = 28;	// Y Offset
	static final short scrollh = 129;	// Height of Scroll Bar
	static final short sliderw = 7;	// Width of Slider
	static final short sliderh = 7;	// Height of Slider

	// Side Text
	static final short infox = 224;	// X Offset for info
	static final short infoy = 67;	// Y Offset for info
	static final short infow = 92;	// Width of info box
	static final short infoh = 79;	// Height of info box
	static final String infostring = 	// Text format for info
		"Avatar: %1$s\n" +
		"Exp: %2$i  Hp: %3$i\n" +
		"Str: %4$i  Dxt: %5$i\n" +
		"Int: %6$i  Trn: %7$i\n" +
		"\n" +
		"Game Day: %8$i\n" +
		"Game Time: %9$02i:%10$02i\n" +
		"\n" +
		"Save Count: %11$i\n" +
		"Date: %12$i%13$s %s %14$04i\n" +
		"Time: %15$02i:%16$02i";
	static final String months[] = {	// Names of the months
		"Jan",
		"Feb",
		"March",
		"April",
		"May",
		"June",
		"July",
		"Aug",
		"Sept",
		"Oct",
		"Nov",
		"Dec"};
	boolean restored;		// Set to 1 if we restored a game.

	byte	back[];

	SaveInfo	games[];		// The list of savegames
	int		num_games;	// Number of save games
	int		first_free;	// The number of the first free savegame

	VgaFile.ShapeFile cur_shot;		// Screenshot for current game
	SaveGameDetails cur_details;	// Details of current game
	SaveGameParty cur_party[];	// Party of current game

	// Gamedat is being used as a 'quicksave'
	VgaFile.ShapeFile gd_shot;		// Screenshot in Gamedat
	SaveGameDetails gd_details;	// Details in Gamedat
	SaveGameParty gd_party[];	// Parts in Gamedat

	VgaFile.ShapeFile screenshot;		// The picture to be drawn
	SaveGameDetails details;	// The game details to show
	SaveGameParty party[];		// The party to show
	boolean is_readable;		// Is the save game readable
	String filename;		// Filename of the savegame, if exists

	int	list_position;		// The position in the savegame list (top game)
	int	selected;		// The savegame that has been selected (num in list)
	int	cursor;			// The position of the cursor
	int	slide_start;		// Pixel (v) where a slide started
	String newname;			// The new name for the game
	/* ++++++++++++
	int	BackspacePressed();
	int	DeletePressed();
	int	MoveCursor(int count);
	int	AddCharacter(char c);
	*/
	void	LoadSaveGameDetails() {	// Loads (and sorts) all the savegame details
		//+++++++++++FINISH
	}
	void	FreeSaveGameDetails() {	// Frees all the savegame details
		//++++++++++
	}
	public NewFileGump() {
		super(EFile.EXULT_FLX_SAVEGUMP_SHP, ShapeFiles.EXULT_FLX);	
		list_position = -2; 
		selected = -3;
		slide_start = -1;
		setObjectArea(0,0,320,200, -22, 190);
		// +++++++MAYBE NOT HERE tqueue.pause(TimeQueue.ticks);
		back = new byte[gwin.getWidth() * gwin.getHeight()];
		gwin.getWin().get(back, gwin.getWidth(), gwin.getHeight(), 0, 0);

		// Load/Save/Delete
		buttons[0] = buttons[1] = buttons[2] = null;

		// Cancel
		buttons[3] = new NewfileTextButton(this, canceltext,
											btn_cols[3], btn_rows[0], 59);

		// Scrollers.
		buttons[4] = new NewfileButton(this, btn_cols[4], btn_rows[1], EFile.EXULT_FLX_SAV_UPUP_SHP);
		buttons[5] = new NewfileButton(this, btn_cols[4], btn_rows[2], EFile.EXULT_FLX_SAV_UP_SHP);
		buttons[6] = new NewfileButton(this, btn_cols[4], btn_rows[3], EFile.EXULT_FLX_SAV_DOWN_SHP);
		buttons[7] = new NewfileButton(this, btn_cols[4], btn_rows[4], EFile.EXULT_FLX_SAV_DOWNDOWN_SHP);

		LoadSaveGameDetails();
	}
	public void load() {			// 'Load' was clicked.
		// Shouldn't ever happen.
		if (selected == -2 || selected == -3)
			return;	
		// Aborts if unsuccessful.
		if (selected != -1) 
			gwin.read(games[selected].num);
		else // Read Gamedat
			gwin.read();

		// Set Done
		done = true;
		restored = true;
		
		// Reset Selection
		selected = -3;
		buttons[0] = null;
		buttons[1] = null;
		buttons[2] = null;
	}
	public void save() {			// 'Save' was clicked.
		// Shouldn't ever happen.
		if (newname.length() == 0 || selected == -3)
			return;	
		// Already a game in this slot? If so ask to delete
		/* ++++++++++FINISH
		if (selected != -2) if (!Yesno_gump::ask("Okay to write over existing saved game?"))
			return;
		*/
		
		int num = selected >= 0 ? games[selected].num 
				: (selected == -2 ? first_free : -1);
		if (num >= 0)	// Write to gamedat, then to savegame file.
			gwin.write(num, newname);
		else try {
			gwin.write();
		} catch (IOException e) {
			System.out.println("Error during quick save");
		}
		System.out.println("Saved game #" + selected + " successfully.");

		// Reset everything
		selected = -3;
		buttons[0] = null;
		buttons[1] = null;
		buttons[2] = null;

		FreeSaveGameDetails();
		LoadSaveGameDetails();
		paint();
		gwin.setPainted();
	}
	public void delete_file() {		// 'Delete' was clicked.
		//+++++++++++++++++++
	}
	public void scroll_line(int dir) {	// Scroll Line Button Pressed
		list_position += dir;

		if (list_position > num_games-fieldcount)
			list_position = num_games-fieldcount;
		if (list_position < -2)
			list_position = -2;
		paint();
		gwin.setPainted();
	}
	public void scroll_page(int dir) {	// Scroll Page Button Pressed.
		scroll_line(dir * fieldcount);
	}
	public boolean restoredGame()		// 1 if user restored.
		{ return restored; }
					// Paint it and its contents.
	public void paint() {
		if (games == null)
			return;			// No list, so skip out.
		super.paint();
		ImageBuf win = gwin.getWin();
		// Paint text objects.
		int i;
		for (i = 0; i < fieldcount; i++)
			PaintSaveName (i);
		// Paint Buttons
		for (i = 0; i < 8; i++) 
			if (buttons[i] != null)
				buttons[i].paint();
		// Paint scroller
		// First thing, work out number of positions that the scroller can be in
		int num_pos = (2+num_games)-fieldcount;
		if (num_pos < 1) num_pos = 1;
		// Now work out the position
		int pos = ((scrollh-sliderh)*(list_position+2))/num_pos;
		ShapeFrame slider_shape = ShapeFiles.EXULT_FLX.getShape(
						EFile.EXULT_FLX_SAV_SLIDER_SHP, 0);
		slider_shape.paint(win, x+scrollx , y+scrolly+pos);
		// Now paint the savegame details
		if (screenshot != null) 
			screenshot.getFrame(0).paint(win, x + 222, y + 2);
		// Need to ensure that the avatar's shape actually exists
		/* +++++++++++FINISH
		if (party != null  && !sman.have_si_shapes() &&
			Shapeinfo_lookup::IsSkinImported(party[0].shape)) {
			// Female if odd, male if even
			if (party[0].shape %2) 
				party[0].shape = Shapeinfo_lookup::GetFemaleAvShape();
			else 
				party[0].shape = Shapeinfo_lookup::GetMaleAvShape();
		}
		*/
		if (details != null && party != null) {
			for (i=0; i<4 && i<details.party_size; i++) {
				ShapeFrame shape = party[i].shape_file.getShape(party[i].shape, 16);
				shape.paint(win, x + 249 + i*23, y + 169);
			}
			for (i=4; i<8 && i<details.party_size; i++) {
				ShapeFrame shape = party[i].shape_file.getShape(party[i].shape, 16);
				shape.paint(win, x + 249 + (i-4)*23, y + 198);
			}
			//++++++++char	info[320];
			String suffix = "th";

			if ((details.real_day%10) == 1 && details.real_day != 11)
				suffix = "st";
			else if ((details.real_day%10) == 2 && details.real_day != 12)
				suffix = "nd";
			else if ((details.real_day%10) == 3 && details.real_day != 13)
				suffix = "rd";
			String info = String.format(infostring, 
				party[0].name,
				party[0].exp, party[0].health,
				party[0].str, party[0].dext,
				party[0].intel, party[0].training,
				details.game_day, details.game_hour, details.game_minute,
				details.save_count,
				details.real_day, suffix, months[details.real_month-1], details.real_year,
				details.real_hour, details.real_minute);
			if (filename != null) {
				info += "\nFile: ";
				int offset = filename.length();			
				while (offset-- > 0) {
					if (filename.charAt(offset) == '/' || filename.charAt(offset) == '\\') {
						offset++;
						break;
					}
				}
				info += filename.substring(offset);
			}
			fonts.paintTextBox (win, 4, info, x+infox, y+infoy, infow, infoh,
					0, false, false);
		} else {
			if (filename != null) {
				String info = "File: ";
				int offset = filename.length();
				while (offset-- > 0) {
					if (filename.charAt(offset) == '/' || filename.charAt(offset) == '\\') {
						offset++;
						break;
					}
				}
				info += filename.substring(offset);
				fonts.paintTextBox(win, 4, info, x+infox, y+infoy, infow, infoh,
						0, false, false);
			}
			if (!is_readable) {
				fonts.paintText(2, "Unreadable", x+infox+
						(infow-fonts.getTextWidth(2, "Unreadable"))/2, y+infoy+(infoh-18)/2);
				fonts.paintText(2, "Savegame", x+infox+
						(infow-fonts.getTextWidth(2, "Savegame"))/2, y+infoy+(infoh)/2);
			} else {
				fonts.paintText(4, "No Info", x+infox+
						(infow-fonts.getTextWidth(4, "No Info"))/2, y+infoy+
						(infoh-fonts.getTextHeight(4))/2);
			}
		}
		gwin.setPainted();
	}
	private void PaintSaveName (int line) {
		int	actual_game = line+list_position;
		if (actual_game < -2 || actual_game >= num_games) 
			return;
		String text;
		if (actual_game == -1)
			text = "Quick Save";
		else if (actual_game == -2 && selected != -2)
			text = "Empty Slot";
		else if (actual_game != selected || buttons[0] != null)
			text = games[actual_game].savename;
		else
			text = newname;
		fonts.paintText (2, text, 
			x + fieldx + textx,
			y + fieldy + texty + line*(fieldh + fieldgap));
		// Being Edited? If so paint cursor
		if (selected == actual_game && cursor != -1)
			gwin.getWin().fill8((byte)0, 1, fonts.getTextHeight(2),
				x + fieldx + textx + fonts.getTextWidth(2, text, cursor),
					y + fieldy + texty + line*(fieldh + fieldgap));

		// If selected, show selected icon
		if (selected == actual_game) {
			ShapeFrame icon = ShapeFiles.EXULT_FLX.getShape(
					EFile.EXULT_FLX_SAV_SELECTED_SHP, 0);
			icon.paint(gwin.getWin(), x+fieldx+iconx,
						y+fieldy+icony+line*(fieldh+fieldgap));
		}
	}
	public void close()
		{ done = true; }
					// Handle events:
	public boolean mouseDown(int mx, int my, boolean button) {
		if (!button)
			return false;

		slide_start = -1;
		pushed = super.onButton(mx, my);
					// Try buttons at bottom.
		if (pushed == null) { 
			for (int i = 0; i < buttons.length; i++)
				if (buttons[i] != null && buttons[i].onButton(mx, my) != null) {
				pushed = buttons[i];
				break;
			}
		}
		if (pushed != null) {			// On a button?
			if (!pushed.push(button)) 
				pushed = null;
			return true;
		}
		int gx = mx - x;
		int gy = my - y;
		// Check for scroller
		if (gx >= scrollx && gx < scrollx+sliderw && gy >= scrolly && gy < scrolly+scrollh) {
			int num_pos = (2+num_games)-fieldcount;
			if (num_pos < 1) 
				num_pos = 1;
			// Now work out the position
			int pos = ((scrollh-sliderh)*(list_position+2))/num_pos;
			// Pressed above it
			if (gy < pos+scrolly) {
				scroll_page(-1);
				paint();
				return true;
			}
			// Pressed below it
			else if (gy >= pos+scrolly+sliderh) {
				scroll_page(1);
				paint();
				return true;
			} else { // Pressed on it
				slide_start = gy;
				return true;
			}
		}
		// Now check for text fields
		if (gx < fieldx || gx >= fieldx+fieldw)
			return true;

		int	hit = -1;
		int	i;
		for (i = 0; i < fieldcount; i++) {
			int fy = fieldy + i*(fieldh + fieldgap);
			if (gy >= fy && gy < fy+fieldh) {
				hit = i;
				break;
			}
		}
		if (hit == -1) 
			return true;
		if (hit+list_position >= num_games || hit+list_position < -2 || 
				selected == hit+list_position) 
			return true;
		selected = hit+list_position;
		boolean want_load = true;
		boolean want_delete = true;
		boolean want_save = true;
		if (selected == -2) {
			want_load = false;
			want_delete = false;
			want_save = false;
			screenshot = cur_shot;
			details = cur_details;
			party = cur_party;
			newname = "";
			cursor = 0;
			is_readable = true;
			filename = null;
		} else if (selected == -1) {
			want_delete = false;
			screenshot = gd_shot;
			details = gd_details;
			party = gd_party;
			newname = "Quick Save";
			cursor = -1; // No cursor
			is_readable = true;
			filename = null;
		} else {
			screenshot = games[selected].screenshot;
			details = games[selected].details;
			party = games[selected].party;
			newname = games[selected].savename;
			cursor = newname.length();
			is_readable = want_load = games[selected].readable;
			filename = games[selected].filename;
		}
		if (buttons[0] == null && want_load)
			buttons[0] = new NewfileTextButton(this, loadtext, 
												btn_cols[1], btn_rows[0], 39);
		else if (buttons[0] != null && !want_load) {
			buttons[0] = null;
		}

		if (buttons[1] == null && want_save)
			buttons[1] = new NewfileTextButton(this, savetext,
												btn_cols[0], btn_rows[0], 40);
		else if (buttons[1] != null && !want_save) {
			buttons[1] = null;
		}

		if (buttons[2] == null && want_delete)
			buttons[2] = new NewfileTextButton(this, deletetext,
											btn_cols[2], btn_rows[0], 59);
		else if (buttons[2] != null && !want_delete) {
			buttons[2] = null;
		}
		paint();			// Repaint.
		gwin.setPainted();
		return true;
	}
	public boolean mouseUp(int mx, int my, boolean button) {
		if (!button) 
			return false;
		slide_start = -1;
		if (pushed != null) {			// Pushing a button?
			pushed.unpush(button);
			if (pushed.onButton(mx, my) != null)
				pushed.activate(button);
			pushed = null;
		}
		return true;
	}
	public void mouseDrag(int mx, int my) {
		// If not sliding don't do anything
		if (slide_start == -1) 
			return;
		int gx = mx - x;
		int gy = my - y;
		// First if the position is too far away from the slider 
		// We'll put it back to the start
		int sy = gy - scrolly;
		if (gx < scrollx-20 || gx > scrollx+sliderw+20)
			sy = slide_start - scrolly;
		if (sy < sliderh/2) sy = sliderh/2;
		if (sy > scrollh-sliderh/2) sy = scrollh-sliderh/2;
		sy -= sliderh/2;
		// Now work out the number of positions
		int num_pos = (2+num_games)-fieldcount;
		// Can't scroll if there is less than 1 pos
		if (num_pos < 1) return;
		// Now work out the closest position to here position
		int new_pos = ((sy*num_pos*2)/(scrollh-sliderh)+1)/2-2;
		if (new_pos != list_position) {
			list_position = new_pos;
			paint();
		}
	}
	public void textInput(int chr, int unicode) { // Character typed.
		//+++++++++
	}
	
	static class SaveGameDetails {
		// Time that the game was saved (needed????)
		byte	real_minute;	// 1
		byte	real_hour;	// 2
		byte	real_day;	// 3
		byte	real_month;	// 4
		short	real_year;	// 6

		// The Game Time that the save was done at
		byte	game_minute;	// 7
		byte	game_hour;	// 8
		short	game_day;	// 10

		short	save_count;	// 12
		byte	party_size;	// 13

		byte	unused;		// 14 Quite literally unused

		byte	real_second;	// 15

		//Incase we want to add more later
		byte		reserved0;	// 16
		byte	reserved1[] = new byte[48];	// 64
	};
	static class SaveGameParty
	{
		byte		name[] = new byte[18];	// 18
		short		shape;		// 20
		int	exp;		// 24
		int	flags;		// 28
		int	flags2;		// 32

		byte	food;		// 33
		byte	str;		// 34
		byte	combat;		// 35
		byte	dext;		// 36
		byte	intel;		// 37
		byte	magic;		// 38
		byte	mana;		// 39
		byte	training;	// 40
		short		health;		// 42

		ShapeFiles	shape_file;	// 44

		//Incase we want to add more later
		int		reserved1;	// 48
		int		reserved2;	// 52
		int		reserved3;	// 56
		int		reserved4;	// 60
		int		reserved5;	// 64
	};
	static class SaveInfo {
		int			num;
		String 	filename;
		String	savename;
		boolean			readable;
		SaveGameDetails	details;
		SaveGameParty	party[];
		VgaFile.ShapeFile		screenshot;
		/* +++++++++++
		static int		CompareGames(const void *a, const void *b);
		int			CompareThis(const SaveInfo *other) const;
		*/
		void SetSeqNumber() {
			int i;

			for (i = filename.length() - 1; !Character.isDigit(filename.charAt(i)); i--)
				;
			for (; Character.isDigit(filename.charAt(i)); i--)
				;
			num = Integer.parseInt(filename.substring(i+1));
		}
		SaveInfo() {
			readable = true;
		}
	};
	static class NewfileButton extends GumpWidget.Button {
		public NewfileButton(Gump par, int px, int py, int shapenum) {
			super(par, shapenum, px, py, ShapeFiles.EXULT_FLX);
		}
					// What to do when 'clicked':
		public boolean activate(boolean button) {	
			if (!button) 
				return false;
			int shapenum = getShapeNum();
			if (shapenum == EFile.EXULT_FLX_SAV_DOWNDOWN_SHP)
				((NewFileGump) parent).scroll_page(1);
			else if (shapenum == EFile.EXULT_FLX_SAV_DOWN_SHP)
				((NewFileGump) parent).scroll_line(1);
			else if (shapenum == EFile.EXULT_FLX_SAV_UP_SHP)
				((NewFileGump) parent).scroll_line(-1);
			else if (shapenum == EFile.EXULT_FLX_SAV_UPUP_SHP)
				((NewFileGump) parent).scroll_page(-1);
			return true;
		}
	};
	static class NewfileTextButton extends GumpWidget.TextButton {
		NewfileTextButton(Gump par, String text, int px, int py, int width) {
			super(par, text, px, py, width, 0);
		}
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			if (text == loadtext)
				((NewFileGump) parent).load();
			else if (text == savetext)
				((NewFileGump) parent).save();
			else if (text == deletetext)
				((NewFileGump) parent).delete_file();
			else if (text == canceltext)
				parent.close();
			return true;
		}
	}
}
