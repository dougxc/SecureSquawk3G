package bench.cubes;

import java.io.*;
import java.util.Random;

public class CubeCanvas  {
    private Camera camera;
    private TPoint light;
    private TObject[] objectList;
    private TObject logo;
    private TObject sun;
    private TObject mercury;
    private TObject mercury2;
    private TObject mercury3;
    private TObject mercury4;
    private TObject earth;
    private TObject earth2;
    private TObject earth3;
    private TObject earth4;
    private TObject mars;
    private TObject mars2;
    private TObject mars3;
    private TObject mars4;
    private Random rnd;
    private int viewing_distance;
    private int[] screen_x;
    private int[] screen_y;
    private int[] x;
    private int[] y;
    private static int WIDTH =  240;
    private static int HEIGHT = 280;
    private TPoint camera_translation;
    private TPoint orig_camera_translation;
    private TPoint point;
    private boolean translate_camera;
    private boolean forward;
    private int camera_pitch_step;
    private int camera_yaw_step;
    private TObject tmp_object;

    // display and control
    private boolean paused = false;
    private boolean debug = false;
    //private boolean draw = true;
    public boolean sleep = true;
    private boolean timing_stats = false;
    private int activeOption;
    private int afps;
    private int rfps;
    private int fps_mode;
    private int rnow;
    private int rlast;
    private int frames;
    private int rframes;
   // private long draw_off_timing;

    // timing
    private int clear_cost;
    private int tmp_cost;
    private int poly_cost;
    private int blit_cost;
    private int render_cost;
    private int animation_cost;
    private int total_cost;
    private int last_total;

    // control constants
    private static final int FPS_REGULAR = 0;
    private static final int FPS_ACCUMULATE = 1;
    private static final int FPS_PAUSED = 2;
    private static final int POLY_COUNT = 0;
    private static final int DRAW = 1;
    private static final int PAUSE = 4;
    private static final int TIMING_STATS = 6;
    private static final int DEBUG_EASTEREGG = 37219;

   // private static final int ANCHOR = Graphics.TOP | Graphics.LEFT;



    int fake;
    int currentTimeMillis() {
        return fake++;
    }

