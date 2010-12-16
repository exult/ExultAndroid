package com.exult.android;
import android.graphics.Point;

public final class DraggingInfo extends GameSingletons {
	private GameObject obj;		// What's being dragged.
	private boolean is_new;			// Object was newly created.
	private Gump gump;
	private GumpWidget.Button button;
	private Tile old_pos;		// Original pos. of object if it wasn't
						//   in a container.
	private Rectangle old_foot;		// Original footprint.
	private int old_lift;			// Lift of obj OR its owner.
	private int quantity;			// Amount of object being moved.
	private int readied_index;		// If it was a 'readied' item.
					// Last mouse, paint positions:
	private int mousex, mousey;
	private Point paint;
	//+++++Mouse::Mouse_shapes mouse_shape;// Save starting mouse shape.
	private Rectangle rect;			// Rectangle to repaint.
	private Rectangle paintRect;
	private boolean possible_theft;		// Moved enough to be 'theft'.
	
	private boolean start(int x, int y) { // First motion.
		int deltax = Math.abs(x - mousex),
		deltay = Math.abs(y - mousey);
		if (deltax <= 2 && deltay <= 2)
			return (false);		// Wait for greater motion.
		if (obj != null) {			// Don't want to move walls.
			/* ++++++FINISH
			if (!cheat.in_hack_mover() && !obj.is_dragable() &&
					      !obj.getOwner()) {
				Mouse::mouse.flash_shape(Mouse::tooheavy);
				obj = 0;
				gump = 0;
				return (false);
			}
			*/
			GameObject owner = obj.getOutermost();
			if (owner == obj) {
				/* ++++++++
		    	if (!cheat.in_hack_mover() && 
				!Fast_pathfinder_client::is_grabable(
				   gwin.getMainActor(), obj)) {
		    		Mouse::mouse.flash_shape(Mouse::blocked);
		    		obj = null;
		    		return (false);
				}
				*/
			}
		}
		//+++++++++ Mouse::mouse.set_shape(Mouse::hand);
					// Store original pos. on screen.
		rect = new Rectangle();
		if (gump != null) {
			if (obj != null) {
				gump.getShapeRect(rect, obj);
				ContainerGameObject owner = gump.getContOrActor(x,y);
				// Get the object
				GameObject owner_obj = 
					gump.getContainer().getOutermost(); 
				Actor main_actor = gwin.getMainActor();
				// Check the range
				/* ++++++FINISH
				if (!cheat.in_hack_mover() &&
						!Fast_pathfinder_client::is_grabable(main_actor, owner_obj)) {
					obj = 0;
					gump = 0;
					Mouse::mouse.flash_shape(Mouse::outofrange);
					return false;
				}
				if (owner != null)
					readied_index = owner.findReadied(obj);
				*/
				gump.remove(obj);
			} else {
				gump.getDirty(rect);
				gumpman.removeGump(gump);
			}
		} else {
			gwin.getShapeRect(rect, obj);
			obj.removeThis();
		}
					// Make a little bigger.
		rect.enlarge(deltax > deltay ? deltax : deltay);
		paintRect = new Rectangle(rect);
		gwin.clipToWin(paintRect);
		gwin.paint(paintRect);		// Paint over obj's. area.
		return true;
	}
	private void putBack() {	// Put back object.
		if (gump != null) {			// Put back remaining/orig. piece.
					// And don't check for volume!
			// Restore saved vals.
			obj.setShapePos(old_pos.tx, old_pos.ty);
			// 1st try with dont_check==false so usecode gets called.
			if (!gump.add(obj, -2, -2, -2, -2, false, false))
				gump.add(obj, -2, -2, -2, -2, true, false);
		} else if (is_new) {
			obj.setInvalid();	// It's not in the world.
			obj.removeThis();
		} else				// Normal object.  Put it back.
			obj.move(old_pos);
		obj = null;			// Just to be safe.
		is_new = false;
	}
	/*
	 *	Drop object on a gump.
	 *
	 *	Output:	False if not (all) of object was dropped.
	 */
	private boolean dropOnGump(int x, int y, GameObject to_drop, Gump on_gump) {
		GameObject owner_obj = on_gump.getContainer();
		if (owner_obj != null) 
			owner_obj = owner_obj.getOutermost();
		Actor main_actor = gwin.getMainActor(); 
		// Check the range
		/* +++++++FINISH
		if (owner_obj && !cheat.in_hack_mover() &&
			!Fast_pathfinder_client::is_grabable(main_actor, owner_obj))
			{	  		// Object was not grabable
			Mouse::mouse.flash_shape(Mouse::outofrange);
			return false;
		}
		*/
		if (!checkWeight(to_drop, on_gump.getContOrActor(x,y)))
			return false;
		if (on_gump != gump)		// Not moving within same gump?
			possible_theft = true;
						// Add, and allow to combine.
		if (!on_gump.add(to_drop, x, y, paint.x, paint.y, false, true))
			{			// Failed.
			if (to_drop != obj)
				{		// Watch for partial drop.
				int nq = to_drop.getQuantity();
				if (nq < quantity)
					obj.modifyQuantity(quantity - nq);
				}
			//+++++++++ Mouse::mouse.flash_shape(Mouse::wontfit);
			return false;
			}
		return true;
	}
	/*
	 *	See if there's something blocking an object at a given point.
	 */
	private static boolean isInaccessible
		(
		GameObject obj,
		int x, int y
		) {
		GameObject block = gwin.findObject(x, y);
		if (block != null && block != obj && !block.isDragable()) {
			System.out.println("isInaccessible: returning TRUE");
			return true;
		} else 
			return false;
	}
	/*
	 *	Check weight.
	 *
	 *	Output:	false if too heavy, with mouse flashed.
	 */
	private static boolean checkWeight
		(
		GameObject to_drop,
		GameObject owner		// Who the new owner will be.
		) {
		/* ++++++++++++++
		if (cheat.in_hack_mover())	// hack-mover  . no weight checking
			return true;
		*/
		if (owner == null)
			return true;
		owner = owner.getOutermost();
		if (!owner.getFlag(GameObject.in_party))
			return true;		// Not a party member, so okay.
		int wt = owner.getWeight() + to_drop.getWeight();
		if (wt/10 > owner.getMaxWeight()) {
			// +++++ Mouse::mouse.flash_shape(Mouse::tooheavy);
			return false;
		}
		return true;
	}
	/*
	 *	Drop object onto the map.
	 *
	 *	Output:	False if not (all) of object was dropped.
	 */

