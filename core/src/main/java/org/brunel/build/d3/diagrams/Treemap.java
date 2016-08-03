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

package org.brunel.build.d3.diagrams;

import org.brunel.build.d3.D3Interaction;
import org.brunel.build.d3.element.ElementDetails;
import org.brunel.build.d3.element.ElementRepresentation;
import org.brunel.build.util.ScriptWriter;
import org.brunel.data.Dataset;
import org.brunel.model.VisSingle;

class Treemap extends D3Diagram {

    public Treemap(VisSingle vis, Dataset data, D3Interaction interaction, ScriptWriter out) {
        super(vis, data, interaction, out);
    }

    public ElementDetails initializeDiagram() {
        out.comment("Define treemap (hierarchy) data structures");
        makeHierarchicalTree();

        // Create the d3 layout
        out.add("var treemap = d3.treemap().tile(d3.treemapResquarify)")
                .addChained("size([geom.inner_width, geom.inner_height])")
                .addChained("padding(2).paddingTop(function(d) { return d.depth < 2 ? 15 : 12})")
                .endStatement();

        return ElementDetails.makeForDiagram(vis, ElementRepresentation.rect, "polygon", "treemap(tree).descendants()");
    }

    public void writeDefinition(ElementDetails details) {
        out.addChained("attr('class', function(d) { return (d.children ? 'element L' + d.depth : 'leaf element " + element.name() + "') })")
                .addChained("attr('x', function(d) { return d.x0 })")
                .addChained("attr('y', function(d) { return d.y0 })")
                .addChained("attr('width', function(d) { return d.x1-d.x0 })")
                .addChained("style('width', function(d) { return d.x1-d.x0 })")
                .addChained("attr('height', function(d) { return d.y1-d.y0 })");
        addAestheticsAndTooltips(details);

        labelBuilder.addTreeInternalLabels();
    }

    public boolean needsDiagramLabels() {
        return true;
    }

    public String getRowKey() {
        return "d.key == null ?  data._key(d.row) : d.key";
    }

}
