package com.exult.android;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;

public class ContainerGameObject extends IregGameObject {
	private int volumeUsed;		// Amount of volume occupied.
	private byte resistance;	// Resistance to attack.
	protected ObjectList objects;
	
	public ContainerGameObject(int shapenum, int framenum, int tilex, 
			int tiley, int lft,	int res) {
		super(shapenum, framenum, tilex, tiley, lft);
		resistance = (byte)res;
		objects = new ObjectList();
	}
	public void remove(GameObject obj) {
		if (objects.isEmpty())
			return;
		volumeUsed -= obj.getVolume();
		obj.setOwner(null);
		objects.remove(obj);
		obj.setInvalid();		// No longer part of world.
	}
	/*
	 *	Add an object.
	 *
	 *	Output:	1, meaning object is completely contained in this.  Obj may
	 *			be deleted in this case if combine==true.
	 *		0 if not enough space, although obj's quantity may be
	 *			reduced if combine==true.
	 */
	public boolean add(GameObject obj, boolean dont_check,
			boolean combine, boolean noset) {
		// Prevent dragging the avatar into a container.
		if (obj == gwin.getMainActor())
			return false;
		ShapeInfo info = getInfo();
		if (getShapeNum() == obj.getShapeNum()	// Shape can't be inside itself.
				|| (!dont_check && info.isContainerLocked()))	// Locked container.
			return false;
		/*++++++++++++
		if (!info.isShapeAccepted(obj.getShapeNum()))	// Shape can't be inside.
			return false;
		*/
		// Always check this. ALWAYS!
		GameObject parent = this;
		do				// Watch for snake eating itself.
			if (obj == parent)
				return false;
		while ((parent = parent.getOwner()) != null);

		if (combine) {			// Should we try to combine?
			ShapeInfo oinfo = obj.getInfo();
			int quant = obj.getQuantity();
						// Combine, but don't add.
			int newquant = addQuantity(quant, obj.getShapeNum(),
				oinfo.hasQuality() ? obj.getQuality() : EConst.c_any_qual,
							obj.getFrameNum(), true);
			if (newquant == 0) {	// All added?
				obj.removeThis();
				return true;
			} else if (newquant < quant)	// Partly successful.
				obj.modifyQuantity(newquant - quant);
			}
		int objvol = obj.getVolume();
		/* +++++++++++FINISH ?
		if (!cheat.in_hack_mover() && !dont_check)
			{
			int maxvol = getMaxVolume();
			// maxvol = 0 means infinite (ship's hold, hollow tree, etc...)
			if (maxvol > 0 && objvol + volumeUsed > maxvol)
				return false;	// Doesn't fit.
			}
		*/
		volumeUsed += objvol;
		obj.setOwner(this);		// Set us as the owner.
		objects.append(obj);		// Append to chain.
						// Guessing:
		if (getFlag(GameObject.okay_to_take))
			obj.setFlag(GameObject.okay_to_take);
		return true;
	}
	public void changeMemberShape(GameObject obj, int newshape) {
		int oldvol = obj.getVolume();
		obj.setShape(newshape);
						// Update total volume.
		volumeUsed += obj.getVolume() - oldvol;
	}
	private boolean canBeAdded(int shapenum) {
		return true; /* ++++++++FINISH
		ShapeInfo info = getInfo();
		return !(getShapeNum() == shapenum	// Shape can't be inside itself.
				|| info.isContainerLocked()	// Locked container.
				|| !info.isShapeAccepted(shapenum)); */	// Shape can't be inside.
	}
	/*
	 *	Recursively add a quantity of an item to those existing in
	 *	this container, and create new objects if necessary.
	 *
	 *	Output:	Delta decremented # added.
	 */
	public int addQuantity
		(
		int delta,			// Quantity to add.
		int shapenum,			// Shape #.
		int qual,			// Quality, or EConst.c_any_qual for any.
		int framenum,			// Frame, or EConst.c_any_framenum for any.
		boolean dontcreate			// If 1, don't create new objs.
		) {
		if (delta <= 0 || !canBeAdded(shapenum))
			return delta;

		int cant_add = 0;		// # we can't add due to weight.
		int maxweight = getMaxWeight();// Check weight.
		if (maxweight != 0) {	
			maxweight *= 10;	// Work in .1 stones.
			int avail = maxweight - getOutermost().getWeight();
			int objweight = super.getWeight(shapenum, delta);
			if (objweight != 0 && objweight > avail) {
						// Limit what we can add.
						// Work in 1/100ths.
				int weight1 = (10*objweight)/delta;
				cant_add = delta - (10*avail)/(weight1 != 0 ? weight1 : 1);
				if (cant_add >= delta)
					return delta;	// Can't add any.
				delta -= cant_add;
			}
		}
		ShapeInfo info = ShapeID.getInfo(shapenum);
		boolean hasQuantity = info.hasQuantity();	// Quantity-type shape?
		boolean hasQuantity_frame = hasQuantity ? 
				info.hasQuantityFrames() : false;
						// Note:  quantity is ignored for
						//   figuring volume.
		GameObject obj;
		if (!objects.isEmpty()) {
						// First try existing items.
			ObjectList.ObjectIterator next = objects.getIterator();
			while (delta != 0 && (obj = next.next()) != null) {
				if (hasQuantity && obj.getShapeNum() == shapenum &&
			    	 (framenum == EConst.c_any_framenum || hasQuantity_frame ||
						obj.getFrameNum() == framenum))

					delta = obj.modifyQuantity(delta);
				/* +++++++++++++
						// Adding key to SI keyring?
				else if (GAME_SI && shapenum == 641 &&
					 obj.getShapeNum() == 485 && delta == 1)
					delta -= Add2keyring(qual, framenum);
				*/
			}
			next.reset();			// Now try recursively.
			while ((obj = next.next()) != null)
				delta = obj.addQuantity(
						delta, shapenum, qual, framenum, true);
		}
		if (delta == 0 || dontcreate)	// All added?
			return (delta + cant_add);
		else
			return cant_add + createQuantity(delta, shapenum, qual,
					framenum == EConst.c_any_framenum ? 0 : framenum, false);
	}