	private boolean dropOnMap(int x, int y, GameObject to_drop) {
		// Attempting to drop off screen?
		if (x < 0 || y < 0 || x >= gwin.getWidth() || y >= gwin.getHeight()) {
			/* ++++++++++
			Mouse::mouse.flash_shape(Mouse::redx);
			Audio::get_ptr().play_sound_effect(Audio::game_sfx(76));
			*/
			return false;
		}
		int max_lift = /* +++++++ cheat.in_hack_mover() ? 255 : */
						gwin.getMainActor().getLift() + 5;
		int skip = gwin.getRenderSkipLift();
		if (max_lift >= skip)		// Don't drop where we cannot see.
			max_lift = skip - 1;
						// Drop where we last painted it.
		int posx = paint.x, posy = paint.y;
		if (posx == -1000)		// Unless we never painted.
			{ posx = x; posy = y; }
		int lift;
						// Was it dropped on something?
		GameObject found = gwin.findObject(x, y);
		int dropped = 0;		// 1 when dropped.
		if (found != null && found != obj) {
			if (!checkWeight(to_drop, found))
				return false;
			/* +++++++++FINISH
			if (found.drop(to_drop)) {
				dropped = 1;
				possible_theft = true;
			} else */ if ((lift = found.getLift() +
				     found.getInfo().get3dHeight()) <= max_lift)
				// Try to place on 'found'.
				dropped = dropAtLift(to_drop,posx, posy, lift);
			else {		// Too high.
				/*+++++++++
				Mouse::mouse.flash_shape(Mouse::redx);
				Audio::get_ptr().play_sound_effect(
								Audio::game_sfx(76));
				*/
				return false;
			}
		}
						// Find where to drop it, but stop if
						//   it will end up hidden (-1).
		for (lift = old_lift; dropped == 0 && lift <= max_lift; lift++)
			dropped = dropAtLift(to_drop, posx, posy, lift);
		System.out.println("Dropping " + to_drop.getShapeNum() +
				" with old_lift = " + old_lift);
		if (dropped <= 0) {
			/* +++++++++++
			Mouse::mouse.flash_shape(Mouse::blocked);
			Audio::get_ptr().play_sound_effect(Audio::game_sfx(76));
			*/
			System.out.println("Could not find spot to drop " + 
					to_drop.getShapeNum());
			return false;
		}
		System.out.println("New pos is " + to_drop.getTileX() +
				", " + to_drop.getTileY() + ", " + to_drop.getLift());
						// Moved more than 2 tiles.
		if (gump == null && !possible_theft) {
			Tile t = new Tile();
			to_drop.getTile(t);
			if (t.distance(old_pos) > 2)
				possible_theft = true;
		}
		return true;
	}	
	public static boolean startDragging(int x, int y) {
		drag = new DraggingInfo();
		if (drag.init(x, y))
			return true;
		drag = null;
		return false;
	}
	public DraggingInfo() {
		old_lift = -1;
		readied_index = -1;
	}
	public boolean init(int x, int y) {		
		mousex = x; mousey = y;
		// First see if it's a gump.
		gump = gumpman.findGump(x, y);
		if (gump != null) {
			obj = gump.findObject(x, y);
			if (obj != null) {		// Save location info.
				paint = new Point();
				gump.getShapeLocation(paint, obj);
				old_pos = new Tile(obj.getTx(), obj.getTy(), 0);
			} else if ((button = gump.onButton(x, y)) != null) {
				gump = null;
				if (!button.isDraggable())
					return false;
				button.push(true);
						// Pushed button, so make noise.
				/*++++++++++
				Audio::get_ptr().play_sound_effect(
						Audio::game_sfx(73));
				*/
				gwin.setPainted();
			} else if (gump.isDraggable()) {	// Dragging whole gump.
				paint = new Point(gump.getX(), gump.getY());
			} else 			// the gump isn't draggable
				return false;
		} else if (x >0 && y > 0 && 
				   x < gwin.getWidth() && y < gwin.getHeight()) {
			obj = gwin.findObject(x, y);
			if (obj == null)
				return false;
						// Get coord. where painted.
			gwin.getShapeLocation(paint = new Point(), obj);
			old_pos = new Tile();
			obj.getTile(old_pos);
			old_foot = new Rectangle();
			obj.getFootprint(old_foot);
		}
		if (obj != null) {
			quantity = obj.getQuantity();
						// Save original lift.
			old_lift = obj.getOutermost().getLift();
		}
		return true;
	}
	/*
	 *	Mouse was moved while dragging.
	 *
	 *	Output:	true iff movement started/continued.
	 */
	public boolean moved(int x, int y) {	// Mouse moved.
		if (obj == null && gump == null)
			return (false);
		if (rect == null) {
			if (!start(x, y))
				return false;
		} else {
			gwin.clipToWin(rect);
			gwin.addDirty(rect);
		}
		gwin.setPainted();
		int deltax = x - mousex, deltay = y - mousey;
		mousex = x;
		mousey = y;
						// Shift to new position.
		rect.shift(deltax, deltay);
		paint.x += deltax;
		paint.y += deltay;
		if (gump != null && obj == null)			// Dragging a gump?
			gump.setPos(paint.x, paint.y);
		gwin.clipToWin(rect);
		gwin.addDirty(rect);
		return true;
	}
	public void paint() {			// Paint object being dragged.
		if (rect == null)			// Not moved enough yet?
			return;
		if (obj != null) {
			/*++++++++++
			if (obj.getFlag(GameObject.invisible))
				obj.paintInvisible(paint.x, paint.y);
			else */
				obj.paintShape(paint.x, paint.y);
		} else if (gump != null) {
			gump.paint();
		}
	}
	/*
	 *	Mouse was released, so drop object. 
	 *      Return true iff the dropping mouseclick has been handled. 
	 *		(by buttonpress, drag)
	 */
	public boolean drop(int x, int y) {	// Drop obj. at given position.
		// Get orig. loc. info.
		int oldcx = old_pos.tx/EConst.c_tiles_per_chunk, 
		    oldcy = old_pos.ty/EConst.c_tiles_per_chunk;
		GameObject to_drop = obj;	// If quantity, split it off.
		ShapeInfo info = obj.getInfo();
						// Being liberal about taking stuff:
		boolean okay_to_move = to_drop.getFlag(GameObject.okay_to_take);
		int old_top = old_pos.tz + info.get3dHeight();
						// First see if it's a gump.
		Gump on_gump = gumpman.findGump(x, y);
						// Don't prompt if within same gump.
		/* ++++++++FINISH
		if (quantity > 1 && (on_gump == null || on_gump != gump))
			quantity = gumpman.prompt_for_number(0, quantity, 1, quantity);
		*/
		if (quantity <= 0)
			return false;
		if (quantity < obj.getQuantity()) {
						// Need to drop a copy.
			to_drop = IregGameObject.create(info,
					obj.getShapeNum(), obj.getFrameNum());
			to_drop.modifyQuantity(quantity - 1);
			if (okay_to_move)	// Make sure copy is okay to take.
				to_drop.setFlag(GameObject.okay_to_take);
		}
						// Drop it.
		if (!(on_gump != null ? dropOnGump(x, y, to_drop, on_gump)
				  : dropOnMap(x, y, to_drop)))
			return false;
						// Make a 'dropped' sound.
		// ++++++ Audio::get_ptr().play_sound_effect(Audio::game_sfx(74));
		/* ++++++++FINISH
		if (gump == null)			// Do eggs where it came from.
			gmap.getChunk(oldcx, oldcy).activateEggs(obj,
				    old_pos.tx, old_pos.ty, old_pos.tz, 
						old_pos.tx, old_pos.ty);
						// Special:  BlackSword in SI.
		else if (readied_index >= 0 && obj.getShapeNum() == 806)
						// Do 'unreadied' usecode.
			gump.getContOrActor(x,y).callReadiedUsecode(
				readied_index, obj, UsecodeMachine.unreadied);
						// On a barge?
		Barge_object *barge = gwin.get_moving_barge();
		if (barge)
			barge.set_to_gather();	// Refigure what's on barge.
						// Check for theft.
		if (!okay_to_move && !cheat.in_hack_mover() && possible_theft &&
		    !gwin.is_in_dungeon())
			gwin.theft();			
		*/
		if (to_drop == obj) {		// Whole thing?
						// Watch for stuff on top of it.
			/* ++++++FINISH
			if (old_foot.w > 0)
				MapChunk.gravity(old_foot, old_top);
			*/
			return true;		// All done.
		}
						// Subtract quantity moved.
		obj.modifyQuantity(-quantity);
		return false;			// Put back the rest.
	}
	/*
	 *	Mouse was released, so drop object. 
	 *      Return true iff the dropping mouseclick has been handled. 
	 *		(by buttonpress, drag)
	 */
	public boolean drop(int x, int y, boolean moved) {
		boolean handled = moved;
		// ++++++++++ Mouse::mouse.set_shape(mouse_shape);
		if (button != null) {
			button.unpush(true);
			if (button.onWidget(x, y))
						// Clicked on button.
				button.activate(true);
			handled = true;
		} else if (obj == null) {		// Only dragging a gump?
			if (gump == null)
				return handled;
			if (!moved)		// A click just raises it to the top.
				gumpman.removeGump(gump);
			gumpman.addGump(gump);
		}
		else if (!moved)		// For now, if not moved, leave it.
			return handled;
		else if (!drop(x, y))		// Drop it.
			putBack();		// Wasn't (all) moved.
		obj = null;			// Clear so we don't paint them.
		gump = null;
		gwin.paint();
		return handled;
	}
	/*
	 *	Try to drop at a given lift.  Note:  None of the drag state variables
	 *	may be used here, as it's also called from the outside.
	 *
	 *	Output:	1 if successful.
	 *		0 if blocked
	 *		-1 if it would end up hidden by a non-moveable object.
	 */

