/*
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brunel.data.auto;

import org.brunel.data.Data;
import org.brunel.data.stats.DateStats;
import org.brunel.data.util.DateUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class contains methods to define a numeric scale, called via static methods
 * They are each initialized using a field, a boolean 'nice' to indicate whether to expand the range to nice numbers,
 * a fractional amount to pad the field's domain by (e.g. 0.02) and a desired number of ticks,
 * as well as other parameters for specific scales.
 *
 * When called, the effect is to return a NumericScale that has all properties set to useful values
 */
public class NumericScale {

	private static final double HALF_LOG = 3;

	public static NumericScale makeDateScale(NumericExtentDetail info, boolean nice, double[] padFraction, int desiredTickCount) {

		double a = info.low;
		double b = info.high;

		if (a == b) {
			a = Data.asNumeric(DateUnit.increment(Data.asDate(a), info.dateUnit, -1));
			b = Data.asNumeric(DateUnit.increment(Data.asDate(b), info.dateUnit, 1));
		} else {
			a -= padFraction[0] * (b - a);
			b += padFraction[1] * (b - a);
		}
		double desiredDaysGap = (b - a) / (desiredTickCount - 1);
		DateUnit unit = DateStats.getUnit(desiredDaysGap * 4);

		int multiple = NumericScale.bestDateMultiple(unit, desiredDaysGap);

		// x is the nice lower value
		Date x = DateUnit.floor(Data.asDate(a), unit, multiple);
		if (nice) a = Data.asNumeric(x);

		List<Double> d = new ArrayList<>();
		while (true) {
			double v = Data.asNumeric(x);
			if (v >= b) {
				// We have come to the end of the range
				if (nice || v == b) {
					// If we want a nice upper, or this value happens to be exactly the nice upper, add this in
					b = v;
					d.add(v);
				}
				break;
			}
			if (v >= a) d.add(v);
			x = DateUnit.increment(x, unit, multiple);
		}

		if (nice) b = Data.asNumeric(x);

		Double[] data = d.toArray(new Double[d.size()]);
		return new NumericScale("date", a, b, data, false);
	}

	private static int bestDateMultiple(DateUnit unit, double desiredDaysGap) {
		double target = desiredDaysGap / unit.approxDaysPerUnit;
		int multiple = 1;
		// Try all other multiples that divide evenly into the base
		for (int i = 2; i <= unit.base / 2; i++) {
			if (unit.base % i != 0) continue;
			if (i == 4) continue;                                // This isn't good in practice
			if (i == 6 && unit.base == 60) continue;            // This isn't good in practice
			if (Math.abs(target - i) <= Math.abs(target - multiple))
				multiple = i;
		}
		return multiple;
	}

