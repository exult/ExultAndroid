package com.exult.android;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.lang.InterruptedException;
import android.graphics.Point;

public class UsecodeMachine extends GameSingletons {
	public boolean debug = false;
	private byte gflags[];	// Global flags.
	// Functions: I'th entry contains funs for ID's 256*i + n.
	private Vector<Vector<UsecodeFunction>> funs = 
				new Vector<Vector<UsecodeFunction>>();
	private Vector<UsecodeValue> statics;		// Global persistent vars.
	private LinkedList<StackFrame> callStack = new LinkedList<StackFrame>();
	private StackFrame frame;
	private boolean modifiedMap;	//  We add/deleted/moved an object.
	private TreeMap<Integer, Integer> timers = new TreeMap<Integer,Integer>();
	private GameObject caller_item;
	private TextGump book;		// Book/scroll being displayed.
	private boolean found_answer;		// Did we already handle the conv. option?
	private Tile saved_pos;		// For a couple SI intrinsics.
	private int saved_map;
	private String theString;	// The single string register.
	private UsecodeValue stack[];
	private UsecodeIntrinsics intrinsics;
	private UsecodeValue intrinsicParms[] = new UsecodeValue[12];
	private Point clickPoint = new Point();
	private int sp;				// Stack-pointer index.
	public static int running;	// >0 when we are running.
	private final Semaphore available = new Semaphore(1, true);

