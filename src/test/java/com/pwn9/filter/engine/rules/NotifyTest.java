/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2016 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.filter.engine.rules;

import com.pwn9.filter.engine.FilterService;
import com.pwn9.filter.engine.api.FilterContext;
import com.pwn9.filter.engine.rules.action.minecraft.MinecraftAction;
import com.pwn9.filter.engine.rules.action.targeted.TargetedAction;
import com.pwn9.filter.engine.rules.chain.InvalidChainException;
import com.pwn9.filter.engine.rules.chain.RuleChain;
import com.pwn9.filter.util.tag.RegisterTags;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for Actions
 * User: Sage905
 * Date: 13-05-04
 * Time: 11:28 AM
 */

public class NotifyTest {

    RuleChain rs;
    FilterService filterService = new FilterService(new TestStatsTracker());
    final TestAuthor author = new TestAuthor();

    @BeforeClass
    public static void init() {
        RegisterTags.all();
    }

    @Before
    public void setUp() {
        filterService.getActionFactory().addActionTokens(MinecraftAction.class);
        filterService.getActionFactory().addActionTokens(TargetedAction.class);
        File rulesDir = new File(getClass().getResource("/rules").getFile());
        filterService.getConfig().setRulesDir(rulesDir);
        try {
            rs = filterService.parseRules(new File(rulesDir, "notifyTests.txt"));
        } catch (InvalidChainException ex) {
            fail();
        }
    }

    @Test
    public void testNotifyAddsMessage() {
        FilterContext testState = new FilterContext("foo", author, new TestClient());
        rs.execute(testState, filterService);
        assertEquals(testState.getNotifyMessages().get("pwnfilter.admins"),
                "Player " + author.getName() + " has broken " + testState.getMatchedRules().get(0).getId());
    }

    @Test
    public void testNotifyOnlyKeepsLastMessage() {
        FilterContext testState = new FilterContext("foo bar", author, new TestClient());
        rs.execute(testState, filterService);
        assertEquals(testState.getNotifyMessages().get("pwnfilter.admins"),
                "Player " + author.getName() + " has broken " + testState.getMatchedRules().get(1).getId());
    }

    @Test
    public void testNotifySendsOneMessagePerPerm() {
        FilterContext testState = new FilterContext("foo bar baz", author, new TestClient());
        rs.execute(testState, filterService);
        assertEquals(testState.getNotifyMessages().get("pwnfilter.admins"),
                "Player " + author.getName() + " has broken " + testState.getMatchedRules().get(1).getId());
        assertEquals(testState.getNotifyMessages().get("pwnfilter.baz"),
                "Player " + author.getName() + " has broken " + testState.getMatchedRules().get(2).getId());
        assertEquals(testState.getNotifyMessages().size(), 2);
    }

    @Test
    public void testNotifySentAtEndOfProcessing() {
        FilterContext testState = new FilterContext("foo bar baz", author, new TestClient());
        TestNotifier target = new TestNotifier();
        filterService.registerNotifyTarget(target);
        rs.execute(testState, filterService);
        assertEquals(testState.getNotifyMessages().get("pwnfilter.admins"), target.getNotification("pwnfilter.admins"));
        assertEquals(testState.getNotifyMessages().get("pwnfilter.baz"), target.getNotification("pwnfilter.baz"));

    }

}
