package com.exult.android;
import java.util.Vector;
import java.util.LinkedList;
import android.graphics.Point;

public class UsecodeIntrinsics extends GameSingletons {
	private static Tile tempTile = new Tile();
	private static Vector<GameObject> foundVec = new Vector<GameObject>();
	// Stack of last items created with intrins. x24.
	private static LinkedList<GameObject> last_created = new LinkedList<GameObject>();
	private static final GameObject getItem(UsecodeValue v) {
		return ucmachine.get_item(v);
	}
	
	/*
	 * The intrinsics:
	 */
	private final UsecodeValue getRandom(UsecodeValue p0) {
		int range = p0.getIntValue();
		if (range == 0)
			return UsecodeValue.getZero();
		return new UsecodeValue.IntValue(1 + (EUtil.rand() % range));
	}
	private int getFaceShape(UsecodeValue arg1, Actor npc, int frame) {
		int shape = -1;
		if (arg1 instanceof UsecodeValue.IntValue) {
			shape = Math.abs(arg1.getIntValue());
			if (shape == 356)	// Avatar.
				shape = 0;
		} else if (npc != null)
			shape = npc.getFaceShapeNum();
		if (shape < 0)	// No need to do anything else.
			return shape;
		// Checks for Petra flag.
		/*++++++++++++
		shape = Shapeinfo_lookup::GetFaceReplacement(shape);

		Actor iact;
		if (Game::get_game_type() == SERPENT_ISLE)
				{			// Special case: Nightmare Smith.
							//   (But don't mess up Guardian.)
				if (shape == 296 && this->frame->caller_item &&
				    (iact = this->frame->caller_item->as_actor()) != 0 &&
				    iact->get_npc_num() == 277)
					shape = 277;
				}

			// Another special case: map face shape 0 to
			// the avatar's correct face shape and frame:
			if (shape == 0)
				{
				Actor *ava = gwin->get_main_actor();
				bool sishapes = Shape_manager::get_instance()->have_si_shapes();
				Skin_data *skin = Shapeinfo_lookup::GetSkinInfoSafe(
						ava->get_skin_color(), npc ? (npc->get_type_flag(Actor::tf_sex)!=0)
							: (ava->get_type_flag(Actor::tf_sex)!=0), sishapes);
				if (gwin->get_main_actor()->get_flag(Obj_flags::tattooed))
					{
					shape = skin->alter_face_shape;
					frame = skin->alter_face_frame;
					}
				else
					{
					shape = skin->face_shape;
					frame = skin->face_frame;
					}
				}
		*/
		return shape;
	}
	private final void showNpcFace(UsecodeValue p0, UsecodeValue p1,
				int slot) {	// 0, 1, or -1 to find free spot.
		ucmachine.show_pending_text();
		GameObject item = getItem(p0);
		Actor npc = item.asActor();
		int frame = p1.getIntValue();
		int shape = getFaceShape(p0, npc, frame);
		if (shape < 0)
			return;
	
		if (true /* +++++ Game::get_game_type() == BLACK_GATE*/ && npc != null) {
			// Only do this if the NPC is the caller item.
			if (npc.getNpcNum() != -1) 
				npc.setFlag (GameObject.met);
		}
		if (conv.getNumFacesOnScreen() == 0)
			eman.removeTextEffects();
		// Only non persistent
		/* ++++++++++++
		if (gumpman->showing_gumps(true)) {
			gumpman->close_all_gumps();
			gwin->set_all_dirty();
			init_conversation();	// jsf-Added 4/20/01 for SI-Lydia.
		}
		*/
		gwin.paintDirty();
		conv.showFace(shape, frame, slot);
		//	user_choice = 0;		// Seems like a good idea.
		// Also seems to create a conversation bug in Test of Love :-(
	}

	private final void removeNpcFace(UsecodeValue p0) {
		ucmachine.show_pending_text();
		GameObject item = p0.getObjectValue();
		Actor npc = item.asActor();
		int shape = getFaceShape(p0, npc, 0);
		if (shape < 0)
			return;
		conv.removeFace(shape);
	}

	private final void addAnswer(UsecodeValue p0) {
		conv.addAnswer(p0);
		//	user_choice = 0;
	}

	private final void removeAnswer(UsecodeValue p0) {
		conv.removeAnswer(p0);
	// Commented out 'user_choice = 0' 8/3/00 for Tseramed conversation.
//		user_choice = 0;
	}

	private final void pushAnswers() {
		conv.pushAnswers();
	}

	private final void popAnswers() {
		if (!conv.stackEmpty()) {
			conv.popAnswers();
			conv.setUserChoice(null);	// Added 7/24/2000.
		}
	}

