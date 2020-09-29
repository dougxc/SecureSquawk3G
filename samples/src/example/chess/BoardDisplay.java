package example.chess;

import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
  A java component used to display a connect-four board.
  */
class BoardDisplay extends Canvas implements Runnable, MouseListener {
  // Size of each image.
  // private static final int IMAGE_WIDTH = 50;
  // private static final int IMAGE_HEIGHT = 50;



  // RLL
  private int IMAGE_WIDTH = 50;

  private int IMAGE_HEIGHT = 50;

  private int current_image=0;

  private String imagePath = "";

  //private int IMAGE_WIDTH = 32;

  //private int IMAGE_HEIGHT = 32;

  //private int current_image=1;


  boolean autoflip = false;



  // Images for displaying the board contents (chess needs a lot of them)
  private static Image rr_pawn_image = null;
  private static Image rb_pawn_image = null;
  private static Image br_pawn_image = null;
  private static Image bb_pawn_image = null;
  private static Image rr_rook_image = null;
  private static Image rb_rook_image = null;
  private static Image br_rook_image = null;
  private static Image bb_rook_image = null;
  private static Image rr_knight_image = null;
  private static Image rb_knight_image = null;
  private static Image br_knight_image = null;
  private static Image bb_knight_image = null;
  private static Image rr_bishop_image = null;
  private static Image rb_bishop_image = null;
  private static Image br_bishop_image = null;
  private static Image bb_bishop_image = null;
  private static Image rr_queen_image = null;
  private static Image rb_queen_image = null;
  private static Image br_queen_image = null;
  private static Image bb_queen_image = null;
  private static Image rr_king_image = null;
  private static Image rb_king_image = null;
  private static Image br_king_image = null;
  private static Image bb_king_image = null;
  private static Image r_empty_image = null;
  private static Image b_empty_image = null;

  // Board object that this is responsible for displaying
  private Board my_board;

  // Added June 16, 1999 by RL

  private Board run_board;

  private Board ponder_board;

  private Move  lastMove;

  private String imageDir;

  // Applet containing this board display
  private Game my_appl;

  // Thread used to let the computer choose and make its move.
  private Thread computer_thread = null;
  private Thread ponder_thread = null;

  private Thread my_thread = null;


  // Which side gets to make the next move.
  // Zero when the game is over.
  private int turn = Board.R_SIDE;

  // Which side is the human?
  private int human_side = Board.R_SIDE;

  // Currently selected row and column
  private int selected_row = -1, selected_col;



  private boolean flip_board=false;



  public BoardDisplay( Board board, Game appl, String imageDir )
  {


    // Remember the applet that created us.
    my_appl = appl;

    // Remember the image we are supposed to display
    my_board = board;


    int len = imageDir.length();
    if (len > 0 && imageDir.charAt(len - 1) != '/' && imageDir.charAt(len - 1) == '\\') {
        imageDir += '/';
    }
    this.imageDir = imageDir;

    // RL

    addMouseListener(this);



    // RL July 10, 1999

    setImage(1);

    rr_pawn_image = null;

    loadImage();



  }


  public void destroy() {

//     removeMouseListener(this);

  }


  // RL July 10, 199

  private void loadImage()