	/*
	 *	Recursively create a quantity of an item.  Assumes weight check has
	 *	already been done.
	 *
	 *	Output:	Delta decremented # added.
	 */

	public int createQuantity
		(
		int delta,			// Quantity to add.
		int shnum,			// Shape #.
		int qual,			// Quality, or EConst.c_any_qual for any.
		int frnum,			// Frame.
		boolean temporary			// Create temporary quantity
		) {
		if (!canBeAdded(shnum))
			return delta;
				// Usecode container?
		ShapeInfo info = ShapeID.getInfo(getShapeNum());
		/*+++++++++FINISH
		if (info.getReadyType() == ucont)
			return delta;
		*/
		ShapeInfo shp_info = ShapeID.getInfo(shnum);
		if (!shp_info.hasQuality())	// Not a quality object?
			qual = EConst.c_any_qual;	// Then don't set it.
		while (delta != 0) {			// Create them here first.
			GameObject newobj = IregGameObject.create(
							shp_info, shnum, frnum,0,0,0);
			if (!add(newobj, false))
				break;
			// Set temporary
			if (temporary) 
				newobj.setFlag (GameObject.is_temporary);
			if (qual != EConst.c_any_qual)	// Set desired quality.
				newobj.setQuality(qual);
			delta--;
			if (delta > 0)
				delta =  newobj.modifyQuantity(delta);
		}
		if (delta == 0)			// All done?
			return (0);
						// Now try those below.
		GameObject obj;
		if (objects.isEmpty())
			return (delta);
		ObjectList.ObjectIterator next = objects.getIterator();
		while ((obj = next.next()) != null)
			delta = obj.createQuantity(delta, shnum, qual, frnum, false);
		return (delta);
	}		

	/*
	 *	Recursively remove a quantity of an item from those existing in
	 *	this container.
	 *
	 *	Output:	Delta decremented by # removed.
	 */

