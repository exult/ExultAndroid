package com.exult.android;
import java.util.Vector;
import java.util.LinkedList;
import android.graphics.Point;

public final class Conversation extends GameSingletons {
	private NpcFaceInfo faceInfo[];	// NPC's on-screen faces in convers.
	private int numFaces;
	private int lastFaceShown;		// Index of last npc face shown.
	private Rectangle avatarFace;		// Area take by Avatar in conversation.
	private Rectangle convChoices[];	// Choices during a conversation.
	private int highlighted = -1;		// The choice currently highlighted.
	private Point clicked;
	private String userChoice;
	private Vector<String> answers;
	private LinkedList<Vector<String> > answerStack;

	public Conversation() {
		faceInfo = new NpcFaceInfo[2];
		avatarFace = new Rectangle();
		answers = new Vector<String>();
		answerStack = new LinkedList<Vector<String> >();
		clicked = new Point();
	}
	public final String getUserChoice() {
		return userChoice;
	}
	public final void setUserChoice(String c) {
		userChoice = c;
		highlighted = -1;
	}
	public final int getNumAnswers() {
		return answers.size();
	}
	public final String getAnswer(int num) {
		return answers.elementAt(num);
	}
	public final int getNumFacesOnScreen() { 
		return numFaces; 
	}
	public void clearAnswers() {
		answers.clear();
	}
	public boolean stackEmpty() { 
		return answerStack.isEmpty(); 
	}
	private void addAnswer(String str) {
		removeAnswer(str);
		answers.add(str);
	}
	/*
	 *	Add an answer to the list.
	 */
	public void addAnswer(UsecodeValue val) {
		String str;
		int size = val.getArraySize();
		if (size > 0) {			// An array?
			for (int i = 0; i < size; i++)
				addAnswer(val.getElem(i));
		}
		else if ((str = val.getStringValue()) != null)
			addAnswer(str);
	}
	private void removeAnswer(String str) {
		answers.remove(str);
	}
	/*
	 *	Remove an answer from the list.
	 */
	public void removeAnswer(UsecodeValue val)
	{
		String str;
		if (val.isArray()) {
			int size = val.getArraySize();
			for (int i=0; i < size; i++) {
				str = val.getElem(i).getStringValue();
				if (str != null) 
					removeAnswer(str);
			}
		} else {
			str = val.getStringValue();
			if (str != null)
				removeAnswer(str);
		}
	}
	/*
	 *	Initialize face list.
	 */
	public void initFaces() {
		int maxFaces = faceInfo.length;
		for (int i = 0; i < maxFaces; i++) {
			faceInfo[i] = null;
		}
		numFaces = 0;
		lastFaceShown = -1;
	}
	private void setFaceRect
		(
		NpcFaceInfo info,
		NpcFaceInfo prev,
		int screenw,
		int screenh
		) {
		int textHeight = fonts.getTextHeight(0);
					// Figure starting y-coord.
					// Get character's portrait.
		ShapeFrame face = info.shape.getShapeNum() >= 0 ? info.shape.getShape() 
													: null;
		int face_w = 32, face_h = 32;
		if (face != null) {
			face_w = face.getWidth(); 
			face_h = face.getHeight();
		}
		int starty;
		if (prev != null) {
			starty = prev.textRect.y + prev.lastTextHeight;
			if (starty < prev.faceRect.y + prev.faceRect.h)
				starty = prev.faceRect.y + prev.faceRect.h;
			starty += 2*textHeight;
			if (starty + face_h > screenh - 1)
				starty = screenh - face_h - 1;
			}
		else
			starty = 1;
		info.faceRect.set(8, starty, face_w + 4, face_h + 4);
		gwin.clipToWin(info.faceRect);
		Rectangle fbox = info.faceRect;
					// This is where NPC text will go.
		info.textRect.set(fbox.x + fbox.w + 3, fbox.y + 3,
			screenw - fbox.x - fbox.w - 6, 4*textHeight);
		gwin.clipToWin(info.textRect);
					// No room?  (Serpent?)
		if (info.textRect.w < 16 || info.textRect.h < 16) {
					// Show in lower center.
			int x = screenw/5, y = 3*(screenh/4);
			info.textRect.set(x, y, screenw-(2*x), screenh - y - 4);
			info.largeFace = true;
		}
		info.lastTextHeight = info.textRect.h;
	}
	/*
	 *	Show a "face" on the screen.  NpcTextRect is also set.
	 *	If shape < 0, an empty space is shown.
	 */
	public void showFace(int shape, int frame, int slot)
	{
		ShapeID faceSid = new ShapeID(shape, frame, ShapeFiles.FACES_VGA);

		int maxFaces = faceInfo.length;

		// Make sure mode is set right.
		Palette pal = gwin.getPal();	// Watch for weirdness (lightning).
		if (pal.getBrightness() >= 300)
			pal.set(-1, 100, null);
		Shortcuts.clearZoom();
						// Get screen dims.
		int screenw = gwin.getWidth(), screenh = gwin.getHeight();
		NpcFaceInfo info = null;
						// See if already on screen.
		for (int i = 0; i < maxFaces; i++)
			if (faceInfo[i] != null && faceInfo[i].faceNum == shape) {
				info = faceInfo[i];
				lastFaceShown = i;
				System.out.println("Found face slot " + i);
				break;
			}
		if (info == null) {			// New one?
			if (numFaces == maxFaces)
						// None free?  Steal last one.
				removeSlotFace(maxFaces - 1);
			info = new NpcFaceInfo(faceSid, shape);
			if (slot == -1)		// Want next one?
				slot = numFaces;
						// Get last one shown.
			NpcFaceInfo prev = slot != 0 ? faceInfo[slot - 1] : null;
			System.out.println("New face slot is " + slot + " for shape " + shape);
			lastFaceShown = slot;
			if (faceInfo[slot] == null)
				numFaces++;	// We're adding one (not replacing).
			faceInfo[slot] = info;
			setFaceRect(info, prev, screenw, screenh);
		}
		paintFaces(false);			// Paint all faces.
	}