  {

    Board board = my_board;

    Game appl = my_appl;

    Toolkit tk = Toolkit.getDefaultToolkit();



    // Media Tracker used to regulate image loading.
    MediaTracker tracker = new MediaTracker( appl );

    // Load images if we don't have them yet.
    if( rr_pawn_image == null ){

      int rs = Board.R_SIDE, bs = Board.B_SIDE;

      int p = Board.PAWN,   r = Board.ROOK,

        b = Board.BISHOP, n = Board.KNIGHT,

          q = Board.QUEEN,  k = Board.KING,

          x = Board.EMPTY;


      // Load each of the images we need
      byte []image;

      rr_pawn_image = tk.createImage( image=readImage(p, rs, rs), 0, image.length );
      tracker.addImage( rr_pawn_image, 0 );

      rb_pawn_image = tk.createImage( image=readImage(p, rs, bs), 0, image.length );
      tracker.addImage( rb_pawn_image, 0 );

      br_pawn_image = tk.createImage( image=readImage(p, bs, rs), 0, image.length );
      tracker.addImage( br_pawn_image, 0 );

      bb_pawn_image = tk.createImage( image=readImage(p, bs, bs), 0, image.length );
      tracker.addImage( bb_pawn_image, 0 );

      rr_rook_image = tk.createImage( image=readImage(r, rs, rs), 0, image.length );
      tracker.addImage( rr_rook_image, 0 );

      rb_rook_image = tk.createImage( image=readImage(r, rs, bs), 0, image.length );
      tracker.addImage( rb_rook_image, 0 );

      br_rook_image = tk.createImage( image=readImage(r, bs, rs), 0, image.length );
      tracker.addImage( br_rook_image, 0 );

      bb_rook_image = tk.createImage( image=readImage(r, bs, bs), 0, image.length );
      tracker.addImage( bb_rook_image, 0 );

      rr_knight_image = tk.createImage( image=readImage(n, rs, rs), 0, image.length );
      tracker.addImage( rr_knight_image, 0 );

      rb_knight_image = tk.createImage( image=readImage(n, rs, bs), 0, image.length );
      tracker.addImage( rb_knight_image, 0 );

      br_knight_image = tk.createImage( image=readImage(n, bs, rs), 0, image.length );
      tracker.addImage( br_knight_image, 0 );

      bb_knight_image = tk.createImage( image=readImage(n, bs, bs), 0, image.length );
      tracker.addImage( bb_knight_image, 0 );

      rr_bishop_image = tk.createImage( image=readImage(b, rs, rs), 0, image.length );
      tracker.addImage( rr_bishop_image, 0 );

      rb_bishop_image = tk.createImage( image=readImage(b, rs, bs), 0, image.length );
      tracker.addImage( rb_bishop_image, 0 );

      br_bishop_image = tk.createImage( image=readImage(b, bs, rs), 0, image.length );
      tracker.addImage( br_bishop_image, 0 );

      bb_bishop_image = tk.createImage( image=readImage(b, bs, bs), 0, image.length );
      tracker.addImage( bb_bishop_image, 0 );

      rr_queen_image = tk.createImage( image=readImage(q, rs, rs), 0, image.length );
      tracker.addImage( rr_queen_image, 0 );

      rb_queen_image = tk.createImage( image=readImage(q, rs, bs), 0, image.length );
      tracker.addImage( rb_queen_image, 0 );

      br_queen_image = tk.createImage( image=readImage(q, bs, rs), 0, image.length );
      tracker.addImage( br_queen_image, 0 );

      bb_queen_image = tk.createImage( image=readImage(q, bs, bs), 0, image.length );
      tracker.addImage( bb_queen_image, 0 );

      rr_king_image = tk.createImage( image=readImage(k, rs, rs), 0, image.length );
      tracker.addImage( rr_king_image, 0 );

      rb_king_image = tk.createImage( image=readImage(k, rs, bs), 0, image.length );
      tracker.addImage( rb_king_image, 0 );

      br_king_image = tk.createImage( image=readImage(k, bs, rs), 0, image.length );
      tracker.addImage( br_king_image, 0 );

      bb_king_image = tk.createImage( image=readImage(k, bs, bs), 0, image.length );
      tracker.addImage( bb_king_image, 0 );

      r_empty_image = tk.createImage( image=readImage(x, x, rs), 0, image.length );
      tracker.addImage( r_empty_image, 0 );

      b_empty_image = tk.createImage( image=readImage(x, x, bs), 0, image.length );
      tracker.addImage( b_empty_image, 0 );

      // Force the tracker to load all of our images.
      do{
        try{
          tracker.waitForAll();
        }
        catch( InterruptedException e ){
        }
      }while( !tracker.checkAll() );
    }




    // Set the size of this component according to what it contains.
    setSize( IMAGE_WIDTH * Board.WIDTH, IMAGE_HEIGHT * Board.HEIGHT );


    // Resize the Applet

    my_appl.setSize( IMAGE_WIDTH * Board.WIDTH+10, IMAGE_HEIGHT * Board.HEIGHT + 70);

    my_appl.validate();

  }

  /**
    Restart the game.
    */
  public void restart()
  {
    Board.startAll();



    // Kill of the computer move thread if it's trying to make a move.
    terminateComputerThread();

    // Reset the starting board configuration
    my_board.startingConfig();
    turn = Board.R_SIDE;
    human_side = Board.R_SIDE;
    selected_row = -1;
    flip_board=false;

    loadImage();


    // Redraw with the new configuration
    repaint();

    // Tell the user it's time to move.
    my_appl.setStateMessage( "Select a move." );



    lastMove = null;



    //startPondering();
  }

  /**
    Flip the board.
    */
  public void flip()
  {
    // Kill of the computer move thread if it's trying to make a move.
    terminateComputerThread();

    // Switch sides and switch who has the current turn.
    human_side = -human_side;
    turn = -turn;



  //  flip_board = !flip_board;



    switchTurns();
  }

