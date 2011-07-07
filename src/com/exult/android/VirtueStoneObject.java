package com.exult.android;
import java.io.OutputStream;
import java.io.IOException;

/*
 *	A virtue stone can be set to a position on the map.
 */
public class VirtueStoneObject extends IregGameObject {
	private Tile pos;			// Position it teleports to.
	private int map;				// Map to teleport to.

	public VirtueStoneObject(int shapenum, int framenum, int tilex, 
				int tiley, int lft) {
		super(shapenum, framenum, tilex, tiley, lft);
		pos = new Tile();
	}
	public void setTargetPos(Tile t)	// Set/get position.
		{ pos.set(t); }
	// Set position from IREG data.
	public void setTargetPos(byte tilex, byte tiley, byte schunk, byte lft) {
		pos.tx = (short)((schunk%12)*EConst.c_tiles_per_schunk + tilex);
		pos.ty = (short)((schunk/12)*EConst.c_tiles_per_schunk + tiley);
		pos.tz = lift;
	}
	public Tile getTargetPos()
		{ return pos; }
	public final int getTargetMap()	// Get/set map.
		{ return map; }
	public final void setTargetMap(int m)
		{ map = m; }
	@Override				// Write out to IREG file.
	public void writeIreg(OutputStream out) throws IOException {
		byte buf[] = new byte[20];		// 12-byte entry.
		int ind = writeCommonIreg(12, buf);
						// Write tilex, tiley.
		buf[ind++] = (byte)(pos.tx%EConst.c_tiles_per_schunk);
		buf[ind++] = (byte)(pos.ty%EConst.c_tiles_per_schunk);
						// Get superchunk index.
		int sx = pos.tx/EConst.c_tiles_per_schunk,
		    sy = pos.ty/EConst.c_tiles_per_schunk;
		buf[ind++] = (byte)(sy*12 + sx);		// Write superchunk #.
		buf[ind++] = (byte) pos.tz;		// Finally, lift in entry[7].??Guess+++
		buf[ind++] = 0;			// Entry[8] unknown.
		buf[ind++] = (byte)((getLift()&15)<<4);	// Stone's lift in entry[9].
		buf[ind++] = (byte)map;		// Entry[10].  Unknown; using to store map.
		buf[ind++] = 0;			// Entry[11].  Unknown.
		out.write(buf, 0, ind);
	}
	@Override			// Get size of IREG. Returns -1 if can't write to buffer
	public int getIregSize() {
		// These shouldn't ever happen, but you never know
		if (gumpman.findGump(this) != null || UsecodeScript.find(this) != null)
			return -1;

		return 8 + getCommonIregSize();
	}
}