	/*
	 *	Change the frame of the face on given slot.
	 */

	public void changeFaceFrame(int frame, int slot) {
		int maxFaces = faceInfo.length;
		// Make sure mode is set right.
		Palette pal = gwin.getPal();	// Watch for weirdness (lightning)
		if (pal.getBrightness() >= 300)
			pal.set(-1, 100, null);
		if (slot >= maxFaces || faceInfo[slot] == null)
			return;			// Invalid slot.

		lastFaceShown = slot;
		NpcFaceInfo info = faceInfo[slot];
						// These are needed in case conversation is done.
		if (info.shape.getShapeNum() < 0 ||
			frame > info.shape.getNumFrames())
			return;		// Invalid frame.

		if (frame == info.shape.getFrameNum())
			return;		// We are done here.

		info.shape.setFrame(frame);
			// Get screen dims.
		int screenw = gwin.getWidth(), screenh = gwin.getHeight();
		NpcFaceInfo prev = slot != 0 ? faceInfo[slot - 1] : null;
		setFaceRect(info, prev, screenw, screenh);

		paintFaces(false);			// Paint all faces.
	}

	/*
	 *	Remove face from screen.
	 */
	public void removeFace(int shape) {
		int maxFaces = faceInfo.length;
		int i;				// See if already on screen.
		for (i = 0; i < maxFaces; i++)
			if (faceInfo[i] != null && faceInfo[i].faceNum == shape)
				break;
		if (i == maxFaces)
			return;			// Not found.
		removeSlotFace(i);
	}

