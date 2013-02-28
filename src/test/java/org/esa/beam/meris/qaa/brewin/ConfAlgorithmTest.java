package org.esa.beam.meris.qaa.brewin;

import org.esa.beam.meris.qaa.algorithm.ImaginaryNumberException;
import org.esa.beam.meris.qaa.algorithm.QaaResult;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfAlgorithmTest {

    @Test
    public void testProcess_Meris() throws ImaginaryNumberException {
        final float[] rrs_in = {0.030262154f, 0.031086152f, 0.022717977f, 0.013177891f, 0.0072450927f, 0.0028870495f, 0.0024475828f};

        final ConfAlgorithm algorithm = new ConfAlgorithm(new MerisConfig());

        final QaaResult result = algorithm.process(rrs_in, null);
        final float[] a_total = result.getA_Total();
        assertEquals(0.0326309, a_total[0], 1e-6);
        assertEquals(0.0264908, a_total[1], 1e-6);
        assertEquals(0.0289172, a_total[2], 1e-6);
        assertEquals(0.0455736, a_total[3], 1e-6);
        assertEquals(0.0657829, a_total[4], 1e-6);

        final float[] bb_spm = result.getBB_SPM();
        assertEquals(0.0205040, bb_spm[0], 1e-6);
        assertEquals(0.0171448, bb_spm[1], 1e-6);
        assertEquals(0.0133822, bb_spm[2], 1e-6);
        assertEquals(0.0121625, bb_spm[3], 1e-6);
        assertEquals(0.00977813, bb_spm[4], 1e-6);

        final float[] a_pig = result.getA_PIG();
        assertEquals(0.00263956, a_pig[0], 1e-6);
        assertEquals(0.00337634, a_pig[1], 1e-6);
        assertEquals(0.00614989, a_pig[2], 1e-6);

        final float[] a_ys = result.getA_YS();
        assertEquals(0.0254953, a_ys[0], 1e-6);
        assertEquals(0.0160453, a_ys[1], 1e-6);
        assertEquals(0.0077673, a_ys[2], 1e-6);
    }

    @Test
    public void testProcess_Modis() throws ImaginaryNumberException {
        final float[] rrs_in = {0.0019080009f, 0.0029860009f, 0.0029160008f, 0.0030800009f, 0.0029520008f, 0.0011980009f};

        final ConfAlgorithm algorithm = new ConfAlgorithm(new ModisConfig());

        final QaaResult result = algorithm.process(rrs_in, null);
        final float[] a_total = result.getA_Total();
        assertEquals(0.326133, a_total[0], 1e-6);
        assertEquals(0.177058, a_total[1], 1e-6);
        assertEquals(0.147228, a_total[2], 1e-6);
        assertEquals(0.118495, a_total[3], 1e-6);
        assertEquals(0.117016, a_total[4], 1e-6);

        final float[] bb_spm = result.getBB_SPM();
        assertEquals(0.0131830, bb_spm[0], 1e-6);
        assertEquals(0.0111027, bb_spm[1], 1e-6);
        assertEquals(0.00902051, bb_spm[2], 1e-6);
        assertEquals(0.00765894, bb_spm[3], 1e-6);
        assertEquals(0.00725599, bb_spm[4], 1e-6);

        final float[] a_pig = result.getA_PIG();
        assertEquals(-0.0426686, a_pig[0], 1e-6);
        assertEquals(-0.0501741, a_pig[1], 1e-6);
        assertEquals(0.0267035, a_pig[2], 1e-6);

        final float[] a_ys = result.getA_YS();
        assertEquals(0.364251, a_ys[0], 1e-6);
        assertEquals(0.220163, a_ys[1], 1e-6);
        assertEquals(0.106008, a_ys[2], 1e-6);
    }

    @Test
    public void testProcess_SeaWiFS() throws ImaginaryNumberException {
        final float[] rrs_in = {0.00167972470255084f, 0.00186919071018569f, 0.0027188008445359f, 0.00309262196610828f, 0.00406382197640373f, 0.00120514585009823f};

        final ConfAlgorithm algorithm = new ConfAlgorithm(new SeaWifsConfig());

        final QaaResult result = algorithm.process(rrs_in, null);
        final float[] a_total = result.getA_Total();
        assertEquals(0.585938, a_total[0], 1e-6);
        assertEquals(0.477027, a_total[1], 1e-6);
        assertEquals(0.293892, a_total[2], 1e-6);
        assertEquals(0.24889, a_total[3], 1e-6);
        assertEquals(0.176759, a_total[4], 1e-6);

        final float[] bb_spm = result.getBB_SPM();
        assertEquals(0.0208942, bb_spm[0], 1e-6);
        assertEquals(0.0188968, bb_spm[1], 1e-6);
        assertEquals(0.0168142, bb_spm[2], 1e-6);
        assertEquals(0.0161515, bb_spm[3], 1e-6);
        assertEquals(0.0149729, bb_spm[4], 1e-6);

        final float[] a_pig = result.getA_PIG();
        assertEquals(0.240965, a_pig[0], 1e-6);
        assertEquals(0.268238, a_pig[1], 1e-6);
        assertEquals(0.187655, a_pig[2], 1e-6);

        final float[] a_ys = result.getA_YS();
        assertEquals(0.340423, a_ys[0], 1e-6);
        assertEquals(0.201719, a_ys[1], 1e-6);
        assertEquals(0.0912376, a_ys[2], 1e-6);
    }

    @Test
    @Ignore
    public void testProcess_Meris_oldCoeffs() throws ImaginaryNumberException {
        final float[] rrs_in = {0.030262154f, 0.031086152f, 0.022717977f, 0.013177891f, 0.0072450927f, 0.0028870495f, 0.0024475828f};

        final ConfAlgorithm algorithm = new ConfAlgorithm(new MerisConfigOldCoeffs());

        final QaaResult result = algorithm.process(rrs_in, null);
        final float[] a_total = result.getA_Total();
        assertEquals(0.03845500573515892f, a_total[0], 1e-8);
        assertEquals(0.030030209571123123f, a_total[1], 1e-8);
        assertEquals(0.030713409185409546f, a_total[2], 1e-8);
        assertEquals(0.046738818287849426f, a_total[3], 1e-8);
        assertEquals(0.06614950299263f, a_total[4], 1e-8);

        final float[] bb_spm = result.getBB_SPM();
        assertEquals(0.007518719881772995f, bb_spm[0], 1e-8);
        assertEquals(0.006027825176715851f, bb_spm[1], 1e-8);
        assertEquals(0.004540313966572285f, bb_spm[2], 1e-8);
        assertEquals(0.0040666270069777966f, bb_spm[3], 1e-8);
        assertEquals(0.0032066269777715206f, bb_spm[4], 1e-8);

        final float[] a_pig = result.getA_PIG();
        assertEquals(0.0028468116652220488f, a_pig[0], 1e-8);
        assertEquals(0.0036492901854217052f, a_pig[1], 1e-8);
        assertEquals(0.006425064522773027f, a_pig[2], 1e-8);

        final float[] a_ys = result.getA_YS();
        assertEquals(0.030918193981051445f, a_ys[0], 1e-8);
        assertEquals(0.019170919433236122f, a_ys[1], 1e-8);
        assertEquals(0.009288343600928783f, a_ys[2], 1e-8);
    }
}
