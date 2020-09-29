package example.cubes;

import java.io.*;

public class TObject {
    public TPoint[] vertexList;
    public TPoint[] origVertexList;
	public TPolygon[] polyList;
    public TPoint location;
    public TPoint origLocation;
    public TPoint rotationOrigin;
    public int rotate_x = 0;
    public int rotate_y = 0;
    public int rotate_z = 0;
    public int object_rotate_x = 0;
    public int object_rotate_y = 0;
    public int object_rotate_z = 0;
    public int distance;
    public int radius;
    private static TPolygon tmp_poly;
    private static TPoint point;

    public TObject() {
        point = new TPoint();
        location = new TPoint();
        origLocation = new TPoint();
    }


    // create new instance from PLG file
    public TObject(InputStream raw) {
        this();

        try {
            InputStreamReader isr = new InputStreamReader(raw);
            LineReader in = new LineReader(isr);
            int color_index, vertexCount, polyCount, i, j, n;
            Tokenizer st;
            String line;

            for (;;) {
                line = in.readLine();
                if (line == null) break;

                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') continue;

                st = new Tokenizer(line);
                st.nextToken();
                vertexCount = Integer.parseInt(st.nextToken());
                polyCount = Integer.parseInt(st.nextToken());

                vertexList = new TPoint[vertexCount];
                origVertexList = new TPoint[vertexCount];
                for (i=0; i<vertexCount; i++) {
                    st = new Tokenizer(in.readLine());
                    vertexList[i] = new TPoint(
                        Integer.parseInt(st.nextToken()),
                        Integer.parseInt(st.nextToken()),
                        Integer.parseInt(st.nextToken())
                    );
                    origVertexList[i] = new TPoint();
                    origVertexList[i].assignFrom(vertexList[i]);
                }

                polyList = new TPolygon[polyCount];
                for (i=0; i<polyCount; i++) {
                    line = in.readLine();
                    line = line.substring(2);
                    st = new Tokenizer(line);

                    polyList[i] = new TPolygon(this);

                    n = Integer.parseInt(st.nextToken(), 16);
                    if ((n & 0x8000) != 0) polyList[i].doubleSided = true;

                    switch (n) {
                        case 0x17FA: color_index = 0; break;
                        case 0x1AFA: color_index = 1; break;
                        case 0x1FFA: color_index = 2; break;
                        default: color_index = 3;
                    }

                    polyList[i].color_index = color_index;

                    vertexCount = Integer.parseInt(st.nextToken());
                    polyList[i].vertexList = new int[vertexCount];
                    for (j=0; j<vertexCount; j++) {
                        polyList[i].vertexList[j] = Integer.parseInt(st.nextToken());
                    }
                }
            }

            calcRadius();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void translate(int x, int y, int z) {
        location.x += x;
        location.y += y;
        location.z += z;

        origLocation.x = location.x;
        origLocation.y = location.y;
        origLocation.z = location.z;
    }

    public void calcRadius() {
        int r;

        radius = 0;
        for (int i=0; i<vertexList.length; i++) {
            r = TPoint.ORIGIN.distance(vertexList[i]);
            if (r > radius) radius = r;
        }
    }

    public void setBaseLuminance(int base_luminance) {
        for (int i=0; i<polyList.length; i++) {
            polyList[i].base_luminance = base_luminance;
        }
    }

    public void commitRotation() {
        for (int i=0; i<vertexList.length; i++) {
            origVertexList[i].assignFrom(vertexList[i]);
        }
    }

    public void reset() {
        for (int i=0; i<vertexList.length; i++) {
            vertexList[i].assignFrom(origVertexList[i]);
        }

        location.assignFrom(origLocation);
    }

    public void scaleUp(int factor) {
        radius *= factor;

        for (int i=0; i<vertexList.length; i++) {
            vertexList[i].scaleUp(factor);
            origVertexList[i].scaleUp(factor);
        }
    }

    public void scaleDown(int factor) {
        radius /= factor;

        for (int i=0; i<vertexList.length; i++) {
            vertexList[i].scaleDown(factor);
            origVertexList[i].scaleDown(factor);
        }
    }

    public void setColorIndex(int color_index) {
        for (int i=0; i<polyList.length; i++) {
            polyList[i].color_index = color_index;
        }
    }

    public void sortPolygons(Camera camera) {
        if (polyList == null) return;

        for (int i=0; i<polyList.length; i++) {
            polyList[i].calcMaximumDistance(camera.location);
        }

        if (polyList.length <= 20) insertionSort(camera.location);
        else shellSort(camera.location);
    }

    private void insertionSort(TPoint p) {
        for (int i=1; i<polyList.length; i++) {
            for (int j=i; j>0 && polyList[j-1].max_distance < polyList[j].max_distance; j--) {
                tmp_poly = polyList[j];
                polyList[j] = polyList[j - 1];
                polyList[j - 1] = tmp_poly;
            }
        }
    }

    private void shellSort(TPoint p) {
        int inc, i, j;

        for (inc = polyList.length; inc > 0;) {
            for (i=inc; i < polyList.length; i++) {
                j = i;
                tmp_poly= polyList[i];

                while (j >= inc && tmp_poly.max_distance > polyList[j - inc].max_distance) {
                    polyList[j] = polyList[j - inc];
                    j -= inc;
                }

                polyList[j] = tmp_poly;
            }

            inc = (inc > 1) && (inc < 5) ? 1 : 5 * inc / 11;
        }
    }

    public void rotate(int pitch, int yaw, int roll) {
        for (int i=0; i<vertexList.length; i++) {
            vertexList[i].rotate(pitch, yaw, roll);
        }
    }

    public void objectRotate(int pitch, int yaw, int roll) {
        if (rotationOrigin == null || (pitch == 0 && yaw == 0 && roll == 0)) return;

        point.x = location.x - rotationOrigin.x;
        point.y = location.y - rotationOrigin.y;
        point.z = location.z - rotationOrigin.z;

        point.rotate(pitch, yaw, roll);

        location.x = rotationOrigin.x + point.x;
        location.y = rotationOrigin.y + point.y;
        location.z = rotationOrigin.z + point.z;
    }

    public void animate() {
        reset();
        objectRotate(object_rotate_x, object_rotate_y, object_rotate_z);
        rotate(rotate_x, rotate_y, rotate_z);
	}

    class LineReader {
        private Reader in;

        public LineReader(Reader in) {
            this.in = in;
        }

        public String readLine() {
            StringBuffer buf = new StringBuffer();
            int c = -1;

            for (;;) {
                try {c = in.read();}
                catch (IOException e) {return null;}

                if (c < 0) return null;

                if (c == '\n') break;

                buf.append((char)c);
            }

            return buf.toString();
        }
    }

    class Tokenizer {
        private char[] buf;
        int offset;

        public Tokenizer(String line) {
            offset = 0;
            buf = new char[line.length() + 1];
            System.arraycopy(line.toCharArray(), 0, buf, 0, line.length());
            buf[line.length()] = ' ';
            eatSpace();
        }

        private final void eatSpace() {
            while (offset < buf.length
                && (buf[offset] == ' ' || buf[offset] == '\t'))
            {offset++;}
        }

        public String nextToken() {
            int count = 0;

            for (int i = offset; i<buf.length; i++, count++) {
                if (buf[i] == ' ' || buf[i] == '\t') {
                    String str = new String(buf, offset, count);
                    offset = i;
                    eatSpace();
                    return str;
                }
            }

            return null;
        }
    }
}