  /**
    A java trick, don't bother to erase on an update.
    It all gets redrawn in paint() anyway.
    */
  public void update( Graphics g )
  {
    paint( g );
  }

  /**
    Paint a representation of the current board state.
   */
  public void paint( Graphics g )
  {
    int x, y;

    if (flip_board) y = IMAGE_HEIGHT*(Board.HEIGHT-1);

    else y = 0;
    for( int row = 0; row < Board.HEIGHT; row++ ){
      if (flip_board) x = IMAGE_WIDTH*(Board.WIDTH-1);

      else x = 0;
      for( int col = 0; col < Board.WIDTH; col++ ){
        // Fill in this space with the appropariate image
        Image img;
        if( ( row + col ) % 2 == 0 )
          img = r_empty_image;
        else
          img = b_empty_image;


        // changed June 15, 1999 by RL
        // int sym = my_board.space[row][col];

        int sym = my_board.Space(row, col);

        // Decide which image we should use for this space.
        if( sym == Board.R_PAWN )
          if( ( row + col ) % 2 == 0 )
            img = rr_pawn_image;
          else
            img = rb_pawn_image;

        if( sym == Board.B_PAWN )
          if( ( row + col ) % 2 == 0 )
            img = br_pawn_image;
          else
            img = bb_pawn_image;

        if( sym == Board.R_ROOK )
          if( ( row + col ) % 2 == 0 )
            img = rr_rook_image;
          else
            img = rb_rook_image;

        if( sym == Board.B_ROOK )
          if( ( row + col ) % 2 == 0 )
            img = br_rook_image;
          else
            img = bb_rook_image;

        if( sym == Board.R_KNIGHT )
          if( ( row + col ) % 2 == 0 )
            img = rr_knight_image;
          else
            img = rb_knight_image;

        if( sym == Board.B_KNIGHT )
          if( ( row + col ) % 2 == 0 )
            img = br_knight_image;
          else
            img = bb_knight_image;

        if( sym == Board.R_BISHOP )
          if( ( row + col ) % 2 == 0 )
            img = rr_bishop_image;
          else
            img = rb_bishop_image;

        if( sym == Board.B_BISHOP )
          if( ( row + col ) % 2 == 0 )
            img = br_bishop_image;
          else
            img = bb_bishop_image;

        if( sym == Board.R_QUEEN )
          if( ( row + col ) % 2 == 0 )
            img = rr_queen_image;
          else
            img = rb_queen_image;

        if( sym == Board.B_QUEEN )
          if( ( row + col ) % 2 == 0 )
            img = br_queen_image;
          else
            img = bb_queen_image;

        if( sym == Board.R_KING )
          if( ( row + col ) % 2 == 0 )
            img = rr_king_image;
          else
            img = rb_king_image;

        if( sym == Board.B_KING )
          if( ( row + col ) % 2 == 0 )
            img = br_king_image;
          else
            img = bb_king_image;

        // Draw the chosen image.
        g.drawImage( img, x, y, my_appl );

        if( selected_row == row && selected_col == col ){
          g.setColor( Color.green );
          g.drawRect( x, y, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1 );
          g.drawRect( x + 1, y + 1, IMAGE_WIDTH - 3, IMAGE_HEIGHT - 3 );
        }

        if(lastMove != null) {

          if (lastMove.from_row() == row && lastMove.from_col() == col ) {

            g.setColor( Color.blue );

            g.drawRect( x, y, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1 );

            g.drawRect( x + 1, y + 1, IMAGE_WIDTH - 3, IMAGE_HEIGHT - 3 );

          } else if (lastMove.to_row() == row && lastMove.to_col() == col ) {

            g.setColor( Color.red );

            g.drawRect( x, y, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1 );

            g.drawRect( x + 1, y + 1, IMAGE_WIDTH - 3, IMAGE_HEIGHT - 3 );

          }

        }



        // Step ahead to the next column on the screen.
        if (flip_board) x -= IMAGE_WIDTH;

        else x += IMAGE_WIDTH;
      }

      // Step ahead to the next row on the screen
      if (flip_board) y -= IMAGE_HEIGHT;

      else y += IMAGE_HEIGHT;
    }

if (autoflip) {
    Main.println("*** Flipped");
    autoflip = false;
    flip();
    ++flipCount;
}
  }

  int flipCount = 0;



  public void mousePressed(MouseEvent e) {

  }



  public void mouseClicked(MouseEvent e) {

  }



  public void mouseEntered(MouseEvent e) {

  }



