package org.envirocar.algorithm;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.commands.PID;
import org.envirocar.obd.commands.response.DataResponse;
import org.envirocar.obd.commands.response.entity.LambdaProbeVoltageResponse;
import org.envirocar.obd.events.PropertyKeyEvent;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class InterpolationMeasurementProviderTest {

    @Test
    public void testInterpolateTwo() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider(null);

        PropertyKeyEvent s1 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 52, 1000);
        PropertyKeyEvent s2 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 95, 4000);

        //the temporal center, should be the average
        double result = imp.interpolateTwo(s1.getValue(), s2.getValue(), 2500, s1.getTimestamp(), s2.getTimestamp());

        Assert.assertThat(result, CoreMatchers.is(73.5));

        //more at the and of the window
        result = imp.interpolateTwo(s1.getValue(), s2.getValue(), 3000, s1.getTimestamp(), s2.getTimestamp());

        BigDecimal bd = new BigDecimal(result);
        bd = bd.setScale(2, RoundingMode.HALF_UP);

        Assert.assertThat(bd.doubleValue(), CoreMatchers.is(80.67));
    }

    @Test
    public void testInterpolation() {
        InterpolationMeasurementProvider imp = new InterpolationMeasurementProvider(null);

        PropertyKeyEvent m1 = new PropertyKeyEvent(Measurement.PropertyKey.MAF, 16.0, 1000);
        PropertyKeyEvent m2 = new PropertyKeyEvent(Measurement.PropertyKey.MAF, 48.0, 3500); // this should be the result
        PropertyKeyEvent m3 = new PropertyKeyEvent(Measurement.PropertyKey.MAF, 32.0, 5000);

        //the result should be 68.125
        PropertyKeyEvent s1 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 52, 2000);
        PropertyKeyEvent s2 = new PropertyKeyEvent(Measurement.PropertyKey.SPEED, 95, 6000);

        imp.consider(s1);
        imp.consider(s2);
        imp.consider(m1);
        imp.consider(m2);
        imp.consider(m3);

        imp.newPosition(new MeasurementProvider.Position(1000, 52.0, 7.0));
        imp.newPosition(new MeasurementProvider.Position(3500, 52.5, 7.25)); //this should be the result

        TestSubscriber<Measurement> ts = new TestSubscriber<Measurement>();

        imp.measurements(500)
                .subscribeOn(Schedulers.immediate())
                .observeOn(Schedulers.immediate())
                .first()
                .subscribe(ts);

        List<Measurement> events = ts.getOnNextEvents();
        Assert.assertThat(events.size(), CoreMatchers.is(1));

        Measurement first = events.get(0);

        Assert.assertThat(first.getTime(), CoreMatchers.is(3500L));

        Assert.assertThat(first.getProperty(Measurement.PropertyKey.MAF), CoreMatchers.is(48.0));
        Assert.assertThat(first.getProperty(Measurement.PropertyKey.SPEED), CoreMatchers.is(68.125));

        Assert.assertThat(first.getLatitude(), CoreMatchers.is(52.5));
        Assert.assertThat(first.getLongitude(), CoreMatchers.is(7.25));
    }

}
