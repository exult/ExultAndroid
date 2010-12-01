package com.exult.android;
import java.util.Vector;
import java.util.LinkedList;

public class UsecodeIntrinsics extends GameSingletons {
	private static Tile tempTile = new Tile();
	// Stack of last items created with intrins. x24.
	private LinkedList<GameObject> last_created = new LinkedList<GameObject>();
	
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
	
	//	For BlackGate.
	public UsecodeValue execute(int id, int event, int num_parms, UsecodeValue parms[]) {
		switch (id) {
		case 0x00:
			return getRandom(parms[0]);
		case 0x0d:
			setItemShape(parms[0], parms[1]); break;
		case 0x11:
			return getItemShape(parms[0]);
		case 0x12:
			return getItemFrame(parms[0]);
		case 0x13:
			setItemFrame(parms[0], parms[1]); break;
		case 0x18:
			return getObjectPosition(parms[0]);
		case 0x25:
			return setLastCreated(parms[0]);
		case 0x26:
			return updateLastCreated(parms[0]);
		default:
			System.out.println("*** UNHANDLED intrinsic # " + id);
			break;
		}
		return UsecodeValue.getZero();
	}
}