    public CubeCanvas(int maxPaints) {
        this.maxPaints = maxPaints;
        int distance, factor, tmp, index = 4;

        screen_x = new int[250];
        screen_y = new int[250];
        x = new int[12];
        y = new int[12];

        rnd = new Random();

        objectList = new TObject[14];

        sun = objectList[0] = new TObject(getCubeData());
        sun.setColorIndex(ColorTable.getIndexForColor(ColorTable.red));
        sun.scaleDown(2);

        mercury = objectList[1] = new TObject(getCubeData());
        mercury.translate(0, 0, -300);
        mercury.setColorIndex(ColorTable.getIndexForColor(ColorTable.gray));
        mercury.scaleDown(2);
        mercury.rotationOrigin = sun.origLocation;

        mercury2 = objectList[2] = new TObject(getCubeData());
        mercury2.translate(0, 0, 300);
        mercury2.setColorIndex(ColorTable.getIndexForColor(ColorTable.gray));
        mercury2.scaleDown(2);
        mercury2.rotationOrigin = sun.origLocation;

        mercury3 = objectList[3] = new TObject(getCubeData());
        mercury3.translate(300, 0, 0);
        mercury3.setColorIndex(ColorTable.getIndexForColor(ColorTable.gray));
        mercury3.scaleDown(2);
        mercury3.rotationOrigin = sun.origLocation;

        mercury4 = objectList[4] = new TObject(getCubeData());
        mercury4.translate(-300, 0, 0);
        mercury4.setColorIndex(ColorTable.getIndexForColor(ColorTable.gray));
        mercury4.scaleDown(2);
        mercury4.rotationOrigin = sun.origLocation;

        earth = objectList[5] = new TObject(getCubeData());
        earth.translate(0, IntegerMath.multiSin(300, 180 + 60), IntegerMath.multiSin(300, 30));
        earth.setColorIndex(ColorTable.getIndexForColor(ColorTable.blue));
        earth.scaleDown(2);
        earth.rotationOrigin = sun.origLocation;

        earth2 = objectList[6] = new TObject(getCubeData());
        earth2.translate(0, IntegerMath.multiSin(300, 60), IntegerMath.multiSin(300, 180 + 30));
        earth2.setColorIndex(ColorTable.getIndexForColor(ColorTable.blue));
        earth2.scaleDown(2);
        earth2.rotationOrigin = sun.origLocation;

        earth3 = objectList[7] = new TObject(getCubeData());
        earth3.translate(0, IntegerMath.multiSin(300, 180 + 30), IntegerMath.multiSin(300, 180 + 60));
        earth3.setColorIndex(ColorTable.getIndexForColor(ColorTable.blue));
        earth3.scaleDown(2);
        earth3.rotationOrigin = sun.origLocation;

        earth4 = objectList[8] = new TObject(getCubeData());
        earth4.translate(0, IntegerMath.multiSin(300, 30), IntegerMath.multiSin(300, 60));
        earth4.setColorIndex(ColorTable.getIndexForColor(ColorTable.blue));
        earth4.scaleDown(2);
        earth4.rotationOrigin = sun.origLocation;

        mars = objectList[9] = new TObject(getCubeData());
        mars.translate(IntegerMath.multiSin(300, 180 + 30), IntegerMath.multiSin(300, 60), 0);
        mars.setColorIndex(ColorTable.getIndexForColor(ColorTable.yellow));
        mars.scaleDown(2);
        mars.rotationOrigin = sun.origLocation;

        mars2 = objectList[10] = new TObject(getCubeData());
        mars2.translate(IntegerMath.multiSin(300, 30), IntegerMath.multiSin(300, 180 + 60), 0);
        mars2.setColorIndex(ColorTable.getIndexForColor(ColorTable.yellow));
        mars2.scaleDown(2);
        mars2.rotationOrigin = sun.origLocation;

        mars3 = objectList[11] = new TObject(getCubeData());
        mars3.translate(IntegerMath.multiSin(300, 60), IntegerMath.multiSin(300, 30), 0);
        mars3.setColorIndex(ColorTable.getIndexForColor(ColorTable.yellow));
        mars3.scaleDown(2);
        mars3.rotationOrigin = sun.origLocation;

        mars4 = objectList[12] = new TObject(getCubeData());
        mars4.translate(IntegerMath.multiSin(300, 180 + 60), IntegerMath.multiSin(300, 180 + 30), 0);
        mars4.setColorIndex(ColorTable.getIndexForColor(ColorTable.yellow));
        mars4.scaleDown(2);
        mars4.rotationOrigin = sun.origLocation;

        logo = objectList[13] = new TObject(getLogoData());
        logo.translate(0, 0, -1200);
        logo.setColorIndex(ColorTable.getIndexForColor(ColorTable.red));
        logo.scaleUp(10);
        logo.setBaseLuminance(ColorTable.MAX_SHADES);

        camera = new Camera();
        camera.location = new TPoint(0, 0, -1300);

        camera_translation = new TPoint();
        translate_camera = false;

        light = new TPoint(-1000, 1000, -1000);
        point = new TPoint();

        rlast = currentTimeMillis() / 1000;
        fps_mode = FPS_REGULAR;
        viewing_distance = WIDTH * 3 / 2;
        forward = true;
    }

    public void sortObjects() {
        int i;

        for (i=0; i<objectList.length; i++) {
            tmp_object = objectList[i];
            tmp_object.distance = tmp_object.location.distance(camera.location);
        }

        for (i=1; i<objectList.length; i++) {
            for (int j=i; j>0 && objectList[j-1].distance < objectList[j].distance; j--) {
                tmp_object = objectList[j];
                objectList[j] = objectList[j - 1];
                objectList[j - 1] = tmp_object;
            }
        }

    }

    private void transformWorldToCamera(TPoint point) {
        // translate to camera location
        point.x -= camera.location.x;
        point.y -= camera.location.y;
        point.z -= camera.location.z;

        point.rotate(360 - camera.pitch, 360 - camera.yaw, 360 - camera.roll);
    }

