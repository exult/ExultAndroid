package com.exult.android;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.LinkedList;
import java.util.TreeMap;

public abstract class UsecodeMachine extends GameSingletons {
	private boolean gflags[] = new boolean[EConst.c_last_gflag + 1];	// Global flags.
	// ++++ protected Conversation conv;		// Handles conversations.
	// Functions: I'th entry contains funs for ID's 256*i + n.
	private Vector<Vector<UsecodeFunction>> funs = 
				new Vector<Vector<UsecodeFunction>>();
	private LinkedList<StackFrame> callStack = new LinkedList<StackFrame>();
	private StackFrame frame;
	private TreeMap<Integer, Integer> timers = new TreeMap<Integer,Integer>();
	private GameObject caller_item;
	private Vector<GameObject> last_created;// Stack of last items created with 
											//   intrins. x24.
	private Actor path_npc;		// Last NPC in path_run_usecode().
	private String user_choice;	// String user clicked on.
	private boolean found_answer;		// Did we already handle the conv. option?
	private Tile saved_pos;		// For a couple SI intrinsics.
	private String theString;	// The single string register.
	private UsecodeValue stack[] = new UsecodeValue[1024];
	private int sp;				// Stack-pointer index.
	
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
	public final boolean getGlobalFlag(int i)	// Get/set ith flag.
		{ return gflags[i]; }
	public final void setGlobalFlag(int i, int val)
		{ gflags[i] = (val == 1); }
	public final void readUsecode(InputStream file, boolean patch) throws IOException {
		int size = file.available();	// Get file size.
		file.mark(16);
		long magic = EUtil.Read4(file);	// Test for symbol table.
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

				int current_IP = frame.ip - frame.code;
				int opcode = frame.fcode[frame.ip];
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
						offset = EUtil.Read2(frame.fcode, frame.ip);
					else
						offset = EUtil.Read4(frame.fcode, frame.ip);
					
					found_answer = false;
					if (get_user_choice() == null)  // Exit conv. if no choices.
						frame.ip += offset; // (Emps and honey.)
					break;
				}
				case 0x05:		// JNE.
				{
					offset = (short) EUtil.Read2(frame.fcode, frame.ip);
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
					offset = (short) EUtil.Read2(frame.fcode, frame.ip);
					frame.ip += offset;
					break;
				case 0x86:		// JMP32
					offset = EUtil.Read4(frame.fcode,frame.ip);
					frame.ip += offset;
					break;
				case 0x07:		// CMPS.
				case 0x87: // (32 bit version)
				{
					int cnt = EUtil.Read2(frame.fcode, frame.ip);	// # strings.
					if (opcode < 0x80)
						offset = (short) EUtil.Read2(frame.fcode, frame.ip);
					else
						offset = EUtil.Read4(frame.fcode,frame.ip);
					
					boolean matched = false;
					
					// only try to match if we haven't found an answer yet
					while (!matched && !found_answer && cnt-- > 0) {
						UsecodeValue s = pop();
						String str = s.getStringValue();
						if (str != null && str.equals(user_choice)) {
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
					offset = EUtil.Read2(frame.fcode, frame.ip);
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
					pushi(!(val1 == val2) ? 1 : 0);
					break;
				}
				case 0x1c:		// ADDSI.
					offset = EUtil.Read2(frame.fcode, frame.ip);
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
					offset = EUtil.Read2(frame.fcode, frame.ip);
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
				/*
				case 0x1e:		// ARRC.
				{		// Get # values to pop into array.
					int num = EUtil.Read2(frame.fcode, frame.ip);
					int cnt = num;
					UsecodeValue.ArrayUsecodeValue arr = 
								new UsecodeValue.ArrayUsecodeValue(num);
					int to = 0;	// Store at this index.
					while (cnt-- > 0) {
						UsecodeValue val = pop();
						to += arr.add_values(to, val);
					}
					if (to < num)// 1 or more vals empty arrays?
						arr.resize(to);
					push(arr);
				}
				break;/*++++++++++++++
				case 0x1f:		// PUSHI.
				{		// Might be negative.
					short ival = EUtil.Read2(frame.fcode, frame.ip);
					pushi(ival);
					break;
				}
				case 0x9f:		// PUSHI32
				{
					int ival = (sint32)EUtil.Read4(frame.fcode,frame.ip);
					pushi(ival);
					break;
				}
				case 0x21:		// PUSH.
					offset = EUtil.Read2(frame.fcode, frame.ip);
					if (offset < 0 || offset >= num_locals) {
						LOCAL_VAR_ERROR(offset);
						pushi(0);
					}
					else {
						push(frame.locals[offset]);
					}
					break;
				case 0x22:		// CMPEQ.
				{
					UsecodeValue val1 = pop();
					UsecodeValue val2 = pop();
					pushi(val1 == val2);
					break;
				}
				case 0x24:		// CALL.
				{
					offset = EUtil.Read2(frame.fcode, frame.ip);
					if (offset < 0 || offset >= frame.num_externs) {
						EXTERN_ERROR();
						break;
					}
						
					uint8 *tempptr = frame.externs + 2*offset;
					int funcid = Read2(tempptr);

					call_function(funcid, frame.eventid);
					frame_changed = true;
					break;
				}
				case 0xa4:		// 32-bit CALL.
				{
					offset = (sint32)EUtil.Read4(frame.fcode,frame.ip);
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
					UsecodeValue *val;
					if (opcode == 0x26) {
						offset = EUtil.Read2(frame.fcode, frame.ip);
						if (offset < 0 || offset >= num_locals) {
							LOCAL_VAR_ERROR(offset);
							pushi(0);
							break;
						}
						val = &(frame.locals[offset]);
					} else if (opcode == 0x5d) {
						offset = EUtil.Read2(frame.fcode, frame.ip);
						UsecodeValue& ths = frame.get_this();
						if (offset < 0 || offset >= ths.get_class_var_count()) {
							cerr << "Class variable #" << (offset) << " out of range!";\
							CERR_CURRENT_IP();
							break;
						}
						val = &(ths.nth_class_var(offset));
					} else {
						offset = (sint16)EUtil.Read2(frame.fcode, frame.ip);
						if (offset < 0) {// Global static.
							if ((unsigned)(-offset) < statics.size())
								val = &(statics[-offset]);
							else {
								cerr << "Global static variable #" << (offset) << " out of range!";\
								pushi(0);
								break;
							}
						} else {
							if ((unsigned)offset < frame.function.statics.size())
								val = &(frame.function.statics[offset]);
							else {
								cerr << "Local static variable #" << (offset) << " out of range!";\
								pushi(0);
								break;
							}
						}
					}
					if (sval < 0) {
						cerr << "AIDX: Negative array index: " << sval << endl;
						pushi(0);
						break;
					}
					if (val.is_array() && sval >= val.get_array_size())
						pushi(0);	// Matches originals.
					else if (sval == 0) // needed for SS keyring (among others)
						push(val.get_elem0());
					else
						push(val.get_elem(sval));
					break;
				}
				case 0x2d:		// RET. (Return from function)
				{
					// ++++ Testing.
					show_pending_text();
					UsecodeValue r = pop();

					return_from_function(r);
					frame_changed = true;
					break;
				}
				case 0x2e:		// INITLOOP (1st byte of loop)
				case 0xae:		// (32 bit version)   
				{
					int nextopcode = *(frame.ip);
					// No real reason to have 32-bit version of this instruction;
					// keeping it for backward compatibility only.
	#if 0
					if ((opcode & 0x80) != (nextopcode & 0x80)) {
						cerr << "32-bit instruction mixed with 16-bit instruction in loop usecode!" << endl;
						break;
					}
	#endif
					nextopcode &= 0x7f;
					if (nextopcode != 0x02 && nextopcode != 0x5c &&
							nextopcode != 0x5f) {
						cerr << "Invalid 2nd byte in loop!" << endl;
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
					int local1 = EUtil.Read2(frame.fcode, frame.ip);
					// Total count.
					int local2 = EUtil.Read2(frame.fcode, frame.ip);
					// Current value of loop var.
					int local3 = EUtil.Read2(frame.fcode, frame.ip);
					// Array of values to loop over.
					int local4;
					boolean is_32bit = (opcode > 0x80);
					// Mask off 32bit flag.
					opcode &= 0x7f;
					if (opcode == 0x5C)
						local4 = (sint16)EUtil.Read2(frame.fcode, frame.ip);
					else
						local4 = EUtil.Read2(frame.fcode, frame.ip);
					// Get offset to end of loop.
					if (is_32bit)
						offset = (sint32) EUtil.Read4(frame.fcode,frame.ip); // 32 bit offset
					else
						offset = (short) EUtil.Read2(frame.fcode, frame.ip);


					if (local1 < 0 || local1 >= num_locals) {
						LOCAL_VAR_ERROR(local1);
						break;
					}
					if (local2 < 0 || local2 >= num_locals) {
						LOCAL_VAR_ERROR(local2);
						break;
					}
					if (local3 < 0 || local3 >= num_locals) {
						LOCAL_VAR_ERROR(local3);
						break;
					}
					if (opcode == 0x5c) {
						if (local4 < 0) {// Global static.
							if ((unsigned)(-local4) >= statics.size()) {
								cerr << "Global static variable #" << (-local4) << " out of range!";\
								CERR_CURRENT_IP();
								break;
							}
						} else {
							if ((unsigned)local4 >= frame.function.statics.size()) {
								cerr << "Local static variable #" << (local4) << " out of range!";\
								CERR_CURRENT_IP();
								break;
							}
						}
					} else if (opcode == 0x5f) {
						UsecodeValue& ths = frame.get_this();
						if (local4 < 0 || local4 >= ths.get_class_var_count()) {
							cerr << "Class variable #" << (local4) << " out of range!";\
							CERR_CURRENT_IP();
							break;
						}
					} else {
						if (local4 < 0 || local4 >= num_locals) {
							LOCAL_VAR_ERROR(local4);
							break;
						}
					}
					
					// Get array to loop over.
					UsecodeValue& arr = opcode == 0x5C ? 
							(local4 < 0 ? statics[-local4]
								: frame.function.statics[local4])
							: (opcode == 0x5f ?
								frame.get_this().nth_class_var(local4) :
								frame.locals[local4]);
	 				if (initializing_loop && arr.is_undefined())
					{	// If the local 'array' is not initialized, do not loop
						// (verified in FoV and SS):
						initializing_loop = false;
						frame.ip += offset;
						break;
					}

					int next = frame.locals[local1].get_int_value();

					if (initializing_loop)
					{	// Initialize loop.
						initializing_loop = false;
						int cnt = arr.is_array() ?
							arr.get_array_size() : 1;
						frame.locals[local2] = UsecodeValue(cnt);
						frame.locals[local1] = UsecodeValue(0);

						next = 0;
					}

					// in SI, the loop-array can be modified in-loop, it seems
					// (conv. with Spektran, 044D:00BE)
				   
					// so, check for changes of the array size, and adjust
					// total count and next value accordingly.

					// Allowing this for BG too.

					int cnt = arr.is_array() ? arr.get_array_size() : 1;

					if (cnt != frame.locals[local2].get_int_value()) {
					
						// update new total count
						frame.locals[local2] = UsecodeValue(cnt);
						
						if (std::abs(cnt-frame.locals[local2].get_int_value())==1)
						{
							// small change... we can fix this
							UsecodeValue& curval = arr.is_array() ?
								arr.get_elem(next - 1) : arr;
							
							if (curval != frame.locals[local3]) {
								if (cnt>frame.locals[local2].get_int_value()){
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

					if (cnt != frame.locals[local2].get_int_value()) {

						// update new total count
						frame.locals[local2] = UsecodeValue(cnt);

						UsecodeValue& curval = arr.is_array() ?
							arr.get_elem(next - 1) : arr;
						
						if (curval != frame.locals[local3]) {
							if (cnt > frame.locals[local2].get_int_value()) {
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
					if (next >= frame.locals[local2].get_int_value()) {
						frame.ip += offset;
					} else		// Get next element.
					{
						frame.locals[local3] = arr.is_array() ?
							arr.get_elem(next) : arr;
						frame.locals[local1] = UsecodeValue(next + 1);
					}
					break;
				}
				case 0x2f:		// ADDSV.
				{
					offset = EUtil.Read2(frame.fcode, frame.ip);
					if (offset < 0 || offset >= num_locals) {
						LOCAL_VAR_ERROR(offset);
						break;
					}

					const char *str = frame.locals[offset].get_str_value();
					if (str)
						append_string(str);
					else		// Convert integer.
					{
					// 25-09-2001 - Changed to >= 0 to fix money-counting in SI.
					//				if (locals[offset].get_int_value() != 0) {
						if (frame.locals[offset].get_int_value() >= 0) {
							char buf[20];
							snprintf(buf, 20, "%ld",
					 frame.locals[offset].get_int_value());
							append_string(buf);
						}
					}
					break;
				}
				case 0x30:		// IN.  Is a val. in an array?
				{
					UsecodeValue arr = pop();
					// If an array, use 1st elem.
					UsecodeValue val = pop().get_elem0();
					pushi(arr.find_elem(val) >= 0);
					break;
				}
				case 0x31:		// Unknown.
				case 0xB1:		// (32 bit version)
				// this opcode only occurs in the 'audition' usecode function (BG)
				// not sure what it's supposed to do, but this function results
				// in the same behaviour as the original
					frame.ip += 2;
					if (opcode < 0x80)
						offset = (short)EUtil.Read2(frame.fcode, frame.ip);
					else
						offset = (sint32)EUtil.Read4(frame.fcode,frame.ip);
					
					if (!found_answer)
						found_answer = true;
					else
						frame.ip += offset;
					break;

				case 0x32:		// RET. (End of function reached)
				{
					show_pending_text();

					UsecodeValue zero(0);
					return_from_function(zero);
					frame_changed = true;
					break;
				}
				case 0x33:		// SAY.
					say_string();
					break;
				case 0x38:		// CALLIS.
				{
					offset = EUtil.Read2(frame.fcode, frame.ip);
					sval = *(frame.ip)++;  // # of parameters.
					UsecodeValue ival = call_intrinsic(frame.eventid,
														offset, sval);
					push(ival);
					frame_changed = true;
					break;
				}
				case 0x39:		// CALLI.
					offset = EUtil.Read2(frame.fcode, frame.ip);
					sval = *(frame.ip)++; // # of parameters.
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
						offset = EUtil.Read2(frame.fcode, frame.ip);
					if (offset < 0 || (unsigned)offset >= sizeof(gflags)) {
						FLAG_ERROR(offset);
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
						offset = EUtil.Read2(frame.fcode, frame.ip);
					if (offset < 0 || (unsigned)offset >= sizeof(gflags)) {
						FLAG_ERROR(offset);
					} else {
						gflags[offset] = (unsigned char) popi();
						if (gflags[offset]) {
							Notebook_gump::add_gflag_text(offset);
	#ifdef DEBUG
							cout << "Setting global flag: "
									<< offset << endl;
	#endif
						}
						// ++++KLUDGE for Monk Isle:
						if (offset == 0x272 && Game::get_game_type() ==
							SERPENT_ISLE)
							gflags[offset] = 0;
					}
					break;
				case 0x44:		// PUSHB.
					pushi(*(frame.ip)++);
					break;
				case 0x46:		// Set array element.
				case 0x5B:		// Set static array element.
				case 0x5E:		// Set class member array element.
				{
					UsecodeValue *arr;
					if (opcode == 0x46) {
						offset = EUtil.Read2(frame.fcode, frame.ip);
						// Get # of local array.
						if (offset < 0 || offset >= num_locals) {
							LOCAL_VAR_ERROR(offset);
							break;
						}
						arr = &(frame.locals[offset]);
					} else if (opcode == 0x5e) {
						offset = EUtil.Read2(frame.fcode, frame.ip);
						UsecodeValue& ths = frame.get_this();
						if (offset < 0 || offset >= ths.get_class_var_count()) {
							cerr << "Class variable #" << (offset) << " out of range!";\
							CERR_CURRENT_IP();
							break;
						}
						arr = &(ths.nth_class_var(offset));
					} else {
						offset = (sint16)EUtil.Read2(frame.fcode, frame.ip);
						if (offset < 0) {// Global static.
							if ((unsigned)(-offset) < statics.size())
								arr = &(statics[-offset]);
							else {
								cerr << "Global static variable #" << (offset) << " out of range!";\
								CERR_CURRENT_IP();
								break;
							}
						} else {
							if ((unsigned)offset < frame.function.statics.size())
								arr = &(frame.function.statics[offset]);
							else {
								cerr << "Local static variable #" << (offset) << " out of range!";\
								CERR_CURRENT_IP();
								break;
							}
						}
					}
					short index = popi();
					index--;	// It's 1-based.
					UsecodeValue val = pop();
					int size = arr.get_array_size();
					if (index >= 0 && 
						(index < size || arr.resize(index + 1)))
						arr.put_elem(index, val);
					break;
				}
				case 0x47:		// CALLE.  Stack has caller_item.
				case 0xc7:		// 32-bit version.
				{
					UsecodeValue ival = pop();
					Game_object *caller = get_item(ival);
					push(ival); // put caller_item back on stack
					if (opcode < 0x80)
						offset = EUtil.Read2(frame.fcode, frame.ip);
					else
						offset = (sint32)EUtil.Read4(frame.fcode,frame.ip);
					call_function(offset, frame.eventid, caller);
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
					frame.line_number = EUtil.Read2(frame.fcode, frame.ip);
					break;
				}
				case 0x4d: // debugging opcode from spanish SI (function init)
				case 0xcd: // 32 bit debugging function init
				{
					int funcname;
					int paramnames;
					if (opcode < 0x80)
					{
						funcname = EUtil.Read2(frame.fcode, frame.ip);
						paramnames = EUtil.Read2(frame.fcode, frame.ip);
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
				case 0x50:		// PUSH static.
					offset = (sint16)EUtil.Read2(frame.fcode, frame.ip);
					if (offset < 0) {// Global static.
						if ((unsigned)(-offset) < statics.size())
							push(statics[-offset]);
						else
							pushi(0);
					} else {
						if ((unsigned)offset < frame.function.statics.size())
							push(frame.function.statics[offset]);
						else
							pushi(0);
					}
					break;
				case 0x51:		// POP static.
				{
					offset = (sint16)EUtil.Read2(frame.fcode, frame.ip);
					// Get value.
					UsecodeValue val = pop();
					if (offset < 0) {
						if ((unsigned)(-offset) >= statics.size())
							statics.resize(-offset + 1);
						statics[-offset] = val;
					} else {
						if ((unsigned)offset >= frame.function.statics.size())
							frame.function.statics.resize(offset + 1);
						frame.function.statics[offset]=val;
					}
				}
					break;
				case 0x52:		// CALLO (call original).
				{			// Otherwise, like CALLE.
					UsecodeValue ival = pop();
					Game_object *caller = get_item(ival);
					push(ival); // put caller_item back on stack

					offset = EUtil.Read2(frame.fcode, frame.ip);
					call_function(offset, frame.eventid, caller,
									false, true);
					frame_changed = true;
					break;
				}
				case 0x53:		// CALLIND:  call indirect.
				{			//  Function # is on stack.
					UsecodeValue funval = pop();
					int offset = funval.get_int_value();
					UsecodeValue ival = pop();
					Game_object *caller = get_item(ival);
					call_function(offset, frame.eventid, caller);
					frame_changed = true;
					break;
				}
				case 0x54:		// PUSH class this.var.
				{
					offset = EUtil.Read2(frame.fcode, frame.ip);
					UsecodeValue& ths = frame.get_this();
					push(ths.nth_class_var(offset));
					break;
				}
				case 0x55:		// POP class this.var.
				{
					// Get value.
					UsecodeValue val = pop();
					offset = EUtil.Read2(frame.fcode, frame.ip);
					UsecodeValue& ths = frame.get_this();
					ths.nth_class_var(offset) = val;
					break;
				}
				case 0x56:		// CALLM - call method, use var vtable.
				case 0x57:		// CALLMS - call method, use pushed vtable.
				{
					offset = EUtil.Read2(frame.fcode, frame.ip);
					Usecode_class_symbol *c;
					if (opcode == 0x56)
						{
						UsecodeValue thisptr = peek();
						c = thisptr.get_class_ptr();
						}
					else
						{
						UsecodeValue thisptr = EUtil.Read2(frame.fcode, frame.ip);
						c = get_class(thisptr.get_int_value());
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
					int cnum = EUtil.Read2(frame.fcode, frame.ip);
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
				case 0x60:		// PUSHCHOICE
					pushs(user_choice);
					break;
				*/
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
	 *	Output:	->user choice string.
	 *		0 if no possible choices or user quit.
	 */
	private String get_user_choice() {
		/*+++++++++FINISH
		if (!conv->get_num_answers())
			return null;		// This does happen (Emps-honey).
		//	if (!user_choice)		// May have already been done.
		// (breaks conversation with Cyclops on Dagger Isle ('foul magic' option))
		get_user_choice_num();
		return (user_choice);
		*/
		return null;
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
		// back up a stack frame
		previous_stack_frame();
		// push the return value
		push(retval);
	}
	private void return_from_procedure() {
		// back up a stack frame
		previous_stack_frame();
	}
	private void abort_function() {
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
			return new UsecodeValue.IntUsecodeValue(0);
		}
		return stack[--sp]; 
	}
	private UsecodeValue peek() {
		return stack[sp-1];
	}
	private void pushref(GameObject obj) {
		push(new UsecodeValue.ObjectUsecodeValue(obj));
	} 
	private void pushi(int val) {		// Push/pop integers.
		push(new UsecodeValue.IntUsecodeValue(val));
	}

	private int popi()
	{
		UsecodeValue val = pop();
		return val.needIntValue();
	}

	// Push/pop strings.
	private void pushs(String s) {
		push(new UsecodeValue.StringUsecodeValue(s));
	}

	/*
	 * One Usecode function.
	 */
	public static class UsecodeFunction {
		int id;			// Function #.
		UsecodeFunction orig;	// Orig., if this was from a patch.
		byte code[];	// What gets executed.
		boolean extended;	// 32-bit function.
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
		String getDataString(int offset) {
			int cnt;
			for (cnt = 0; fcode[data + offset + cnt] != 0; ++cnt)
				;
			return new String(fcode, data + offset, cnt);
		}
	}
	
}
