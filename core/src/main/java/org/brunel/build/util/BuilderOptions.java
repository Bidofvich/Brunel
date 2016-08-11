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

package org.brunel.build.util;

/**
 * Options that can be set for a builder
 */
public class BuilderOptions {

    public String visIdentifier = "visualization";              // The HTML ID of the SVG element containing the vis
    public String controlsIdentifier = "controls";              // The HTML ID of the DIV element containing the vis controls
    public String dataName = "table%d";                         // Pattern for the data table ID. %d is the index.
    public String className = "BrunelVis";                      // Name of the base function
    public DataMethod includeData = DataMethod.columns;         // What level of data to include
    public boolean generateBuildCode = true;                    // if true, Add javascript to build the chart initially
    public boolean readableJavascript = true;                   // Readable or shorter
    public boolean accessibleContent = false;                   // If true, generate accessible content
    public String locJavaScript = "https://brunelvis.org/js";   // The location of the javascript libraries
    public String locMaps = "https://brunelvis.org/geo";        // The location of the mapping resources
    public String locD3 = "https://cdnjs.cloudflare.com/ajax/libs/d3/4.2.1/d3.min.js";            //Location of D3
    public String locTopoJson = "https://cdnjs.cloudflare.com/ajax/libs/topojson/1.6.20/topojson.min.js";  //Location of D3's TopoJson support
    public String version = "2.0";                              // Which online version to use

    public static BuilderOptions make(String[] args) {
        BuilderOptions options = new BuilderOptions();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.equals("-a") || arg.equals("-accessible"))
                options.accessibleContent = true;
            if (i + 1 < args.length) {
                if (arg.equals("-v") || arg.equals("-version"))
                    options.version = args[i + 1];
                if (arg.equals("-js") || arg.equals("-javascript"))
                    options.locJavaScript = args[i + 1];
                if (arg.equals("-m") || arg.equals("-maps"))
                    options.locMaps = args[i + 1];
            }
        }
        return options;
    }

    public static BuilderOptions makeFromENV() {
        BuilderOptions options = new BuilderOptions();

        String config = System.getenv("BRUNEL_CONFIG");
        if (config == null) return options;
        String[] configs = config.split(";");
        for (String c : configs) {
            String[] keyVal = c.trim().split("=");
            if (keyVal[0].trim().equalsIgnoreCase("locJavaScript")) options.locJavaScript = keyVal[1].trim();
            else if (keyVal[0].trim().equalsIgnoreCase("locMaps")) options.locMaps = keyVal[1].trim();
            else if (keyVal[0].trim().equalsIgnoreCase("locD3")) options.locD3 = keyVal[1].trim();
            else if (keyVal[0].trim().equalsIgnoreCase("locTopoJson")) options.locTopoJson = keyVal[1].trim();
        }

        return options;
    }

    /**
     * none -  no data described
     * full - send full data set
     * columns - send only required columns
     * minimal - send the minimal data needed by the system
     */
    public enum DataMethod {
        none, full, columns, minimal
    }

}