    public void paint(/*Graphics graphics*/) {
        TObject object;
        TPolygon poly;
        String msg;
        int x_per, y_per;
        int poly_count, i, n;
        boolean clipped;

        clear_cost = currentTimeMillis();
        //if (draw) {
        //    graphics.setColor(ColorTable.black);
        //    graphics.fillRect(0, 0, Display.WIDTH, Display.HEIGHT);
        //}
        clear_cost = currentTimeMillis() - clear_cost;

        render_cost = currentTimeMillis();
        poly_count = 0;

        // sort objects by distance to
        // camera in ascending order
        sortObjects();


        // render objects
        for (n=0; n<objectList.length; n++) {
            object = objectList[n];

            // object space clipping
            point.assignFrom(object.location);
            transformWorldToCamera(point);
            if (point.z - object.radius < 30 || point.z + object.radius > 1000000
                || point.x + object.radius < ((-WIDTH * point.z) / (2 * viewing_distance))
                || point.x - object.radius > ((WIDTH * point.z) / (2 * viewing_distance))
                || point.y + object.radius < ((-HEIGHT * point.z) / (2 * viewing_distance))
                || point.y - object.radius > ((HEIGHT * point.z) / (2 * viewing_distance)))
            {
                continue;
            }

            // convert all object vertices
            // to screen points
            for (i=0; i<object.vertexList.length; i++) {
                point.assignFrom(object.vertexList[i]);
                point.translate(object.location);

                transformWorldToCamera(point);

                x_per = point.x * viewing_distance / point.z;
                y_per = point.y * viewing_distance / point.z;

                screen_x[i] = (int)(x_per + WIDTH/2);
                screen_y[i] = (int)(-y_per + HEIGHT/2);
            }


            object.sortPolygons(camera);

            // render polygons for current object
            poly_cost = 0;
            for (i=0; i<object.polyList.length; i++) {
                poly = object.polyList[i];

                poly.shadeAndCull(light, camera.location);

                if (!poly.isVisible) continue;

                clipped = false;

                for (int j=0; j<poly.vertexList.length; j++) {
                    x[j] = screen_x[poly.vertexList[j]];
                    y[j] = screen_y[poly.vertexList[j]];

                    if (x[j] <= 0 || x[j] >= WIDTH
                        || y[j] <= 0 || y[j] >= HEIGHT)
                    {
                        clipped = true;
                        break;
                    }
                }

                if (clipped) continue;

                tmp_cost = currentTimeMillis();
                //if (draw) {
                //    graphics.setColor(poly.color);
                //    graphics.fillPolygon(x, y, poly.vertexList.length);
                //}
                poly_cost += (currentTimeMillis() - tmp_cost);
                poly_count++;
            }
        }

        render_cost = currentTimeMillis() - render_cost;
        render_cost -= poly_cost;

        //if (draw) {
        //    graphics.setColor(ColorTable.white);
        //    graphics.drawString("afps: " + afps, 10, 10, ANCHOR);
        //    graphics.drawString("rfps: " + rfps, 100, 10, ANCHOR);
        //}

        //if (draw && timing_stats) {
        //    graphics.setColor(ColorTable.white);
        //    graphics.drawString("clear screen: " + clear_cost, 10, 110, ANCHOR);
        //    graphics.drawString("draw polys  : " + poly_cost, 10, 130, ANCHOR);
        //    graphics.drawString("screen blit : " + blit_cost, 10, 150, ANCHOR);
        //    graphics.drawString("render      : " + render_cost, 10, 170, ANCHOR);
        //    graphics.drawString("animation   : " + animation_cost, 10, 190, ANCHOR);
        //    graphics.drawString("total       : " + total_cost, 10, 210, ANCHOR);
        //}

        switch (activeOption) {
            case POLY_COUNT       : msg = "poly count: " + poly_count; break;
            case DRAW             : msg = "draw: " + (fps_mode == FPS_REGULAR ? "on" : (fps_mode == FPS_ACCUMULATE ? "off" : "paused")); break;
            case PAUSE            : msg = "paused: " + paused; break;
            case TIMING_STATS     : msg = "timing stats: " + timing_stats; break;
            default: msg = "";
        }


        //if (draw) {
        //    graphics.setColor(debug ? ColorTable.green : ColorTable.yellow);
        //    graphics.drawString(msg, 10, HEIGHT - 30, ANCHOR);
        //}

        rnow = currentTimeMillis() / 1000;
        if (rnow == rlast) {
            rframes++;
        } else {
            if (fps_mode != FPS_PAUSED) rfps = rframes;
            if (fps_mode != FPS_ACCUMULATE) rframes = 0;
            rlast = rnow;
        }

        blit_cost = currentTimeMillis();
        blit_cost = currentTimeMillis() - blit_cost;

        ++paints;
    }

