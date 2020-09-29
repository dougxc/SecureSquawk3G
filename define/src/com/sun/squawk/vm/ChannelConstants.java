/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk.vm;

public final class ChannelConstants {

    /**
     * The channel identifier for the GUI input channel.
     */
    public final static int CHANNEL_GENERIC = 1;

    /**
     * The channel identifier for the GUI input channel.
     */
    public final static int CHANNEL_GUIIN   = 2;

    /**
     * The channel identifier for the GUI output channel.
     */
    public final static int CHANNEL_GUIOUT  = 3;

    /**
     * The GUI input repaint message.
     */
    public final static int GUIIN_REPAINT   = 0;

    /**
     * The GUI key input message.
     */
    public final static int GUIIN_KEY       = 1;

    /**
     * The GUI mouse message.
     */
    public final static int GUIIN_MOUSE     = 2;

    /**
     * The GUI exit message.
     */
    public final static int GUIIN_EXIT      = 3;

    /**
     * The GUI input repaint message.
     */
    public final static int GUIIN_HIBERNATE = 4;


    public final static int

        /* Channel I/O result codes */

        RESULT_OK                       = 0,
        RESULT_BADCONTEXT               = -1,
        RESULT_EXCEPTION                = -2,
        RESULT_BADPARAMETER             = -3,

        /* I/O channel opcodes */

        GLOBAL_CREATECONTEXT            = 1,
        GLOBAL_DELETECONTEXT            = 2,
        GLOBAL_HIBERNATECONTEXT         = 3,
        GLOBAL_GETEVENT                 = 4,
        GLOBAL_WAITFOREVENT             = 5,
        CONTEXT_GETCHANNEL              = 6,
        CONTEXT_FREECHANNEL             = 7,
        CONTEXT_GETRESULT               = 8,
        CONTEXT_GETRESULT_2             = 9,
        CONTEXT_GETERROR                = 10,
        OPENCONNECTION                  = 11,       /* Opcodes for Generic connections */
        CLOSECONNECTION                 = 12,
        ACCEPTCONNECTION                = 13,
        OPENINPUT                       = 14,
        CLOSEINPUT                      = 15,
        WRITEREAD                       = 16,
        READBYTE                        = 17,
        READSHORT                       = 18,
        READINT                         = 19,
        READLONG                        = 20,
        READBUF                         = 21,
        SKIP                            = 22,
        AVAILABLE                       = 23,
        MARK                            = 24,
        RESET                           = 25,
        MARKSUPPORTED                   = 26,
        OPENOUTPUT                      = 27,
        FLUSH                           = 28,
        CLOSEOUTPUT                     = 29,
        WRITEBYTE                       = 30,
        WRITESHORT                      = 31,
        WRITEINT                        = 32,
        WRITELONG                       = 33,
        WRITEBUF                        = 34,
        SETWINDOWNAME                   = 35,       /* Opcodes for KAWT graphics API */
        SCREENWIDTH                     = 36,
        SCREENHEIGHT                    = 37,
        BEEP                            = 38,
        SETOFFSCREENMODE                = 39,
        FLUSHSCREEN                     = 40,
        CREATEIMAGE                     = 41,
        CREATEMEMORYIMAGE               = 42,
        GETIMAGE                        = 43,
        IMAGEWIDTH                      = 44,
        IMAGEHEIGHT                     = 45,
        DRAWIMAGE                       = 46,
        FLUSHIMAGE                      = 47,
        CREATEFONTMETRICS               = 48,
        FONTSTRINGWIDTH                 = 49,
        FONTGETHEIGHT                   = 50,
        FONTGETASCENT                   = 51,
        FONTGETDESCENT                  = 52,
        SETFONT                         = 53,
        SETCOLOR                        = 54,
        SETCLIP                         = 55,
        DRAWSTRING                      = 56,
        DRAWLINE                        = 57,
        DRAWOVAL                        = 58,
        DRAWRECT                        = 59,
        FILLRECT                        = 60,
        DRAWROUNDRECT                   = 61,
        FILLROUNDRECT                   = 62,
        FILLARC                         = 63,
        FILLPOLYGON                     = 70,
        REPAINT                         = 71,

