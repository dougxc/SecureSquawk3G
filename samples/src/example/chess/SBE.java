package example.chess;

/**
  Class containing Static Board Evaluator for a particular game.
 */
public abstract class SBE {



  /**
    Return an integer score indicating which side is winning in the board,
    b.
   */
  public abstract int evaluate( Board b );
}
