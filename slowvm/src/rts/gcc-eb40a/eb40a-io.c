/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
struct switchRequest {
	int eventNumber;
	int mask;
	struct switchRequest *next;
};
typedef struct switchRequest SwitchRequest;

SwitchRequest *switchRequests;

int retValue = 0;  // holds the value to be returned on the next "get result" call

/*
 * Java has requested wait for switch to be pressed. Store the request,
 * and each time Java asks for events, signal the event if the switch is pressed
 * 
 * @return the event number
 */
int storeSwitchRequest (int switchMask) {
	SwitchRequest* newRequest = (SwitchRequest*)malloc(sizeof(SwitchRequest));
	if (newRequest == NULL) {
		//TODO set up error message for GET_ERROR and handle
		//one per channel and clean on new requests.
		return ChannelConstants_RESULT_EXCEPTION;
	}
	
	newRequest->next = NULL;
	newRequest->mask = switchMask;
	
	if (switchRequests == NULL) {
		switchRequests = newRequest;
		newRequest->eventNumber = 1;
	} else {
		SwitchRequest* current = switchRequests;
		while (current->next != NULL) {
			current = current->next;
		}
		current->next = newRequest;
		newRequest->eventNumber = current->eventNumber + 1;
	}
	return newRequest->eventNumber;
}

// Forward declaration
int getEventPrim(int);

/*
 * If there are outstanding switchRequests and one of them is for a switch that is
 * currently pressed remove it and return its eventNumber. Otherwise return 0
 */
int getEvent() {
	return getEventPrim(1);
}

/*
 * If there are outstanding switchRequests and one of them is for a switch that is
 * currently pressed return its eventNumber. Otherwise return 0
 */
int checkForEvents() {
	return getEventPrim(0);
}

/* 
 * If there are outstanding switchRequests and one of them is for a switch that is
 * currently pressed return its eventNumber. If removeEventFlag is true, then 
 * also remove the event from the queue. If no requests match the current switch state
 * return 0.
 */
int getEventPrim(int removeEventFlag) {
	int res = 0;
	if (switchRequests == NULL) {
		return 0;
	}
	SwitchRequest* current = switchRequests;
	SwitchRequest* previous = NULL;
	while (current != NULL) {
		if (sw_is_pressed(current->mask)) {
			res = current->eventNumber;
			//unchain
			if (removeEventFlag) {
				if (previous == NULL) {
					switchRequests = current->next;
				} else {
					previous->next = current->next;
				}
				free(current);
			}
			break;
		} else {
			previous = current;
			current = current->next;
		}
	}
	return res;
}


/**
 * Executes an operation on a given channel for an isolate.
 *
 * @param  context the I/O context
 * @param  op      the operation to perform
 * @param  channel the identifier of the channel to execute the operation on
 * @param  i1
 * @param  i2
 * @param  i3
 * @param  i4
 * @param  i5
 * @param  i6
 * @param  send
 * @param  receive
 * @return the operation result
 */
 static void ioExecute(void) {
    int     context = java_lang_ServiceOperation_context;
    int     op      = java_lang_ServiceOperation_op;
    int     channel = java_lang_ServiceOperation_channel;
    int     i1      = java_lang_ServiceOperation_i1;
    int     i2      = java_lang_ServiceOperation_i2;
    int     i3      = java_lang_ServiceOperation_i3;
    int     i4      = java_lang_ServiceOperation_i4;
    int     i5      = java_lang_ServiceOperation_i5;
    int     i6      = java_lang_ServiceOperation_i6;
    Address send    = java_lang_ServiceOperation_o1;
    Address receive = java_lang_ServiceOperation_o2;
 	
    int res = ChannelConstants_RESULT_OK;
    
    switch (op) {
    	case ChannelConstants_GLOBAL_CREATECONTEXT: 
    		res = 1; //let all Isolates share a context for now
    		break;
    	case ChannelConstants_CONTEXT_GETCHANNEL: {
	    		int channelType = i1;
	    		if (channelType == ChannelConstants_CHANNEL_LED) {
	    			res = 1;
	    		} else if (channelType != ChannelConstants_CHANNEL_SW) {
	    			res = 2;
	    		} else {
	    			res = ChannelConstants_RESULT_BADPARAMETER;
	    		}
	    	}
    		break;
    	case ChannelConstants_LED_OFF: {
	    		int mask = i1;
	    		turn_off_leds(mask);
    		}
    		break;
    	case ChannelConstants_LED_ON: {
	    		int mask = i1;
    			turn_on_leds(mask);
    		}
    		break;
    	case ChannelConstants_SW_READ: {
	    		int mask = i1;
	    		if (sw_is_pressed(mask)) {
	    			res = 0;
	    		} else {
		    		res = storeSwitchRequest(mask);
	    		}
    		}
    		break;
        case ChannelConstants_PEEK: {
	    		unsigned int *address = (unsigned int*)i1;
	    		retValue = *address;
    		}
    		break;
    	case ChannelConstants_POKE: {
	    		unsigned int *address = (unsigned int*)i1;
	    		int value = i2;
	    		*address = value;
    		}
    		break;
    	case ChannelConstants_CONTEXT_GETRESULT:
    	case ChannelConstants_CONTEXT_GETRESULT_2:
    	case ChannelConstants_CONTEXT_GETERROR:
    		res = retValue;
    		retValue = 0;
    		break;
    	case ChannelConstants_GLOBAL_GETEVENT:
    		res = getEvent();
    		break;
    	case ChannelConstants_GLOBAL_WAITFOREVENT: {
    			long long millisecondsToWait = i1;
    			millisecondsToWait = (millisecondsToWait << 32) | ((unsigned long long)i2 & 0xFFFFFFFF);
    			long long target = getMilliseconds() + millisecondsToWait;
				long long maxValue = 0x7FFFFFFFFFFFFFFFLL;
				if (target <= 0) target = maxValue; // overflow detected
				
    			
    			while (1) {
    				if (checkForEvents()) break;
					if (getMilliseconds() > target) break;
    			}
    			res = 0;
    		}
    		break;
    	case ChannelConstants_GLOBAL_DELETECONTEXT:
    		// TODO delete all the outstanding events on the context
    		// But will have to wait until we have separate contexts for each isolate
    		res=0;
    		break;
		default:    		
    		res = ChannelConstants_RESULT_BADPARAMETER;
    }
    java_lang_ServiceOperation_result = res;
}

/**
 * Initializes the IO subsystem.
 *
 * @param  jniEnv      the table of JNI function pointers which is only non-null if Squawk was
 *                     launched via a JNI call from a Java based launcher
 * @param  classPath   the class path with which to start the embedded JVM (ignored if 'jniEnv' != null)
 * @param  args        extra arguments to pass to the embedded JVM (ignored if 'jniEnv' != null)
 * @param  argc        the number of extra arguments in 'args' (ignored if 'jniEnv' != null)
 */

void CIO_initialize(JNIEnv *jniEnv, char *classPath, char** args, int argc) {
}