	/*
	 *	Remove face from indicated slot (SI).
	 */
	public void removeSlotFace(int slot) {
		int maxFaces = faceInfo.length;
		if (slot >= maxFaces || faceInfo[slot] == null)
			return;			// Invalid.
		NpcFaceInfo info = faceInfo[slot];
						// These are needed in case conversa-
						//   tion is done.
		gwin.addDirty(info.faceRect);
		gwin.addDirty(info.textRect);
		faceInfo[slot] = null;
		numFaces--;
		if (lastFaceShown == slot) {	// Just in case.
			int j;
			for (j = maxFaces - 1; j >= 0; j--)
				if (faceInfo[j] != null)
					break;
			lastFaceShown = j;
		}
	}
	/*
	 *	Show what the NPC had to say.
	 */
	public void showNpcMessage(String msg) {
		System.out.println("showNpcMessage: lastFaceShown = " + lastFaceShown);
		if (lastFaceShown == -1)
			return;
		NpcFaceInfo info = faceInfo[lastFaceShown];
		int font = info.largeFace ? 7 : 0;	// Use red for Guardian, snake.
		info.curText = msg;
		Rectangle box = info.textRect;
		int height;			// Break at punctuation.
		/* NOTE:  The original centers text for Guardian, snake.	*/
		while ((height = fonts.paintTextBox(gwin.getWin(), font, info.curText, 
				box.x, box.y, box.w,box.h, -1, true, info.largeFace)) < 0) {
						// More to do?
			String nxtMsg = msg.substring(-height, info.curText.length());
			gwin.addDirty(info.textRect);
			ExultActivity.getClick(clicked);
			info.curText = nxtMsg;
			gwin.addDirty(info.textRect);
		}
						// All fit?  Store height painted.
		info.lastTextHeight = height;
		info.textPending = true;
		gwin.addDirty(info.textRect);
	}
	/*
	 *	Is there NPC text that the user hasn't had a chance to read?
	 */
	boolean isNpcTextPending() {
		int maxFaces = faceInfo.length;
		for (int i = 0; i < maxFaces; i++)
			if (faceInfo[i] != null && faceInfo[i].textPending)
				return true;
		return false;
	}
	/*
	 *	Clear text-pending flags.
	 */
	public void clearTextPending() {
		int maxFaces = faceInfo.length;
		for (int i = 0; i < maxFaces; i++)	// Clear 'pending' flags.
			if (faceInfo[i] != null)
				faceInfo[i].textPending = false;
	}
	/*
	 *	Show the Avatar's conversation choices (and face).
	 */
	public void showAvatarChoices() {
		boolean SI = game.isSI();
		// Actor mainActor = gwin.getMainActor();
		int maxFaces = faceInfo.length;
						// Get screen rectangle.
		Rectangle sbox = new Rectangle(0, 0, gwin.getWidth(), gwin.getHeight());
		int x = 0, y = 0;		// Keep track of coords. in box.
		int height = fonts.getTextHeight(0);
		int spaceWidth = fonts.getTextWidth(0, " ");

		// Get main actor's portrait, checking for Petra flag.
		int shape = 0; // +++++++FINISH Shapeinfo_lookup::GetFaceReplacement(0);
		int frame = 0;
		/* +++++++++FINISH
		if (shape == 0) {
			Skin_data *skin = Shapeinfo_lookup::GetSkinInfoSafe(MainActor); 
			if (MainActor.getFlag(GameObject.tattooed))
				{
				shape = skin.alterFace_shape;
				frame = skin.alterFaceFrame;
				}
			else
				{
				shape = skin.face_shape;
				frame = skin.faceFrame;
				}
			}
		*/
		ShapeID faceSid = new ShapeID(shape, frame, ShapeFiles.FACES_VGA);
		ShapeFrame face = faceSid.getShape();
		int empty;			// Find face prev. to 1st empty slot.
		for (empty = 0; empty < maxFaces; empty++)
			if (faceInfo[empty] == null)
				break;
						// Get last one shown.
		NpcFaceInfo prev = empty != 0 ? faceInfo[empty - 1] : null;
		int fx = prev != null ? prev.faceRect.x + prev.faceRect.w + 4 : 16;
		int fy;
		if (SI) {
			if (numFaces == maxFaces)
						// Remove face #1 if still there.
				removeSlotFace(maxFaces - 1);
			fy = sbox.h - 2 - face.getHeight();
			fx = 8;
		} else if (prev == null)
			fy = sbox.h - face.getHeight() - 3*height;
		else {
			fy = prev.textRect.y + prev.lastTextHeight;
			if (fy < prev.faceRect.y + prev.faceRect.h)
				fy = prev.faceRect.y + prev.faceRect.h;
			fy += height;
		}
		Rectangle mbox = new Rectangle(fx, fy, face.getWidth(), face.getHeight());
		mbox.intersect(sbox);
		avatarFace = mbox;		// Repaint entire width.
						// Set to where to draw sentences.
		Rectangle tbox = new Rectangle(mbox.x + mbox.w + 8, mbox.y + 4,
					sbox.w - mbox.x - mbox.w - 16,
//					sbox.h - mbox.y - 16);
					5*height);// Try 5 lines.
		tbox.intersect(sbox);
						// Draw portrait.
		faceSid.paintShape(mbox.x + face.getXLeft(), mbox.y + face.getYAbove());
		// Set up new list of choices.	
		synchronized(this) {
		int numChoices = answers.size();
		convChoices = new Rectangle[numChoices];
		for (int i = 0; i < numChoices; i++) {
			String text = (char)(127) +  answers.elementAt(i);	// 127 is a circle.
			int width = fonts.getTextWidth(0, text);
			if (x > 0 && x + width >= tbox.w) {		// Start a new line.
				x = 0;
				y += height - 1;
			}
						// Store info.
			convChoices[i] = new Rectangle(tbox.x + x, tbox.y + y,
						width, height);
			convChoices[i].intersect(sbox);
			avatarFace.add(convChoices[i]);
			if (i == highlighted)
				gwin.getWin().fill8(ShapeID.getSpecialPixel(ShapeID.HIT_PIXEL), 
						convChoices[i].w, convChoices[i].h, convChoices[i].x, convChoices[i].y);
			fonts.paintTextBox(gwin.getWin(), 0, text, tbox.x + x, tbox.y + y,
				width + spaceWidth, height, 0, false, false);
			x += width + spaceWidth;
		}
		} // synchronized
		avatarFace.enlarge((3*EConst.c_tilesize)/4);		// Encloses entire area.
		avatarFace.intersect(sbox);
		clearTextPending();
		gwin.setPainted();
	}
	public void clearAvatarChoices() {
		gwin.addDirty(avatarFace);
		avatarFace.w = 0;
	}
	public void setHighlighted(int n) {
		if (n != highlighted) {
			highlighted = n;
			gwin.addDirty(avatarFace);
		}
	}
	/*
	 *	User clicked during a conversation.
	 *
	 *	Output:	Index (0-n) of choice, or -1 if not on a choice.
	 */
	public int conversationChoice(int x, int y) {
		synchronized(this) {
			int i, cnt = convChoices.length;
			for (i = 0; i < cnt &&
				!convChoices[i].hasPoint(x, y); i++)
				;
			if (i < cnt)	// Found one?
			return (i);
		else
			return (-1);
		}
	}

