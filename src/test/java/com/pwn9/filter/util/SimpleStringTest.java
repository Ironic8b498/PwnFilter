/*
 *  PwnFilter - Chat and user-input filter with the power of Regex
 *  Copyright (C) 2016 Pwn9.com / Sage905 <sage905@takeflight.ca>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.pwn9.filter.util;

import org.junit.Test;

import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for the SimpleString object
 * <p>
 * Created by Sage905 on 15-09-12.
 */
public class SimpleStringTest {
    private final String original = "This is the string under test";
    private final SimpleString testSs = new SimpleString(original);

    @Test
    public void testReplaceText() throws Exception {
        assertEquals(testSs.replaceText(Pattern.compile("string"), "toast"),
                new SimpleString("This is the toast under test"));
    }

    @Test
    public void testPatternToLower() throws Exception {
        assertEquals(testSs.patternToLower(Pattern.compile("This")),
                new SimpleString("this is the string under test"));
    }

    @Test
    public void testPatternToUpper() throws Exception {
        assertEquals(testSs.patternToUpper(Pattern.compile("This")),
                new SimpleString("THIS is the string under test"));

    }

    @Test
    public void testLength() throws Exception {
        assertEquals(testSs.length(), original.length());
    }

    @Test
    public void testCharAt() throws Exception {
        assertEquals(testSs.charAt(5), original.charAt(5));
    }

    @Test
    public void testSubSequence() throws Exception {
        assertEquals(testSs.subSequence(4, 12),
                original.subSequence(4, 12));
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue(testSs.equals(new SimpleString(original)));
    }

    @Test
    public void testGetRaw() throws Exception {
        assertEquals(testSs.getRaw(), original);
    }

    @Test
    public void testToString() throws Exception {
        assertEquals(testSs.toString(), original);
    }
}