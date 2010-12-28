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
	/* +++++++++
	static final String infostring = 	// Text format for info
		"Avatar: %s\n"
		"Exp: %i  Hp: %i\n"
		"Str: %i  Dxt: %i\n"
		"Int: %i  Trn: %i\n"
		"\n"
		"Game Day: %i\n"
		"Game Time: %02i:%02i\n"
		"\n"
		"Save Count: %i\n"
		"Date: %i%s %s %04i\n"
		"Time: %02i:%02i";
	*/
	static final String months[] = new String[12];	// Names of the months
	boolean restored;		// Set to 1 if we restored a game.

	byte	back[];

	SaveInfo	games[];		// The list of savegames
	int		num_games;	// Number of save games
	int		first_free;	// The number of the first free savegame

	VgaFile.ShapeFile cur_shot;		// Screenshot for current game
	SaveGameDetails cur_details;	// Details of current game
	SaveGameParty cur_party;	// Party of current game

	// Gamedat is being used as a 'quicksave'
	VgaFile.ShapeFile gd_shot;		// Screenshot in Gamedat
	SaveGameDetails gd_details;	// Details in Gamedat
	SaveGameParty gd_party;	// Parts in Gamedat

	VgaFile.ShapeFile screenshot;		// The picture to be drawn
	SaveGameDetails details;	// The game details to show
	SaveGameParty party;		// The party to show
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
	void	PaintSaveName (int line) {
		//++++++++++++++++
	}
	public NewFileGump() {
		super(EFile.EXULT_FLX_SAVEGUMP_SHP, ShapeFiles.EXULT_FLX);	
		list_position = -2; 
		selected = -3;
		slide_start = -1;
		setObjectArea(0,0,320,200, -22, 190);
		tqueue.pause(TimeQueue.ticks);
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
		//++++++++
	}
	public void close()
		{ done = true; }
					// Handle events:
	public boolean mouseDown(int mx, int my, int button) {
		//+++++++++
		return true;
	}
	public boolean mouseUp(int mx, int my, int button) {
		//+++++++++
		return true;
	}
	public void mouseDrag(int mx, int my) {
		//+++++++++
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

		short		shape_file;	// 44

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
		SaveGameParty	party;
		ShapeFiles		screenshot;
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