        /*
         * Internal codes used to execeute C code on the service stack.
         */
        INTERNAL_SETSTREAM              = 1000,
        INTERNAL_OPENSTREAM             = 1001,
        INTERNAL_PRINTCHAR              = 1002,
        INTERNAL_PRINTSTRING            = 1003,
        INTERNAL_PRINTINT               = 1004,
        INTERNAL_PRINTUWORD             = 1005,
        INTERNAL_PRINTOFFSET            = 1006,
        INTERNAL_PRINTLONG              = 1007,
        INTERNAL_PRINTADDRESS           = 1008,
        INTERNAL_GETTIME_LOW            = 1009,
        INTERNAL_GETTIME_HIGH           = 1010,
        INTERNAL_STOPVM                 = 1011,
        INTERNAL_COPYBYTES              = 1012,
        INTERNAL_PRINTCONFIGURATION     = 1013,
        INTERNAL_PRINTGLOBALOOPNAME     = 1014,
        INTERNAL_PRINTGLOBALS           = 1015,
        INTERNAL_MATH                   = 1016,
        INTERNAL_GETPATHSEPARATORCHAR   = 1017,
        INTERNAL_GETFILESEPARATORCHAR   = 1018,

        DUMMY = 999;

    private static final String[] Mnemonics = {
        "[invalid opcode]",
        "GLOBAL_CREATECONTEXT",     // 1
        "GLOBAL_DELETECONTEXT",     // 2
        "GLOBAL_HIBERNATECONTEXT",  // 3
        "GLOBAL_GETEVENT",          // 4
        "GLOBAL_WAITFOREVENT",      // 5
        "CONTEXT_GETCHANNEL",       // 6
        "CONTEXT_FREECHANNEL",      // 7
        "CONTEXT_GETRESULT",        // 8
        "CONTEXT_GETRESULT_2",      // 9
        "CONTEXT_GETERROR",         // 10
        "OPENCONNECTION ",          // 11
        "CLOSECONNECTION ",         // 12
        "ACCEPTCONNECTION ",        // 13
        "OPENINPUT",                // 14
        "CLOSEINPUT",               // 15
        "WRITEREAD",                // 16
        "READBYTE",                 // 17
        "READSHORT",                // 18
        "READINT",                  // 19
        "READLONG",                 // 20
        "READBUF",                  // 21
        "SKIP",                     // 22
        "AVAILABLE",                // 23
        "MARK",                     // 24
        "RESET",                    // 25
        "MARKSUPPORTED",            // 26
        "OPENOUTPUT",               // 27
        "FLUSH",                    // 28
        "CLOSEOUTPUT",              // 29
        "WRITEBYTE",                // 30
        "WRITESHORT",               // 31
        "WRITEINT",                 // 32
        "WRITELONG",                // 33
        "WRITEBUF",                 // 34
        "SETWINDOWNAME",            // 35
        "SCREENWIDTH",              // 36
        "SCREENHEIGHT",             // 37
        "BEEP",                     // 38
        "SETOFFSCREENMODE",         // 39
        "FLUSHSCREEN",              // 40
        "CREATEIMAGE",              // 41
        "CREATEMEMORYIMAGE",        // 42
        "GETIMAGE",                 // 43
        "IMAGEWIDTH",               // 44
        "IMAGEHEIGHT",              // 45
        "DRAWIMAGE",                // 46
        "FLUSHIMAGE",               // 47
        "CREATEFONTMETRICS",        // 48
        "FONTSTRINGWIDTH",          // 49
        "FONTGETHEIGHT",            // 50
        "FONTGETASCENT",            // 51
        "FONTGETDESCENT",           // 52
        "SETFONT",                  // 53
        "SETCOLOR",                 // 54
        "SETCLIP",                  // 55
        "DRAWSTRING",               // 56
        "DRAWLINE",                 // 57
        "DRAWOVAL",                 // 58
        "DRAWRECT",                 // 59
        "FILLRECT",                 // 60
        "DRAWROUNDRECT",            // 61
        "FILLROUNDRECT",            // 62
        "FILLARC",                  // 63
        "FILLPOLYGON",              // 70
        "REPAINT"                   // 71
    };

    public static String getMnemonic(int op) {
        try {
            return Mnemonics[op];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return "invalid opcode:" + op;
        }
    }

/*if[FLASH_MEMORY]*/
    /**
     * The channel identifier for the LED output channel.
     */
    public static final int CHANNEL_LED = 101;

    /**
     * The channel identifier for the SW input channel.
     */
    public static final int CHANNEL_SW = 102;

    /**
     * The LED off message.
     */
    public static final int LED_OFF = 201;

    /**
     * The LED on message.
     */
    public static final int LED_ON = 202;

    /**
     * The SW read message.
     */
    public static final int SW_READ = 203;

    public static final int PEEK = 301;

    public static final int POKE = 302;

/*end[FLASH_MEMORY]*/


}