  public void mouseExited(MouseEvent e) {

  }




  //
  // Respond to user mouse clicks, generally, when the user is making
  // a move.
  //
  // public boolean mouseDown(Event evt, int x, int y) {



  public void mouseReleased(MouseEvent e) {

    int x,y;

    if (flip_board){

      y = IMAGE_HEIGHT*Board.HEIGHT - e.getY();

      x = IMAGE_WIDTH*Board.WIDTH - e.getX();

    } else {

      y = e.getY();

      x = e.getX();

    }



    // Don't let the user choose anything if the game is
    // over or if it's the computer's turn to move.
    if( turn != human_side )
      return;

    // First, figure out row and column of the selected space
    int row = y / IMAGE_HEIGHT;
    int col = x / IMAGE_WIDTH;


    // Make sure the selected space is actually on the board
    if( row < 0 || col < 0 || row >= Board.HEIGHT ||
        col >= Board.WIDTH )
      return;

    // See if this is the choice for the source or destination position.
    if( selected_row >= 0 ){
      // See if the user is trying to un-select the chosen space.
      if( row == selected_row && col == selected_col ){
        // Flag the current selection as empty.
        selected_row = -1;
        repaint();
      } else {
        // Get the list of all legal moves from the current board.
        Move move_list = my_board.legalMoves( turn );
        Move m;

        // Look for the user's choice on that list.
        // changed June 16, 1999 by RL

        // change reference to from_row, from_col, to_row, to_col as methods

        // for( m = move_list; m != null &&
        //        ( m.from_row() != selected_row || m.from_col() != selected_col ||
        //          m.to_row() != row || m.to_col() != col );
        //      m = m.next );

        for( m = move_list; m != null &&

               ( m.from_row() != selected_row || m.from_col() != selected_col ||

                 m.to_row() != row ||

                ( m.to_col() != col &&

                  (m.piece() != Board.KING*human_side ||

                   (((selected_col-2 != col && col != 0) || m.from_col()-2 < m.to_col()) &&

                    ((selected_col+2 != col && col != Board.WIDTH-1) || m.from_col()+2 > m.to_col()) ) )

                ) );

             m = m.next );



        // If the chosen move is legal, apply it.
        if( m != null ){
          m.apply( my_board );

          lastMove = m;

          // Turn off the currently selected position.
          selected_row = -1;

          // Try to let the computer have a turn.
          switchTurns();
        }

        // RLL

         else if (my_board.Space(row,col)*human_side == -Board.KING &&

                  my_board.Space(selected_row,selected_col)*human_side == Board.KING){

                  toggleImage();

        }
      }
    } else {
      // Make sure the user selected a space occupied by one of their
      // pieces.

      // changed June 15, 1999 by RL
      // if( my_board.space[ row ][ col ] * human_side >= 1 ){
      if( my_board.Space(row,col) * human_side >= 1 ){

        selected_row = row;
        selected_col = col;
        repaint();
      }
    }

  }

  public void switchTurns()
  {

    int winner = my_board.checkForWin();

    // See if either side has won.
    if( winner != 0 ){
      turn = 0;

      // changed June 23,1999   by RL

      // win/lose/draw

      if( winner == -human_side )
        my_appl.setStateMessage( "Game over. You lose!" );
      else
      if( winner == human_side )

        my_appl.setStateMessage( "Game over. You Win!" );

      else

        my_appl.setStateMessage( "Game over. It's a Draw!" );

    } else {
      // No winner yet.  Can we switch Turns?
      if( my_board.legalMoves( -turn ) != null )
        // We can switch turns.
        turn = -turn;
      else
        // See if there are any legal moves left.
        if( my_board.legalMoves( turn ) == null )
          turn = 0;

      // Show a message for the current state.
      if( turn == 0 )
        my_appl.setStateMessage( "Game over. It's a tie." );

      if( turn == -human_side ){
        my_appl.setStateMessage( "Selecting computer move." );

        if (ponder_thread != null && ponder_board.Hint(lastMove))

          startThinking();

        else
          startComputerMove();
      }

      if( turn == human_side ) {
        my_appl.setStateMessage( "Select your move." );

        //startPondering();
//Main.println("*** Flip");
        autoflip =  true;

      }


    }

    repaint();

  }

  //
  // Terminate the computer thread early, before it has completed
  // making its move.
  //
  private void terminateComputerThread()
  {
    if( computer_thread != null ){

      // added June 16,1999 by RL

      if (run_board != null) run_board.stop();
      //computer_thread.stop();
      computer_thread = null;
    }
    terminatePonderThread();

  }