	public int remove_quantity
		(
		int delta,			// Quantity to remove.
		int shapenum,			// Shape #.
		int qual,			// Quality, or EConst.c_any_qual for any.
		int framenum			// Frame, or EConst.c_any_framenum for any.
		)
		{
		if (objects.isEmpty() || !canBeAdded(shapenum))
			return delta;		// Empty.
		GameObject obj = objects.getFirst();
		GameObject last = obj.getPrev();	// Save last.
		GameObject next;
		while (obj != null && delta != 0) {
						// Might be deleting obj.
			next = obj == last ? null : obj.getNext();
			boolean del = false;	// Gets 'deleted' flag.
			if (obj.getShapeNum() == shapenum &&
			    (qual == EConst.c_any_qual || obj.getQuality() == qual) &&
			    (framenum == EConst.c_any_framenum || 
					(obj.getFrameNum()&31) == framenum))
				delta = -obj.modifyQuantity(-delta);

			if (!del)		// Still there?
						// Do it recursively.
				delta = obj.removeQuantity(delta, shapenum, 
								qual, framenum);
			obj = next;
		}
		return (delta);
	}
	/*
	 *	Find and return a desired item.
	 *
	 *	Output:	.object if found, else 0.
	 */
	public GameObject findItem
		(
		int shapenum,			// Shape #.
		int qual,			// Quality, or EConst.c_any_qual for any.
		int framenum			// Frame, or EConst.c_any_framenum for any.
		)
		{
		if (objects.isEmpty() || !canBeAdded(shapenum))
			return null;		// Empty.
		GameObject obj;
		ObjectList.ObjectIterator next = objects.getIterator();
		while ((obj = next.next()) != null) {
			if (obj.getShapeNum() == shapenum &&
			    (framenum == EConst.c_any_framenum || 
					(obj.getFrameNum()&31) == framenum) &&
			    (qual == EConst.c_any_qual || obj.getQuality() == qual))
				return (obj);

						// Do it recursively.
			GameObject found = obj.findItem(shapenum, qual, framenum);
			if (found != null)
				return (found);
		}
		return null;
	}
	/*
	 *	Displays the object's gump.
	 *	Returns true if the gump has been handled.
	 */
	public final boolean showGump(int event) {
		ShapeInfo inf = getInfo();
		int gump;
		/* ++++++++FINISH
		if (inf.has_object_flag(getFrameNum(),
				inf.hasQuality() ? getQuality() : -1, Frame_flags.force_usecode))
				// Run normal usecode fun.
			return false;
		else if ((gump = inf.getGumpShape()) >= 0)
			{
			Gump_manager *gump_man = gumpman;
			gump_man.add_gump(this, gump);
			return true;
			}
		*/
		return false;
	}
	/*
	 *	Run usecode when double-clicked.
	 */
	public void activate(int event) {
		if (!showGump(event))
						// Try to run normal usecode fun.
			ucmachine.callUsecode(getUsecode(), this, event);
	}	
	/*
	 *	Get (total) weight.
	 */

	public int getWeight() {
		int wt = super.getWeight();
		GameObject obj;
		ObjectList.ObjectIterator next = objects.getIterator();
		while ((obj = next.next()) != null)
			wt += obj.getWeight();
		return wt;
	}
	/*
	 *	Drop another onto this.
	 *
	 *	Output:	0 to reject, 1 to accept.
	 */
	public boolean drop
		(
		GameObject obj		// May be deleted if combined.
		)
		{
		if (getOwner() == null)		// Only accept if inside another.
			return false;
		return (add(obj, false, true, false));	// We'll take it, and try to combine.
	}
	/*
	 *	Recursively count all objects of a given shape.
	 */
	public int countObjects
		(
		int shapenum,			// Shape#, or EConst.c_any_shapenum for any.
		int qual,			// Quality, or EConst.c_any_qual for any.
		int framenum			// Frame#, or EConst.c_any_framenum for any.
		)
		{
		if (!canBeAdded(shapenum))
			return 0;
		int total = 0;
		GameObject obj;
		ObjectList.ObjectIterator next = objects.getIterator();
		while ((obj = next.next()) != null) {
			if ((shapenum == EConst.c_any_shapenum || obj.getShapeNum() == shapenum) &&
						// Watch for reflection.
			    (framenum == EConst.c_any_framenum || (obj.getFrameNum()&31) == framenum) &&
			    (qual == EConst.c_any_qual || obj.getQuality() == qual)) {
						// Check quantity.
				int quant = obj.getQuantity();
				total += quant;
			}
						// Count recursively.
			total += obj.countObjects(shapenum, qual, framenum);
		}
		return (total);
	}
	/*
	 *	Recursively get all objects of a given shape.
	 */
	public int getObjects
		(
		Vector<GameObject> vec,	// Objects returned here.
		int shapenum,			// Shape#, or EConst.c_any_shapenum for any.
		int qual,			// Quality, or EConst.c_any_qual for any.
		int framenum			// Frame#, or EConst.c_any_framenum for any.
		)
		{
		int vecsize = vec.size();
		GameObject obj;
		ObjectList.ObjectIterator next = objects.getIterator();
		while ((obj = next.next()) != null)
			{
			if ((shapenum == EConst.c_any_shapenum || obj.getShapeNum() == shapenum) &&
			    (qual == EConst.c_any_qual || obj.getQuality() == qual) &&
						// Watch for reflection.
			    (framenum == EConst.c_any_framenum || (obj.getFrameNum()&31) == framenum))
				vec.add(obj);
						// Search recursively.
			obj.getObjects(vec, shapenum, qual, framenum);
		}
		return (vec.size() - vecsize);
	}
	/*
	 *	Set a flag on this and all contents.
	 */
	public void setFlagRecursively(int flag) {
		setFlag(flag);
		GameObject obj;
		ObjectList.ObjectIterator next = objects.getIterator();
		while ((obj = next.next()) != null)
			obj.setFlagRecursively(flag);
	}
	/*
	 *	Write out container and its members.
	 */
	public void writeIreg(OutputStream out) {
		/* +++++++FINISH
		unsigned char buf[20];		// 12-byte entry.
		uint8 *ptr = write_common_ireg(12, buf);
		GameObject first = objects.get_first(); // Guessing: +++++
		unsigned short tword = first ? first.get_prev().getShapeNum() 
										: 0;
		Write2(ptr, tword);
		*ptr++ = 0;			// Unknown.
		*ptr++ = getQuality();
		*ptr++ = 0;		// "Quantity".
		*ptr++ = (get_lift()&15)<<4;	// Lift 
		*ptr++ = (unsigned char)resistance;		// Resistance.
						// Flags:  B0=invis. B3=okay_to_take.
		*ptr++ = (getFlag(GameObject.invisible) != 0) +
			 ((getFlag(GameObject.okay_to_take) != 0) << 3);
		out.write((char*)buf, ptr - buf);
		writeContents(out);		// Write what's contained within.
						// Write scheduled usecode.
		Game_map.write_scheduled(out, this);	
		*/
		}
	// Get size of IREG. Returns -1 if can't write to buffer
	public int getIregSize() {
		/* ++++++++++
		// These shouldn't ever happen, but you never know
		if (gumpman.find_gump(this) || Usecode_script.find(this))
			return -1;
		*/
		int total_size = 8 + getCommonIregSize();

		// Now what's inside.
		if (!objects.isEmpty()) {
			GameObject obj;
			ObjectList.ObjectIterator next = objects.getIterator();
			while ((obj = next.next()) != null) {
				int size = obj.getIregSize();
				if (size < 0) return -1;
				total_size += size;
			}
			total_size += 1;
		}
		return total_size;
	}
	/*
	 *	Write contents (if there is any).
	 */

