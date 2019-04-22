package com.nuvoton.opengl

data class RGB_COLOR_INFO_T(var R: Int = 0, var G: Int = 0, var B: Int = 0)

class NuColor {
    companion object {
        val COLOR_TABLE_LOWER_SIZE = 2131  // -273.1 Celsius degree to -60   Celsius degree
        val COLOR_TABLE_MIDDLE_SIZE = 1200 //    -60 Celsius degree to 60    Celsius degree
        val COLOR_TABLE_UPPER_SIZE = 4859  //     60 Celsius degree to 545.9 Celsius degree

        val RGB_ColorTable0 = arrayListOf(
            RGB_COLOR_INFO_T(	176, 	0,		240),	//	0.25
            RGB_COLOR_INFO_T(	142,	0,		240),	//	0.75
            RGB_COLOR_INFO_T(	108,	0,		240),	//	1.25
            RGB_COLOR_INFO_T(	65,		0,		240),	//	1.75
            RGB_COLOR_INFO_T(	0,		0,		235),	//	2.25
            RGB_COLOR_INFO_T(	0,		0,		218),	//	2.75
            RGB_COLOR_INFO_T(	0,		0,		201),	//	3.25
            RGB_COLOR_INFO_T(	0,		0,		184),	//	3.75
            RGB_COLOR_INFO_T(	0,		0,		163),	//	4.25
            RGB_COLOR_INFO_T(	0,		19,		133),	//	4.75
            RGB_COLOR_INFO_T(	0,		48,		110),	//	5.25
            RGB_COLOR_INFO_T(	0,		74,		104),	//	5.75
            RGB_COLOR_INFO_T(	0,		100,	110),	//	6.25
            RGB_COLOR_INFO_T(	0,		97,		119),	//	6.75
            RGB_COLOR_INFO_T(	0,		104,	151),	//	7.25
            RGB_COLOR_INFO_T(	0,		119,	160),	//	7.75
            RGB_COLOR_INFO_T(	0,		136,	160),	//	8.25
            RGB_COLOR_INFO_T(	0,		153,	168),	//	8.75
            RGB_COLOR_INFO_T(	0,		170,	170),	//	9.25
            RGB_COLOR_INFO_T(	0,		189,	189),	//	9.75
            RGB_COLOR_INFO_T(	0,		206,	206),	//	10.25
            RGB_COLOR_INFO_T(	0,		219,	219),	//	10.75
            RGB_COLOR_INFO_T(	0,		228,	228),	//	11.25
            RGB_COLOR_INFO_T(	0,		236,	236),	//	11.75
            RGB_COLOR_INFO_T(	0,		240,	225),	//	12.25
            RGB_COLOR_INFO_T(	0,		235,	204),	//	12.75
            RGB_COLOR_INFO_T(	0,		232,	155),	//	13.25
            RGB_COLOR_INFO_T(	0,		225,	117),	//	13.75
            RGB_COLOR_INFO_T(	0,		217,	119),	//	14.25
            RGB_COLOR_INFO_T(	0,		200,	119),	//	14.75
            RGB_COLOR_INFO_T(	0,		184,	110),	//	15.25
            RGB_COLOR_INFO_T(	0,		174,	80),	//	15.75
            RGB_COLOR_INFO_T(	0,		157,	80),	//	16.25
            RGB_COLOR_INFO_T(	0,		140,	80),	//	16.75
            RGB_COLOR_INFO_T(	0,		133,	72),	//	17.25
            RGB_COLOR_INFO_T(	0,		140,	34),	//	17.75
            RGB_COLOR_INFO_T(	25,		142,	0),		//	18.25
            RGB_COLOR_INFO_T(	55,		160,	0),		//	18.75
            RGB_COLOR_INFO_T(	65,		177,	0),		//	19.25
            RGB_COLOR_INFO_T(	82,		194,	0),		//	19.75
            RGB_COLOR_INFO_T(	104,	206,	0),		//	20.25
            RGB_COLOR_INFO_T(	131,	214,	0),		//	20.75
            RGB_COLOR_INFO_T(	157,	223,	0),		//	21.25
            RGB_COLOR_INFO_T(	189,	231,	0),		//	21.75
            RGB_COLOR_INFO_T(	231,	232,	0),		//	22.25
            RGB_COLOR_INFO_T(	224,	223,	0),		//	22.75
            RGB_COLOR_INFO_T(	224,	206,	0),		//	23.25
            RGB_COLOR_INFO_T(	224,	180,	0),		//	23.75
            RGB_COLOR_INFO_T(	224,	153,	0),		//	24.25
            RGB_COLOR_INFO_T(	224,	128,	0),		//	24.75
            RGB_COLOR_INFO_T(	224,	102,	0),		//	25.25
            RGB_COLOR_INFO_T(	224,	76,		0),		//	25.75
            RGB_COLOR_INFO_T(	224,	51,		0),		//	26.25
            RGB_COLOR_INFO_T(	219,	17,		0),		//	26.75
            RGB_COLOR_INFO_T(	206,	0,		0),		//	27.25
            RGB_COLOR_INFO_T(	183,	0,		0),		//	27.75
            RGB_COLOR_INFO_T(	157,	0,		0),		//	28.25
            RGB_COLOR_INFO_T(	131,	0,		0),		//	28.75
            RGB_COLOR_INFO_T(	104,	0,		0),		//	29.25
            RGB_COLOR_INFO_T(	80,		0,		0),		//	29.75
            RGB_COLOR_INFO_T(	80,		0,		0)		//	29.75
        )
        
        val RGB_ColorTable : Array<RGB_COLOR_INFO_T> by lazy {
            val colorTableArray = Array<RGB_COLOR_INFO_T>(8190) { RGB_COLOR_INFO_T(0, 0, 0) }
            for (i in 0 until COLOR_TABLE_LOWER_SIZE) {
                val array = RGB_ColorTable0[0]
                colorTableArray[i] = array
            }

            for (i in 0 until 60) {
                for (j in 0 until 20) {
                    colorTableArray[i * 20 + j + COLOR_TABLE_LOWER_SIZE].R = (RGB_ColorTable0[i].R.toFloat() + (RGB_ColorTable0[i+1].R.toFloat() - RGB_ColorTable0[i].R.toFloat()) / 20 * j).toInt();
                    colorTableArray[i * 20 + j + COLOR_TABLE_LOWER_SIZE].G = (RGB_ColorTable0[i].G.toFloat() + (RGB_ColorTable0[i+1].G.toFloat() - RGB_ColorTable0[i].G.toFloat()) / 20 * j).toInt();
                    colorTableArray[i * 20 + j + COLOR_TABLE_LOWER_SIZE].B = (RGB_ColorTable0[i].B.toFloat() + (RGB_ColorTable0[i+1].B.toFloat() - RGB_ColorTable0[i].B.toFloat()) / 20 * j).toInt();
                }
            }

            for ( i in COLOR_TABLE_LOWER_SIZE + COLOR_TABLE_MIDDLE_SIZE until COLOR_TABLE_UPPER_SIZE) {
                colorTableArray[i].R = RGB_ColorTable0[60].R;
                colorTableArray[i].G = RGB_ColorTable0[60].G;
                colorTableArray[i].B = RGB_ColorTable0[60].B;
            }
            colorTableArray
        }
    }
}