package com.exult.android;
import java.io.OutputStream;
import java.io.IOException;

/*
 * These are moveable objects.
 */
public class IregGameObject extends GameObject {
	private ContainerGameObject owner;	// Container this is in, or 0.
	protected int flags;		// 32 flags used in 'usecode'.
	protected int flags2;		// Another 32 flags used in 'usecode'.
	private static final byte writeBuf[] = new byte[20];
	
	public IregGameObject(int shapenum, int framenum, int tilex, int tiley, int lft) {
		super(shapenum, framenum, tilex, tiley, lft);
	}
	public final void setFlags(int f) {
		flags = f;
	}
	public final int getFlags() {
		return flags;
	}
	public final int getFlags2() {
		return flags2;
	}
	public boolean getFlag(int flag) {
		if (flag >= 0 && flag < 32)
			return (flags & (1 << flag)) != 0;
		else if (flag >= 32 && flag < 64)
			return (flags2 & (1 << (flag-32))) != 0;
		return false;
	}
	public void setFlag(int flag) {
		if (flag >= 0 && flag < 32)
			flags |= (1 << flag);
		else if (flag >= 32 && flag < 64)
			flags2 |= (1 << (flag-32));
	}
	public void clearFlag(int flag) {
		if (flag >= 0 && flag < 32)
			flags &= ~(1 << flag);
		else if (flag >= 32 && flag < 64)
			flags2 &= ~(1 << (flag-32));
	}
	public ContainerGameObject getOwner() {
		return owner;
	}
	public void setOwner(ContainerGameObject o) {
		owner = o;
	}
	public void removeThis() {
		if (owner != null)			// In a bag, box, or person.
			owner.remove(this);
		else if (chunk != null)			// In the outside world.
			chunk.remove(this);
	}
	public boolean isDragable() {
		return getInfo().getWeight() > 0;	// 0 means 'too heavy'.
	}
	public static IregGameObject create(ShapeInfo info, int shnum, int frnum) {
		return create(info, shnum, frnum, 0, 0, 0);
	}
	public static IregGameObject create
		(
		ShapeInfo info,		// Info. about shape.
		int shnum, int frnum,		// Shape, frame.
		int tilex, int tiley,		// Tile within chunk.
		int lift			// Desired lift.
		) {
			// (These are all animated.)
		if (info.isField() && info.getFieldType() >= 0)
			return new EggObject.Field(shnum, frnum, tilex, tiley,
					lift, (byte)(EggObject.fire_field + info.getFieldType()));
		else if (info.isAnimated() || info.hasSfx())
			return new Animated(shnum, frnum, tilex, tiley, lift);
		else if (shnum == 607)		// Path.
			return new EggObject.PathMarker(
					shnum, frnum, tilex, tiley, lift);
		else if (info.isMirror())	// Mirror
			return new EggObject.Mirror(shnum, frnum, tilex, tiley, lift);
		else if (info.isBodyShape())
			return new Actor.DeadBody(shnum, frnum, tilex, tiley, lift, -1);
		else if (info.getShapeClass() == ShapeInfo.virtue_stone)
			return new VirtueStoneObject(
					shnum, frnum, tilex, tiley, lift);
		else if (info.getShapeClass() == ShapeInfo.spellbook) {
			final byte circles[] = {0, 0, 0, 0, 0, 0, 0, 0, 0};
			return new SpellbookObject(
				shnum, frnum, tilex, tiley, lift, circles, (byte)0);
		} else if (info.getShapeClass() == ShapeInfo.container) {
			/*
			if (info.is_jawbone())
				return new Jawbone_object(shnum, frnum, tilex, tiley,
									lift);
			else */
				return new ContainerGameObject(shnum, frnum, 
							tilex, tiley, lift, 0);
		} else
			return new IregGameObject(shnum, frnum, tilex, tiley, lift);
	}
	public static IregGameObject create(int shnum, int frnum) {
		return create(ShapeID.getInfo(shnum), shnum, frnum, 0, 0, 0);
	}
	// Write 1 IREG data.
	public final int getCommonIregSize() {
		return (getShapeNum() >= 1024 || getFrameNum() >= 64) 
								? 7 : 5; 
	}
	@Override
	public int getIregSize() {
		return iregGetIregSize();
	}
	protected int iregGetIregSize() {
		// These shouldn't ever happen, but you never know
		if (gumpman.findGump(this) != null || UsecodeScript.find(this) != null)
			return -1;
		return 6 + getCommonIregSize();
	}
	/*
	 *	Write the common IREG data for an entry.
	 *	Note:  Length is incremented if this is an extended entry (shape# >
	 *		1023).
	 *	Output:	Index past data written.
	 */
	protected int writeCommonIreg
		(
		int norm_len,			// Normal length (if not extended).
		byte buf[]		// Buffer to be filled.
		)
		{
		int endptr;
		int ind = 0;
		int shapenum = getShapeNum(), framenum = getFrameNum();
		if (shapenum >= 1024 || framenum >= 64) {
			buf[ind++] = (byte)GameMap.IREG_EXTENDED;
			norm_len++;
			buf[3] = (byte)(shapenum&0xff);
			buf[4] = (byte)((shapenum>>8)&0xff);
			buf[5] = (byte)framenum;
			endptr = 6;
		} else {
			buf[3] = (byte)(shapenum&0xff);
			buf[4] = (byte)(((shapenum>>8)&3) | (framenum<<2));
			endptr = 5;
		}
		buf[0] = (byte)norm_len;
		if (owner != null) {			// Coords within gump.
			buf[1] = (byte)getTx();
			buf[2] = (byte)getTy();
		} else {			// Coords on map.
			int cx = chunk != null ? chunk.getCx() : 255;
			int cy = chunk != null ? chunk.getCy() : 255;
			buf[1] = (byte)(((cx%16) << 4) | (getTx()&0xff));
			buf[2] = (byte)(((cy%16) << 4) | (getTy()&0xff));
		}
		return endptr;
	}
	@Override
	public void writeIreg(OutputStream out) throws IOException {
		iregWriteIreg(out);
	}
	public void iregWriteIreg(OutputStream out) throws IOException {	
		int ind = writeCommonIreg(10, writeBuf);
		writeBuf[ind++] = (byte)((getLift()&15)<<4);
		writeBuf[ind] = (byte)getQuality();
		ShapeInfo info = getInfo();
		if (info.hasQualityFlags()) {			// Store 'quality_flags'.
			writeBuf[ind] = (byte)((getFlag(GameObject.invisible)?1:0) +
					((getFlag(GameObject.okay_to_take)?1:0) << 3));
		}
					// Special case for 'quantity' items:
		else if (getFlag(GameObject.okay_to_take) && info.hasQuantity())
			writeBuf[ind] |= 0x80;
		++ind;
		writeBuf[ind++] = (byte)(getFlag(GameObject.is_temporary) ? 1 : 0);
		writeBuf[ind++] = 0;			// Filler, I guess.
		writeBuf[ind++] = 0;
		writeBuf[ind++] = 0;
		out.write(writeBuf, 0, ind);
					// Write scheduled usecode.
		GameMap.writeScheduled(out, this, false);	
	}
	public static class Animated extends IregGameObject {
		private Animator animator;
		public Animated(int shapenum, int framenum, int tilex, int tiley, int lft) {
			super(shapenum, framenum, tilex, tiley, lft);
			animator = Animator.create(this);
		}
		@Override
		public void removeThis() {
			super.removeThis();
			animator.delete();
		}
		@Override
		public void paint() {
			animator.wantAnimation();	// Be sure animation is on.
			super.paint();
		}
		// Get coord. where this was placed.
		@Override
		public void getOriginalTileCoord(Tile t) {
			getTile(t);
			t.tx -= animator.getDeltax();
			t.ty -= animator.getDeltay();
		}
					// Write out to IREG file.
		public void writeIreg(OutputStream out) throws IOException {
			int oldframe = getFrameNum();
			setFrame(animator.getFrameNum());
			super.writeIreg(out);
			setFrame(oldframe);
		}
	}
}
