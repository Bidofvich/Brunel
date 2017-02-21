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

package org.brunel.build.d3.element;

import org.brunel.build.d3.D3Interaction;
import org.brunel.build.d3.D3ScaleBuilder;
import org.brunel.build.info.ChartCoordinates;
import org.brunel.build.info.ElementStructure;
import org.brunel.build.util.ScriptWriter;
import org.brunel.model.VisTypes;

class CoordinateElementBuilder extends ElementBuilder {

	CoordinateElementBuilder(ElementStructure structure, ScriptWriter out, D3ScaleBuilder scales, D3Interaction interaction) {
		super(structure, interaction, scales, out);
	}

	public void addAdditionalElementGroups() {
		// None needed
	}

	private String getCommonSymbol() {
		// If we have a defined symbol, we use that
		if (structure.styleSymbol != null) return structure.styleSymbol;

		// We show a rectangle if we have a coordinate system which is all categorical
		if (structure.vis.tElement == VisTypes.Element.point && hasAllCategoricalScales()) return "rect";

		// No knowledge
		return null;
	}

	private boolean hasAllCategoricalScales() {
		// Any geo means we are not all categorical
		if (structure.geo != null) return false;

		// Otherwise we consult the coordinates to see if both X and Y are categorical
		ChartCoordinates coordinates = structure.chart.coordinates;
		return coordinates.xCategorical && coordinates.yCategorical;
	}

	public ElementDetails makeDetails() {
		String symbol = getCommonSymbol();
		return ElementDetails.makeForCoordinates(structure, symbol);
	}

	public void preBuildDefinitions() {
	}

	public void writeBuildCommands() {
	}

	public void writeDiagramDataStructures() {
	}

	public void writePerChartDefinitions() {
	}

	protected void defineLabeling(ElementDetails details) {
		out.onNewLine().ln().comment("Define labeling for the selection")
				.onNewLine().add("function label(selection, transitionMillis) {")
				.indentMore().onNewLine();
		writeElementLabelsAndTooltips(details, labelBuilder);
		out.indentLess().onNewLine().add("}").ln();
	}

	/* The key function ensure we have object constancy when animating */
	protected String getKeyFunction() {
		return "function(d) { return d.key }";
	}

	protected void defineAllElementFeatures(ElementDetails details) {

		writeCoordinateFunctions(details);
		if (details.representation == ElementRepresentation.wedge) {
			// Deal with the case of wedges (polar intervals)
			out.onNewLine().comment("Define the path for pie wedge shapes");
			defineWedgePath();
		} else if (details.requiresSplitting()) {
			// Define paths needed in the element, and make data splits
			out.onNewLine().comment("Define paths");
			definePathsAndSplits(details);
		}
	}

	protected void writeDiagramEntry(ElementDetails details) {
	}

	protected void defineUpdateState(ElementDetails details) {
		// Define the update to the merged data
		out.onNewLine().ln().comment("Define selection update operations on merged data")
				.onNewLine().add("function updateState(selection) {").indentMore()
				.onNewLine().add("selection").onNewLine();
		writeCoordinateDefinition(details);
		writeElementAesthetics(details, true, vis, out);
		out.endStatement();

		out.indentLess().onNewLine().add("}").ln();
	}
}