	public void  writeContents(OutputStream out) throws IOException {
		if (!objects.isEmpty()) {	// Now write out what's inside.
			GameObject obj;
			ObjectList.ObjectIterator next = objects.getIterator();
			while ((obj = next.next()) != null)
				obj.writeIreg(out);
			out.write((byte)0x01);		// A 01 terminates the list.
			}
	}
	public boolean extractContents(ContainerGameObject targ) {
		if (objects.isEmpty())
			return true;
		boolean status = true;

		GameObject obj;

		while ((obj = objects.getFirst()) != null) {
			remove(obj);
			if (targ != null) {
				targ.add(obj, true); // add without checking volume
			} else {
				obj.setInvalid(); // set to invalid chunk so move() doesn't fail
				MapChunk c = getChunk();
				if ((c.getCx() == 255) && (c.getCy() == 255)) {
					obj.removeThis();
					status = false;
				} else {
					obj.move(getTileX(), getTileY(), getLift());
				}
			}
		}
		return status;
	}
	public void  deleteContents() {
		if (objects.isEmpty())
			return;
		GameObject obj;
		while ((obj = objects.getFirst()) != null) {
			remove(obj);
			obj.deleteContents(); // recurse into contained containers
			obj.removeThis();
		}
	}
	public void removeThis(boolean nodel) {
						// Needs to be saved, as it is invalidated below but needed
						// shortly after.
		ContainerGameObject safe_owner =  getOwner();
						// Special case to avoid recursion.
		if (safe_owner != null) {
						// First remove from owner.
			super.removeThis();
			if (nodel)		// Not deleting?  Then done.
				return;
		}
		if (!nodel)
			extractContents(safe_owner);
		super.removeThis();
	}
	/*
	 *	Find ammo used by weapon.
	 *
	 *	Output:	.object if found. Additionally, is_readied is set to
	 *	true if the ammo is readied.
	 */

	public GameObject find_weapon_ammo
		(
		int weapon,			// Weapon shape.
		int needed,
		boolean recursive
		)
		{
		/* +++++++++++FINISH 
		if (weapon < 0 || !canBeAdded(this, weapon))
			return 0;
		WeaponInfo winf = ShapeID.getInfo(weapon).get_weapon_info();
		if (!winf)
			return 0;
		int family = winf.get_ammo_consumed();
		if (family >= 0)
			return 0;

		Game_object_vector vec;		// Get list of all possessions.
		vec.reserve(50);
		getObjects(vec, EConst.c_any_shapenum, EConst.c_any_qual, EConst.c_any_framenum);
		for (Game_object_vector.const_iterator it = vec.begin(); 
					it != vec.end(); ++it)
			{
			GameObject obj = *it;
			if (obj.getShapeNum() != weapon)
				continue;
			ShapeInfo inf = obj.getInfo();
			if (family == -2)
				{
				if (!inf.hasQuality() || obj.getQuality() >= needed)
					return obj;
				}
				// Family -1 and family -3.
			else if (obj.getQuantity() >= needed)
				return obj;
			}
		*/
		return null;
	}

}
