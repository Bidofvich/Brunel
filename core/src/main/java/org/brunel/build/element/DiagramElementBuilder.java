/*
 * Copyright (c) 2016 IBM Corporation and others.
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

package org.brunel.build.element;

import org.brunel.build.ScaleBuilder;
import org.brunel.build.diagrams.D3Diagram;
import org.brunel.build.diagrams.GeoMap;
import org.brunel.build.info.ElementStructure;
import org.brunel.build.util.ScriptWriter;
import org.brunel.model.VisTypes;

import static org.brunel.model.VisTypes.Diagram.map;

class DiagramElementBuilder extends ElementBuilder {

	private final D3Diagram diagram;

	public DiagramElementBuilder(ElementStructure structure, ScriptWriter out, ScaleBuilder scales) {
		super(structure, scales, out);
		this.diagram = structure.diagram;
	}

	public void addAdditionalElementGroups() {
		if (diagram.needsDiagramExtras()) {
			out.add(",").ln().indent().add("diagramExtras = elementGroup.append('g').attr('class', 'extras')");
		}
		if (diagram.needsDiagramLabels()) {
			out.add(",").ln().indent()
					.add("diagramLabels = BrunelD3.undoTransform(elementGroup.append('g').attr('class', 'diagram labels').attr('aria-hidden', 'true'), elementGroup)");
		}
	}

	public ElementDetails makeDetails() {
		return diagram.makeDetails();
	}

	public void preBuildDefinitions() {
		diagram.preBuildDefinitions(out);
	}

	public void writeBuildCommands() {
		diagram.writeBuildCommands(out);
	}

	public void writeDiagramDataStructures() {
		diagram.writeDataStructures(out);
	}

	public void writePerChartDefinitions() {
		diagram.writePerChartDefinitions(out);
	}

	protected void defineAllElementFeatures(ElementDetails details) {
		if (vis.tElement == VisTypes.Element.point && vis.tDiagram == map) {
			// Points on maps do need the coordinate functions
			writeCoordinateFunctions(details);
		} else {
			// Set the diagram group class for CSS
			out.add("main.attr('class',", diagram.getStyleClasses(), ")").endStatement();
			diagram.defineCoordinateFunctions(details, out);
		}
	}

	protected void defineLabeling(ElementDetails details) {
		// Do not write if no need for them
		if (!structure.needsLabels() && !structure.needsTooltips()) return;

		out.onNewLine().ln().comment("Define labeling for the selection")
				.onNewLine().add("function label(selection, transitionMillis) {")
				.indentMore().onNewLine();
		diagram.writeLabelsAndTooltips(details, labelBuilder);
		out.indentLess().onNewLine().add("}").ln();
	}

	protected void defineUpdateState(ElementDetails details) {
		// Define the update to the merged data
		out.onNewLine().ln().comment("Define selection update operations on merged data")
				.onNewLine().add("function updateState(selection) {").indentMore()
				.onNewLine().add("selection").onNewLine();
		if (diagram instanceof GeoMap) {
			writeCoordinateDefinition(details);
			writeElementAesthetics(details, true, vis, out);
		}
		diagram.writeDiagramUpdate(details, out);
		writeDependencyHookups();
		out.endStatement();
		out.indentLess().onNewLine().add("}").ln();
	}

	/* The key function ensure we have object constancy when animating */
	protected String getKeyFunction() {
		return diagram.getRowKeyFunction();
	}

	protected void writeDiagramEntry(ElementDetails details) {
		diagram.writeDiagramEnter(details, labelBuilder, out);
	}
}
