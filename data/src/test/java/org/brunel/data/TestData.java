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

package org.brunel.data;

import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestData {

  @Test
  public void testCompare() {
    assertEquals(">", compare(2.0, 1));
    assertEquals("<", compare(2.0, 10));
    assertEquals("=", compare(2.0, 2));

    assertEquals(">", compare("c", "b"));
    assertEquals("<", compare("b", "z"));
    assertEquals("=", compare("A", "A"));

  }

  private String compare(Object a, Object b) {
    int x = Data.compare(a, b);
    if (x == 0) {
      return "=";
    } else {
      return x < 0 ? "<" : ">";
    }
  }

  @Test
  public void testConversions() {
    assertEquals(1.0, Data.asNumeric(1), 0.01);
    assertEquals(1.0, Data.asNumeric(1.0), 0.01);
    assertEquals(1.0, Data.asNumeric("1"), 0.01);
    assertEquals(null, Data.asNumeric("foo"));
    assertEquals(null, Data.asNumeric(null));
    assertEquals(null, Data.asNumeric("NaN"));
    assertEquals(null, Data.asNumeric("Inf"));
    assertEquals(null, Data.asNumeric("1/8/70"));
    assertEquals(null, Data.asNumeric("abc -1 ddd"));
  }

  @Test
  public void testDateConversion() {
    Field f = Fields.makeColumnField("a", "label", new Object[]{"Jan 4, 1980", "Jan 4, 1988", "a", "2", null, "Jan 9, 1978"});

    Field g = Data.toDate(f, null);
    assertNotEquals(f, g);
    assertEquals("a", g.name);
    assertEquals("label", g.label);
    assertEquals(6, g.rowCount());
    assertEquals(3, g.valid());

    // Should not change
    assertEquals(g, Data.toDate(g, null));
  }

  @Test
  public void testDateConversionForBadDates() {
    Object[] data = new Object[1000];
    for (int i = 0; i < data.length; i++) {
      data[i] = i % 10 == 0 ? "name" : "-";
    }
    Field f = Fields.makeColumnField("a", "label", data);
    Field g = Data.toDate(f);
    assertNotEquals(f, g);

    assertEquals("a", g.name);
    assertEquals("label", g.label);
    assertEquals(1000, g.rowCount());
    assertEquals(0, g.valid());
  }

  @Test
  public void testDates() {
    assertEquals(0.0, Data.asNumeric(Data.asDate("1970-01-01")), 0.001);
    assertEquals(0.0, Data.asNumeric(Data.asDate("1/1/70")), 0.001);
    assertEquals(1.0, Data.asNumeric(Data.asDate("1/2/70")), 0.001);
    assertEquals(1.0, Data.asNumeric(Data.asDate("Jan 2 1970")), 0.001);
    assertEquals(0.25, Data.asNumeric(Data.asDate("January 1, 1970 6:00:00")), 0.001);
    assertEquals(0.125, Data.asNumeric(Data.asDate("January 1, 1970 3:00:00")), 0.001);
    assertEquals(1.0 / 3600 / 24, Data.asNumeric(Data.asDate("1970-01-01T00:00:01")), 0.000001);
    assertEquals(1.0 / 3600 / 24, Data.asNumeric(Data.asDate("00:00:01")), 0.000001);
    assertEquals((11 * 60 + 50) / 60.0 / 24, Data.asNumeric(Data.asDate("11:50")), 0.000001);
    assertEquals((7 + 45 / 60.0 + 22 / 3600.0) / 24.0, Data.asNumeric(Data.asDate("07:45:22")), 0.000001);
    assertEquals((10 + 45 / 60.0 + 22 / 3600.0) / 24.0, Data.asNumeric(Data.asDate("10:45:22")), 0.000001);
    assertEquals(null, Data.asDate(null));
    assertEquals(null, Data.asDate("a"));
    assertEquals(null, Data.asDate("Not a date"));
    assertEquals(null, Data.asDate("WURGH 53, 2000"));
  }

  @Test
  public void testDeQuoting() {
    assertEquals(("hello"), Data.deQuote("'hello'"));
    assertEquals(("1.0"), Data.deQuote("'1.0'"));
    assertEquals(("say \"hello\""), Data.deQuote("'say \"hello\"'"));
    assertEquals(("John's"), Data.deQuote("\"John's\""));
    assertEquals(("test \n"), Data.deQuote("'test \\n'"));
    assertEquals(("test \n\t"), Data.deQuote("'test \\n\\t'"));
    assertEquals(("test \n\t\\"), Data.deQuote("'test \\n\\t\\\\'"));
    assertEquals(("slashes: \\"), Data.deQuote("'slashes: \\\\'"));
    assertEquals(("\"'Quotes mismatched\"'"), Data.deQuote("\"\\\"'Quotes mismatched\\\"'\""));
    assertEquals(("\\Nasty:\"'\\'\""), Data.deQuote("\"\\\\Nasty:\\\"'\\\\'\\\"\""));
  }

  @Test
  public void testParseInt() {
    assertEquals(55, Data.parseInt("55"));
    assertEquals(55, Data.parseInt("55.0"));
    assertEquals(55, Data.parseInt("55.0"));

    try {
      Data.parseInt("a");
      fail("should not parse");
    } catch (NumberFormatException ignored) {
    }
  }

  @Test
  public void testParseDouble() {
    assertEquals(55, Data.parseDouble("55"), 1e-12);
    assertEquals(55, Data.parseInt("55.0"), 1e-12);
    assertEquals(55, Data.parseDouble("5.5e1"), 1e-12);

    try {
      Data.parseInt("a");
      fail("should not parse");
    } catch (NumberFormatException ignored) {
    }
  }

  @Test
  public void testIndexOf() {
    Double[] DATA = new Double[]{1.0, 3.0, 6.0, 9.0, 11.0, 120.0};
    assertEquals(-1, Data.indexOf(0.0, DATA));
    assertEquals(0, Data.indexOf(1.0, DATA));
    assertEquals(2, Data.indexOf(7.0, DATA));
    assertEquals(2, Data.indexOf(6.0, DATA));
    assertEquals(3, Data.indexOf(9.0, DATA));
    assertEquals(4, Data.indexOf(100.0, DATA));
    assertEquals(5, Data.indexOf(1000.0, DATA));
  }

  @Test
  public void testMakingField() {
    Field f = Fields.makeColumnField("a", "label", new Object[]{"1", "2", "a", "2", null, 1});
    assertEquals("a", f.name);
    assertEquals("label", f.label);
    assertEquals(6, f.rowCount());
    assertEquals(5, f.valid());
  }

  @Test
  public void testNumericConversion() {
    Field f = Fields.makeColumnField("a", "label", new Object[]{"1", "2", "a", "2", null, 1});
    Field g = Data.toNumeric(f);
    assertNotEquals(f, g);
    assertEquals("a", g.name);
    assertEquals("label", g.label);
    assertEquals(6, g.rowCount());
    assertEquals(4, g.valid());

    // Should not change
    assertEquals(g, Data.toNumeric(g));
  }

  @Test
  public void testNumericFormat() {
    assertEquals("0", Data.formatNumeric(0.0, null, true));
    assertEquals("0", Data.formatNumeric(1e-9, 8, true));
    assertEquals("1.0e-9", Data.formatNumeric(1e-9, 10, true));
    assertEquals("22", Data.formatNumeric((double) 22, null, true));
    assertEquals("22", Data.formatNumeric(22.0000000000001, null, true));
    assertEquals("22", Data.formatNumeric(21.999999999, null, true));
    assertEquals("22.0001", Data.formatNumeric(22.0001, null, true));
    assertEquals("22.0001", Data.formatNumeric(22.000100000001, null, true));
    double d14 = -22;
    assertEquals("-22", Data.formatNumeric(d14, null, true));
    double d13 = -22.0000000000001;
    assertEquals("-22", Data.formatNumeric(d13, null, true));
    double d12 = -21.999999999;
    assertEquals("-22", Data.formatNumeric(d12, null, true));
    double d11 = -22.0001;
    assertEquals("-22.0001", Data.formatNumeric(d11, null, true));
    double d10 = -22.000100000001;
    assertEquals("-22.0001", Data.formatNumeric(d10, null, true));

    assertEquals("1,500", Data.formatNumeric((double) 1500, null, true));
    double d9 = -1500.000001;
    assertEquals("-1,500", Data.formatNumeric(d9, null, true));
    double d8 = -1499.999999;
    assertEquals("-1,500", Data.formatNumeric(d8, null, true));

    assertEquals("15,000", Data.formatNumeric((double) 15000, null, true));
    assertEquals("10,000", Data.formatNumeric((double) 10000, null, true));
    assertEquals("9,999", Data.formatNumeric((double) 9999, null, true));

    assertEquals("0.00001", Data.formatNumeric(0.00001, null, true));
    assertEquals("1.0e-6", Data.formatNumeric(0.000001, null, true));
    assertEquals("5.73e-7", Data.formatNumeric(0.000000573, null, true));
    assertEquals("5.734e-7", Data.formatNumeric(0.00000057339, null, true));
    double d7 = -0.00001;
    assertEquals("-0.00001", Data.formatNumeric(d7, null, true));
    double d6 = -0.000001;
    assertEquals("-1.0e-6", Data.formatNumeric(d6, null, true));
    double d5 = -0.00000057305;
    assertEquals("-5.73e-7", Data.formatNumeric(d5, null, true));

    assertEquals("99,999.99", Data.formatNumeric(99999.99, null, true));
    assertEquals("999,999.99", Data.formatNumeric(999999.99, null, true));
    assertEquals("10,000,000", Data.formatNumeric(9999999.99, null, true));
    assertEquals("100,000,000", Data.formatNumeric(99999999.99, null, true));
    assertEquals("1.0e9", Data.formatNumeric(999999999.99, null, true));

    double d4 = -99999.99;
    assertEquals("-99,999.99", Data.formatNumeric(d4, null, true));
    double d3 = -999999.99;
    assertEquals("-999,999.99", Data.formatNumeric(d3, null, true));
    double d2 = -9999999.99;
    assertEquals("-10,000,000", Data.formatNumeric(d2, null, true));
    double d1 = -99999999.99;
    assertEquals("-100,000,000", Data.formatNumeric(d1, null, true));
    double d = -999999999.99;
    assertEquals("-1.0e9", Data.formatNumeric(d, null, true));

    assertEquals("1.0e9", Data.formatNumeric(1.00001e9, null, true));
    assertEquals("1.001e9", Data.formatNumeric(1.001e9, null, true));
    assertEquals("1.01e9", Data.formatNumeric(1.01e9, null, true));
    assertEquals("1.1e9", Data.formatNumeric(1.1e9, null, true));

    assertEquals("13.333333", Data.formatNumeric(13.333333333333333333333333, null, true));

  }

  @Test
  public void testQuoting() {
    assertEquals("'hello'", Data.quote("hello"));
    assertEquals("'1.0'", Data.quote("1.0"));
    assertEquals("'say \"hello\"'", Data.quote("say \"hello\""));
    assertEquals("\"John's\"", Data.quote("John's"));
    assertEquals("'test \\n'", Data.quote("test \n"));
    assertEquals("'test \\n\\t'", Data.quote("test \n\t"));
    assertEquals("'test \\n\\t\\\\'", Data.quote("test \n\t\\"));
    assertEquals("'slashes: \\\\'", Data.quote("slashes: \\"));
    assertEquals("\"\\\"'Quotes mismatched\\\"'\"", Data.quote("\"'Quotes mismatched\"'"));
    assertEquals("\"\\\\Nasty:\\\"'\\\\'\\\"\"", Data.quote("\\Nasty:\"'\\'\""));               // "\\Nasty:\"'\\'\""

    assertEquals("'hey \\u2017'", Data.quote("hey \u2017"));
  }

  @Test
  public void testShortenA() {
    assertEquals("the long fun text", Data.shorten("the long fun text", 18));
    assertEquals("the long fun text", Data.shorten("the long fun text", 17));
    assertEquals("long fun text", Data.shorten("the long fun text", 16));
    assertEquals("long fun text", Data.shorten("the long fun text", 15));
    assertEquals("long fun text", Data.shorten("the long fun text", 14));
    assertEquals("long fun text", Data.shorten("The long fun text", 13));
    assertEquals("long fn text", Data.shorten("the long fun text", 12));
    assertEquals("long fn txt", Data.shorten("the long fun text", 11));
    assertEquals("lng fn txt", Data.shorten("the long fun text", 10));
    assertEquals("long\u2026text", Data.shorten("the long fun text", 9));
    assertEquals("long\u2026txt", Data.shorten("the long fun text", 8));
    assertEquals("lng\u2026txt", Data.shorten("the long fun text", 7));
    assertEquals("long\u2026", Data.shorten("the long fun text", 6));
    assertEquals("long\u2026", Data.shorten("the long fun text", 5));
    assertEquals("long", Data.shorten("the long fun text", 4));
    assertEquals("lon", Data.shorten("the long fun text", 3));
    assertEquals("lo", Data.shorten("the long fun text", 2));
    assertEquals("l", Data.shorten("the long fun text", 1));
    assertEquals("", Data.shorten("the long fun text", 0));
    assertEquals("", Data.shorten("the long fun text", -1));
  }

  @Test
  public void testShortenB() {
    assertEquals("ford focus 2018", Data.shorten("ford focus 2018", 15));
    assertEquals("ford focs 2018", Data.shorten("ford focus 2018", 14));
    assertEquals("ford fcs 2018", Data.shorten("ford focus 2018", 13));
    assertEquals("frd fcs 2018", Data.shorten("ford focus 2018", 12));
    assertEquals("ford\u20262018", Data.shorten("ford focus 2018", 11));
    assertEquals("ford\u20262018", Data.shorten("ford focus 2018", 10));
    assertEquals("ford\u20262018", Data.shorten("ford focus 2018", 9));
    assertEquals("frd\u20262018", Data.shorten("ford focus 2018", 8));
    assertEquals("ford\u2026", Data.shorten("ford focus 2018", 7));
  }

  @Test
  public void testShortenC() {
    assertEquals("Quentin Tarantino", Data.shorten("Quentin Tarantino", 17));
    assertEquals("Quentin Tarantno", Data.shorten("Quentin Tarantino", 16));
    assertEquals("Quentin Tarntno", Data.shorten("Quentin Tarantino", 15));
    assertEquals("Quentin Trntno", Data.shorten("Quentin Tarantino", 14));
    assertEquals("Quentn Trntno", Data.shorten("Quentin Tarantino", 13));
    assertEquals("Quntn Trntno", Data.shorten("Quentin Tarantino", 12));
    assertEquals("Qntn Trntno", Data.shorten("Quentin Tarantino", 11));
    assertEquals("Quentin\u2026", Data.shorten("Quentin Tarantino", 10));
    assertEquals("Quentin\u2026", Data.shorten("Quentin Tarantino", 9));
    assertEquals("Quentin\u2026", Data.shorten("Quentin Tarantino", 8));
  }


  @Test
  public void testShortenOtherCases() {
    assertEquals("the long fun text", Data.shorten("the long fun text (in dollars)", 20));
    assertEquals("$32,6", Data.shorten("$32,60975", 5));
  }

}
