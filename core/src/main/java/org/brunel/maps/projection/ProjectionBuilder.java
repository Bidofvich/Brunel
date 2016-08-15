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

package org.brunel.maps.projection;

import org.brunel.geom.Point;
import org.brunel.geom.Rect;

/**
 * Builds projections
 */
public class ProjectionBuilder {


    public static final Projection MERCATOR = new Mercator();           // Mercator projection
    public static final Projection WINKEL3 = new WinkelTripel();        // Winkel Tripel
    public static final Projection ALBERS_USA = new AlbersUSA();        // Albers for the U.S.A.

    /**
     * Choose a suitable projection for the given lat/long bounds
     *
     * @param bounds lat/long bounds
     * @return a Projection
     */
    public static Projection makeProjection(Rect bounds) {
        // Are we USA only?
        if (bounds.left < -100 && bounds.top > 17 && bounds.bottom > 35 && bounds.bottom < 73) {
            // If we need Alaska and/or Hawaii use the AlbersUSA, otherwise plain Mercator
            if (bounds.left < -120) return ALBERS_USA;
            else return MERCATOR;
        }

        // Mercator if the distortion is tolerable
        if (getMercatorDistortion(bounds) <= 1.8) return MERCATOR;

        // If we cover a wide area, use winkel triple
        if (bounds.right - bounds.left > 180 && bounds.bottom - bounds.top > 90) return WINKEL3;

        // Otherwise albers is our best bet
        return makeAlbers(bounds);

    }

    /**
     * The distortion is the ratio of the projected areas of a small rectangle between the top and bottom
     * of the projected area.
     *
     * @param bounds area to look at
     * @return Ratio always greater than 1
     */
    private static double getMercatorDistortion(Rect bounds) {
        if (bounds.top < -89 || bounds.bottom > 89) return 100;
        double a = MERCATOR.getTissotArea(new Point(bounds.cx(), bounds.top));
        double b = MERCATOR.getTissotArea(new Point(bounds.cx(), bounds.bottom));
        return a < b / 100 || b < a / 100 ? 100 : Math.max(b / a, a / b);
    }

    // The Albers projection needs standard parallels and a rotation angle
    public static Albers makeAlbers(Rect b) {
        // Parallels at 1/6 and 5/6 of the latitude
        double parallelA = (b.top + b.bottom * 5) / 6;           // Parallels at 1/6 and 5/6
        double parallelB = (b.top * 5 + b.bottom) / 6;           // Parallels at 1/6 and 5/6
        double angle = -b.cx();                                  // Rotation angle
        return new Albers(parallelA, parallelB, angle);
    }
}
