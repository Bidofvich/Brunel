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

package org.brunel.data.modify;

import org.brunel.data.Data;
import org.brunel.data.Dataset;
import org.brunel.data.Field;
import org.brunel.data.Fields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This transform converts a set of "y" values into a single "#values" field and adds another
 * field with the "y" field names that is given the special name "#series".
 * It takes a list of y values, comma separated, and a then a list of other fields that need to be in the result.
 * Any field not in either of those will be dropped.
 * Note that it is possible (and useful) to have a field both in the 'y' and 'other' lists
 */
public class ConvertSeries extends DataOperation {

    public static Dataset transform(Dataset base, String commands) {
        if (commands.isEmpty()) return base;

        // The first section consists of a list of 'y' values to be made into series and values
        // The second section is a list of fields to be preserved as-is
        String[] sections = strings(commands, ';');
        String[] yFields = strings(sections[0], ',');

        int nY = yFields.length;            // Number of Y fields
        int nR = base.rowCount();           // The rows in the original data

        // This also handles the case when there is a range specified (empty yFields)
        if (nY < 2) return base;

        // If there are no other fields, there is only one section
        String items = sections.length < 2 ? "" : sections[1];
        String[] otherFields = addRequired(strings(items, ','));

        Field[] y = new Field[nY];
        for (int i = 0; i < nY; i++) {
            y[i] = base.field(yFields[i]);
            if (y[i] == null) throw new NullPointerException("ConvertSeries Could not find field for name: " + yFields[i]);
        }


        /*
            We handle four different categories of field:
            VALUES:
                this will be replaced with the stacked Y fields
            SERIES:
                this is an indicator (by name) of which field the Y value came from
            Y FIELDS:
                all of these get converted to one field 'values', and they are stacked on top of each other in order
            REST:
                these will also be stacked using indexing so they org.brunel.app.match the Y values
         */


        int[] seriesIndexing = new int[nY * nR];        // EG:  0,0,0,0,   1,1,1,1,   2,2,2,2
        int[] valuesIndexing = new int[nY * nR];        // EG:  0,1,2,3,   0,1,2,3,   0,1,2,3
        Object[] data = new Object[nY * nR];            // Will store the Y values
        for (int i = 0; i < nY; i++)
            for (int j = 0; j < nR; j++) {
                seriesIndexing[i * nR + j] = i;
                valuesIndexing[i * nR + j] = j;
                data[i * nR + j] = y[i].value(j);
            }

        // Make the field for values, copying properties from the first Y field (we assume they are simialr)
        Field values = Fields.makeColumnField("#values", Data.join(yFields), data);
        Fields.copyBaseProperties(y[0], values);

        // Create the series field
        Field temp = Fields.makeColumnField("#series", "Series", yFields);
        Field series = Fields.permute(temp, seriesIndexing, false);
        series.setCategories(yFields);

        // All other fields use the valuesIndexing
        List<Field> resultFields = new ArrayList<>();
        resultFields.add(series);
        resultFields.add(values);
        for (String fieldName : otherFields) {
            // The special fields have already been added
            if (fieldName.equals("#series") || fieldName.equals("#values")) continue;
            Field f = base.field(fieldName);
            resultFields.add(Fields.permute(f, valuesIndexing, false));
        }

        // Assemble result
        Field[] fields = resultFields.toArray(new Field[resultFields.size()]);
        return base.replaceFields(fields);
    }

    private static String[] addRequired(String[] list) {
        // Ensure #count and #row are present
        List<String> result = new ArrayList<>();
        Collections.addAll(result, list);
        if (!result.contains("#row")) result.add("#row");
        if (!result.contains("#count")) result.add("#count");
        if (!result.contains("#selection")) result.add("#selection");
        return result.toArray(new String[result.size()]);
    }
}