    int paints = 0;
    final int maxPaints; // for benchmarking

    public void run() {
        int last = currentTimeMillis() / 1000;
        int now;
        int counter = 0;

        orig_camera_translation = new TPoint(0, 0, 80);
        last_total = currentTimeMillis();
        frames = 0;

        while (paints < maxPaints) {
            total_cost = currentTimeMillis() - last_total;
            last_total = currentTimeMillis();

            camera_translation.assignFrom(orig_camera_translation);
            camera_translation.rotate(camera.pitch, camera.yaw, camera.roll);

            if (translate_camera) camera.location.translate(camera_translation);
translate_camera = false;
            camera.pitch += camera_pitch_step;
            if (camera.pitch < 0) camera.pitch += 360;
            if (camera.pitch >= 360) camera.pitch -= 360;

            camera.yaw += camera_yaw_step;
camera_yaw_step = 0;
            if (camera.yaw < 0) camera.yaw += 360;
            if (camera.yaw >= 360) camera.yaw -= 360;


            if (!paused) {
                logo.rotate_x -= 1;
                logo.rotate_z -= 1;

                sun.rotate_x += 1;
                sun.rotate_z += 1;

                mercury.rotate_x += -2;
                mercury.rotate_z += -2;
                mercury.object_rotate_y -= 2;

                mercury2.rotate_x += -2;
                mercury2.rotate_z += -2;
                mercury2.object_rotate_y -= 2;

                mercury3.rotate_x += -2;
                mercury3.rotate_z += -2;
                mercury3.object_rotate_y -= 2;

                mercury4.rotate_x += -2;
                mercury4.rotate_z += -2;
                mercury4.object_rotate_y -= 2;

                earth.rotate_x += 2;
                earth.rotate_z += -2;
                earth.object_rotate_x -= 2;

                earth2.rotate_x += 2;
                earth2.rotate_z += -2;
                earth2.object_rotate_x -= 2;

                earth3.rotate_x += 2;
                earth3.rotate_z += -2;
                earth3.object_rotate_x -= 2;

                earth4.rotate_x += 2;
                earth4.rotate_z += -2;
                earth4.object_rotate_x -= 2;

                mars.rotate_x += 2;
                mars.rotate_z += 2;
                mars.object_rotate_z -= 2;

                mars2.rotate_x += 2;
                mars2.rotate_z += 2;
                mars2.object_rotate_z -= 2;

                mars3.rotate_x += 2;
                mars3.rotate_z += 2;
                mars3.object_rotate_z -= 2;

                mars4.rotate_x += 2;
                mars4.rotate_z += 2;
                mars4.object_rotate_z -= 2;

                for (int i=0; i<objectList.length; i++) {
                    objectList[i].animate();
                }
            }

            animation_cost = currentTimeMillis() - last_total;

            //repaint();
            paint();

            //if (sleep) {
            //    try {Thread.sleep(30);}
            //    catch (InterruptedException ie) {}
            //}

            now = currentTimeMillis() / 1000;
            if (now == last) {
                frames++;
            } else {
                if (fps_mode != FPS_PAUSED) afps = frames;
                if (fps_mode != FPS_ACCUMULATE) frames = 0;
                last = now;
            }

            counter++;
        }
    }
/*
    public void keyPressed(int key) {
        key = getGameAction(key);

        if (key == KEY_NUM0) {
            debug = !debug;
        }

        if (debug) {
            if (key == UP) {
                activeOption--;
                if (activeOption < POLY_COUNT) activeOption = TIMING_STATS;
            }

            if (key == DOWN) {
                activeOption++;
                if (activeOption > TIMING_STATS) activeOption = POLY_COUNT;
            }

            if (key == LEFT || key == RIGHT) {
                switch (activeOption) {
                    case DRAW        : {
                        if (fps_mode == FPS_REGULAR) {
                            draw = false;
                            fps_mode = FPS_ACCUMULATE;
                            draw_off_timing = currentTimeMillis();
                        } else if (fps_mode == FPS_ACCUMULATE) {
                            draw_off_timing = currentTimeMillis() - draw_off_timing;
                            fps_mode = FPS_PAUSED;
                            paused = true;
                            afps = frames * 1000 / (int)draw_off_timing;
                            rfps = rframes * 1000 / (int)draw_off_timing;
                            draw = true;
                        } else if (fps_mode == FPS_PAUSED) {
                            paused = false;
                            frames = rframes = 0;
                            fps_mode = FPS_REGULAR;
                        }

                        break;
                    }

                    case PAUSE       : paused = !paused; break;
                    case TIMING_STATS: timing_stats = !timing_stats; break;
                }
            }
        } else {
            if (key == UP) {
                if (!forward) orig_camera_translation.invert();

                translate_camera = true;
                forward = true;
            }

            if (key == DOWN) {
                if (forward) orig_camera_translation.invert();

                translate_camera = true;
                forward = false;
            }

            if (key == LEFT) camera_yaw_step = -2;
            if (key == RIGHT) camera_yaw_step = 2;
        }
    }

    public void keyReleased(int key) {
        key = getGameAction(key);

//        if (key == UP) translate_camera = false;
//        if (key == DOWN) translate_camera = false;
//        if (key == LEFT) camera_yaw_step = 0;
//        if (key == RIGHT) camera_yaw_step = 0;
    }
*/
    private InputStream getCubeData() {
        String str = ""
            + "cube 8 6\n"
            + "-100 100 -100\n"
            + "-100 -100 -100\n"
            + "100 -100 -100\n"
            + "100 100 -100\n"
            + "-100 100 100\n"
            + "-100 -100 100\n"
            + "100 -100 100\n"
            + "100 100 100\n"
            + "0x1ffa 4 0 1 2 3\n"
            + "0x1ffa 4 4 0 3 7\n"
            + "0x1ffa 4 3 2 6 7\n"
            + "0x1ffa 4 4 5 1 0\n"
            + "0x1ffa 4 1 5 6 2\n"
            + "0x1ffa 4 7 6 5 4\n";

        return new ByteArrayInputStream(str.getBytes());
    }