	/*
	 *	Repaint everything.
	 */

	public void paint
		(
		)
		{
		paintFaces(true);
		if (avatarFace.w > 0)		// Choices?
			showAvatarChoices();
		}

	/*
	 *	Repaint the faces.   Assumes clip has already been set to screen.
	 */

	public void paintFaces
		(
		boolean text			// Show text too.
		) {
		if (numFaces == 0)
			return;
		int maxFaces = faceInfo.length;
		for (int i = 0; i < maxFaces; i++)
			{
			NpcFaceInfo finfo = faceInfo[i];
			if (finfo == null)
				continue;
			ShapeFrame face = finfo.faceNum >= 0 ? 
					finfo.shape.getShape() : null;
			int faceXLeft = 0, faceYAbove = 0;
			if (face != null)
				{
				faceXLeft = face.getXLeft();
				faceYAbove = face.getYAbove();
						// Use translucency.
				finfo.shape.paintShapeTranslucent(
					finfo.faceRect.x + faceXLeft,
					finfo.faceRect.y + faceYAbove);
				}
			if (text && finfo.curText != null)		// Show text too?
				{
				Rectangle box = finfo.textRect;
						// Use red for Guardian, snake.
				int font = finfo.largeFace ? 7 : 0;
				fonts.paintTextBox(gwin.getWin(), font, finfo.curText, 
					box.x,box.y,box.w,box.h, -1, true, 
					finfo.largeFace);
				}
			}
		}


	/*
	 *  return nr. of conversation option 'str'. -1 if not found
	 */

	public int locateAnswer(String str) {
		int num = answers.indexOf(str);
		return num;
	}

	public void pushAnswers()
	{
	  answerStack.addFirst(answers);
	  answers = new Vector<String>();
	}

	public void popAnswers()
	{
	  answers=answerStack.removeFirst();
	  gwin.setAllDirty();
	}

	/*
	 *	Store information about an NPC's face and text on the screen during
	 *	a conversation:
	 */
	private static class NpcFaceInfo {
	  ShapeID shape;
	  int faceNum;			// NPC's face shape #.
	  //int frame;
	  boolean textPending;		// Text has been written, but user
				  	//   has not yet been prompted.
	  Rectangle faceRect;		// Rectangle where face is shown.
	  Rectangle textRect;		// Rectangle NPC statement is shown in.
	  boolean largeFace;		// Guardian, snake.
	  int lastTextHeight;		// Height of last text painted.
	  String curText;		// Current text being shown.
	  NpcFaceInfo(ShapeID sid, int num) {
		  shape = sid;
		  faceNum = num; 
		  faceRect = new Rectangle();
		  textRect = new Rectangle();
	  }
	}
}