	public static NumericScale makeLinearScale(NumericExtentDetail info, boolean nice, double includeZeroTolerance, double[] padFraction, int desiredTickCount, boolean forBinning) {

		// Guard against having no data
		if (info == null) return new NumericScale("linear", 0, 1, new Double[]{0.0, 1.0}, false);

		// Convert padding from fraction to data values and apply
		double a = info.low - padFraction[0] * info.range();
		double b = info.high + padFraction[1] * info.range();

		// Include zero if it doesn't expand too much
		if (a > 0 && a / b <= includeZeroTolerance) a = 0;
		if (b < 0 && b / a <= includeZeroTolerance) b = 0;

		// Handle nice ranges that we want to keep very nice
		if (a == 0) {
			if (info.high <= 1 + 1e-4 && b > 1) b = 1;                       // 0 - 1
			if (info.high < 100 + 1e-3 && b > 100) b = 100;                  // 0 - 100
		}

		// For degenerate data expand out
		if (a + 1e-9 > b) {
			b = Math.max(0, 2 * a);
			a = Math.min(0, 2 * a);
			if (a == 0 && b == 0) {
				a = 0;
				b = 1;
			}
		}

		double desiredDivCount = Math.max(desiredTickCount - 1, 1);

		String transform = info.transform;
		double granularity = info.granularity;
		double granularDivs = (b - a) / granularity;
		if ((forBinning || info.preferCategorical) && granularDivs > desiredDivCount / 2 && granularDivs < desiredDivCount * 2) {
			Double[] data = makeGranularDivisions(a, b, granularity, nice);
			return new NumericScale(transform, a, b, data, true);
		}

		// Work out a likely delta based on powers of ten
		double rawDelta = (b - a) / desiredDivCount;
		double deltaLog10 = Math.floor(Math.log(rawDelta) / Math.log(10));
		double delta = Math.pow(10, deltaLog10);

		// Then look around that multiple, using decimal-friendly multipliers
		double bestDiff = 1e9;
		double[] choices = new double[]{delta, delta * 10, delta / 10, delta * 5, delta / 2, delta * 2, delta / 5};
		for (double d : choices) {
			double low = d * Math.ceil(a / d);
			double high = d * Math.floor(b / d);
			double dCount = Math.round((high - low) / d) + 1;
			if (nice && a < low) dCount++;
			if (nice && b > high) dCount++;
			double diff = Math.abs(dCount - desiredTickCount);
			if (dCount > desiredTickCount) diff -= 0.001;            // For ties, prefer one more
			if (diff < bestDiff) {
				bestDiff = diff;
				delta = d;
			}
		}

		// x is the nice lower value; it may be set as the actual, lower value if niceLower is true
		double x = delta * Math.floor(a / delta);
		if (nice) {
			a = x;
			b = delta * Math.ceil(b / delta);
		}

		// Make sure x >= a and then add ticks up until we hit b
		if (x < a - 1e-6) x += delta;
		List<Double> d = new ArrayList<>();
		while (x < b + 1e-6) {
			d.add(x);
			x += delta;
		}

		Double[] data = d.toArray(new Double[d.size()]);
		return new NumericScale(transform, a, b, data, false);
	}

	private static Double[] makeGranularDivisions(double min, double max, double granularity, boolean nice) {
		List<Double> div = new ArrayList<>();
		if (!nice) {
			// inside the bounds only
			min += granularity;
			max -= granularity;
		}
		double at = min - granularity / 2;
		while (at < max + granularity) {
			div.add(at);
			at += granularity;
		}
		return div.toArray(new Double[div.size()]);
	}

	public static NumericScale makeLogScale(NumericExtentDetail info, boolean nice, double[] padFraction, double includeZeroTolerance, int desiredTickCount) {
		double a = Math.log(info.low) / Math.log(10);
		double b = Math.log(info.high) / Math.log(10);

		double pad = Math.max(padFraction[0], padFraction[1]);
		a -= pad * (b - a);
		b += pad * (b - a);

		if (includeZeroTolerance > 0.5 && a == 0) a = -0.5;

		// Include zero (actually one in untransformed space) if it doesn't expand too much
		if (a > 0 && a / b <= includeZeroTolerance) a = 0;

		if (nice) {
			a = Math.floor(a);
			b = Math.ceil(b);
		}

		double n = b - a + 1;
		boolean add5 = n < desiredTickCount * 0.666;                    //If true, add divisions at '5's
		double factor = n > desiredTickCount * 1.66 ? 100 : 10;

		List<Double> d = new ArrayList<>();
		double low = Math.pow(10, a);
		double high = Math.pow(10, b);
		if (add5 && high / 2 > info.high) high /= 2;
		double x = Math.pow(10, Math.ceil(a));
		double tolerantHigh = high * 1.001;
		while (x < tolerantHigh) {
			d.add(x);
			if (add5 && x * HALF_LOG < tolerantHigh) d.add(x * HALF_LOG);
			x *= factor;
		}

		Double[] data = d.toArray(new Double[d.size()]);
		return new NumericScale("log", low, high, data, false);
	}

	public final Double[] divisions;
	public final double max;
	public final double min;
	public final String type;
	public final boolean granular;

	private NumericScale(String type, double min, double max, Double[] divs, boolean granular) {
		this.type = type;
		this.min = min;
		this.max = max;
		divisions = divs;
		this.granular = granular;
	}

}