	public static int dropAtLift
		(
		GameObject to_drop,
		int x, int y,			// Pixel coord. in window.
		int at_lift
		)
		{
		x += at_lift*4 - 1;		// Take lift into account, round.
		y += at_lift*4 - 1;
		int tx = (gwin.getScrolltx() + x/EConst.c_tilesize)%EConst.c_num_tiles;
		int ty = (gwin.getScrollty() + y/EConst.c_tilesize)%EConst.c_num_tiles;
		int cx = tx/EConst.c_tiles_per_chunk;
		int cy = ty/EConst.c_tiles_per_chunk;
		MapChunk chunk = gmap.getChunk(cx, cy);
		int lift = 0;			// Can we put it here?
		ShapeInfo info = to_drop.getInfo();
		int frame = to_drop.getFrameNum();
		int xtiles = info.get3dXtiles(frame), ytiles = info.get3dYtiles(frame);
		int max_drop, move_flags;
		/* +++++++++++++++
		if (cheat.in_hack_mover())
			{
			max_drop = at_lift - cheat.get_edit_lift();
//			max_drop = max_drop < 0 ? 0 : max_drop;
			if (max_drop < 0)	// Below lift we're editing?
				return 0;
			move_flags = MOVE_WALK|MOVE_MAPEDIT;
			}
		else */ {			// Allow drop of 5;
			max_drop = 5;
			move_flags = EConst.MOVE_WALK;
		}
		Tile loc = new Tile(tx - xtiles + 1, ty - ytiles + 1, at_lift);
		if (!MapChunk.areaAvailable(xtiles, ytiles, info.get3dHeight(),
							loc, move_flags, max_drop, -1) /*++++ ||
		      (!cheat.in_hack_mover() &&
						// Check for path to location.
		    !Fast_pathfinder_client::is_grabable(main_actor, 
				Tile_coord(tx, ty, lift)))*/)
			return 0;
		System.out.println("Past areaAvailable check.");
		lift = loc.tz;
		to_drop.setInvalid();
		to_drop.move(tx, ty, lift);
		Rectangle rect = new Rectangle();
		gwin.getShapeRect(rect, to_drop);
						// Avoid dropping behind walls.
		if (isInaccessible(to_drop, rect.x + 2, rect.y + 2) &&
		    isInaccessible(to_drop, 
					rect.x + rect.w - 3, rect.y + 2) &&
		    isInaccessible(to_drop, 
					rect.x + 2, rect.y + rect.h - 3) &&
		    isInaccessible(to_drop, 
					rect.x + rect.w - 3, rect.y + rect.h - 3) &&
			isInaccessible(to_drop, 
					rect.x + (rect.w >> 1), rect.y + (rect.h >> 1))) {
			to_drop.removeThis();
			System.out.println("dropAtLift returning -1");
			return -1;
		}
						// On an egg?
		//++++++ FINISH chunk.activate_eggs(to_drop, tx, ty, lift, tx, ty);
		if (to_drop == gwin.getMainActor()) {
			gwin.centerView(to_drop.getTileX(), to_drop.getTileY());
			gwin.paint();
		}
		return (1);
	}

}