	public static final int 	// enum Usecode_events
		npc_proximity = 0,
		double_click = 1,
		internal_exec = 2,	// Internal call via intr. 1 or 2.
		egg_proximity = 3,
		weapon = 4,		// From weapons.dat.
		readied = 5,		// Wear an item.
		unreadied = 6,		// Removed an item.
		died = 7,		// In SI only, I think.
		chat = 9	// When a NPC wants to talk to you in SI
		;
	public static final int 	// enum Global_flag_names
		did_first_scene = 0x3b,	// Went through 1st scene with Iolo.
		have_trinsic_password = 0x3d,
		found_stable_key = 0x3c,
		left_trinsic = 0x57,
		avatar_is_thief = 0x2eb
		;
	public UsecodeMachine() {
		/*+++++
		 keyring = new Keyring();
		 saved_pos = new Tile(-1, -1, -1);
		 saved_map = -1;
		 */
		gflags = new byte[EConst.c_last_gflag + 1];
		stack = new UsecodeValue[1024];
		intrinsics = new UsecodeIntrinsics();	// ++++FOR NOW. Later do BG, SI.
		if (conv == null)
			conv = new Conversation();
		sp = 0;
		InputStream file;                // Read in usecode.
		try {
			file = EUtil.U7openStream(EFile.USECODE);
			readUsecode(file, false);
			file.close();
		} catch (IOException e) {
			System.out.println("Couldn't open '" + EFile.USECODE + "'!");
			return;
		}
						// Get custom usecode functions.
		try {
			file = EUtil.U7openStream(EFile.PATCH_USECODE);
			readUsecode(file, true);
			file.close();
		} catch (IOException e) { }
	}
	public boolean inUsecode() {
		return running > 0;
	}
	public final boolean getGlobalFlag(int i)	// Get/set ith flag.
		{ return gflags[i] != 0; }
	public final void setGlobalFlag(int i, int val)
		{ gflags[i] = (byte)((val == 1)?1:0); }
	public final void setModifiedMap() {
		modifiedMap = true;
	}
	public Integer getTimer(int tnum) {
		return timers.get(tnum);
	}
	public void setTimer(int tnum, int val) {
		timers.put(tnum, val);
	}
	public final void setBook(TextGump b) {
		gumpman.closeGump(book);
		book = b;
	}
	public final int getCurrentFunction() {
		return frame.function.id;
	}
	public final GameObject get_caller_item() {
		return caller_item;
	}
	public final void readUsecode(InputStream file, boolean patch) throws IOException {
		file.mark(16);
		/*UNUSED long magic = */ EUtil.Read4(file);	// Test for symbol table.
		/*
		if (magic == UCSYMTBL_MAGIC0 && (magic = EUtil.Read4(file)) 
							== UCSYMTBL_MAGIC1) {
			symtbl = new Usecode_symbol_table();
			symtbl.read(file);
		} else
		*/
			file.reset();
					// Read in all the functions.
		while (file.available() > 0) {
			UsecodeFunction fun = new UsecodeFunction(file);
			int slotnum = fun.id/0x100;
			if (slotnum >= funs.size())
				funs.setSize(slotnum < 10 ? 10 : slotnum + 1);
			Vector<UsecodeFunction> vec = funs.elementAt(slotnum);
			int i = fun.id%0x100;
			if (vec == null) {
				vec = new Vector<UsecodeFunction>(i + 1);
				funs.setElementAt(vec, slotnum);
			} 
			if (i >= vec.size())
				vec.setSize(i + 1);
			else if (vec.elementAt(i) != null) {		// Already have one there.
				if (patch) {	// Patching?
					if (vec.elementAt(i).orig != null) {	// Patching a patch.
						fun.orig = vec.elementAt(i).orig;
					} else		// Patching fun. from static.
						fun.orig = vec.elementAt(i);
				}
			}
			vec.set(i, fun);
		}
	}
	static class UsecodeThread extends Thread {
		private int id, event;
		private GameObject item;
		public UsecodeThread(int i, GameObject itm, int ev) {
			id = i; item = itm; event = ev;
		}
		public void run() {
			try {		// Want to wait until existing thread is done.
				ucmachine.available.acquire();
			} catch (InterruptedException e) { }
			if (ucmachine.call_function(id, event, item, true, false)) {
				ucmachine.myRun();// ? 1 : 0;
			} else {
				ucmachine.available.release();
			}
		}
	}
	/*
	 *	This is the main entry for outside callers.
	 *
	 *	Output:	-1 if not found. +++++ CURRENTLY always returns 1 or 0.
	 *		0 if can't execute now or if aborted.
	 *		1 otherwise.
	 */
	public int callUsecode
		(
		int id, 			// Function #.
		GameObject item,		// Item ref.
		int event
		) {
						// Avoid these when already execing.
		if (!callStack.isEmpty() && 
			event == npc_proximity /* ++++ && Game::get_game_type() ==
									BLACK_GATE*/)
			return (0);
		conv.clearAnswers();
		System.out.printf("UsecodeMachine.callUsecode: %1$04x with event %2$d\n", id, event);
		UsecodeThread t = new UsecodeThread(id, item, event);
		t.start();
		return 1;
	}
	/*
	 *	Lookup function name in symbol table.  Prints error if not found.
	 */
	public int findFunction(String nm, boolean noerr) {
		/* +++++++++FINISH
		Usecode_symbol *ucsym = symtbl ? (*symtbl)[nm] : 0;
		if (!ucsym)
			{
			if (!noerr)
				cerr << "Failed to find Usecode symbol '" << nm
						<< "'." << endl;
			return -1;
			}
		return ucsym->get_val();
		*/ return -1;
	}
	/*
	 *	Lookup function id in symbol table.
	 */
	String findFunctionName(int funcid) {
		/* +++++FINISH
		Usecode_symbol *ucsym = symtbl ? (*symtbl)[funcid] : 0;
		if (!ucsym)
			return 0;
		return ucsym->get_name();
		*/ return null;
	}
	/*
	 *	Start speech, or show text if speech isn't enabled.
	 */
	public void doSpeech(int num) {
		intrinsics.setSpeechTrack(num);		// Used in Usecode function.
		if (!audio.startSpeech(num))
						// No speech?  Call text function.
			callUsecode(0x614, null, double_click);
	}
	/*
	 *	Are we in a usecode function for a given item and event?
	 */
	public boolean inUsecodeFor(GameObject item, int event) {
		for (StackFrame f : callStack) {
			if (f.eventid == event && f.caller_item == item)
				return true;
		}
		return false;
	}
	public void initConversation() {
		conv.initFaces();
	}
	public int getShapeFun(int n) {
		return n < 0x400 ? n :
			(/* ++++ symtbl != null ? symtbl.get_high_shape_fun(n)
				// Default to 'old-style' high shape functions.
				: */ 0x1000 + (n - 0x400));
	}
	// Acquire 'available' before calling this.
	private boolean myRun() {
		++running;
		boolean ret = run();
		
		setBook(null);
						// Left hanging (BG)?
		if (conv.getNumFacesOnScreen() > 0) {
			System.out.println("myRun: initFaces(), running = " + running);
			conv.initFaces();	// Remove them.
			gwin.setAllDirty();	// Force repaint.
		}
		if (modifiedMap) {	// On a barge, and we changed the map.
			BargeObject barge = gwin.getMovingBarge();
			if (barge != null)
				barge.setToGather();	// Refigure what's on barge.
			modifiedMap = false;
		}
		--running;
		System.out.println("End of myRun");
		available.release();
		return ret;
	}
	/*
	 * MAIN interpreter
	 */
	public boolean run() {
		boolean aborted = false;
		boolean initializing_loop = false;

		while ((frame = callStack.getFirst()) != null) {
			int num_locals = frame.num_vars + frame.num_args;
			int offset;
			int sval;

			boolean frame_changed = false;

			// set some variables for use in other member functions
			caller_item = frame.caller_item;

			/*
			 *	Main loop.
			 */
			while (!frame_changed)
			{


				if ((frame.ip >= frame.endp) ||
					(frame.ip < frame.code)) {
					abort_function();
					frame_changed = true;
					continue;
				}

				//UNUSED int current_IP = frame.ip - frame.code;
				int opcode = frame.fcode[frame.ip];
				// System.out.println("Opcode is " + opcode);
				/*
				if (frame.ip + get_opcode_length(opcode) > frame.endp) {
					cerr << "Operands lie outside of code segment. ";
					CERR_CURRENT_IP();
					continue;
				}
				*/
				frame.ip++;

				switch (opcode) {
				case 0x04:  // start conversation
				case 0x84: // (32 bit version)
				{
					if (opcode < 0x80)
						offset = frame.Read2();
					else
						offset = frame.Read4();
					
					found_answer = false;
					if (get_user_choice() == null)  // Exit conv. if no choices.
						frame.ip += offset; // (Emps and honey.)
					break;
				}
				case 0x05:		// JNE.
				{
					offset = (short) frame.Read2();
					UsecodeValue val = pop();
					if (val.isFalse())
						frame.ip += offset;
					break;
				}
				case 0x85:		// JNE32
				{
					offset = EUtil.Read4(frame.fcode,frame.ip);
					UsecodeValue val = pop();
					if (val.isFalse())
						frame.ip += offset;
					break;
				}
				case 0x06:		// JMP.
					offset = (short) frame.Read2();
					frame.ip += offset;
					break;
				case 0x86:		// JMP32
					offset = EUtil.Read4(frame.fcode,frame.ip);
					frame.ip += offset;
					break;
				case 0x07:		// CMPS.
				case 0x87: // (32 bit version)
				{
					int cnt = frame.Read2();	// # strings.
					if (opcode < 0x80)
						offset = (short) frame.Read2();
					else
						offset = EUtil.Read4(frame.fcode,frame.ip);
					
					boolean matched = false;
					
					// only try to match if we haven't found an answer yet
					while (!matched && !found_answer && cnt-- > 0) {
						UsecodeValue s = pop();
						String str = s.getStringValue();
						String userChoice = conv.getUserChoice();
						if (str != null && str.equals(userChoice)) {
							matched = true;
							found_answer = true;
						}
					}
					while (cnt-- > 0)	// Pop rest of stack.
						pop();
					if (!matched)		// Jump if no match.
						frame.ip += offset;
				}
				break;
				case 0x09:		// ADD.
				{
					UsecodeValue v2 = pop();
					UsecodeValue v1 = pop();
					UsecodeValue sum = v1.plus(v2);
					push(sum);
					break;
				}
				case 0x0a:		// SUB.
					sval = popi();
					pushi(popi() - sval);
					break;
				case 0x0b:		// DIV.
					sval = popi();
					pushi(popi()/sval);
					break;
				case 0x0c:		// MUL.
					pushi(popi()*popi());
					break;
				case 0x0d:		// MOD.
					sval = popi();
					pushi(popi() % sval);
					break;
				case 0x0e:		// AND.
				{
					UsecodeValue v1 = pop();
					UsecodeValue v2 = pop();
					int result = (v1.isTrue() && v2.isTrue()) ? 1 : 0;
					pushi(result);
					break;
				}
				case 0x0f:		// OR.
				{
					UsecodeValue v1 = pop();
					UsecodeValue v2 = pop();
					int result = (v1.isTrue() || v2.isTrue()) ? 1 : 0;
					pushi(result);
					break;
				}
				case 0x10:		// NOT.
					pushi(!pop().isTrue() ? 1 : 0);
					break;
				case 0x12:		// POP into a variable.
				{
					offset = frame.Read2();
					// Get value.
					UsecodeValue val = pop();
					if (offset < 0 || offset >= num_locals) {
						; // +++++ LOCAL_VAR_ERROR(offset);
					} else {
						frame.locals[offset] = val;
					}
				}
				break;
				case 0x13:		// PUSH true.
					pushi(1);
					break;
				case 0x14:		// PUSH false.
					pushi(0);
					break;
				case 0x16:		// CMPGT.
					sval = popi();
					pushi(popi() > sval ? 1 : 0);	// Order?
					break;
				case 0x17:		// CMPL.
					sval = popi();
					pushi(popi() < sval ? 1 : 0);
					break;
				case 0x18:		// CMPGE.
					sval = popi();
					pushi(popi() >= sval ? 1 : 0);
					break;
				case 0x19:		// CMPLE.
					sval = popi();
					pushi(popi() <= sval ? 1 : 0);
					break;
				case 0x1a:		// CMPNE.
				{
					UsecodeValue val1 = pop();
					UsecodeValue val2 = pop();
					pushi(!(val1 == val2 || val1.eq(val2)) ? 1 : 0);
					break;
				}
				case 0x1c:		// ADDSI.
					offset = frame.Read2();
					if (offset < 0 || frame.data + offset >= frame.externs-6) {
						// +++++ DATA_SEGMENT_ERROR();
						break;
					}
					append_string(frame.getDataString(offset));
					break;
				case 0x9c:		// ADDSI32
					offset = EUtil.Read4(frame.fcode,frame.ip);
					if (offset < 0 || frame.data + offset >= frame.externs-6) {
						// +++++ DATA_SEGMENT_ERROR();
						break;
					}
					append_string(frame.getDataString(offset));
					break;
				case 0x1d:		// PUSHS.
					offset = frame.Read2();
					if (offset < 0 || frame.data + offset >= frame.externs-6) {
						//  +++++DATA_SEGMENT_ERROR();
						break;
					}
					pushs(frame.getDataString(offset));
					break;
				case 0x9d:		// PUSHS32
					offset = EUtil.Read4(frame.fcode,frame.ip);
					if (offset < 0 || frame.data + offset >= frame.externs-6) {
						// +++++DATA_SEGMENT_ERROR();
						break;
					}
					pushs(frame.getDataString(offset));
					break;
				case 0x1e:		// ARRC.
				{		// Get # values to pop into array.
					int num = frame.Read2();
					int cnt = num;
					Vector<UsecodeValue> vals= new Vector<UsecodeValue>(num);
					while (cnt-- > 0) {
						UsecodeValue val = pop();
						UsecodeValue.ArrayValue.addValues(vals, val);
					}				
					UsecodeValue.ArrayValue arr = 
								new UsecodeValue.ArrayValue(vals);
					push(arr);
				}
				break;
				case 0x1f:		// PUSHI.
				{		// Might be negative.
					short ival = (short) frame.Read2();
					pushi(ival);
					break;
				}
				case 0x9f:		// PUSHI32
				{
					int ival = EUtil.Read4(frame.fcode,frame.ip);
					pushi(ival);
					break;
				}
				case 0x21:		// PUSH.
					offset = (short) frame.Read2();
					if (offset < 0 || offset >= num_locals) {
						// LOCAL_VAR_ERROR(offset);
						System.out.println("PUSH: bad offset " + offset +
									", num_locals = " + num_locals);
						pushi(0);
					} else {
						UsecodeValue val = frame.locals[offset];
						if (val == null)
							val = UsecodeValue.getZero();
						push(val);
					}
					break;
				case 0x22:		// CMPEQ.
				{
					UsecodeValue val1 = pop();
					UsecodeValue val2 = pop();
					pushi(val1 == val2 || val1.eq(val2) ? 1 : 0);
					break;
				}
				case 0x24:		// CALL.
				{
					offset = (short) frame.Read2();
					if (offset < 0 || offset >= frame.num_externs) {
						// EXTERN_ERROR();
						break;
					}
					int tempptr = frame.externs + 2*offset;
					int funcid = EUtil.Read2(frame.fcode, tempptr);
					call_function(funcid, frame.eventid);
					frame_changed = true;
					break;
				}
				case 0xa4:		// 32-bit CALL.
				{
					offset = frame.Read4();
					call_function(offset, frame.eventid);
					frame_changed = true;
					break;
				}
				case 0x25:		// RET. (End of procedure reached)
				case 0x2C:		// RET. (Return from procedure)
					show_pending_text();
					return_from_procedure();
					frame_changed = true;
					break;
				case 0x26:		// AIDX.
				case 0x5A:		// AIDXS.
				case 0x5D:		// AIDXTHV.
				{
					sval = popi();	// Get index into array.
					sval--;		// It's 1 based.
					// Get # of local to index.
					UsecodeValue val;
					if (opcode == 0x26) {
						offset = (short) frame.Read2();
						if (offset < 0 || offset >= num_locals) {
							// LOCAL_VAR_ERROR(offset);
							pushi(0);
							break;
						}
						val = frame.locals[offset];
					} else if (opcode == 0x5d) {
						offset = (short) frame.Read2();
						UsecodeValue ths = frame.getThis();
						if (offset < 0 || offset >= ths.getClassVarCount()) {
							System.out.println("Class variable #" + (offset) +
									" out of range!");
							//CERR_CURRENT_IP();
							break;
						}
						//+++++ val = &(ths.nth_class_var(offset));
						val = null;	// WILL CRASH
					} else {
						offset = (short)frame.Read2();
						if (offset < 0) {// Global static.
							if ((-offset) < statics.size())
								val = statics.elementAt(-offset);
							else {
								System.out.println("Global static variable #" +
										(offset) + " out of range!");
								pushi(0);
								break;
							}
						} else {
							if (frame.function.statics != null &&
									offset < frame.function.statics.size())
								val = frame.function.statics.elementAt(offset);
							else {
								System.out.println("Local static variable #" +
										(offset) + " out of range!");
								pushi(0);
								break;
							}
						}
					}
					if (sval < 0) {
						System.out.println("AIDX: Negative array index: " + sval);
						pushi(0);
						break;
					}
					if (val.isArray() && sval >= val.getArraySize())
						pushi(0);	// Matches originals.
					else if (sval == 0) // needed for SS keyring (among others)
						push(val.getElem0());
					else
						push(val.getElem(sval));
					break;
				}
				case 0x2d:		// RET. (Return from function)
				{
					show_pending_text();
					UsecodeValue r = pop();
					return_from_function(r);
					frame_changed = true;
					break;
				}
				case 0x2e:		// INITLOOP (1st byte of loop)
				case 0xae:		// (32 bit version)   
				{
					int nextopcode = frame.fcode[frame.ip];
					// No real reason to have 32-bit version of this instruction;
					// keeping it for backward compatibility only.
					nextopcode &= 0x7f;
					if (nextopcode != 0x02 && nextopcode != 0x5c &&
							nextopcode != 0x5f) {
						System.out.println("Invalid 2nd byte in loop!");
						break;
					} else {
						initializing_loop = true;
					}
					break;
				}
				case 0x02:	// LOOP (2nd byte of loop)
				case 0x82:  // (32 bit version)
				case 0x5c:	// LOOP (2nd byte of loop) using static array
				case 0xdc:	// (32 bit version)
				case 0x5f:	// LOOP (2nd byte of loop) using class member array
				case 0xdf:	// (32 bit version)
				{
					// Counter (1-based).
					int local1 = (short)frame.Read2();
					// Total count.
					int local2 = (short)frame.Read2();
					// Current value of loop var.
					int local3 = (short)frame.Read2();
					// Array of values to loop over.
					int local4;
					boolean is_32bit = (opcode > 0x80);
					// Mask off 32bit flag.
					opcode &= 0x7f;
					if (opcode == 0x5C)
						local4 = (short)frame.Read2();
					else
						local4 = frame.Read2();
					// Get offset to end of loop.
					if (is_32bit)
						offset = EUtil.Read4(frame.fcode,frame.ip); // 32 bit offset
					else
						offset = (short) frame.Read2();
					if (local1 < 0 || local1 >= num_locals) {
						//LOCAL_VAR_ERROR(local1);
						break;
					}
					if (local2 < 0 || local2 >= num_locals) {
						//LOCAL_VAR_ERROR(local2);
						break;
					}
					if (local3 < 0 || local3 >= num_locals) {
						//LOCAL_VAR_ERROR(local3);
						break;
					}
					if (opcode == 0x5c) {
						if (local4 < 0) {// Global static.
							if ((-local4) >= statics.size()) {
								// "Global static variable #" << (-local4) << " out of range!";\
								// CERR_CURRENT_IP();
								break;
							}
						} else {
							if (local4 >= frame.function.statics.size()) {
								//cerr << "Local static variable #" << (local4) << " out of range!";\
								//CERR_CURRENT_IP();
								break;
							}
						}
					} else if (opcode == 0x5f) {
						UsecodeValue ths = frame.getThis();
						if (local4 < 0 || local4 >= ths.getClassVarCount()) {
							//cerr << "Class variable #" << (local4) << " out of range!";\
							//CERR_CURRENT_IP();
							break;
						}
					} else {
						if (local4 < 0 || local4 >= num_locals) {
							// LOCAL_VAR_ERROR(local4);
							break;
						}
					}		
					// Get array to loop over.
					UsecodeValue arr = opcode == 0x5C ? 
							(local4 < 0 ? statics.elementAt(-local4)
								: frame.function.statics.elementAt(local4))
							: (opcode == 0x5f ?
								frame.getThis().nthClassVar(local4) :
								frame.locals[local4]);
					/* ++++++Don't know how this can happen.
	 				if (initializing_loop && arr.is_undefined())
					{	// If the local 'array' is not initialized, do not loop
						// (verified in FoV and SS):
						initializing_loop = false;
						frame.ip += offset;
						break;
					}
					*/

					int next;
					if (initializing_loop) {	// Initialize loop.
						initializing_loop = false;
						int cnt = arr.isArray() ?
							arr.getArraySize() : 1;
						frame.locals[local2] = new UsecodeValue.IntValue(cnt);
						frame.locals[local1] = UsecodeValue.getZero();
						next = 0;
					} else
						next = frame.locals[local1].getIntValue();
					// in SI, the loop-array can be modified in-loop, it seems
					// (conv. with Spektran, 044D:00BE)
				   
					// so, check for changes of the array size, and adjust
					// total count and next value accordingly.

					// Allowing this for BG too.

					int cnt = arr.isArray() ? arr.getArraySize() : 1;

					if (cnt != frame.locals[local2].getIntValue()) {
					
						// update new total count
						frame.locals[local2] = new UsecodeValue.IntValue(cnt);
						
						if (Math.abs(cnt-frame.locals[local2].getIntValue())==1) {
							// small change... we can fix this
							UsecodeValue curval = arr.isArray() ?
								arr.getElem(next - 1) : arr;
							
							if (!curval.eq(frame.locals[local3])) {
								if (cnt>frame.locals[local2].getIntValue()){
									//array got bigger, it seems
									//addition occured before the current value
									next++;
								} else {
									//array got smaller
									//deletion occured before the current value
									next--;
								}
							} else {
								//addition/deletion was after the current value
								//so don't need to update 'next'
							}
						}
						else
						{
								// big change... 
								// just update total count to make sure
								// we don't crash
						}
					}

					if (cnt != frame.locals[local2].getIntValue()) {

						// update new total count
						frame.locals[local2] = new UsecodeValue.IntValue(cnt);

						UsecodeValue curval = arr.isArray() ?
							arr.getElem(next - 1) : arr;
						
						if (curval != frame.locals[local3]) {
							if (cnt > frame.locals[local2].getIntValue()) {
								// array got bigger, it seems
								// addition occured before the current value
								next++;
							} else {
								// array got smaller
								// deletion occured before the current value
								next--;
							}
						} else {
							// addition/deletion was after the current value
							// so don't need to update 'next'
						}
					}

					// End of loop?
					if (next >= frame.locals[local2].getIntValue()) {
						frame.ip += offset;
					} else		// Get next element.
					{
						frame.locals[local3] = arr.isArray() ?
							arr.getElem(next) : arr;
						frame.locals[local1] = 
								new UsecodeValue.IntValue(next + 1);
					}
					break;
				}
				case 0x2f:		// ADDSV.
				{
					offset = (short) frame.Read2();
					if (offset < 0 || offset >= num_locals) {
						// LOCAL_VAR_ERROR(offset);
						break;
					}

					String str = frame.locals[offset].getStringValue();
					if (str != null)
						append_string(str);
					else		// Convert integer.
					{
					// 25-09-2001 - Changed to >= 0 to fix money-counting in SI.
					//				if (locals[offset].getIntValue() != 0) {
						if (frame.locals[offset].getIntValue() >= 0) {
							str = Integer.toString(frame.locals[offset].getIntValue());
							append_string(str);
						}
					}
					break;
				}
				case 0x30:		// IN.  Is a val. in an array?
				{
					UsecodeValue arr = pop();
					// If an array, use 1st elem.
					UsecodeValue val = pop().getElem0();
					pushi(arr.findElem(val) >= 0 ? 1 : 0);
					break;
				}
				case 0x31:		// Unknown.
				case 0xB1:		// (32 bit version)
				// this opcode only occurs in the 'audition' usecode function (BG)
				// not sure what it's supposed to do, but this function results
				// in the same behaviour as the original
					frame.ip += 2;
					if (opcode < 0x80)
						offset = (short)frame.Read2();
					else
						offset = EUtil.Read4(frame.fcode,frame.ip);
					
					if (!found_answer)
						found_answer = true;
					else
						frame.ip += offset;
					break;

				case 0x32:		// RET. (End of function reached)
				{
					show_pending_text();
					return_from_function(UsecodeValue.getZero());
					frame_changed = true;
					break;
				}
				case 0x33:		// SAY.
					say_string();
					break;
				case 0x38:		// CALLIS.
				{
					offset = frame.Read2();
					sval = frame.fcode[frame.ip++];  // # of parameters.
					UsecodeValue ival = call_intrinsic(frame.eventid,
														offset, sval);
					push(ival);
					frame_changed = true;
					break;
				}
				case 0x39:		// CALLI.
					offset = frame.Read2();
					sval = frame.fcode[frame.ip++]; // # of parameters.
					call_intrinsic(frame.eventid, offset, sval);
					frame_changed = true;
					break;
				case 0x3e:		// PUSH ITEMREF.
					pushref(frame.caller_item);
					break;
				case 0x3f:		// ABRT.
					show_pending_text();

					abort_function();
					frame_changed = true;
					aborted = true;
					break;
				case 0x40:		// end conversation
					found_answer = true;
					break;
				case 0x42:		// PUSHF.
				case 0xC2:		// PUSHF2.
					if (opcode > 0x80)
						offset = popi();
					else
						offset = (short) frame.Read2();
					if (offset < 0 || offset >= gflags.length) {
						// FLAG_ERROR(offset);
						pushi(0);
					} else {
						pushi(gflags[offset]);
					}
					break;
				case 0x43:		// POPF.
				case 0xC3:		// POPF2.
					if (opcode > 0x80)
						offset = popi();
					else
						offset = (short) frame.Read2();
					if (offset < 0 || offset >= gflags.length) {
						// FLAG_ERROR(offset);
					} else {
						gflags[offset] = popi() == 0 ? (byte)0 : (byte)1;
						/* ++++++LATER maybe
						if (gflags[offset]) {
							Notebook_gump::add_gflag_text(offset);
						}
						*/
						// ++++KLUDGE for Monk Isle:
						/*++++++++
						if (offset == 0x272 && Game::get_game_type() ==
							SERPENT_ISLE)
							gflags[offset] = 0;
						*/
					}
					break;
				case 0x44:		// PUSHB.
					pushi(frame.fcode[frame.ip++]);
					break;
				case 0x46:		// Set array element.
				case 0x5B:		// Set static array element.
				case 0x5E:		// Set class member array element.
				{
					UsecodeValue arr;
					offset = (short)frame.Read2();
					short index = (short)popi();
					index--;	// It's 1-based.
					UsecodeValue val = pop();
					if (opcode == 0x46) {
						
						// Get # of local array.
						if (offset < 0 || offset >= num_locals) {
							//LOCAL_VAR_ERROR(offset);
							break;
						}
						frame.locals[offset] = 
							UsecodeValue.ArrayValue.forceElem(
										frame.locals[offset], index, val);
					/* ++++++MAYBE LATER
					} else if (opcode == 0x5e) {
						UsecodeValue ths = frame.getThis();
						if (offset < 0 || offset >= ths.getClassVarCount()) {
							//cerr << "Class variable #" << (offset) << " out of range!";\
							//CERR_CURRENT_IP();
							break;
						}
						arr = ths.nthClassVar(offset);
					*/
					} else {
						if (offset < 0) {// Global static.
							if ((-offset) < statics.size()) {
								arr = statics.elementAt(-offset).putElem(index, val);
								statics.setElementAt(arr, -offset);
							} else {
								//cerr << "Global static variable #" << (offset) << " out of range!";\
								//CERR_CURRENT_IP();
								break;
							}
						} else {
							if (offset < frame.function.statics.size()) {
								arr = frame.function.statics.elementAt(offset);
								arr = UsecodeValue.ArrayValue.forceElem(
															arr, index, val);
								frame.function.statics.setElementAt(arr, offset);
							} else {
								//cerr << "Local static variable #" << (offset) << " out of range!";\
								//CERR_CURRENT_IP();
								break;
							}
						}
					}
					break;
				}
				case 0x47:		// CALLE.  Stack has caller_item.
				case 0xc7:		// 32-bit version.
				{
					UsecodeValue ival = pop();
					GameObject caller = get_item(ival);
					push(ival); // put caller_item back on stack
					if (opcode < 0x80)
						offset = frame.Read2();
					else
						offset = EUtil.Read4(frame.fcode,frame.ip);
					call_function(offset, frame.eventid, caller, false, false);
					frame_changed = true;
					break;
				}
				case 0x48:		// PUSH EVENTID.
					pushi(frame.eventid);
					break;
				case 0x4a:		// ARRA.
				{
					UsecodeValue val = pop();
					UsecodeValue arr = pop();
					push(arr.concat(val));
					break;
				}
				case 0x4b:		// POP EVENTID.
					frame.eventid = popi();
					break;
				case 0x4c: // debugging opcode from spanish SI (line number)
				{
					frame.line_number = frame.Read2();
					break;
				}
				/*
				case 0x4d: // debugging opcode from spanish SI (function init)
				case 0xcd: // 32 bit debugging function init
				{
					int funcname;
					int paramnames;
					if (opcode < 0x80)
					{
						funcname = frame.Read2();
						paramnames = frame.Read2();
					}
					else
					{
						funcname = (sint32)EUtil.Read4(frame.fcode,frame.ip);
						paramnames = (sint32)EUtil.Read4(frame.fcode,frame.ip);
					}
					if (funcname < 0 || frame.data + funcname >= frame.externs-6)
					{
						DATA_SEGMENT_ERROR();
						break;
					}
					if (paramnames < 0 || frame.data + paramnames >= frame.externs-6)
					{
						DATA_SEGMENT_ERROR();
						break;
					}
					cout << "Debug opcode found at function = " << hex << setw(4)
						 << setfill('0') << frame.function.id << ", ip = "
						 << current_IP << dec << setfill(' ') << "." << endl;
					cout << "Information is: funcname = '"
						// This is a complete guess:
					     << (char*)(frame.data + funcname) << "'." << endl;
					char *ptr = (char*)(frame.data + paramnames);
						// This is an even bigger complete guess:
					if (*ptr)
					{
						int nargs = frame.num_args;
						if (is_object_fun(frame.function.id))
							nargs--;	// Function has an 'item'.
						if (nargs < 0)	// Just in case.
							nargs = 0;
						std::vector<std::string> names;
						names.resize(nargs);
						int i;
							// Reversed to match the order in which they are
							// passed in UCC.
						for (i = nargs-1; i >= 0 && *ptr; i--)
						{
							std::string name(ptr);
							names[i] = name;
							ptr += name.length() + 1;
						}
						cout << "Parameter names follow: ";
						for (i = 0; i < nargs; i++)
						{
							cout << "#" << hex << setw(4) << setfill('0')
								 << i << " = ";
							if (names[i].length())
								cout << "'" << names[i] << "'";
							else
								cout << "(missing)";
							if (i < nargs)
								cout << ", ";
						}
						cout << endl << "Variable names follow: ";
						for (i = 0; i < frame.num_vars && *ptr; i++)
						{
							std::string name(ptr);
							ptr += name.length() + 1;
							cout << "#" << hex << setw(4) << setfill('0')
								 << (i + nargs) << " = ";
							if (name.length())
								cout << "'" << name << "'";
							else
								cout << "(missing)";
							if (i < frame.num_vars)
								cout << ", ";
						}
						for (; i < frame.num_vars; i++)
						{
							cout << "#" << hex << setw(4) << setfill('0')
								 << (i + nargs) << " = (missing)";
							if (i < frame.num_vars)
								cout << ", ";
						}
					}
					else
						cout << endl;
					break;
				}
				*/
				case 0x50:		// PUSH static.
					offset = (short)frame.Read2();
					if (offset < 0) {// Global static.
						if ((-offset) < statics.size())
							push(statics.elementAt(-offset));
						else
							pushi(0);
					} else {
						if (offset < frame.function.statics.size())
							push(frame.function.statics.elementAt(offset));
						else
							pushi(0);
					}
					break;
				case 0x51:		// POP static.
				{
					offset = (short)frame.Read2();
					// Get value.
					UsecodeValue val = pop();
					if (offset < 0) {
						if ((-offset) >= statics.size())
							statics.setSize(-offset + 1);
						statics.setElementAt(val, -offset);
					} else {
						if (offset >= frame.function.statics.size())
							frame.function.statics.setSize(offset + 1);
						frame.function.statics.setElementAt(val, offset);;
					}
				}
					break;
				case 0x52:		// CALLO (call original).
				{			// Otherwise, like CALLE.
					UsecodeValue ival = pop();
					GameObject caller = get_item(ival);
					push(ival); // put caller_item back on stack

					offset = frame.Read2();
					call_function(offset, frame.eventid, caller,
									false, true);
					frame_changed = true;
					break;
				}
				case 0x53:		// CALLIND:  call indirect.
				{			//  Function # is on stack.
					UsecodeValue funval = pop();
					offset = funval.getIntValue();
					UsecodeValue ival = pop();
					GameObject caller = get_item(ival);
					call_function(offset, frame.eventid, caller, false, false);
					frame_changed = true;
					break;
				}
				/* +++++++++LATER
				case 0x54:		// PUSH class this.var.
				{
					offset = frame.Read2();
					UsecodeValue& ths = frame.get_this();
					push(ths.nth_class_var(offset));
					break;
				}
				case 0x55:		// POP class this.var.
				{
					// Get value.
					UsecodeValue val = pop();
					offset = frame.Read2();
					UsecodeValue& ths = frame.get_this();
					ths.nth_class_var(offset) = val;
					break;
				}
				case 0x56:		// CALLM - call method, use var vtable.
				case 0x57:		// CALLMS - call method, use pushed vtable.
				{
					offset = frame.Read2();
					Usecode_class_symbol *c;
					if (opcode == 0x56)
						{
						UsecodeValue thisptr = peek();
						c = thisptr.get_class_ptr();
						}
					else
						{
						UsecodeValue thisptr = frame.Read2();
						c = get_class(thisptr.getIntValue());
						}
					if (!c) {
						THIS_ERROR();
						(void) pop();
						break;
					}
					int index = c.get_method_id(offset);
					call_function(index, frame.eventid);
					frame_changed = true;
					break;
				}
				case 0x58:		// CLSCREATE
				{
					int cnum = frame.Read2();
					Usecode_class_symbol *cls = symtbl.get_class(cnum);
					if (!cls) {
						cerr << "Can't create obj. for class #" << cnum << endl;
						pushi(0);
						break;
					}
					int cnt = cls.get_num_vars();
					UsecodeValue new_class = UsecodeValue(0);
					new_class.class_new(cls, cnt);

					int to = 0;	// Store at this index.
					// We are trusting UCC output here.
					while (cnt--)
					{
						UsecodeValue val = pop();
						new_class.nth_class_var(to++) = val;
					}
					push(new_class);
					break;
				}
				
				case 0x59:		//CLASSDEL
				{
					UsecodeValue cls = pop();
					cls.class_delete();
					break;
				}
				*/
				case 0x60:		// PUSHCHOICE
					pushs(conv.getUserChoice());
					break;
				default:
					System.out.println("Opcode " + opcode + " not known. ");
					// +++++ CERR_CURRENT_IP();
					break;
				}
			}		
		}
		if (callStack.getFirst() == null) {
			// pop the NULL frame from the stack
			callStack.removeFirst();
		}
		return !aborted;
	}
	/*
	 *	Get user's choice from among the possible responses.
	 *
	 *	Output:	.user choice string.
	 *		0 if no possible choices or user quit.
	 */
	public String get_user_choice() {
		if (conv.getNumAnswers() == 0)
			return null;		// This does happen (Emps-honey).
		//	if (!user_choice)		// May have already been done.
		// (breaks conversation with Cyclops on Dagger Isle ('foul magic' option))
		get_user_choice_num();
		return (conv.getUserChoice());
	} 
	/*
	 *	Get user's choice from among the possible responses.
	 *
	 *	Output:	User choice is set, with choice # returned.
	 *		-1 if no possible choices.
	 */
	public int get_user_choice_num() {
		conv.setUserChoice(null);
		conv.showAvatarChoices();
					// Get click.
		int choice_num;
		do {
			//UNUSED char chr;		// Allow '1', '2', etc.
			gwin.paint();		// Paint scenery.
			ExultActivity.getClick(clickPoint);
			/*  +++++++++
			int result=Get_click(x, y, Mouse::hand, &chr, false, conv, true);
			if (result<=0) {	// ESC pressed, select 'bye' if poss.
				choice_num = conv.locate_answer("bye");
			} else if (chr) {		// key pressed
				if (chr>='1' && chr <='0'+conv.get_num_answers()) {
					choice_num = chr - '1';
				} else
					choice_num = -1;	//invalid key
			} else */
				choice_num = conv.conversationChoice(clickPoint.x, clickPoint.y);
		}
						// Wait for valid choice.
		while (choice_num  < 0 || choice_num >= conv.getNumAnswers());

		conv.clearAvatarChoices();
						// Store .answer string.
		conv.setUserChoice(conv.getAnswer(choice_num));
		return (choice_num);		// Return choice #.
		}

