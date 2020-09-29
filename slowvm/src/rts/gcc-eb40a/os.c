/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

// Override setting from platform.h
#undef PLATFORM_UNALIGNED_LOADS
#define PLATFORM_UNALIGNED_LOADS false

#define FLASH_MEMORY
#define SERVICE_CHUNK_SIZE (4*1024)
#define IODOTC "eb40a-io.c"


#include <stdlib.h>
#include <sys/time.h>
#include <dlfcn.h>
typedef long long int64_t;
typedef unsigned long long u_int64_t;
#define jlong  int64_t
#include "jni.h"

/**
 * Define LED usage
 */
#define WATCHDOG_LED 0x80  // LED 8
#define DONE_LED 0x40      // LED 7
#define AVAILABLE_LEDS 0x1F // reserve LEDs 6-8


jlong sysTimeMillis(void) {
    jlong res = getMilliseconds();
    return res;
}

jlong sysTimeMicro() {
    return sysTimeMillis() * 1000;
}

void startTicker(int interval) {
    fprintf(stderr, "Profiling not implemented");
    exit(0);
}

void wait() {
	unsigned int i, j;
	for (i = 1; i<350000; i++) {
		j = j + i;
	}
}
	
#ifdef EB40AFLASH
static int loadFlash(int updateVM);
#endif


/**
 * Program entrypoint.
 *
 * @param argc the number of command line parameters
 * @param argv the parameter argument vector
 */
#define OSMAIN
int main(int argc, char *argv[]) {
    int ignore = initializeGlobals();
	int led_no;
    //initialise the LEDS and switches
    configure_leds_and_sws();
    turn_off_all_leds();
    
    for (led_no = 0; led_no < 8; led_no++) {
    	wait();
    	turn_on_leds(1 << led_no);
    	wait();
    	turn_off_leds(1 << led_no);
    }
    wait();
    
#ifdef EB40AFLASH

	#ifdef DB_DEBUG
		//if debugging is compiled in, let user turn it on or off
		extern int db_debug_enabled;
		db_debug_enabled = sw_is_pressed(1 << 3); //SW4 is fourth bit
	#endif

    //check if user has requested flash load using SW1
    if (sw_is_pressed(5)) {
    	// SW1 and SW3 pressed - user wants to update VM
        exit(loadFlash(1) ? 0 : -1);
    }
    if (sw_is_pressed(1)) {
    	// Just SW1 pressed - user wants to load an app
        exit(loadFlash(0) ? 0 : -1);
    }
    
#endif

    // start the system timer
    init_watchdog_timer();

    iprintf("\n");
    iprintf("Squawk VM Starting...\n");

    // Hardcode command line arguments
    const int fakeArgc = 5;
    #ifdef EB40AFLASH
        const char *fakeArgv[] = {"-verbose", "-Xmx:64000", "-Xmxnvm:8", "-flashsuite:010A0000", "squawk.application.Startup"};
//        const char *fakeArgv[] = {"-verbose", "-Xmx:64000", "-Xmxnvm:8", "java.lang.Test"};
    #else
        const char *fakeArgv[] = {"-verbose", "-Xmx:20000", "-Xmxnvm:8", "-flashsuite:011A0000", "squawk.application.Startup"};
//        const char *fakeArgv[] = {"-verbose", "-Xmx:12000", "-Xmxnvm:8", "java.lang.Test"};
    #endif

    return Squawk_main(fakeArgc, fakeArgv);
}

#ifdef EB40AFLASH
/********************************************************
 * Flash load support
 */
int loadAndFlashAt (unsigned int addressOffsetToFlash);
byte deadbeef[] = {0xDE, 0xAD, 0xBE, 0xEF};

/**
 * Routine to call flash loader
 *
 * returns TRUE if loaded ok
 */
static int loadFlash(int updateVM) {
    // check that memory is mapped the way we expect
    // we expect the address 0x010A0000 to contain 0xDEADBEEF
    byte* mem = (byte*) 0x10A0000;
    int i;
    int result = 1;
    for (i=0; i < 4; i++) {
        if (mem[i] != deadbeef[i]) {
            iprintf ("The 4-byte value at 0x10A0000 does not contain the\n");
            iprintf ("magic word 0xDEADBEEF. Either the memory is not mapped\n");
            iprintf ("correctly or an application suite has never been loaded\n");
            iprintf ("on this board. As a security measure you will need to\n");
            iprintf ("load it the first time using EBLOAD or similar.\n");
            result = 0;
            break;
        }
    }
    if (updateVM) {
    	for (i=0; i < 50; i++) {
    		turn_on_leds(5);
    		wait();
    		wait();
    		turn_off_leds(5);
    		wait();
    		wait();
    	}
		wait();
		wait();
    	result = loadAndFlashAt (0x000000); // The C code
	    if (result <= 0) {
	        iprintf ("--Failed to load and flash\n");
	        result = 0;
	    } else {
	    	for (i=0; i < 50; i++) {
	    		turn_on_leds(4);
	    		wait();
	    		wait();
	    		turn_off_leds(4);
	    		wait();
	    		wait();
	    	}
			wait();
			wait();
	    	result = loadAndFlashAt (0x040000); // The bootstrap bytecodes
	    }  	
    } else {
    	result = loadAndFlashAt (0x0A0000); // the parameter is an offset from 0x1000000
    }
    if (result <= 0) {
        iprintf ("--Failed to load and flash\n");
        result = 0;
    }
    return result;
}
/*
 ********************************************************/
#endif

/**
 * Flip the watchdog LED every 5,000 bytecodes
 */
int count = 0;
int led_is_on = 0;
void updateLEDStatus() {
    count = count + 1;
    if (count % 250 == 0) {
        //iprintf("Count: %i\n", count);
        if (led_is_on) {
            turn_off_leds (WATCHDOG_LED);
            led_is_on = 0;
        } else {
            turn_on_leds (WATCHDOG_LED);
            led_is_on = 1;
        }
    }
}

/**
 * Light LED to show termination
 */
void updateLEDFinished() {
    turn_off_leds (WATCHDOG_LED);
    turn_on_leds (DONE_LED);
}

/**
 * Support for util.h
 */

long sysconf(int code) {
    if (code == _SC_PAGESIZE) {
        return 4;
    } else {
        return -1; // failure
    }
}

int mprotect(void * address, size_t len, int props) {
    // no-op on eb40a platform
    return 0; // success
}

inline void osloop() {
	//no-op on eb40a platform
}

inline void osbackbranch() {
    updateLEDStatus();
}

inline void osfinish() {
    updateLEDFinished();
}
