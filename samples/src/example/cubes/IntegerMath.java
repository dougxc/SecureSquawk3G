package example.cubes;

public final class IntegerMath {
    static final int sqq_table[] = {
           0,  16,  22,  27,  32,  35,  39,  42,  45,  48,  50,  53,  55,  57,
          59,  61,  64,  65,  67,  69,  71,  73,  75,  76,  78,  80,  81,  83,
          84,  86,  87,  89,  90,  91,  93,  94,  96,  97,  98,  99, 101, 102,
         103, 104, 106, 107, 108, 109, 110, 112, 113, 114, 115, 116, 117, 118,
         119, 120, 121, 122, 123, 124, 125, 126, 128, 128, 129, 130, 131, 132,
         133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 144, 145,
         146, 147, 148, 149, 150, 150, 151, 152, 153, 154, 155, 155, 156, 157,
         158, 159, 160, 160, 161, 162, 163, 163, 164, 165, 166, 167, 167, 168,
         169, 170, 170, 171, 172, 173, 173, 174, 175, 176, 176, 177, 178, 178,
         179, 180, 181, 181, 182, 183, 183, 184, 185, 185, 186, 187, 187, 188,
         189, 189, 190, 191, 192, 192, 193, 193, 194, 195, 195, 196, 197, 197,
         198, 199, 199, 200, 201, 201, 202, 203, 203, 204, 204, 205, 206, 206,
         207, 208, 208, 209, 209, 210, 211, 211, 212, 212, 213, 214, 214, 215,
         215, 216, 217, 217, 218, 218, 219, 219, 220, 221, 221, 222, 222, 223,
         224, 224, 225, 225, 226, 226, 227, 227, 228, 229, 229, 230, 230, 231,
         231, 232, 232, 233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238,
         239, 240, 240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246,
         246, 247, 247, 248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253,
         253, 254, 254, 255
    };

    static final int dividend[] = {
           0, 3050, 5582, 6159, 1627, 5440, 7578, 3703, 3659, 9461, 2993,
        4887,  226, 7813, 8902, 7469, 6260, 1054, 5473, 6862, 3701, 5349,
        4523,  978, 9455, 9604, 1604, 2008, 6059, 6048,    1, 3990, 6695,
        1275, 6679, 8431, 4456, 4841, 6329, 3241, 3302, 5690, 1901,  860,
        2796, 5741, 9414, 4187, 3469, 1923, 4309, 2870, 6152, 3863, 5473,
        5603, 6047, 3457, 1412, 5011, 2521, 5462, 3319, 1496, 3801, 4411,
        3244, 4157, 7398, 6297, 5625,  538, 5266, 8623, 8015,  652, 7317,
        9010, 5819,  374, 7325, 5375, 5393, 5859, 1997, 6283,  819, 2186,
        3281, 6565,    1
    };

    static final int divider[] = {
             1, 174761, 159945, 117682,  23324,  62417,  72497,  30385,
         26291,  60479,  17236,  25612,   1087,  34732,  36797,  28858,
         22711,   3605,  17711,  21077,  10821,  14926,  12074,   2503,
         23246,  22725,   3659,   4423,  12906,  12475,      2,   7747,
         12634,   2341,  11944,  14699,   7581,   8044,  10280,   5150,
         5137,    8673,   2841,   1261,   4025,   8119,  13087,   5725,
         4668,    2548,   5625,   3693,   7807,   4837,   6765,   6840,
         7294,    4122,   1665,   5846,   2911,   6245,   3759,   1679,
         4229,    4867,   3551,   4516,   7979,   6745,   5986,    569,
         5537,    9017,   8338,    675,   7541,   9247,   5949,    381,
         7438,    5442,   5446,   5903,   2008,   6307,    821,   2189,
         3283,    6566,      1
    };

    private static final int multiSin90(int multiplier, int sin) {
        return(multiplier * dividend[(int)sin] / divider[sin]);
    }

/*
    public static final int OLDmultiSin(int multiplier, int sin) {
        while (sin < 0) sin += 360;
        sin %= 360;

        if (sin <=  90) return multiSin90(multiplier, (int)sin);
        if (sin <= 180) return multiSin90(multiplier, (int)(180 - sin));
        if (sin <= 270) return -multiSin90(multiplier, (int)(sin - 180));
        return -multiSin90(multiplier, (int)(360 - sin));
    }
*/