	private UsecodeFunction findFunction(int funcid) {
		UsecodeFunction fun;
		// locate function
		int slotnum = funcid/0x100;
		if (slotnum >= funs.size())
			fun = null;
		else {
			Vector<UsecodeFunction> slot = funs.elementAt(slotnum);
			int index = funcid%0x100;
			fun = index < slot.size() ? slot.elementAt(index) : null;
		}
		if (fun == null) {
			// Error?
		}
		return fun;
	}
	private final boolean is_object_fun(int n) {
		// +++++++ if (symtbl == null)
			return (n < 0x800);
		// ++++ return symtbl.is_object_fun(n);
	}
	private boolean call_function(int funcid, int eventid) {
		return call_function(funcid, eventid, null, false, false);
	}
	private boolean call_function(int funcid,
				 int eventid,
				 GameObject caller,
				 boolean entrypoint, boolean orig) {
		UsecodeFunction fun = findFunction(funcid);
		if (fun == null)
			return false;
		if (orig && fun != fun.orig) {
			return false;
		}
		int depth, oldstack, chain;
		if (entrypoint) {
			depth = 0;
			oldstack = 0;
			chain = StackFrame.getCallChainID();
		} else {
			StackFrame parent = callStack.getFirst();
			// find new depth
			depth = parent.call_depth + 1;
			// find number of elements available to pop from stack (as arguments)
			oldstack = sp - parent.save_sp;
			chain = parent.call_chain;
			if (caller == null)
				caller = parent.caller_item; // use parent's
		}
		StackFrame frame = new StackFrame(fun, eventid, caller, chain, depth);
		int num_args = frame.num_args;
		// Many functions have 'itemref' as a 'phantom' arg.
		// In the originals, this was probably so that the games
		// could know how much memory the function would need.
		if (is_object_fun(funcid)) {
			if (--num_args < 0) {
				// Backwards compatibility with older mods.
				num_args = 0;
			}
		}
		while (num_args > oldstack) { // Not enough args pushed?
			pushi(0); // add zeroes
			oldstack++;
		}
		int i;
		// Store args in first num_args locals
		
		for (i = 0; i < num_args; i++) {
			UsecodeValue val = pop();
			frame.locals[num_args - i - 1] = val;
		}
		if (debug) {
			System.out.printf("Running usecode %1$04x ()", funcid);
			for (i = 0; i < num_args; i++) {
				if (i > 0)
					System.out.printf(", ");
				System.out.printf(frame.locals[i].toString());
			}
			System.out.println(")");
		}
		// save stack pointer
		frame.save_sp = sp;
		// add new stack frame to top of stack
		callStack.addFirst(frame);
		return true;
	}
	private void previous_stack_frame() {
		// remove current frame from stack
		StackFrame frame = callStack.removeFirst();

		// restore stack pointer
		sp = frame.save_sp;

		if (frame.call_depth == 0) {
			// this was the function called from 'the outside'
			// push a marker (NULL) for the interpreter onto the call stack,
			// so it knows it has to return instead of continuing
			// further up the call stack
			callStack.addFirst(null);
		}
	}
	private void return_from_function(UsecodeValue retval) {
		int oldfunction = callStack.getFirst().function.id;	// For debug.
		// back up a stack frame
		previous_stack_frame();
		// push the return value
		push(retval);
		if (debug) {
			StackFrame parent_frame = callStack.getFirst();
			System.out.printf("Returning (%1$s) from usecode %2$04x\n",
					retval.toString(), oldfunction);
			if (parent_frame != null) {
				int newfunction = callStack.getFirst().function.id;
				System.out.printf("...back into usecode %1$04x\n", newfunction);
			}
		}
	}
	private void return_from_procedure() {
		// back up a stack frame
		previous_stack_frame();
	}
	private void abort_function() {
		if (debug) {
			int funid = callStack.getFirst().function.id;
			System.out.printf("Aborting from usecode %1$04x\n", funid);
		}
		// clear the entire call stack up to the entry point
		while (callStack.getFirst() != null)
			previous_stack_frame();
	}
	private void append_string(String str) {
		if (str == null)
			return;
		if (theString != null)
			theString = theString + str;
		else
			theString = str;
	}
	// Push/pop stack.
	private void push(UsecodeValue val) {
		stack[sp++] = val;
	}
	private UsecodeValue pop() { 
		if (sp <= 0) {		// Happens in SI #0x939
			System.out.println("Stack underflow");
			return new UsecodeValue.IntValue(0);
		}
		UsecodeValue ret = stack[--sp];
		stack[sp] = null;
		return ret;
	}
	/* UNUSED
	private UsecodeValue peek() {
		return stack[sp-1];
	} */
	private void pushref(GameObject obj) {
		push(new UsecodeValue.ObjectValue(obj));
	} 
	private void pushi(int val) {		// Push/pop integers.
		push(new UsecodeValue.IntValue(val));
	}