  //
  // Put the move made by the computer into the current board.
  // This is a separate method so that it can be synchronized.
  //
  private void submitComputerMove( Board alt_board )
  {


    // Switch over to the new alt_board.
    my_board = alt_board.Duplicate(my_board);



    lastMove = my_board.LastMove();


    // See if the game is over
    switchTurns();

    // Flag that the computer move is done.
    computer_thread = null;



    // RL
     //System.gc();

  }

  //
  // Select a new computer move, then exit.
  // Computer moves are made in a separate thread.  That way, we don't
  // tie up the user's thread for other UI activity.
  //
  // change June 16, 1999 by RL

  // change alt_board to run_board

  public void run()
  {

    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    Thread tmp=my_thread;



    if (ponder_thread == my_thread) {

      ponder_board = my_board.Duplicate(null);



      ponder_board.Ponder( -human_side );

    } else

    if (computer_thread == my_thread) {
      // Make a scratch copy of the current board to work with.
      // Board alt_board = my_board.Duplicate();
      run_board = my_board.Duplicate(null);


      // Now, let the computer choose a response.
      run_board.computerMove( -human_side );

    }


    if (computer_thread == tmp) {

      // Apply the computer's move to the main board.
      submitComputerMove( run_board );

    } else

    if (ponder_thread == tmp)

       ponder_thread = null;


  }

  /**
    Start up a spare thread and use it to let the computer choose it's move.
   */
  private void startComputerMove()
  {
    // Update the state of the applet.
    my_appl.disableDefer();



    terminatePonderThread();



    // Make a new thread in which the computer can choose
    // its move.

    computer_thread = new Thread( this );
    my_thread = computer_thread;

    computer_thread.start();
  }


  /**

    Start up a spare thread and use it to let the computer pondering on

    opponent's time.

   */

  private void startPondering()

  {

    ponder_thread = new Thread( this );

    my_thread = ponder_thread;

    ponder_thread.start();

  }



  private void terminatePonderThread()

  {

    if( ponder_thread != null ) {

      if (ponder_board != null) ponder_board.stop();

      // ponder_thread.stop();

      ponder_thread = null;

    }

  }



  private void startThinking()

  {

      Board tmp=run_board;

      run_board = ponder_board;

      ponder_board = tmp;

      computer_thread = ponder_thread;

      ponder_thread = null;

      run_board.Switch();

  }



  public void stop()

  {

     Board.stopAll();

     terminateComputerThread();

  }



  private String imageString(int p, int col, int sq)

  {

    if (current_image==0) {

      String[] ps = { "empty", "pawn", "rook", "knight", "bishop", "queen", "king"};

      String[] ss = { "r", "",  "b" };



      // "rb.pawn.gif" "r.empy.gif"

      return imageDir + ss[col+1] + ss[sq+1] + "." + ps[p] + ".gif";

    } else {

      String[] ps = { "0", "p", "r", "n", "b", "q", "k"};

      String[] ss = { "w", "",  "b" };



      // "bp-w.gif" "0-w.gif"

      return imageDir + ss[col+1] + ps[p] + "-" + ss[sq+1] + ".gif";

    }

  }

    private byte[] readImage(int p, int col, int sq) {
        String name = imageString(p, col, sq);
        byte[] img = readImage0("resource:"+name);
        if (img == null) {
            img = readImage0("file://"+name);
        }
        if (img == null) {
            System.err.println("Could not find '" + name + "' -- make sure that the base directory of this file (most likely '.') is part of the classpath");
            System.exit(-1);
        }
        return img;
    }

    private byte[] readImage0(String url) {
        try {
            InputStream in = javax.microedition.io.Connector.openInputStream(url);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ch;
            while ((ch = in.read()) != -1) {
                os.write((byte)ch);
            }
            byte[] res = os.toByteArray();
            in.close();
            os.close();
            return res;
        } catch(javax.microedition.io.ConnectionNotFoundException ex) {
            return null;
        } catch(IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
            return null;
        }
    }









  // RLL

  private void toggleImage()

  {



    if (current_image == 0)

      setImage(1);

    else

      setImage(0);



    // RL July 10, 199

    rr_pawn_image = null;

    loadImage();

    //System.gc();



  }





  private void setImage(int img)

  {

     current_image = img;

     switch(img)

     {

       case 0:

          IMAGE_WIDTH = 50;

          IMAGE_HEIGHT = 50;

          break;

       case 1:

          IMAGE_WIDTH = 32;

          IMAGE_HEIGHT = 32;

          break;

     }

  }



}