    public static final int multiSin(int multiplier, int sin) {
        int fact;

        while (sin < 0) sin += 360;
        sin %= 360;
  //      while (sin > 360) sin -= 360;


        if (sin <=  90) {
            fact = 1;
        } else if (sin <= 180) {
            fact = 1;
            sin = 180 - sin;
        } else if (sin <= 270) {
            fact = -1;
            sin = sin - 180;
        } else {
            fact = -1;
            sin = 360 - sin;
        }

        //int res = multiSin90(multiplier, (int)sin);
        int res = multiplier * dividend[sin] / divider[sin];
        return (fact == 1) ? res : 0 - res;
    }

/*
    public static final int multiSin(int multiplier, int sin) {
        int old = OLDmultiSin(multiplier, sin);
        int nu  = NEWmultiSin(multiplier, sin);
        if (old != nu) {
            throw new RuntimeException();
        }
        return nu;

    }
*/

    public static final int OLDmultiCos(int multiplier, int cos) {
        return multiSin(multiplier, cos + 90);
    }


    public static final int multiCos(int multiplier, int sin) {
        int fact;

        sin += 90;

        while (sin < 0) sin += 360;
        sin %= 360;
        //while (sin > 360) sin -= 360;

        if (sin <=  90) {
            fact = 1;
        } else if (sin <= 180) {
            fact = 1;
            sin = 180 - sin;
        } else if (sin <= 270) {
            fact = -1;
            sin = sin - 180;
        } else {
            fact = -1;
            sin = 360 - sin;
        }

        //int res = multiSin90(multiplier, (int)sin);
        int res = multiplier * dividend[sin] / divider[sin];
//        int res = multiplier * ((dividend[sin]*65535) / divider[sin]);
//        res /= 65535;

        return (fact == 1) ? res : 0 - res;
    }





    // Integer Square Root function
    // Contributors include Arne Steinarson for the basic approximation idea, Dann
    // Corbit and Mathew Hendry for the first cut at the algorithm, Lawrence Kirby
    // for the rearrangement, improvments and range optimization and Paul Hsieh
    // for the round-then-adjust idea.
    public static final int sqrt(int x) {
        boolean round, adjust;
        int xn;

        round = adjust = true;
        x = Math.abs(x);

        if (x >= 0x10000)
            if (x >= 0x1000000)
                if (x >= 0x10000000)
                    if (x >= 0x40000000) {
                        if (x >= Integer.MAX_VALUE)
                            return 65535;
                        xn = sqq_table[x>>24] << 8;
                    } else
                        xn = sqq_table[x>>22] << 7;
                else
                    if (x >= 0x4000000)
                        xn = sqq_table[x>>20] << 6;
                    else
                        xn = sqq_table[x>>18] << 5;
            else {
                if (x >= 0x100000)
                    if (x >= 0x400000)
                        xn = sqq_table[x>>16] << 4;
                    else
                        xn = sqq_table[x>>14] << 3;
                else
                    if (x >= 0x40000)
                        xn = sqq_table[x>>12] << 2;
                    else
                        xn = sqq_table[x>>10] << 1;

                round = false;
            }
        else
            if (x >= 0x100) {
                if (x >= 0x1000)
                    if (x >= 0x4000)
                        xn = (sqq_table[x>>8] >> 0) + 1;
                    else
                        xn = (sqq_table[x>>6] >> 1) + 1;
                else
                    if (x >= 0x400)
                        xn = (sqq_table[x>>4] >> 2) + 1;
                    else
                        xn = (sqq_table[x>>2] >> 3) + 1;

                adjust = false;
            } else
                return sqq_table[x] >> 4;

        if (round) xn = (xn + 1 + x / xn) / 2;
        if (adjust) xn = (xn + 1 + x / xn) / 2;

        if (xn * xn > x)
           xn--;

        return xn;
    }

    public static final int abs(int n) {
        return n >=0 ? n : -n;
    }
}
