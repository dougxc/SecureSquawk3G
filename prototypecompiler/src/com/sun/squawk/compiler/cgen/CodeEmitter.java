/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.compiler.cgen;

import com.sun.squawk.compiler.*;

/**
 * Class that specifies the code emitter interface.
 *
 * @author   Nik Shaylor
 */
public abstract class CodeEmitter {

    /**
     * Emitter interface function.
     */
    abstract void reset                  ();

    /**
     * Emitter interface function.
     *
     * @param sl the stack local to free
     */
    public abstract void freeLocal              (StackLocal         sl);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitUnOp               (UnOp               inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitBinOp              (BinOp              inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitCvtOp              (CvtOp              inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitDupOp              (DupOp              inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitAllocaOp           (AllocaOp           inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitLoadParmOp         (LoadParmOp         inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitStoreParmOp        (StoreParmOp        inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitCommentOp          (CommentOp          inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitLoadOp             (LoadOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitStoreOp            (StoreOp            inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitReadOp             (ReadOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitWriteOp            (WriteOp            inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitIntLiteralOp       (IntLiteralOp       inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitLongLiteralOp      (LongLiteralOp      inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitFloatLiteralOp     (FloatLiteralOp     inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitDoubleLiteralOp    (DoubleLiteralOp    inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitArrayLiteralOp     (ArrayLiteralOp     inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitLabelLiteralOp     (LabelLiteralOp     inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitSymbolOp           (SymbolOp           inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitBranchOp           (BranchOp           inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitJumpOp             (JumpOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitCallOp             (CallOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitDeadCodeOp         (DeadCodeOp         inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitDropOp             (DropOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitStackCheckOp       (StackCheckOp       inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitPushOp             (PushOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitPopOp              (PopOp              inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitPopAllOp           (PopAllOp           inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitPeekReceiverOp     (PeekReceiverOp     inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitRetOp              (RetOp              inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitPhiOp              (PhiOp              inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitEnterOp            (EnterOp            inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitLeaveOp            (LeaveOp            inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitAllocateOp         (AllocateOp         inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitDeallocateOp       (DeallocateOp       inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitDataOp             (DataOp             inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitLiteralData        (LiteralData        inst);

    /**
     * Emitter interface function.
     *
     * @param inst the ir to emit
     */
    public abstract void emitFramePointerOp     (FramePointerOp     inst);


}