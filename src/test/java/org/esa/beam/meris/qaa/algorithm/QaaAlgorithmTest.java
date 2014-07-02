package org.esa.beam.meris.qaa.algorithm;

import org.esa.beam.meris.qaa.ImaginaryNumberException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

public class QaaAlgorithmTest {

    private QaaAlgorithm algorithm;

    @Before
    public void setUp() {
        algorithm = new QaaAlgorithm();
    }

    @Test
    public void testProcess_MERIS() throws ImaginaryNumberException {
        final float[] rrs = {0.030262154f, 0.031086152f, 0.022717977f, 0.013177891f, 0.0072450927f, 0.0028870495f, 0.0024475828f};

        final QaaResult result = algorithm.process(rrs, null);

        final int flags = result.getFlags();
        assertEquals(1, flags);

        final float[] a_total = result.getA_Total();
        assertEquals(QaaConstants.NUM_A_TOTAL_BANDS, a_total.length);
        assertEquals(0.03845500573515892f, a_total[0], 1e-8);
        assertEquals(0.030030209571123123f, a_total[1], 1e-8);
        assertEquals(0.030713409185409546f, a_total[2], 1e-8);
        assertEquals(0.046738818287849426f, a_total[3], 1e-8);
        assertEquals(0.06614950299263f, a_total[4], 1e-8);

        final float[] bb_spm = result.getBB_SPM();
        assertEquals(QaaConstants.NUM_BB_SPM_BANDS, bb_spm.length);
        assertEquals(0.007518719881772995f, bb_spm[0], 1e-8);
        assertEquals(0.006027825176715851f, bb_spm[1], 1e-8);
        assertEquals(0.004540313966572285f, bb_spm[2], 1e-8);
        assertEquals(0.0040666270069777966f, bb_spm[3], 1e-8);
        assertEquals(0.0032066269777715206f, bb_spm[4], 1e-8);

        final float[] a_pig = result.getA_PIG();
        assertEquals(QaaConstants.NUM_A_PIG_BANDS, a_pig.length);
        assertEquals(0.0028468116652220488f, a_pig[0], 1e-8);
        assertEquals(0.0036492901854217052f, a_pig[1], 1e-8);
        assertEquals(0.006425064522773027f, a_pig[2], 1e-8);

        final float[] a_ys = result.getA_YS();
        assertEquals(QaaConstants.NUM_A_YS_BANDS, a_ys.length);
        assertEquals(0.030918193981051445f, a_ys[0], 1e-8);
        assertEquals(0.019170919433236122f, a_ys[1], 1e-8);
        assertEquals(0.009288343600928783f, a_ys[2], 1e-8);
    }

    // @todo 3 tb/tb test divide by PI 2013-02-22

    @Test
    public void testSetGetConfig() {
        final QaaConfig config = new QaaConfig();

        algorithm.setConfig(config);
        assertEquals(config, algorithm.getConfig());
    }

    @Test
    public void testAlgorithmInitiallyHasConfig() {
        assertNotNull(algorithm.getConfig());
    }

    @Test
    public void testEnsureResult() {
        QaaResult result = QaaAlgorithm.ensureResult(null);
        assertNotNull(result);

        final QaaResult recycle = new QaaResult();
        result = QaaAlgorithm.ensureResult(recycle);
        assertNotNull(result);
        assertSame(recycle, result);
    }

