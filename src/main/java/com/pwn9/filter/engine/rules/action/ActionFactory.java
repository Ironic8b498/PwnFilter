/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.filter.engine.rules.action;

import com.pwn9.filter.engine.api.Action;
import com.pwn9.filter.engine.api.ActionToken;
import com.pwn9.filter.engine.rules.action.core.CoreAction;

import java.util.ArrayList;
import java.util.List;

/**
 * This factory returns an action object selected by the rules file.
 * eg: "then kick" would return the Actionkick object.
 *
 * @author Sage905
 * @version $Id: $Id
 */
public final class ActionFactory {

    private final List<Class<? extends ActionToken>> actionTokens =
            new ArrayList<>();

    public ActionFactory() {
        // Ensure that all instances get the Core Actions
        actionTokens.add(CoreAction.class);
    }

    public Action getActionFromString(String s) throws InvalidActionException {
        String[] parts = s.split("\\s", 2);
        String actionName = parts[0];
        String actionData;
        actionData = ((parts.length > 1) ? parts[1] : "");

        return getAction(actionName, actionData);
    }

    public Action getAction(final String actionName, final String actionData)
    throws InvalidActionException {

        // Scan all tokens for a match

        for ( Class<? extends ActionToken> tokens : actionTokens ) {
            for (ActionToken token : tokens.getEnumConstants() ) {
                if (token.match(actionName.toUpperCase())) {
                    return token.getAction(actionData);
                }
            }
        }
        return null;
    }

    synchronized public void addActionTokens(Class<? extends ActionToken> tokenEnum) {
        actionTokens.add(tokenEnum);
    }

}
