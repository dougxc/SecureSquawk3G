package awtcore;

import java.awt.*;

public class SimpleGridBagLayout implements LayoutManager {


    // constraints: x,y , w, h, colspan, rowspan, [fillX, fillY, ] weightX, weightY

    public void addLayoutComponent (String constraints, Component component) {
    }

    public void layoutContainer (Container container) {
    }

    public void removeLayoutComponent (Component component) {
    }

    public Dimension preferredLayoutSize (Container container) {
    return minimumLayoutSize (container);

    }

    public Dimension minimumLayoutSize (Container container) {
    throw new RuntimeException ();
    }
}