	private int popi()
	{
		UsecodeValue val = pop();
		return val.needIntValue();
	}

	// Push/pop strings.
	private void pushs(String s) {
		push(new UsecodeValue.StringValue(s));
	}
	/*
	 *	Get a game object from an "itemref", which might be the actual
	 *	pointer, or might be -(npc number).
	 *
	 *	Output:	.game object.
	 */
	public GameObject get_item(UsecodeValue itemref) {
						// If array, take 1st element.
		UsecodeValue elemval = itemref.getElem0();
		GameObject obj = elemval.getObjectValue();
		if (obj != null)
			return obj;

		int val = elemval.getIntValue();
		if (val == 0)
			return null;
		
		if (val == -356)		// Avatar.
			return gwin.getMainActor();
		else if (val < -356 && val > -360)	// Special cases.
			return null;
		if (val < 0 && val > -gwin.getNumNpcs())
			obj = gwin.getNpc(-val);
		else if (val >= 0)
			{			// Special case:  palace guards, Time Lord.
			if (val < 0x400 && !itemref.isArray() &&
				caller_item != null && val == caller_item.getShapeNum())
				obj = caller_item;
			else
				return null;
			}
		return obj;
		}

	/*
	 *	Make sure pending text has been seen.
	 */
	public void show_pending_text() {
		if (book != null) {			// Book mode?
			while (book.showNextPage()) /* ++++++ && 
					Get_click(x, y, Mouse::hand, 0, false, book, true)) */
				ExultActivity.getClick(clickPoint);
			gwin.setAllDirty();
		}
						// Normal conversation:
		else if (conv.isNpcTextPending()) {
			System.out.println("show_pending_text: waiting for click");
			click_to_continue();
		}
	}
	/*
	 *	Show book or scroll text.
	 */
	private void show_book() {
		String str = theString;
		book.addText(str);
		theString = null;
	}
	/*
	 *	Say the current string and empty it.
	 */
	private void say_string() {
		if (theString == null)
			return;
		if (book != null) {		// Displaying a book?
			show_book();
			return;
		}
		show_pending_text();		// Make sure prev. text was seen.
		String str = theString;
		int ind = 0;
		char c;			// Look for stopping points ("~~").
		while (ind < str.length() && (c = str.charAt(ind)) != 0) {
			if (c == '*') {	// Just gets an extra click.
				click_to_continue();
				ind++;
				continue;
				}
			int eol = str.indexOf('~', ind);
			if (eol < 0) {		// Not found?
				conv.showNpcMessage(str);
				
				click_to_continue();
				break;
			}
			String text = str.substring(ind, eol);
			conv.showNpcMessage(text);
			if (debug)
				System.out.printf("say_string: %1$s\n", text);
			click_to_continue();
			if (eol < str.length() - 1 && str.charAt(eol + 1) == '~')
				++eol;		// 2 in a row.
			str = str.substring(eol + 1, str.length());
			ind = 0;
		}
		theString = null;
	}
	private UsecodeValue call_intrinsic
		(
		int event,			// Event type.
		int intrinsic,			// The ID.
		int num_parms			// # parms on stack.
		) {
		
		//UNUSED UsecodeValue parms[] = new UsecodeValue[num_parms];	// Get parms.
		for (int i = 0; i < num_parms; i++) {
			UsecodeValue val = pop();
			intrinsicParms[i] = val;
		}
		if (debug) {
			System.out.printf("Intrinsic %1$02x(", intrinsic);
			for (int i = 0; i < num_parms; i++) {
				if (i > 0)
					System.out.printf(", ");
				System.out.printf(intrinsicParms[i].toString());
			}
			System.out.println(")");
		}
		UsecodeValue ret;
		ret = intrinsics.execute(intrinsic, event, num_parms, intrinsicParms);
		if (debug)
			System.out.printf("...returned %1$s\n", ret);
		return ret;
	}
	/*
	 *	Wait for user to click inside a conversation.
	 */
	private void click_to_continue() {
		//UNUSED char c;
		/* +++++ if (!gwin.getPal().is_faded_out()) */ // If black screen, skip!
			{
			gwin.paint();		// Repaint scenery.
			ExultActivity.getClick(clickPoint);
			}
		conv.clearTextPending();
		//	user_choice = 0;		// Clear it.
		}
	/*
	 *	Read in global data from 'gamedat/usecode.dat'.
	 *	(and 'gamedat/keyring.dat')
	 */
	public void read() {
		/* +++++++++++FINISH
		if (game.isSI())
			keyring.read();	// read keyring data
		*/
		InputStream in;
		try {
			in = EUtil.U7openStream(EFile.FLAGINIT);	// Read global flags.
			int filesize = in.available();
			if (filesize > gflags.length)
				filesize = gflags.length;
			Arrays.fill(gflags, (byte)0);
			in.read(gflags, 0, filesize);
			System.out.println("Usecode: Read " + filesize + " global flags.");
			in.close();
		} catch(IOException e) {
			ExultActivity.fileFatal(EFile.FLAGINIT);
		}

		clear_usevars(); // first clear all statics
		try
		{
			in = EUtil.U7openStream(EFile.USEVARS);
			read_usevars(in);
			in.close();
		}
		catch (IOException e) {
			;			// Okay if this doesn't exist.
		}
		try {
			in = EUtil.U7openStream(EFile.USEDAT);
		} catch (IOException e) {
			partyman.setCount(0);
			partyman.linkParty();	// Still need to do this.
			return;			// Not an error if no saved game yet.
		}
		partyman.setCount(EUtil.Read2(in));	// Read party.
		int i;	// Blame MSVC
		for (i = 0; i < PartyManager.EXULT_PARTY_MAX; i++)
			partyman.setMember(i, EUtil.Read2(in));
		partyman.linkParty();
						// Timers.
		int cnt = EUtil.Read4(in);
		if (cnt == -1) {
			int tmr = 0;
			while ((tmr = EUtil.Read2(in)) != 0xffff)
				timers.put(tmr, EUtil.Read4(in));
		} else {
			timers.put(0, cnt);
			for (int t = 1; t < 20; t++)
				timers.put(t, EUtil.Read4(in));
		}
		if (saved_pos == null)
			saved_pos = new Tile();
		saved_pos.tx = (short)EUtil.Read2(in);	// Read in saved position.
		saved_pos.ty = (short)EUtil.Read2(in);
		saved_pos.tz = (short)EUtil.Read2(in);
		saved_map = EUtil.Read2(in);
	}
	/*
	 *	Read in static variables from USEVARS.
	 */
	private void read_usevars(InputStream in) throws IOException {
		int cnt = EUtil.Read4(in);		// Global statics.
		if (statics == null)
			statics = new Vector<UsecodeValue>(cnt);
		statics.setSize(cnt);
		int i;
		for (i = 0; i < cnt; i++)
			statics.setElementAt(UsecodeValue.restore(in), i);
		long funid;
		while (in.available() != 0 && (funid = EUtil.Read4(in)) != 0xffffffff) {
			if (funid == 0xfffffffe) {
				// ++++ FIXME: Write code for the cases when symtbl == 0 or
				// fsym == 0 (neither of which *should* happen...)
				int len = EUtil.Read2(in);
				byte nm[] = new byte[len + 1];
				in.read(nm, 0, len);
				nm[len] = 0;
				/* +++++++++FINISH
				Usecode_symbol *fsym = symtbl ? (*symtbl)[nm] : 0;
				if (fsym)
					funid = fsym->get_val();
				delete [] nm;
				*/
			}
			cnt = EUtil.Read4(in);
			UsecodeFunction fun = findFunction((int)funid);
			if (fun == null)
				continue;
			fun.statics.setSize(cnt);
			for (i = 0; i < cnt; i++)
				fun.statics.setElementAt(UsecodeValue.restore(in), i);
			}
		}
	private void clear_usevars() {
		if (statics != null)
			statics.clear();
		int nslots = funs.size();
		for (int i = 0; i < nslots; ++i) {
			Vector<UsecodeFunction> slot = funs.elementAt(i);
			for (int j = 0; j < slot.size(); ++j) {
				UsecodeFunction fun = slot.elementAt(j);
				if (fun != null && fun.statics != null) 
					fun.statics.clear();
			}
		}
	}
	/*
	 *	Write out global data to 'gamedat/usecode.dat'.
	 *	(and 'gamedat/keyring.dat')
	 */

