/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
#define TRUE 1
#define FALSE 0
#define DB_MAX_BPS 20

int db_bp_set; // TRUE if we have any breakpoints set [optimisation]
int db_debug_enabled = TRUE; //Enabled by default

typedef struct db_breakpoint {
	ByteAddress db_ip; // 0 if not set
} Db_breakpoint;

Db_breakpoint db_bp_table[DB_MAX_BPS];
ByteAddress db_current_ips[DB_MAX_BPS + 1]; // parallel structure for performance

void db_output(char* cmd) {
	printf(cmd);
	fflush(stdout);
}

char* db_input(char* buf, int bufSize) {
	return fgets(buf, bufSize, stdin);
}

int db_printf0(char* format) {
    char buf[200];
    int res = sprintf(buf, format);
    if (res == -1) {
		printf("ERROR - Debug call to asprintf failed\n");
		exit(-1);
    }    	
    db_output(buf);
    return res;
}

int db_printf1(char* format, int i1) {
    char buf[200];
    int res = sprintf(buf, format, i1);
    if (res == -1) {
		printf("ERROR - Debug call to asprintf failed\n");
		exit(-1);
    }    	
    db_output(buf);
    return res;
}

int db_printf2(char* format, int i1, int i2) {
    char buf[200];
    int res = sprintf(buf, format, i1, i2);
    if (res == -1) {
		printf("ERROR - Debug call to sprintf failed\n");
		exit(-1);
    }    	
    db_output(buf);
    return res;
}

void db_send_ready() {	
	//TODO remove hard coded address
    db_printf0("\n");
#ifdef FLASH_MEMORY
#ifdef EB40AFLASH
    db_printf2("*DEBUG*:R:%i:%i\n", (int)java_lang_VM_romStart, 0x10a0030);
#else
    db_printf2("*DEBUG*:R:%i:%i\n", (int)java_lang_VM_romStart, 0x11a0030);
#endif
#else
    db_printf1("*DEBUG*:R:%i\n", (int)java_lang_VM_romStart);
#endif

}

void db_send_bp_hit(int bpnum) {
    db_printf1("*DEBUG*:B:H:%i\n", bpnum);
}

void db_send_exit() {
    db_printf0("*DEBUG*:X\n");
}

void db_send_data_result(int db_opcode, ByteAddress actual_ip, UWordAddress actual_fp) {
    UWordAddress return_fp;
    UWordAddress p;
    UWordAddress sp0;
    int nlocals;
    ByteAddress mid;
    if (db_opcode == OPC_EXTEND) {
        mid = actual_ip - 1;
    } else if (db_opcode == OPC_EXTEND0) {
        mid = actual_ip - 1;
    } else {
        mid = (ByteAddress)getObject(actual_fp, FP_method);
    }
    nlocals = getLocalCount(mid);

    db_printf0("*DEBUG*:D:R:S:");

    p = actual_fp;
    return_fp = (UWordAddress)getUWord(p, FP_returnFP);
    p = p + FP_parm0; // move to first parameter
    db_printf0("P:");
    while (p < (actual_fp + FP_parm0 + db_get_parameter_count(mid) - 1)) {
		db_printf1("%i,", getUWord(p, 0));
		p++;
    }
	db_printf1("%i", getUWord(p, 0));
    db_printf0(":L:");
    p = actual_fp;
    --p; // skip the method id
    sp0 = actual_fp - (nlocals - 1);
    while (p >= sp0) {
        UWord value = getUWord(p, 0);
        if (value == DEADBEEF) {
            db_printf0("X");
        } else {
            db_printf1("%i", value);
            }
        if (p != sp0) {
            db_printf0(",");
        }
        --p;
    }
    db_printf0("\n");
}

void db_send_memory(int addr, int size) {
	unsigned int * ptr = (unsigned int *) addr;
	int i;
    db_printf0("*DEBUG*:D:R:M:");

	for (i = 1; i <= size; i++) {
		db_printf1("%i", *ptr++);
		if (i != size) {
			db_printf0(",");
		}
	}
    db_printf0("\n");
}
		

void db_process_data_cmd(char* buf, int db_opcode, ByteAddress actual_ip, UWordAddress actual_fp) {
	char format[] = "%9s:%c:%c:%i,%i";
	char hdr[9];
	char subcmd;
	char subsubcmd;
	int i1, i2;

	sscanf(buf, format, &hdr, &subcmd, &subsubcmd, &i1, &i2);
	
	switch (subcmd) {
		case 'S': {
			// set - not implemented yet
			printf("ERROR - Debug data set cmd is not implemented\n");
			exit(-1);
			break;
		}
		case 'G': {
			// get
			switch (subsubcmd) {
				case 'S': {
					// get state
					db_send_data_result(db_opcode, actual_ip, actual_fp);
					break;
				}
				case 'M': {
					// get memory
					db_send_memory(i1, i2);
					break;
				}
				default: {
					printf("ERROR - Debug data subcmd %c is not valid\n", subsubcmd);
					exit(-1);
				}
			}
			break;
		}
		default: {
			printf("ERROR - Debug data cmd %c is not valid\n", subcmd);
			exit(-1);
		}
	}
}

