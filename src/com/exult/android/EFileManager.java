package com.exult.android;
import java.util.TreeMap;

public class EFileManager {
	static EFileManager instance;
	private TreeMap fileList;
	private EFileManager() {
		fileList = new TreeMap();
	}
	public static EFileManager instanceOf() {
		if (instance == null)
			instance = new EFileManager();
		return instance;
	}
	public EFile getFileObject(String nm) {
		if (nm == null)
			return null;
		EFile file = (EFile) fileList.get(nm);
		if (file != null)
			return file;
		String fname = EUtil.U7exists(nm);
		if (fname == null)
			return null;
		if (EUtil.isFlex(fname))
			file = new FlexFile(fname);
		/* +++++FINISH
		else if (EUtil.isIff(s.name))
			uf = new IFFFile(s.name);
		else if (Table::is_table(s.name))
			uf = new TableFile(s.name);
		*/
		else
			file = new EFile(fname);	// Flat file.
		// Failed
		if (file == null) {
			return null;
		}
		fileList.put(nm, file);
		return file;	
	}
	/*
	 * Retrieve given obj with file.
	 */
	public byte[] retrieve(String nm, int objnum) {
		EFile file = getFileObject(nm);
		if (file != null) {
			byte res[] = file.retrieve(objnum);
			if (res != null)
				return res;
		}
		return null;
	}
	/*
	 * Try files in reverse order, looking for given objnum in file.
	 */
	public byte[] retrieve(String nm1, String nm2, int objnum) {
		byte res[] = retrieve(nm2, objnum);
		if (res == null)
			res = retrieve(nm1, objnum);
		return res;
	}
}
