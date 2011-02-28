package com.exult.android.shapeinf;
import com.exult.android.*;
import java.util.TreeMap;
import java.util.Vector;

import com.exult.android.EFile;

/*
 *  miscinf.h - Information about several previously-hardcoded shape data.
 *
 *  Copyright (C) 2006  The Exult Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

/*
 *	A class to get the extra information for a given shape.
 */
public class ShapeInfoLookup {
	static Vector<StringIntPair> paperdoll_source_table;
	static Vector<int[]>  imported_gump_shapes;
	static Vector<int[]> blue_shapes;
	static Vector<int[]> imported_skin_shapes;
	static TreeMap<String, Integer> gumpvars;
	static TreeMap<String, Integer> skinvars;

	static TreeMap<Boolean, BaseAvatarInfo> def_av_info;
	static AvatarDefaultSkin base_av_info;
	static Vector<SkinData> skins_table;
	static TreeMap<Integer, Boolean> unselectable_skins;
	static TreeMap<Integer, Integer> petra_table;
	static TreeMap<Integer, UsecodeFunctionData> usecode_funs;
	static int last_skin;
	
	/*
	 *	Base parser class shape data.
	 */
	abstract static class Shapeinfo_entry_parser extends GameSingletons {
		protected int indTemp[] = new int[1];
		abstract void parse_entry(int index, byte buf[],
				boolean for_patch, int version);
		int ReadInt(byte buf[], int ind[], int off) {
			ind[0] += off;
			int b, cur = ind[0], len = buf.length;
			boolean neg = false;
			int i = 0, digits = 0;
			while (cur < len) {
				b = buf[cur++]&0xff;
				if (b == '-' && digits == 0)
					neg = !neg;
				else if (Character.isDigit(b)) {
					i = 10*i + (b - (int)'0');
					++digits;
				} else if (digits != 0 || !Character.isSpace((char)b)) {
					cur--;
					break;
				}
			}
			while (cur < len && Character.isSpace((char)buf[cur]))
				++cur;
			ind[0] = cur;
			if (neg)
				i = -i;
			return digits > 0 ? i : -1;
		}
		String ReadStr(byte buf[], int ind[], int off) {			
			ind[0] += off;
			int start = ind[0];
			int end = start, len = buf.length;
			while (end < len && buf[end] != '/')
				;
			ind[0] = end;
			return new String(buf, start, end - start);
		}
		//	Return string from ind to the end of buf.
		String ReadStr(byte buf[], int ind) {
			int end = ind;
			while (end < buf.length && buf[end] != 0)
				++end;
			return new String(buf, ind, end - ind);
		}
	}
	static class Int_pair_parser extends Shapeinfo_entry_parser {
		TreeMap<Integer,Integer> table;
		public Int_pair_parser(TreeMap<Integer,Integer> t) {
			table = t;
		}
		@Override
		void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			indTemp[0] = 0;
			int key = ReadInt(buf, indTemp, 0);
			int data = ReadInt(buf, indTemp, 1);
			table.put(key, data);
		}
	}
	static class Bool_parser extends Shapeinfo_entry_parser {
		TreeMap<Integer, Boolean> table;
		Bool_parser(TreeMap<Integer, Boolean> t) {
			table = t;
		}
		@Override
		void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			indTemp[0] = 0;
			int key = ReadInt(buf, indTemp, 0);
			table.put(key, true);
		}
	}
	static class Shape_imports_parser extends Shapeinfo_entry_parser
	{
		Vector<int[]> table;
		TreeMap<String, Integer> shapevars;
		int shape;
		Shape_imports_parser(Vector<int[]> t, TreeMap<String, Integer> sh) {
			table = t;
			shapevars = sh;
			shape = EConst.c_max_shapes;
		}
		@Override
		void parse_entry(int index, byte buf[],	boolean for_patch, int version) {
			int data[] = new int[2];
			indTemp[0] = 0;
			data[1] = ReadInt(buf, indTemp, 0);	// The real shape.
			for (int elem[] : table) {
				if (elem[1] == data[1])
					return;	// Do nothing for repeated entries.
			}
			indTemp[0]++;
			if (buf[indTemp[0]] == '%') {
				data[0] = shape;		// The assigned shape.
				String key = ReadStr(buf, indTemp[0]);
				shapevars.put(key, shape);
				shape++;	// Leave it ready for the next shape.
			} else
				data[0] = ReadInt(buf, indTemp, 0);
			table.add(data);
		}
	}
	static class Shaperef_parser extends Shapeinfo_entry_parser {
		Vector<int[]> table;
		TreeMap<String, Integer> shapevars;
		Shaperef_parser(Vector<int[]> t, TreeMap<String, Integer> sh) {
			table = t;
			shapevars = sh;
		}
		@Override
		public void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			int data[] = new int[2];
			indTemp[0] = 0;
			data[0] = ReadInt(buf, indTemp, 0);	// The spot.
			indTemp[0]++;
			if (buf[indTemp[0]] == '%') {
				String key = ReadStr(buf, indTemp[0]);
				Integer val = shapevars.get(key);
				if (val != null)
					data[1] = val;	// The shape #.
				else
					return;	// Invalid reference; bail out.
			} else
			data[1] = ReadInt(buf, indTemp, 0);
			table.add(data);
		}
	}
	static class Paperdoll_source_parser extends Shapeinfo_entry_parser {
		Vector<StringIntPair > table;
		boolean erased_for_patch;
		Paperdoll_source_parser(Vector<StringIntPair > t) {
			table = t;
		}
		@Override
		public void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			if (!erased_for_patch && for_patch)
				table.clear();
			String line = new String(buf);
			if (line == "static" ||
					(game.isSI() && line == "bg") ||
					(game.isSI() && line == "si"))
				table.add(new StringIntPair(EFile.PAPERDOL, -1));
			else if (line == "si")
				table.add(new StringIntPair("<SERPENT_STATIC>/paperdol.vga", -1));
			else if (game.isSI() && line == "flx")
				// ++++ FIMXME: Implement in the future for SI paperdoll patches.
				System.out.println("Paperdoll source file '" + line +
						"' is not implemented yet.");
			else if (game.isSI() && line == "flx") {
				final StringIntPair resource = game.getResource("files/paperdolvga");
				table.add(new StringIntPair(resource.str, resource.num));
			} else
				System.out.println("Unknown paperdoll source file '" + line +
						"' was specified.");
		}
	}
	static class Def_av_shape_parser extends Shapeinfo_entry_parser {
		TreeMap<Boolean, BaseAvatarInfo> table;
		Def_av_shape_parser(TreeMap<Boolean, BaseAvatarInfo> t) {
			table = t;
		}
		@Override
		public void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			indTemp[0] = 0;
			boolean fmale = ReadInt(buf, indTemp, 0) != 0;
			BaseAvatarInfo entry = new BaseAvatarInfo();
			entry.shape_num = ReadInt(buf, indTemp, 1);
			entry.face_shape = ReadInt(buf, indTemp, 1);
			entry.face_frame = ReadInt(buf, indTemp, 1);
			table.put(fmale, entry);
		}
	}
	static class Base_av_race_parser extends Shapeinfo_entry_parser {
		AvatarDefaultSkin table;
		Base_av_race_parser(AvatarDefaultSkin t) {
			table = t;
		}
		@Override
		public void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			indTemp[0] = 0;
			table.default_skin = ReadInt(buf, indTemp, 0);
			table.default_female = ReadInt(buf, indTemp, 1) != 0;
		}
	}
	static class Multiracial_parser extends Shapeinfo_entry_parser {
		Vector<SkinData> table;
		TreeMap<String, Integer> shapevars;
		Multiracial_parser(Vector<SkinData> t, TreeMap<String, Integer> sh) {
			table = t; shapevars = sh;
		}
		int ReadVar(byte buf[], int ind[]) {
			ind[0]++;
			if (buf[ind[0]] == '%') {
				String key = ReadStr(buf, ind, 0);
				Integer val = shapevars.get(key);
				if (val != null)
					return val;	// The var value.
				else
					return -1;	// Invalid reference; bail out.
			} else
			return ReadInt(buf, ind, 0);
		}
		@Override
		public void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			SkinData entry = new SkinData();
			indTemp[0] = 0;
			entry.skin_id = ReadInt(buf, indTemp, 0);
			if (entry.skin_id > last_skin)
				last_skin = entry.skin_id;
			entry.is_female = ReadInt(buf, indTemp, 1) != 0;
			if ((entry.shape_num = ReadVar(buf, indTemp)) < 0)
				return;
			if ((entry.naked_shape = ReadVar(buf, indTemp)) < 0)
				return;
			entry.face_shape = ReadInt(buf, indTemp, 1);
			entry.face_frame = ReadInt(buf, indTemp, 1);
			entry.alter_face_shape = ReadInt(buf, indTemp, 1);
			entry.alter_face_frame = ReadInt(buf, indTemp, 1);
			entry.copy_info = !(version == 2 && indTemp[0] < buf.length && 
									ReadInt(buf, indTemp, 1)==0);
			if (for_patch && !table.isEmpty()) {
				int i;
				int found = -1, sz = table.size();
				for (i = 0; i < sz; i++) {
					SkinData skin = table.elementAt(i);
					if (skin.skin_id == entry.skin_id && skin.is_female == entry.is_female) {
						found = i;
						break;
					}
				}
				if (found > -1) {
					table.setElementAt(entry, found);
					return;
				}
			}
			table.add(entry);
		}
	}
	static class Avatar_usecode_parser extends Shapeinfo_entry_parser {
		TreeMap<Integer, UsecodeFunctionData> table;
		Avatar_usecode_parser(TreeMap<Integer, UsecodeFunctionData> t) {
			table = t;
		}
		@Override
		public void parse_entry(int index, byte buf[], boolean for_patch, int version) {
			UsecodeFunctionData entry = new UsecodeFunctionData();
			indTemp[0] = 0;
			int type = ReadInt(buf, indTemp, 1);
			if (buf[indTemp[0]] == ':') {
				String name = ReadStr(buf, indTemp, 1);
				//++++++FINISH entry.fun_id = ucmachine.findFunction(name, true);
			} else
				entry.fun_id = ReadInt(buf, indTemp, 1);
			entry.event_id = ReadInt(buf, indTemp, 1);
			table.put(type, entry);
		}
	}
	private void setupShapeFiles() {
		paperdoll_source_table = new Vector<StringIntPair >();
		imported_gump_shapes = new Vector<int[]>();
		gumpvars = new TreeMap<String, Integer>();
		blue_shapes = new Vector<int[] >();
		imported_skin_shapes = new Vector<int[]>();
		skinvars = new TreeMap<String, Integer>();
		final String sections[] = {
			"paperdoll_source",
			"gump_imports",
			"blue_shapes",
			"multiracial_imports"
			};
		/*++++++++++FINISH
		ShapeinfoEntryParser parsers[] = {
			new Paperdoll_source_parser(paperdoll_source_table),
			new Shape_imports_parser(imported_gump_shapes, gumpvars),
			new Shaperef_parser(blue_shapes, gumpvars),
			new Shape_imports_parser(imported_skin_shapes, skinvars)
			};

		Read_data_file("shape_files", sections, parsers);
		*/
		// For safety.
		if (paperdoll_source_table.size() == 0)
			paperdoll_source_table.add(new StringIntPair(EFile.PAPERDOL, -1));
		// Add in patch paperdolls too.
		paperdoll_source_table.add(new StringIntPair(EFile.PATCH_PAPERDOL, -1));
	}
	private void setupAvatarData() {
		if (skinvars == null)
			setupShapeFiles();
	}
	
	public static class StringIntPair {
		String str;
		int num;
		StringIntPair(String st, int i) {
			str = st; num = i;
		}
	}
	public static class BaseAvatarInfo {
		int shape_num;
		int face_shape;			// Shape and frame for face during the
		int face_frame;			// normal game.
	}
	public static class AvatarDefaultSkin {
		int default_skin;		// The starting skin color.
		boolean default_female;	// True if the default sex if female.
	}
	public static class SkinData {
		int skin_id;
		int shape_num;
		int naked_shape;
		int face_shape;			// Shape and frame for face during the
		int face_frame;			// normal game.
		int alter_face_shape;	// Shape and frame for face to be used
		int alter_face_frame;	// when flag 33 is set.
		boolean is_female;
		boolean copy_info;			// Whether or not Exult should overwrite shape info
					// with info from the default avatar shape
	}
	public static class UsecodeFunctionData {
		int fun_id;
		int event_id;
	}
}