/* Maintain a parallel list of the current breakpoint ips (with 1 subtracted)
 * to support faster checking
 */
void db_regenerate_current_ips() {
	int i;
	int j=0;
	for (i = 0; i < DB_MAX_BPS; i++) {
		if (db_bp_table[i].db_ip != 0) {
			db_current_ips[j++] = db_bp_table[i].db_ip + 1; // should be sync'd with -1 in do_checkBreak
		}
	}
	for (i = j; i < DB_MAX_BPS; i++) {
		db_current_ips[i] = 0;
	}
}	

void db_process_break_cmd(char* buf) {
	char format[] = "%9s:%c:%i:%i";
	char hdr[9];
	char subcmd;
	int bpnum, addr;
	int i;

	sscanf(buf, format, &hdr, &subcmd, &bpnum, &addr);
	
	if (bpnum < 0 || bpnum >= DB_MAX_BPS) {
		printf("ERROR - Breakpoint number %i is not valid\n", bpnum);
		exit(-1);
	}
	switch (subcmd) {
		case 'S': {
			// set
			char op = *((ByteAddress)addr);
			switch (op) {
				case OPC_EXTEND: {
					addr = addr + 2;
					break;
				}
                case OPC_EXTEND0: {
                	addr++;
                	break;
                }
			}				
			db_bp_table[bpnum].db_ip = (ByteAddress)addr;
			db_bp_set = TRUE;
			db_regenerate_current_ips();
			break;
		}
		case 'C': {
			// clear
			db_bp_table[bpnum].db_ip = 0;
			for (i = 0; i < DB_MAX_BPS; i++) {
				db_bp_set = FALSE;
				if (db_bp_table[i].db_ip != 0) {
					db_bp_set = TRUE;
					break;
				}
			}
			db_regenerate_current_ips();
			break;
		}
		default: {
			printf("ERROR - Debug break cmd %c is not valid\n", subcmd);
			exit(-1);
		}
	}
}

void db_process_client_commands(int db_opcode, ByteAddress actual_ip, UWordAddress actual_fp) {
	char buf[100];
	char format[] = "%7s:%c";
	char hdr[7];
	char cmd;
	int done = FALSE;
	char * result;
	while (!done) {
		result = db_input(buf, 100);
		if (result == 0) {
			// io error
			printf("ERROR - No data read\n");
			exit(-1);
		}
		sscanf(buf, format, &hdr, &cmd);
		//validate header
		if (strcmp(hdr, "*DEBUG*") != 0) {
			// bad header
			printf("ERROR - Debug cmd header not *DEBUG*\n");
			exit(-1);
		}
		switch (cmd) {
			case 'B': {
				db_process_break_cmd(buf);
				break;
			}
			case 'C': {
				// continue
				done = TRUE;
				break;
			}
			case 'D': {
				// data
				db_process_data_cmd(buf, db_opcode, actual_ip, actual_fp);
				break;
			}
			default: {
				printf("ERROR - Debug cmd %c is not valid\n", cmd);
				exit(-1);
			}
		}
	}
}

void db_prepare() {
	if (db_debug_enabled) {
		int i;
		for (i = 0; i < DB_MAX_BPS; i++) {
			db_bp_table[i].db_ip = 0;
			db_current_ips[i]=0;
		}
		db_current_ips[DB_MAX_BPS]=0;
		
		db_bp_set = FALSE;
		db_send_ready();
		db_process_client_commands(0, 0, 0);
	}
}
	
inline void db_checkBreak(int opcode, ByteAddress actual_ip, UWordAddress actual_fp) {
	if (db_debug_enabled) {
		if (db_bp_set) {
			// check for hit
			int i=0;
			ByteAddress bp_ip;
			while (bp_ip = db_current_ips[i++]) {
				if (bp_ip == actual_ip) {
					// Breakpoint found - now repeat search more leisurely on the "real" breakpoints.
					int j;
					for (j = 0; j < DB_MAX_BPS; j++) {
						if (db_bp_table[j].db_ip == (actual_ip - 1)) { //TODO - check that -1 is correct
							// hit
							db_send_bp_hit(j);
							db_process_client_commands(opcode, actual_ip, actual_fp);
							break;
						}
					}
				}
			}
		}
	}
}

void db_vm_exiting() {
	if (db_debug_enabled) {
		db_send_exit();
	}
}

int db_get_parameter_count(Address mp) {
	int b0 = getByte(mp, HDR_methodInfoStart) & 0xFF;
    if (b0 < 128) {
    	return b0 >> 2;
    } else {
        return minfoValue(mp, 3);
	}
}

