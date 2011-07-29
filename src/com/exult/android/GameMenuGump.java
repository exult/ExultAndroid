package com.exult.android;

import com.exult.android.Gump.Modal;
import com.exult.android.shapeinf.ShapeInfoLookup;
import com.exult.android.shapeinf.ShapeInfoLookup.AvatarDefaultSkin;
import com.exult.android.shapeinf.ShapeInfoLookup.SkinData;
import com.exult.android.shapeinf.ShapeInfoLookup.StringIntPair;

/*
 * This is the top-level menu for Black Gate or Serpent Isle. It is also the new-game menu.
 */
public class GameMenuGump extends Modal {
	private static final int menuChoices[] = { 0x04, 0x05, 0x08, 0x06, 0x11, 0x12, 0x07 };
	private GumpWidget.Button selected;
	private boolean newGame;
	private VgaFile menuShapes;
	private int topx, topy, centerx;
	
	private void initTop() {
		int menuy = topy + 120;
		int offset = 0, cnt = menuChoices.length;
		for (int i = 0; i < cnt; ++i) {
			ShapeFrame onShape = menuShapes.getShape(menuChoices[i], 1),
					  offShape = menuShapes.getShape(menuChoices[i], 0);
			int width = offShape.getWidth();
			MenuItem entry = new MenuItem(this, i, onShape, offShape, centerx - width/2, menuy + offset);
			addElem(entry);	
			offset += offShape.getYBelow() + 3;
		}
	}
	private void initNewGame() {
		int menuy = topy + 110;
		Boolean si_installed = false;	// FOR NOW.
		FontsVgaFile.Font font = FontsVgaFile.getFont("MENU_FONT");
		// Set up palette.
		DataSource ds = DataSource.create(EFile.PATCH_INTROPAL, 6);
		if (ds == null) {
			ds = DataSource.create(game.getResource("files/gameflx").str, EFile.EXULT_BG_FLX_U7MENUPAL_PAL); // Just the one palette.
			if (ds == null) {
				ds = DataSource.create(EFile.INTROPAL_DAT, 6);
			}
		}
		Palette tmppal = new Palette(gwin.getWin());
		tmppal.load(ds);
		Palette oldpal = gwin.getPal();
		//oldpal.load(EFile.INTROPAL_DAT, EFile.PATCH_INTROPAL, 6);
		byte transTo[] = new byte[256];
		tmppal.createPaletteMap(oldpal, transTo);
		// Skin info
		AvatarDefaultSkin defskin = ShapeInfoLookup.getDefaultAvSkin();
		SkinData skindata =
			ShapeInfoLookup.getSkinInfoSafe(
					defskin.default_skin, defskin.default_female, si_installed);
		addElem(new MenuItem(this, 10, menuShapes.getShape(0xc, 1), menuShapes.getShape(0xc, 0), topx+10, menuy+10));
		ShapeFrame sexShape = menuShapes.getShape(0xa, 0);
		addElem(new MenuItem(this, 11, menuShapes.getShape(0xa, 1), sexShape, topx+10, menuy+25));
		int sexWidth = sexShape.getWidth()+10;
		if (sexWidth > 35) sexWidth += 25; 
		else sexWidth = 60;
		addElem(new GumpWidget(this, menuShapes.getShape(0xB, skindata.isFemale?1:0), topx + sexWidth, menuy + 25));
		// Set up portrait.
		VgaFile facesVga = new VgaFile();		// Get faces.
		final int srcCnt = 3;
		String sources[] = new String[srcCnt];
		int resourceIds[] = new int[srcCnt];
		sources[0] = EFile.FACES_VGA; resourceIds[0] = -1;		
		// Multiracial faces.
		StringIntPair rsc = game.getResource("files/mrfacesvga");
		sources[1] = rsc.str; resourceIds[1] = rsc.num;
		sources[2] = EFile.PATCH_FACES; resourceIds[2] = -1;
		facesVga.load(sources, resourceIds);
		ShapeFrame portrait = facesVga.getShape(skindata.faceShape, skindata.faceFrame);
		portrait = portrait.translatePalette(transTo);//+++++TESTING
		GumpWidget faceItem = new GumpWidget(this, portrait, topx+290, menuy+61);	// FOR NOW.  Probably want a derived class.
		addElem(faceItem);
		// Journey onward, return to menu:
		addElem(new MenuItem(this, 12, menuShapes.getShape(8, 1), menuShapes.getShape(8, 0), topx+10, topy+180));
		addElem(new MenuItem(this, 13, menuShapes.getShape(7, 1), menuShapes.getShape(7, 0), centerx+10, topy+180));
	}
	private void init(boolean isNew) {
		newGame = isNew;
		topx = (gwin.getWidth()-320)/2;
		topy = (gwin.getHeight() - 200)/2;
		centerx = gwin.getWidth()/2;
		menuShapes = game.getMenuShapes();
		if (isNew)
			initNewGame();
		else
			initTop();
		gwin.setAllDirty();
	}
	//	Top-level
	public GameMenuGump(ShapeFrame s) {
		super(s);
		init(false);
	}
	//	New game.
	public GameMenuGump(ShapeFrame s, boolean isNew) {
		super(s);
		init(isNew);
	}
	@Override
	public void close() {
		audio.stopMusic();
		gwin.getPal().fadeOut(EConst.c_fade_out_time);
		super.close();
	}
	// Handle events:
	@Override
	public void onMotion(int mx, int my) {
		GumpWidget.Button item = onButton(mx, my);
		
		if (item != selected) {
			System.out.println("GameMenu: this = " + this);
			mouse.hide();
			if (selected != null) {
				selected.setPushed(false);
				gwin.setAllDirty();	// ++Just add dirty rectangle?
			}
			if (item != null) {
				item.setPushed(true);
				gwin.setAllDirty();	// ++Just add dirty rectangle?
			}
			selected = item;
		}
	}
	@Override
	public void onUp(int mx, int my) {
		GumpWidget.Button item = onButton(mx, my);
		if (item != null)
			item.activate(true);
	}
	public void keyDown(int chr) // Key pressed
		{  }
	public void textInput(int chr, int unicode) // Character typed (unicode)
		{ }
	public void handleChoice(int id) {
		switch (id) {
		//	Top-level choices.
		case 0:	// Intro
			audio.stopMusic();
			ExultActivity.instanceOf().runOnUiThread(new Runnable() {
				public void run() { game.playIntro(null); } });
			break;
		case 1: // New Game
			GameMenuGump newMenu = new GameMenuGump(shape, true);
			Thread t = newMenu.track(Mouse.hand);
			try {
				t.join();
			} catch (InterruptedException e) { }
			gwin.getPal().fadeIn(EConst.c_fade_in_time);	// This should depend on results of newMenu.++++++
			break;
		case 2:	// Journey Onwards
			close(); break;
		case 3:	// Credits
			break;
		case 4:	// Quotes
			break;
		case 5:	// End game
			break;
		case 6:	// Return to Exult menu
			break;
		//	New-game choices.
		case 10:
			close(); break;//+++++++TESTING
		case 11:
			close(); break;//+++++++TESTING
		}
	}
	
	public static class MenuItem extends GumpWidget.Button {
		int id;
		public MenuItem(Gump par, int i, ShapeFrame onShape, ShapeFrame offShape, int px, int py) {
			super(par, onShape, offShape, px, py);
			id = i;
		}
		// What to do when 'clicked':
		@Override
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			((GameMenuGump)parent).handleChoice(id);
			return true;
		}
	}
}
