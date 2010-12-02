package com.exult.android;
import java.util.Vector;
import java.util.LinkedList;

public final class Conversation extends GameSingletons {
	private NpcFaceInfo face_info[];	// NPC's on-screen faces in convers.
	private int num_faces;
	private int last_face_shown;		// Index of last npc face shown.
	private Rectangle avatar_face;		// Area take by Avatar in conversation.
	private Rectangle conv_choices[];	// Choices during a conversation.

	private Vector<String> answers;
	private LinkedList<Vector<String> > answer_stack;

	public Conversation() {
		face_info = new NpcFaceInfo[2];
		avatar_face = new Rectangle();
		answers = new Vector<String>();
		answer_stack = new LinkedList<Vector<String> >();
	}
	public void clear_answers() {
		answers.clear();
	}
	private void add_answer(String str) {
		remove_answer(str);
		answers.add(str);
	}
	/*
	 *	Add an answer to the list.
	 */
	public void add_answer(UsecodeValue val) {
		String str;
		int size = val.getArraySize();
		if (size > 0) {			// An array?
			for (int i = 0; i < size; i++)
				add_answer(val.getElem(i));
		}
		else if ((str = val.getStringValue()) != null)
			add_answer(str);
	}
	private void remove_answer(String str) {
		answers.remove(str);
	}
	/*
	 *	Remove an answer from the list.
	 */
	public void remove_answer(UsecodeValue val)
	{
		String str;
		if (val.isArray()) {
			int size = val.getArraySize();
			for (int i=0; i < size; i++) {
				str = val.getElem(i).getStringValue();
				if (str != null) 
					remove_answer(str);
			}
		} else {
			str = val.getStringValue();
			if (str != null)
				remove_answer(str);
		}
	}
	/*
	 *	Initialize face list.
	 */
	public void init_faces() {
		int max_faces = face_info.length;
		for (int i = 0; i < max_faces; i++) {
			face_info[i] = null;
		}
		num_faces = 0;
		last_face_shown = -1;
	}
	private void set_face_rect
		(
		NpcFaceInfo info,
		NpcFaceInfo prev,
		int screenw,
		int screenh
		) {
		int text_height = fonts.getTextHeight(0);
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
			starty = prev.text_rect.y + prev.last_text_height;
			if (starty < prev.face_rect.y + prev.face_rect.h)
				starty = prev.face_rect.y + prev.face_rect.h;
			starty += 2*text_height;
			if (starty + face_h > screenh - 1)
				starty = screenh - face_h - 1;
			}
		else
			starty = 1;
		info.face_rect.set(8, starty, face_w + 4, face_h + 4);
		gwin.clipToWin(info.face_rect);
		Rectangle fbox = info.face_rect;
					// This is where NPC text will go.
		info.text_rect.set(fbox.x + fbox.w + 3, fbox.y + 3,
			screenw - fbox.x - fbox.w - 6, 4*text_height);
		gwin.clipToWin(info.text_rect);
					// No room?  (Serpent?)
		if (info.text_rect.w < 16 || info.text_rect.h < 16) {
					// Show in lower center.
			int x = screenw/5, y = 3*(screenh/4);
			info.text_rect.set(x, y, screenw-(2*x), screenh - y - 4);
			info.large_face = true;
		}
		info.last_text_height = info.text_rect.h;
	}
	/*
	 *	Show a "face" on the screen.  Npc_text_rect is also set.
	 *	If shape < 0, an empty space is shown.
	 */
	public void show_face(int shape, int frame, int slot)
	{
		ShapeID face_sid = new ShapeID(shape, frame, ShapeFiles.FACES_VGA);

		int max_faces = face_info.length;

		// Make sure mode is set right.
		Palette pal = gwin.getPal();	// Watch for weirdness (lightning).
		/*+++++++++++++
		if (pal.getBrightness() >= 300)
			pal.set(-1, 100);
		*/
						// Get screen dims.
		int screenw = gwin.getWidth(), screenh = gwin.getHeight();
		NpcFaceInfo info = null;
						// See if already on screen.
		for (int i = 0; i < max_faces; i++)
			if (face_info[i] != null && face_info[i].face_num == shape) {
				info = face_info[i];
				last_face_shown = i;
				break;
			}
		if (info == null) {			// New one?
			if (num_faces == max_faces)
						// None free?  Steal last one.
				remove_slot_face(max_faces - 1);
			info = new NpcFaceInfo(face_sid, shape);
			if (slot == -1)		// Want next one?
				slot = num_faces;
						// Get last one shown.
			NpcFaceInfo prev = slot != 0 ? face_info[slot - 1] : null;
			last_face_shown = slot;
			if (face_info[slot] == null)
				num_faces++;	// We're adding one (not replacing).
			face_info[slot] = info;
			set_face_rect(info, prev, screenw, screenh);
		}
		//++++++++MIGHT NEED to sync here.
		gwin.getWin().setClip(0, 0, screenw, screenh);
		paint_faces(false);			// Paint all faces.
		gwin.getWin().clearClip();
		}

	/*
	 *	Change the frame of the face on given slot.
	 */

	public void change_face_frame(int frame, int slot) {
		int max_faces = face_info.length;
		// Make sure mode is set right.
		Palette pal = gwin.getPal();	// Watch for weirdness (lightning).
		/* +++++++++++
		if (pal.getBrightness() >= 300)
			pal.set(-1, 100);
		 */
		if (slot >= max_faces || face_info[slot] == null)
			return;			// Invalid slot.

		last_face_shown = slot;
		NpcFaceInfo info = face_info[slot];
						// These are needed in case conversation is done.
		if (info.shape.getShapeNum() < 0 ||
			frame > info.shape.getNumFrames())
			return;		// Invalid frame.

		if (frame == info.shape.getFrameNum())
			return;		// We are done here.

		info.shape.setFrame(frame);
			// Get screen dims.
		int screenw = gwin.getWidth(), screenh = gwin.getHeight();
		NpcFaceInfo prev = slot != 0 ? face_info[slot - 1] : null;
		set_face_rect(info, prev, screenw, screenh);

		// +++++++++SYNC?
		gwin.getWin().setClip(0, 0, screenw, screenh);
		paint_faces(false);			// Paint all faces.
		gwin.getWin().clearClip();
	}

	/*
	 *	Remove face from screen.
	 */
	public void remove_face(int shape) {
		int max_faces = face_info.length;
		int i;				// See if already on screen.
		for (i = 0; i < max_faces; i++)
			if (face_info[i] != null && face_info[i].face_num == shape)
				break;
		if (i == max_faces)
			return;			// Not found.
		remove_slot_face(i);
	}

	/*
	 *	Remove face from indicated slot (SI).
	 */
	public void remove_slot_face(int slot) {
		int max_faces = face_info.length;
		if (slot >= max_faces || face_info[slot] == null)
			return;			// Invalid.
		NpcFaceInfo info = face_info[slot];
						// These are needed in case conversa-
						//   tion is done.
		gwin.addDirty(info.face_rect);
		gwin.addDirty(info.text_rect);
		face_info[slot] = null;
		num_faces--;
		if (last_face_shown == slot) {	// Just in case.
			int j;
			for (j = max_faces - 1; j >= 0; j--)
				if (face_info[j] != null)
					break;
			last_face_shown = j;
		}
	}
	/*
	 *	Show what the NPC had to say.
	 */
	public void show_npc_message(String msg) {
		if (last_face_shown == -1)
			return;
		NpcFaceInfo info = face_info[last_face_shown];
		int font = info.large_face ? 7 : 0;	// Use red for Guardian, snake.
		info.cur_text = "";
		Rectangle box = info.text_rect;
		gwin.paint();
		int height;			// Break at punctuation.
		/* NOTE:  The original centers text for Guardian, snake.	*/
		while ((height = fonts.paintTextBox(gwin.getWin(), font, msg, box.x, box.y,
					box.w,box.h, -1, true, info.large_face)) < 0) {
						// More to do?
			info.cur_text = msg.substring(-height, msg.length());
			int x, y; char c;
			gwin.paint();		// Paint scenery beneath
			/* +++++++++++FINISH
			Get_click(x, y, Mouse::hand, &c, false, this, true);
			*/
			gwin.paint();
			msg += -height;
		}
						// All fit?  Store height painted.
		info.last_text_height = height;
		info.cur_text = msg;
		info.text_pending = true;
		gwin.setPainted();
//		gwin.show();
	}
	/*
	 *	Is there NPC text that the user hasn't had a chance to read?
	 */
	boolean is_npc_text_pending() {
		int max_faces = face_info.length;
		for (int i = 0; i < max_faces; i++)
			if (face_info[i] != null && face_info[i].text_pending)
				return true;
		return false;
	}
	/*
	 *	Clear text-pending flags.
	 */
	public void clear_text_pending() {
		int max_faces = face_info.length;
		for (int i = 0; i < max_faces; i++)	// Clear 'pending' flags.
			if (face_info[i] != null)
				face_info[i].text_pending = false;
	}
	/*
	 *	Show the Avatar's conversation choices (and face).
	 */
	public void show_avatar_choices(String choices[]) {
		int num_choices = choices.length;
		boolean SI = false; // +++++++ Game::get_game_type()==SERPENT_ISLE;
		Actor mainActor = gwin.getMainActor();
		int max_faces = face_info.length;
						// Get screen rectangle.
		Rectangle sbox = new Rectangle(0, 0, gwin.getWidth(), gwin.getHeight());
		int x = 0, y = 0;		// Keep track of coords. in box.
		int height = fonts.getTextHeight(0);
		int space_width = fonts.getTextWidth(0, " ");

		// Get main actor's portrait, checking for Petra flag.
		int shape = 0; // +++++++FINISH Shapeinfo_lookup::GetFaceReplacement(0);
		int frame = 0;
		/* +++++++++FINISH
		if (shape == 0) {
			Skin_data *skin = Shapeinfo_lookup::GetSkinInfoSafe(MainActor); 
			if (MainActor.getFlag(GameObject.tattooed))
				{
				shape = skin.alter_face_shape;
				frame = skin.alter_face_frame;
				}
			else
				{
				shape = skin.face_shape;
				frame = skin.face_frame;
				}
			}
		*/
		ShapeID face_sid = new ShapeID(shape, frame, ShapeFiles.FACES_VGA);
		ShapeFrame face = face_sid.getShape();
		int empty;			// Find face prev. to 1st empty slot.
		for (empty = 0; empty < max_faces; empty++)
			if (face_info[empty] == null)
				break;
						// Get last one shown.
		NpcFaceInfo prev = empty != 0 ? face_info[empty - 1] : null;
		int fx = prev != null ? prev.face_rect.x + prev.face_rect.w + 4 : 16;
		int fy;
		if (SI) {
			if (num_faces == max_faces)
						// Remove face #1 if still there.
				remove_slot_face(max_faces - 1);
			fy = sbox.h - 2 - face.getHeight();
			fx = 8;
		} else if (prev == null)
			fy = sbox.h - face.getHeight() - 3*height;
		else {
			fy = prev.text_rect.y + prev.last_text_height;
			if (fy < prev.face_rect.y + prev.face_rect.h)
				fy = prev.face_rect.y + prev.face_rect.h;
			fy += height;
		}
		Rectangle mbox = new Rectangle(fx, fy, face.getWidth(), face.getHeight());
		mbox.intersect(sbox);
		avatar_face = mbox;		// Repaint entire width.
						// Set to where to draw sentences.
		Rectangle tbox = new Rectangle(mbox.x + mbox.w + 8, mbox.y + 4,
					sbox.w - mbox.x - mbox.w - 16,
//					sbox.h - mbox.y - 16);
					5*height);// Try 5 lines.
		tbox.intersect(sbox);
						// Draw portrait.
		face_sid.paintShape(mbox.x + face.getXLeft(), mbox.y + face.getYAbove());
		// Set up new list of choices.
		conv_choices = new Rectangle[num_choices + 1];
		for (int i = 0; i < num_choices; i++) {
			String text = (char)(127) + choices[i];	// 127 is a circle.
			int width = fonts.getTextWidth(0, text);
			if (x > 0 && x + width >= tbox.w) {		// Start a new line.
				x = 0;
				y += height - 1;
			}
						// Store info.
			conv_choices[i] = new Rectangle(tbox.x + x, tbox.y + y,
						width, height);
			conv_choices[i].intersect(sbox);
			avatar_face.add(conv_choices[i]);
			fonts.paintTextBox(gwin.getWin(), 0, text, tbox.x + x, tbox.y + y,
				width + space_width, height, 0, false, false);
			x += width + space_width;
			}
		avatar_face.enlarge((3*EConst.c_tilesize)/4);		// Encloses entire area.
		avatar_face.intersect(sbox);
						// Terminate the list.
		conv_choices[num_choices] = new Rectangle(0, 0, 0, 0);
		clear_text_pending();
		gwin.setPainted();
	}

	public void show_avatar_choices() {
		String result[];
		int i;	// Blame MSVC

		result=new String[answers.size()];
		for (i=0;i<answers.size();i++) {
			result[i]=new String(answers.elementAt(i));
		}
		show_avatar_choices(result);
	}

	public void clear_avatar_choices()
	{
//		gwin.paint(avatar_face);	// Paint over face and answers.
		gwin.addDirty(avatar_face);
		avatar_face.w = 0;
	}


	/*
	 *	User clicked during a conversation.
	 *
	 *	Output:	Index (0-n) of choice, or -1 if not on a choice.
	 */

	int conversation_choice(int x, int y)
	{
		int i;
		for (i = 0; conv_choices[i].w != 0 &&
				!conv_choices[i].hasPoint(x, y); i++)
			;
		if (conv_choices[i].w != 0)	// Found one?
			return (i);
		else
			return (-1);
	}

	/*
	 *	Repaint everything.
	 */

	public void paint
		(
		)
		{
		paint_faces(true);
		if (avatar_face.w > 0)		// Choices?
			show_avatar_choices();
		}

	/*
	 *	Repaint the faces.   Assumes clip has already been set to screen.
	 */

	public void paint_faces
		(
		boolean text			// Show text too.
		) {
		if (num_faces == 0)
			return;
		int max_faces = face_info.length;
		for (int i = 0; i < max_faces; i++)
			{
			NpcFaceInfo finfo = face_info[i];
			if (finfo == null)
				continue;
			ShapeFrame face = finfo.face_num >= 0 ? 
					finfo.shape.getShape() : null;
			int face_xleft = 0, face_yabove = 0;
			if (face != null)
				{
				face_xleft = face.getXLeft();
				face_yabove = face.getYAbove();
						// Use translucency.
				finfo.shape.paintShapeTranslucent(
					finfo.face_rect.x + face_xleft,
					finfo.face_rect.y + face_yabove);
				}
			if (text)		// Show text too?
				{
				Rectangle box = finfo.text_rect;
						// Use red for Guardian, snake.
				int font = finfo.large_face ? 7 : 0;
				fonts.paintTextBox(gwin.getWin(), font, finfo.cur_text, 
					box.x,box.y,box.w,box.h, -1, true, 
					finfo.large_face);
				}
			}
		}


	/*
	 *  return nr. of conversation option 'str'. -1 if not found
	 */

	public int locate_answer(String str) {
		int num = answers.indexOf(str);
		return num;
	}

	public void push_answers()
	{
	  answer_stack.addFirst(answers);
	  answers.clear();
	}

	public void copop_answers()
	{
	  answers=answer_stack.removeFirst();
	  gwin.paint();			// Really just need to figure tbox.
	}

	/*
	 *	Store information about an NPC's face and text on the screen during
	 *	a conversation:
	 */
	private static class NpcFaceInfo {
	  ShapeID shape;
	  int face_num;			// NPC's face shape #.
	  //int frame;
	  boolean text_pending;		// Text has been written, but user
				  	//   has not yet been prompted.
	  Rectangle face_rect;		// Rectangle where face is shown.
	  Rectangle text_rect;		// Rectangle NPC statement is shown in.
	  boolean large_face;		// Guardian, snake.
	  int last_text_height;		// Height of last text painted.
	  String cur_text;		// Current text being shown.
	  NpcFaceInfo(ShapeID sid, int num) {
		  shape = sid;
		  face_num = num; 
	  }
	}
}
