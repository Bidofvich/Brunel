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
import org.brunel.build.d3.element.D3ElementBuilder;
import org.brunel.build.d3.element.ElementDetails;
import org.brunel.build.d3.element.ElementRepresentation;
import org.brunel.build.util.ModelUtil;
import org.brunel.build.util.ScriptWriter;
import org.brunel.data.Data;
import org.brunel.data.Dataset;
import org.brunel.model.VisSingle;
import org.brunel.model.style.StyleTarget;

class Chord extends D3Diagram {

    public Chord(VisSingle vis, Dataset data, D3Interaction interaction, ScriptWriter out) {
        super(vis, data, interaction, out);
    }

    public String getRowKey() {
        return "d.source.index + '|' + d.target.index";
    }

    public ElementDetails initializeDiagram() {
        String f1 = vis.positionFields()[0];
        String f2 = position[1];

        out.comment("Define chord data structures");
        out.add("var chordData = BrunelData.diagram_Chord.make(processed,");

        // Always need from and to, but the size may be empty when building the chord data
        if (size == null) out.addQuoted(f1, f2);
        else out.addQuoted(f1, f2, size);
        out.add(")").endStatement();
        out.add("var chord = d3.chord().padAngle(.025).sortSubgroups(d3.descending)").endStatement();
        out.add("var chords = chord(chordData.matrix())").endStatement();

        // take arc path font size into account, adding a bit of padding, to define the arc width
        StyleTarget target = StyleTarget.makeElementTarget("text", "axis", "label");
        double labelSize = ModelUtil.getSize(vis, target, "font-size", 8);
        double arcWidth = labelSize * 1.2;
        out.add("var arc_width =", Data.formatNumeric(arcWidth, null, false), ";").comment("Width of exterior arc");
        out.add("function keyFunction(d) { return d.source.index + '|' + d.target.index };").comment(" special key function for the edges");

        return ElementDetails.makeForDiagram(vis, ElementRepresentation.polygon, "edge", "chords");
    }

    public void writeDefinition(ElementDetails details) {

        // The Chords themselves are simple to create
        out.addChained("attr('d', d3.ribbon().radius(geom.inner_radius-arc_width))")
                .addChained("attr('class', 'element " + element.name() + "')");

        addAestheticsAndTooltips(details);

        // We now need to add the arcs on the outside for the groups
        out.onNewLine().ln().comment("Add in the arcs on the outside for the groups");
        out.add("diagramExtras.attr('class', 'diagram chord arcs')").endStatement();

        // Each chord group will be a group with a donut arc and a text path in it
        out.add("var arcGroup = diagramExtras.selectAll('g').data(chords.groups),")
                .continueOnNextLine().add("addedArcGroups = arcGroup.enter().append('g'),")
                .continueOnNextLine().add("arcPath = d3.arc().innerRadius(geom.inner_radius - arc_width).outerRadius(geom.inner_radius)")
                .endStatement();

        // Add the two parts to each group, linking them through an ID
        out.add("addedArcGroups.append('path').attr('class', 'box')")
                .addChained("attr('id', function(d, i) { return 'arc' + i; })").endStatement();

        out.add("addedArcGroups.append('text').attr('class', 'label')")
                .addChained("attr('dy', arc_width*0.72).attr('class', 'label')")
                .addChained("append('textPath').attr('xlink:href', function(d, i) { return '#arc' + i })")
                .endStatement();

        out.add("var mergedArcGroups = addedArcGroups.merge(arcGroup)").endStatement();

        // Transition to correct arc sizes and locations
        out.add("BrunelD3.tween(mergedArcGroups, transitionMillis, function(d, i) { ")
                .indentMore().indentMore().onNewLine()
                .add("var group = d3.select(this)").endStatement()
                .add("return function() {").indentMore().indentMore().onNewLine()
                .add("group.select('path').attr('d', arcPath(d))").endStatement()
                .add("group.select('textPath').text(chordData.group(i))").endStatement()
                .add("BrunelD3.centerInWedge(group.select('text'), arc_width)").endStatement()
                .indentLess().indentLess().onNewLine().add("}")
                .indentLess().indentLess().onNewLine().add("})").endStatement();

        // Ensure removal on exit
        D3ElementBuilder.writeRemovalOnExit(out, "arcGroup");
    }

    public boolean needsDiagramExtras() {
        return true;
    }

    public void writeDiagramEnter() {
        // Ensure we have a row for each chord, based off the chord start and end points
        out.add("merged.each(function(d) { d.row = chordData.index(d.target.index, d.target.subindex) })").endStatement();
    }
}
