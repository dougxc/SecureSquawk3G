package example.chess;


/**
  Abstract class containing the a move generator for a particular game.
 */
public abstract class Generator {
  /**
    Generate a list of legal moves from the given board configuration.
   */
  public abstract Move generateMoves( Board b, int side );
}