	private final void clearAnswers() {
		conv.clearAnswers();
	}
	
	private final void setItemShape(UsecodeValue itemVal, UsecodeValue shapeVal) {
		int shape = shapeVal.getIntValue();
		GameObject item = getItem(itemVal);
		if (item == null)
			return;
						// See if light turned on/off.
		boolean light_changed = item.getInfo().isLightSource() !=
				    ShapeID.getInfo(shape).isLightSource();
		ContainerGameObject owner = item.getOwner();
		if (owner != null) {		// Inside something?
			owner.changeMemberShape(item, shape);
			if (light_changed)	// Maybe we should repaint all.
				gwin.paint();	// Repaint finds all lights.
			else {
				/* +++++++++FINISH
				Gump *gump = gumpman.find_gump(item);
				if (gump)
					gump.paint();
				*/
			}
			return;
		}
		gwin.addDirty(item);
		MapChunk chunk = item.getChunk();	// Get chunk it's in.
		chunk.remove(item);		// Remove and add to update cache.
		item.setShape(shape);
		chunk.add(item);
		gwin.addDirty(item);
		if (light_changed)
			gwin.paint();		// Complete repaint refigures lights.
	}
	private final UsecodeValue findNearest(UsecodeValue objVal, 
			UsecodeValue shapeVal, UsecodeValue distVal) {
		// Think it rets. nearest obj. near parm0.
		GameObject obj = getItem(objVal);
		if (obj == null)
			return UsecodeValue.getNullObj();
		foundVec.clear();
		obj = obj.getOutermost();	// Might be inside something.
		int dist = distVal.getIntValue();
		int shnum = shapeVal.getIntValue();
						// Kludge for Test of Courage:
		if (ucmachine.getCurrentFunction() == 0x70a && shnum == 0x9a && dist == 0)
			dist = 16;		// Mage may have wandered.
		obj.getTile(tempTile);
		int cnt = gmap.findNearby(foundVec, tempTile, shnum, dist, 0);
		GameObject closest = null;
		int bestdist = 100000;// Distance-squared in tiles.
		int tx1 = obj.getTileX(), ty1 = obj.getTileY(), tz1 = obj.getLift();
		for (int i = 0; i < cnt; ++i) {
			GameObject each = foundVec.elementAt(i);
			each.getTile(tempTile);
			int dx = tx1 - tempTile.tx, dy = ty1 - tempTile.ty, 
				dz = tz1 - tempTile.tz;
			dist = dx*dx + dy*dy + dz*dz;
			if (dist < bestdist) {
				bestdist = dist;
				closest = each;
			}
		}
		return new UsecodeValue.ObjectValue(closest);
	}
	private final UsecodeValue dieRoll(UsecodeValue p0, UsecodeValue p1) {
		// Rand. # within range.
		int low = p0.getIntValue();
		int high = p1.getIntValue();
		if (low > high){
			int tmp = low;
			low = high;
			high = tmp;
		}
		int val = (EUtil.rand() % (high - low + 1)) + low;
		return new UsecodeValue.IntValue(val);
	}
	private final UsecodeValue getItemShape(UsecodeValue p0) {
		GameObject obj = getItem(p0);
		return obj == null ? UsecodeValue.getZero() :
			new UsecodeValue.IntValue(obj.getShapeReal());
	}
	private final UsecodeValue getItemFrame(UsecodeValue p0) {
		GameObject obj = getItem(p0);
		return obj == null ? UsecodeValue.getZero() :
			// Don't count rotated frames.
			new UsecodeValue.IntValue(obj.getFrameNum()&31);
	}
	// Set frame, but don't change rotated bit.
	private final void setItemFrame(UsecodeValue itemVal, UsecodeValue frameVal) {
		setItemFrame(getItem(itemVal), frameVal.getIntValue(), false, false);
	}
	private final void setItemFrame
		(
		GameObject item,
		int frame,
		boolean check_empty,		// If 1, don't set empty frame.
		boolean set_rotated			// Set 'rotated' bit to one in 'frame'.
		) {
		if (item == null)
			return;
							// Added 9/16/2001:
		if (!set_rotated)		// Leave bit alone?
			frame = (item.getFrameNum()&32)|(frame&31);
		if (frame == item.getFrameNum())
			return;			// Already set to that.
		Actor act = item.asActor();
			// Actors have frame replacements for empty frames:
		if (act != null)
			act.changeFrame(frame);
		else {			// Check for empty frame.
			ShapeFiles file = item.getShapeFile();
			ShapeFrame shape = file.getShape(item.getShapeNum(), frame);
			if (shape == null || (check_empty && shape.isEmpty()))
				return;
								// (Don't mess up rotated frames.)
			if ((frame&0xf) < item.getNumFrames())
				item.changeFrame(frame);
		}
		gwin.setPainted();		// Make sure paint gets done.
	}
	private final UsecodeValue getObjectPosition(UsecodeValue p0) {
		// Takes itemref.  ?Think it rets.
		//  hotspot coords: (x, y, z).
		GameObject obj = getItem(p0);
		Tile c = tempTile;
		if (obj != null)		// (Watch for animated objs' wiggles.)
			obj.getOutermost().getOriginalTileCoord(c);
		else
			c.set(0,0,0);
		UsecodeValue vx = new UsecodeValue.IntValue(c.tx), 
					 vy = new UsecodeValue.IntValue(c.ty), 
					 vz = new UsecodeValue.IntValue(c.tz);
		UsecodeValue arr = new UsecodeValue.ArrayValue(vx, vy, vz);
		return(arr);
	}
	private final GameObject createObject(int shapenum, boolean equip) {
		GameObject obj = null;		// Create to be written to Ireg.
		ShapeInfo info = ShapeID.getInfo(shapenum);
		ucmachine.setModifiedMap();
		/* +++++++++FINISH
						// +++Not sure if 1st test is needed.
		if (info.get_monster_info() || info.isNpc()) {
						// (Wait sched. added for FOV.)
			// don't add equipment (Erethian's transform sequence)
			/* +++++FINISH
			Monster_actor *monster = Monster_actor::create(shapenum,
				Tile_coord(-1, -1, -1), Schedule::wait, 
						(int) Actor::neutral, true, equip);
						// FORCE it to be neutral (dec04,01).
			monster.set_alignment((int) Actor::neutral);
			gwin.add_dirty(monster);
			gwin.add_nearby_npc(monster);
			gwin.show();
			last_created.push_back(monster);
			return monster;
		} else */ {
			/* +++++++++++
			if (info.isBodyShape())
				obj = new Dead_body(shapenum, 0, 0, 0, 0, -1);
			else */ {
				obj = IregGameObject.create(ShapeID.getInfo(shapenum), shapenum, 0);
						// Be liberal about taking stuff.
				obj.setFlag(GameObject.okay_to_take);
			}
		}
		obj.setInvalid();		// Not in world yet.
		obj.setFlag(GameObject.okay_to_take);
		last_created.addLast(obj);
		return obj;
	}
	private final UsecodeValue createNewObject(UsecodeValue p0) {
		// create_new_object(shapenum).   Stores it in 'last_created'.
		int shapenum = p0.getIntValue();
		GameObject obj = createObject(shapenum, false);
		return new UsecodeValue.ObjectValue(obj);
	}
	private final UsecodeValue setLastCreated(UsecodeValue p0) {
		// Take itemref off map and set last_created to it.
		GameObject obj = getItem(p0);
		// Don't do it for same object if already there.
		/*
		for (vector<Game_object*>::const_iterator it = last_created.begin();
					it != last_created.end(); ++it)
			if (*it == obj)
				return Usecode_value(0);
		*/
		
		ucmachine.setModifiedMap();
		if (obj != null) {
			gwin.addDirty(obj);		// Set to repaint area.
			last_created.add(obj);
			obj.removeThis();	// Remove.
			}
		UsecodeValue u = new UsecodeValue.ObjectValue(obj);
		return(u);
	}
	private final UsecodeValue updateLastCreated(UsecodeValue p0) {
		/*++++TESTING
		System.out.println("Calling getClick");
		Point pt = new Point();
		ExultActivity.getClick(pt);
		System.out.println("Got x,y = " + pt.x + "," + pt.y);
		*/
		// Think it takes array from 0x18,
		//   updates last-created object.
		//   ??guessing??
		ucmachine.setModifiedMap();
		if (last_created.isEmpty()) {
			return UsecodeValue.getNullObj();
		}
		GameObject obj = last_created.removeLast();
		obj.setInvalid();		// It's already been removed.
		UsecodeValue arr = p0;
		int sz = arr.getArraySize();
		if (sz >= 2) {
			//arr is loc (x, y, z, map) if sz == 4,
			//(x, y, z) for sz == 3 and (x, y) for sz == 2
			
			int tx = arr.getElem(0).getIntValue(),
				ty = arr.getElem(1).getIntValue(),
				tz = sz >= 3 ? arr.getElem(2).getIntValue() : 0;
			obj.move(tx, ty, tz, sz < 4 ? -1 :
				  arr.getElem(3).getIntValue());
			if (/* ++++++ FINISH GAME_BG */ true) {
				return new UsecodeValue.IntValue(1);
			} else {
				return new UsecodeValue.ObjectValue(obj);
			}
						// Taking a guess here:
		} else if (sz == 1) {
			obj.removeThis();
		}
		return new UsecodeValue.IntValue(1);
	}
	private final UsecodeValue findNearby(UsecodeValue objVal, UsecodeValue shapeVal,
						UsecodeValue distVal, UsecodeValue maskVal) {
		int mval = maskVal.getIntValue();// Some kind of mask?  Guessing:
										//   4 == party members only.
										//   8 == non-party NPC's only.
										//  16 == something with eggs???
										//  32 == monsters? invisible?
		int shapenum;
		
		if (shapeVal.isArray()) {
			// fixes 'lightning whip sacrifice' in Silver Seed
			shapenum = shapeVal.getElem(0).getIntValue();
			if (shapeVal.getArraySize() > 1)
				System.out.println("Calling find_nearby with an array > 1 !!!!");
		} else
			shapenum = shapeVal.getIntValue();

			
						// It might be (tx, ty, tz).
		int arraysize = objVal.getArraySize();
		foundVec.clear();
		if (arraysize == 4) {		// Passed result of click_on_item.
			tempTile.set(objVal.getElem(1).getIntValue(),
						 objVal.getElem(2).getIntValue(),
						 objVal.getElem(3).getIntValue());
			
			gmap.findNearby(foundVec, tempTile, shapenum,
				distVal.getIntValue(), mval);
		} else if (arraysize == 3 || arraysize == 5) {
			// Coords(x,y,z) [qual, frame]. Qual is 4th if there.
			int qual = arraysize == 5 ? objVal.getElem(3).getIntValue()
								: EConst.c_any_qual;
						// Frame is 5th if there.
			int frnum = arraysize == 5 ? objVal.getElem(4).getIntValue()
								: EConst.c_any_framenum;
			tempTile.set(objVal.getElem(0).getIntValue(),
						 objVal.getElem(1).getIntValue(),
						 objVal.getElem(2).getIntValue());
			gmap.findNearby(foundVec, tempTile, shapenum,
				distVal.getIntValue(), mval, qual, frnum);
		} else {
			GameObject obj = getItem(objVal);
			if (obj == null)
				return UsecodeValue.getZero();	// +++Exult rets Usecode_value(0,0).
			obj = obj.getOutermost();	// Might be inside something.
			obj.getTile(tempTile);
			gmap.findNearby(foundVec, tempTile, shapenum, distVal.getIntValue(), mval);
		}
		/* +++++++FINISH 
		if (foundVec.size() > 1)		// Sort right-left, near-far to fix
						//   SI/SS cask bug.
			std::sort(vec.begin(), vec.end(), Object_reverse_sorter());
		*/
		UsecodeValue nearby = UsecodeValue.ArrayValue.createObjectsList(foundVec);
		return (nearby);
	}
	private final void removeItem(GameObject obj) {
		if (obj != null) {
			if (!last_created.isEmpty() && obj == last_created.getLast())
				last_created.removeLast();
			gwin.addDirty(obj);
			obj.removeThis();
		}
	}
	private final void removeItem(UsecodeValue p0) {
		removeItem(getItem(p0));
		ucmachine.setModifiedMap();
	}
	
	
	//	For BlackGate.
	public UsecodeValue execute(int id, int event, int num_parms, UsecodeValue parms[]) {
		switch (id) {
		case 0x00:
			return getRandom(parms[0]);
		case 0x03:
			showNpcFace(parms[0], parms[1], -1); break;
		case 0x04:
			removeNpcFace(parms[0]); break;
		case 0x05:
			addAnswer(parms[0]); break;
		case 0x06:
			removeAnswer(parms[0]); break;
		case 0x07:
			pushAnswers(); break;
		case 0x08:
			popAnswers(); break;
		case 0x09:
			clearAnswers(); break;
		case 0x0d:
			setItemShape(parms[0], parms[1]); break;
		case 0x0e:
			return findNearest(parms[0], parms[1], parms[2]);
		case 0x10:
			return dieRoll(parms[0], parms[1]);
		case 0x11:
			return getItemShape(parms[0]);
		case 0x12:
			return getItemFrame(parms[0]);
		case 0x13:
			setItemFrame(parms[0], parms[1]); break;
		case 0x18:
			return getObjectPosition(parms[0]);
		case 0x24:
			return createNewObject(parms[0]);
		case 0x25:
			return setLastCreated(parms[0]);
		case 0x26:
			return updateLastCreated(parms[0]);
		case 0x35:
			return findNearby(parms[0], parms[1], parms[2], parms[3]);
		case 0x6f:
			removeItem(parms[0]); break;
		default:
			System.out.println("*** UNHANDLED intrinsic # " + id);
			break;
		}
		return UsecodeValue.getZero();
	}
}