    private InputStream getLogoData() {
        String str = ""
            + "kvm 39 12\n"
            + "-42 -3 0\n"
            + "-36 5 0\n"
            + "-29 -16 0\n"
            + "-37 -16 0\n"
            + "-45 2 0\n"
            + "-37 16 0\n"
            + "-24 16 0\n"
            + "-45 -8 0\n"
            + "-53 -16 0\n"
            + "-53 16 0\n"
            + "-45 16 0\n"
            + "-45 -16 0\n"
            + "-3 -16 0\n"
            + "5 16 0\n"
            + "13 16 0\n"
            + "3 -16 0\n"
            + "-5 -16 0\n"
            + "-13 16 0\n"
            + "-5 16 0\n"
            + "1 -16 0\n"
            + "23 16 0\n"
            + "23 -16 0\n"
            + "29 -16 0\n"
            + "29 16 0\n"
            + "29 9 0\n"
            + "36 9 0\n"
            + "34 16 0\n"
            + "31 0 0\n"
            + "38 0 0\n"
            + "35 -16 0\n"
            + "41 -16 0\n"
            + "45 0 0\n"
            + "47 9 0\n"
            + "40 9 0\n"
            + "47 16 0\n"
            + "42 16 0\n"
            + "47 -16 0\n"
            + "53 -16 0\n"
            + "53 16 0\n"
            + "0x8009 4 3 2 1 0\n"
            + "0x8009 4 7 6 5 4\n"
            + "0x8009 4 11 10 9 8\n"
            + "0x8009 4 15 14 13 12\n"
            + "0x8009 4 19 18 17 16\n"
            + "0x8009 4 20 21 22 23\n"
            + "0x8009 4 23 24 25 26\n"
            + "0x8009 4 24 27 28 25\n"
            + "0x8009 4 27 29 30 31\n"
            + "0x8009 4 33 28 31 32\n"
            + "0x8009 4 35 33 32 34\n"
            + "0x8009 4 34 36 37 38\n";

        return new ByteArrayInputStream(str.getBytes());
    }
}
