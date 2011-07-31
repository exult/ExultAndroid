package com.exult.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.widget.EditText;

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
	private PortraitItem faceItem;
	private GumpWidget sexItem;
	private TextItem nameItem;
	private boolean newGame, startedNewGame;
	private VgaFile menuShapes;
	private int topx, topy, centerx;
	private Rectangle dirty = new Rectangle();
	
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
		// Name.
		addElem(new MenuItem(this, 10, menuShapes.getShape(0xc, 1), menuShapes.getShape(0xc, 0), topx+10, menuy+10));
		String user = "Droid";
		nameItem = new TextItem(this, user, font, topx + 60, menuy+10);
		addElem(nameItem);
		// Set up portrait.
		faceItem = new PortraitItem(this, 11, topx+290, menuy+61, si_installed, transTo);
		addElem(faceItem);
		// Sex.
		ShapeFrame sexShape = menuShapes.getShape(0xa, 0);
		addElem(new GumpWidget(this, sexShape, topx+10, menuy+25));
		int sexWidth = sexShape.getWidth()+10;
		if (sexWidth > 35) sexWidth += 25; 
		else sexWidth = 60;
		sexItem = new GumpWidget(this, menuShapes.getShape(0xB, faceItem.skinData.isFemale?1:0), topx + sexWidth, menuy + 25);
		addElem(sexItem);
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
		if (!newGame)
			audio.stopMusic();
		gwin.getPal().fadeOut(EConst.c_fade_out_time);
		gwin.setAllDirty();
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
				gwin.addDirty(selected.getDirty(dirty));
			}
			if (item != null) {
				item.setPushed(true);
				gwin.addDirty(item.getDirty(dirty));
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
	public void keyDown(int chr) { // Key pressed
		System.out.println("GameMenuGump: keyDown: " + chr);
		switch (chr) {
    	case KeyEvent.KEYCODE_BACK:
    		/* Doesn't work: if (newGame)
    			close();
    		else */
    			ExultActivity.askToQuit();
		}
    }
	public void textInput(int chr, int unicode) // Character typed (unicode)
		{ }
	private void promptName() {
		Activity ctx = ExultActivity.instanceOf();
		final EditText input = new EditText(ctx);
		input.setText(nameItem.text);
		final AlertDialog.Builder dlg = new AlertDialog.Builder(ctx)
	    .setTitle("Exult")
	    .setMessage("Avatar name:")
	    .setView(input)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            nameItem.text = input.getText().toString(); 
	            gwin.setAllDirty();//++++FOR NOW.  nameItem needs to get correct dirty rect.
	            //gwin.addDirty(nameItem.getDirty(dirty));
	        }
	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Do nothing.
	        }
	    });
		ctx.runOnUiThread(new Runnable() {
			public void run() { dlg.show(); } });
	}
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
			if (newMenu.startedNewGame) {
				close();
			} else 
				gwin.getPal().fadeIn(EConst.c_fade_in_time);
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
		case 10:	// Name
			promptName();
			break;
		case 11:	// Portrait changed.
			gwin.addDirty(faceItem.getDirty(dirty));
			sexItem.setShape(menuShapes.getShape(0xB, faceItem.skinData.isFemale?1:0));
			gwin.addDirty(sexItem.getDirty(dirty));
			break;
		case 12:	// Journey onward with new game.
			game.setAvSkin(faceItem.skinData.skinId);
			game.setAvName(nameItem.text);
			game.setAvSex(faceItem.skinData.isFemale);
			startedNewGame = gwin.initGamedat(true);
			close();
			break;
		case 13: // Return to menu.
			close(); break;
		}
	}
	public static class PortraitItem extends GumpWidget.Button {
		byte transTo[];	// Palette translation.
		SkinData skinData;
		VgaFile facesVga;
		boolean si_installed;
		int id;
		private void setPortrait() {
			ShapeFrame portrait = facesVga.getShape(skinData.faceShape, skinData.faceFrame);
			shape = onShape = portrait.translatePalette(transTo);
		}
		public PortraitItem(Gump par, int i, int px, int py, boolean si, byte trans[]) {
			super(par, null, null, px, py);
			id = i;
			si_installed = si;
			transTo = trans;
			// Skin info
			AvatarDefaultSkin defskin = ShapeInfoLookup.getDefaultAvSkin();
			skinData = ShapeInfoLookup.getSkinInfoSafe(
						defskin.default_skin, defskin.default_female, si_installed);
			facesVga = new VgaFile();		// Get faces.
			final int srcCnt = 3;
			String sources[] = new String[srcCnt];
			int resourceIds[] = new int[srcCnt];
			sources[0] = EFile.FACES_VGA; resourceIds[0] = -1;		
			// Multiracial faces.
			StringIntPair rsc = game.getResource("files/mrfacesvga");
			sources[1] = rsc.str; resourceIds[1] = rsc.num;
			sources[2] = EFile.PATCH_FACES; resourceIds[2] = -1;
			facesVga.load(sources, resourceIds);
			setPortrait();
		}
		@Override
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			skinData = ShapeInfoLookup.getNextSelSkin(skinData, si_installed, true);
			setPortrait();
			((GameMenuGump)parent).handleChoice(id);
			return true;
		}
	}
	public static class MenuItem extends GumpWidget.Button {
		int id;
		public MenuItem(Gump par, int i, ShapeFrame onShape, ShapeFrame offShape, int px, int py) {
			super(par, onShape, offShape, px, py);
			id = i;
		}
		@Override
		public boolean onWidget(int mx, int my) {
			mx -= parent.getX() + x;	// Get point rel. to gump.
			my -= parent.getY() + y;
			// Check for box.
			return shape != null && shape.boxHasPoint(mx, my);
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
	public static class TextItem extends GumpWidget {
		String text;
		FontsVgaFile.Font font;
		public TextItem(Gump par, String txt, FontsVgaFile.Font f, int px, int py) {
			super(par, null, px, py);
			text = txt;
			font = f;
		}
		@Override
		public void paint() {
			int px = 0, py = 0;
			if (parent != null) {
				px = parent.getX();
				py = parent.getY();
			}
			font.drawText(gwin.getWin(), x+px, y+py, text);
		}
	}
}