	public void write() throws IOException {
						// Assume new games will have keyring.
		/* ++++++++++FINISH
		if (!game.isBG())
			keyring.write();	// write keyring data
		*/
		OutputStream out = EUtil.U7create(EFile.FLAGINIT);	// Write global flags.
		out.write(gflags);
		out.close();
		out = EUtil.U7create(EFile.USEDAT);
		EUtil.Write2(out, partyman.getCount());	// Write party.
		int i;	// Blame MSVC
		for (i = 0; i < PartyManager.EXULT_PARTY_MAX; i++)
			EUtil.Write2(out, partyman.getMember(i));
						// Timers.
		EUtil.Write4(out, 0xffffffff);
		for (TreeMap.Entry<Integer,Integer> entry : timers.entrySet()) {
			  Integer key = entry.getKey();
			  Integer value = entry.getValue();
			  if (value != 0) {	// Don't write unused timers.
				  EUtil.Write2(out, key);
				  EUtil.Write4(out, value);
			  }
		}
		EUtil.Write2(out, 0xffff);
		EUtil.Write2(out, saved_pos == null ? -1 : saved_pos.tx);	// Write saved pos.
		EUtil.Write2(out, saved_pos == null ? -1 : saved_pos.ty);
		EUtil.Write2(out, saved_pos == null ? -1 : saved_pos.tz);
		EUtil.Write2(out, saved_map);		// Write saved map.
		out.close();
		out = EUtil.U7create(EFile.USEVARS);		// Static variables. 1st, globals.
		int cnt = statics != null ? statics.size() : 0;
		EUtil.Write4(out, cnt);	// # globals.
		for (i = 0; i < cnt; ++i)
			statics.elementAt(i).save(out);
						// Now do the local statics.
		/* +++++++++FINISH
		int num_slots = funs.size();
		for (i = 0; i < num_slots; i++) {
			Vector<UsecodeFunction> slot = funs.elementAt(i);
			cnt = slot.size();
			for (int j = 0; j < cnt; ++j) {
				UsecodeFunction fun = slot.elementAt(j);
				if (fun == null || fun.statics.isEmpty())
					continue;
				UsecodeSymbol *fsym = symtbl ? (*symtbl)[fun->id] : 0;
				if (fsym)
					{
					const char *nm = fsym.getName();
					EUtil.Write4(out, 0xfffffffe);
					nfile->write2(strlen(nm));
					nfile->write(const_cast<char *>(nm), strlen(nm));
					}
				else
					EUtil.Write4(out, fun->id);
				EUtil.Write4(out, fun->statics.size());
				for (it = fun->statics.begin();
						it != fun->statics.end(); ++it)
					{
					if (!(*it).save(nfile))
						throw file_exception("Could not write static usecode value");
					}
				}
			}
		*/
		EUtil.Write4(out, 0xffffffff);	// End with -1.
		out.close();
	}
	public void interceptClickOnItem(GameObject obj) {
		intrinsics.interceptClickOnItem(obj);
	} 
	public GameObject getInterceptClickOnItem()
		{ return intrinsics.getInterceptClickOnItem(); }
	public void interceptClickOnTile(Tile t) {
		intrinsics.interceptClickOnTile(t);
	}
	public Tile getInterceptClickOnTile() {
		return intrinsics.getInterceptClickOnTile();
	}
	public void restoreIntercept(GameObject obj, Tile t) {
		intrinsics.restoreIntercept(obj, t);
	}
	public int getTelekenesisFun() {
		return intrinsics.getTelekenesisFun();
	}
	public void setTelekenesisFun(int f) {
		intrinsics.setTelekenesisFun(f);
	}
	/*
	 * One Usecode function.
	 */
	public static class UsecodeFunction {
		int id;			// Function #.
		UsecodeFunction orig;	// Orig., if this was from a patch.
		byte code[];	// What gets executed.
		boolean extended;	// 32-bit function.
		Vector<UsecodeValue> statics;	// Local statics.
		UsecodeFunction(InputStream file) throws IOException {
			id = EUtil.Read2(file);
			int len;
			// support for our extended usecode format. (32 bit lengths and ids)
			if (id == 0xfffe) {
				id = EUtil.Read4(file);
				len = EUtil.Read4(file);
				extended = true;
			// older extended usecode format. (32 bit lengths)
			} else if (id == 0xffff) {
				id = EUtil.Read2(file);
				len = EUtil.Read4(file);
				extended = true;
			} else {
				len = EUtil.Read2(file);
				extended = false;
			}

			code = new byte[len];	// Allocate buffer & read it in.
			file.read(code);
		}
	}
	/*
	 * Stack frame for function calls.
	 */
	public static class StackFrame {
		UsecodeFunction function;
		byte[] fcode;
		int ip; // current IP
		int data; // pointer to start of data segment
		int externs; // pointer to start of externs
		int code; // pointer to (actual) code
		int endp; // pointer directly past code segment
		int line_number; // if debugging info present
		int call_chain; // unique ID for this call chain
		int call_depth; // zero for top level function
		int num_externs;
		int num_args;
		int num_vars;
		UsecodeValue locals[];
		int eventid;
		GameObject caller_item;
		int save_sp;

