package matt.v1.kernel;

import com.aparapi.Kernel;
import com.aparapi.Range;

public class DotProductGPU {

    double[] m1;
    double[] m2;
    Kernel kernel;

    public DotProductGPU(double[] m1, double[] m2) {
        this.m1 = m1;
        this.m2 = m2;
    }

    public double calc() {
        double[] localM1 = m1;
        double[] localM2 = m2;
        double[] preResult = new double[m1.length];
        kernel = new Kernel() {
            @Override
            public void run() {
                int i = getGlobalId();
                preResult[i] = localM1[i] * localM2[i];
            }
        };
        kernel.execute(Range.create(preResult.length));
        double r = 0.0;
        for (double rr : preResult) {
            r += rr;
        }
        return r;
    }
}