    @Test
    public void testIsOutOfBounds() {
        assertFalse(QaaAlgorithm.isOutOfBounds(2, 1, 3));
        assertFalse(QaaAlgorithm.isOutOfBounds(-1.45f, -2, -1));
        assertFalse(QaaAlgorithm.isOutOfBounds(-0.45f, -1, 1));

        assertTrue(QaaAlgorithm.isOutOfBounds(-4, 1, 3));
        assertTrue(QaaAlgorithm.isOutOfBounds(0, -2, -1));
        assertTrue(QaaAlgorithm.isOutOfBounds(1.1f, -1, 1));
    }

//    Product:	MER_FRS_2PNMAP20110521_082527_000001943102_00366_48225_0001.N1
//
//    Image-X:	624	pixel
//    Image-Y:	3429	pixel
//    Longitude:	24°59'16" E	degree
//    Latitude:	32°17'59" N	degree
//
//    BandName	Wavelength	Unit	Bandwidth	Unit	Value	Unit	Solar Flux	Unit
//    reflec_1:	412.691	nm	9.937	nm	0.030262154	dl	1675.9521	mW/(m^2*nm)
//    reflec_2:	442.55902	nm	9.946	nm	0.031086152	dl	1836.3625	mW/(m^2*nm)
//    reflec_3:	489.88202	nm	9.957001	nm	0.022717977	dl	1884.6876	mW/(m^2*nm)
//    reflec_4:	509.81903	nm	9.961	nm	0.013177891	dl	1885.2732	mW/(m^2*nm)
//    reflec_5:	559.69403	nm	9.97	nm	0.0072450927	dl	1762.2621	mW/(m^2*nm)
//    reflec_6:	619.601	nm	9.979	nm	0.0028870495	dl	1613.4071	mW/(m^2*nm)
//    reflec_7:	664.57306	nm	9.985001	nm	0.0024475828	dl	1496.9568	mW/(m^2*nm)
//    reflec_8:	680.82104	nm	7.4880004	nm	0.0020630497	dl	1438.8444	mW/(m^2*nm)
//    reflec_9:	708.32904	nm	9.992001	nm	7.4465E-4	dl	1376.0726	mW/(m^2*nm)
//    reflec_10:	753.37103	nm	7.4950004	nm	4.3336122E-4	dl	1237.3849	mW/(m^2*nm)
//    reflec_12:	778.40906	nm	15.010001	nm	1.4038353E-4	dl	1150.6112	mW/(m^2*nm)
//    reflec_13:	864.87604	nm	20.047	nm	-1.3428307E-4	dl	936.69135	mW/(m^2*nm)
//    reflec_14:	884.94403	nm	10.018001	nm	-6.105316E-6	dl	908.79034	mW/(m^2*nm)
//    water_vapour:					0.29999998	g/cm^2
//    algal_1:					0.1517408	mg/m^3
//    algal_2:					0.026619313	mg/m^3
//    yellow_subs:					0.017718133	1/m
//    total_susp:					0.24312341	g/m^3
//    photosyn_rad:					1860.9502	myEinstein/(m^2*s)
//    toa_veg:					NaN	1
//    boa_veg:					NaN	1
//    rect_refl_red:					NaN	dl
//    rect_refl_nir:					NaN	dl
//    surf_press:					NaN	hPa
//    aero_alpha:					0.19606298	dl
//    aero_opt_thick_443:					NaN	dl
//    aero_opt_thick_550:					0.4336939	dl
//    aero_opt_thick_865:					0.3968504	dl
//    cloud_albedo:					NaN	dl
//    cloud_opt_thick:					NaN	dl
//    cloud_top_press:					NaN	hPa
//    cloud_type:					NaN
//    l2_flags:					2359432
//
//    latitude:	32.299618	deg
//    longitude:	24.987894	deg
//    dem_alt:	-1073.2852	m
//    dem_rough:	0.0	m
//    lat_corr:	0.0	deg
//    lon_corr:	0.0	deg
//    sun_zenith:	27.12353	deg
//    sun_azimuth:	109.91053	deg
//    view_zenith:	31.929405	deg
//    view_azimuth:	101.009575	deg
//    zonal_wind:	-1.5785156	m/s
//    merid_wind:	-2.8183596	m/s
//    atm_press:	1015.8	hPa
//    ozone:	296.88425	DU
//    rel_hum:	69.98047	%
//
//    l2_flags.LAND:	false
//    l2_flags.CLOUD:	false
//    l2_flags.WATER:	true
//    l2_flags.PCD_1_13:	false
//    l2_flags.PCD_14:	false
//    l2_flags.PCD_15:	true
//    l2_flags.PCD_16:	false
//    l2_flags.PCD_17:	false
//    l2_flags.PCD_18:	false
//    l2_flags.PCD_19:	false
//    l2_flags.COASTLINE:	false
//    l2_flags.COSMETIC:	false
//    l2_flags.SUSPECT:	false
//    l2_flags.OOADB:	false
//    l2_flags.ABSOA_DUST:	false
//    l2_flags.CASE2_S:	false
//    l2_flags.CASE2_ANOM:	true
//    l2_flags.TOAVI_BRIGHT:	false
//    l2_flags.CASE2_Y:	false
//    l2_flags.TOAVI_BAD:	false
//    l2_flags.ICE_HAZE:	false
//    l2_flags.TOAVI_CSI:	false
//    l2_flags.MEDIUM_GLINT:	false
//    l2_flags.TOAVI_WS:	false
//    l2_flags.LARS_ON:	false
//    l2_flags.BPAC_ON:	true
//    l2_flags.HIGH_GLINT:	false
//    l2_flags.TOAVI_INVAL_REC:	false
//    l2_flags.LOW_SUN:	false
//    l2_flags.LOW_PRESSURE:	false
//    l2_flags.WHITE_SCATTERER:	false

}