		UsecodeValue getThis()
			{ return locals[num_args - 1]; }
		static int LastCallChainID;
		static int getCallChainID() { return ++LastCallChainID; }
		StackFrame(UsecodeFunction fun, int event, GameObject caller,
												int chain, int depth) {
			function = fun;
			fcode = function.code;
			line_number = -1;
			eventid = event;
			call_chain = chain;
			call_depth = depth;
			caller_item = caller;
			ip = 0;
			endp = fcode.length;
			int data_len;
			if (!fun.extended) {
				data_len = EUtil.Read2(fcode, 0);	// Get length of (text) data.
				ip += 2;
			} else {
				data_len = EUtil.Read4(fcode, 0); // 32 bit lengths
				ip += 4;
			}
			data = ip;
			ip += data_len;			// Point past text.
			num_args = EUtil.Read2(fcode, ip);	// # args. this function takes.
			// Local variables follow args.
			num_vars = EUtil.Read2(fcode, ip + 2);
			ip += 4;
			// Allocate locals.
			int num_locals = num_vars + num_args;
			locals = new UsecodeValue[num_locals];
			num_externs = EUtil.Read2(fcode, ip); // external function references
			ip += 2;
			externs = ip;
			ip += 2 * num_externs; // now points to actual code
			code = ip;
		}
		final int Read2() {
			ip += 2;
			return EUtil.Read2(fcode, ip - 2);
		}
		final int Read4() {
			ip += 4;
			return EUtil.Read2(fcode, ip - 4);
		}
		String getDataString(int offset) {
			int cnt;
			for (cnt = 0; fcode[data + offset + cnt] != 0; ++cnt)
				;
			return new String(fcode, data + offset, cnt);
		}
	}
	
}
