/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.filter.engine.rules.parser;

import com.pwn9.filter.engine.FilterService;
import com.pwn9.filter.engine.api.Action;
import com.pwn9.filter.engine.config.FilterConfig;
import com.pwn9.filter.engine.rules.Condition;
import com.pwn9.filter.engine.rules.Rule;
import com.pwn9.filter.engine.rules.ShortCutManager;
import com.pwn9.filter.engine.rules.action.ActionFactory;
import com.pwn9.filter.engine.rules.action.InvalidActionException;
import com.pwn9.filter.engine.rules.chain.Chain;
import com.pwn9.filter.engine.rules.chain.InvalidChain;
import com.pwn9.filter.engine.rules.chain.RuleChain;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Text-file Rule Parser
 * User: Sage905
 * Date: 13-11-16
 * Time: 3:17 PM
 *
 * @author Sage905
 * @version $Id: $Id
 */
public class TextConfigParser implements FilterConfigParser {

    /**
     * <p>Constructor for FileParser.</p>
     */

    private final Map<String, String> shortcuts = new HashMap<>();
    private final Logger logger;
    private final ActionFactory actionFactory;
    private final FilterConfig filterConfig;

    public TextConfigParser(FilterService filterService) {
        this.logger = filterService.getLogger();
        this.actionFactory = filterService.getActionFactory();
        this.filterConfig = filterService.getConfig();
    }

    public Chain parse(File source) {
        List<File> parents = new ArrayList<>();
        return parse(source, parents);
    }

    public Chain parse(File source, List<File> parents) {
        RuleChain.Builder builder = new RuleChain.Builder();
        return parse(source, parents, builder);
    }

    /**
     * Consumes a Reader Stream and outputs a Chain
     *
     * @return Chain containing new chain, or InvalidChain on failure.
     */
    Chain parse(File source, List<File> parents, RuleChain.Builder builder ) {

        RuleStreamReader reader;
        try {
            reader  = new RuleStreamReader(new InputStreamReader(new FileInputStream(source)));
        } catch ( FileNotFoundException ex ) {
            return new InvalidChain("Rule File not found: " + source);
        }

        // Check to make sure this file isn't already in the parent chain
        try {
            if (parents.contains(source.getCanonicalFile())) {
                StringBuilder error = new StringBuilder();
                error.append("Recursion loop! Parent list:\n");
                for (File parent : parents) {
                    error.append(parent).append(",");
                }
                error.append("\n").append(source).
                        append(" has been called recursively.");
                return new InvalidChain(parserError(reader.getLineNumber(),
                        error.toString(), source.toString()));
            } else {
                parents.add(source);
            }
        } catch (IOException | SecurityException ex ) {
            return new InvalidChain(parserError(reader.getLineNumber(),
                    "IO Exception while trying to get canonical filename",
                    source.toString()));
        }


        // Now read the file.  This loop reads the top-level instructions, and passes
        // second-level handling off to individual methods.
        try {
            String line;

            while ((line = reader.readLine()) != null) {

                // Outside of a section, we ignore blank lines.
                if (line.isEmpty()) continue;

                TokenString tokenString = new TokenString(line);
                String command = tokenString.popToken();
//                lineNo = reader.getLineNumber();

                try {
                    // Process an Action Group
                    if (command.equalsIgnoreCase("actiongroup")) {
                        String groupName = tokenString.popToken();
                        parseActionGroup(groupName, reader.readSection(), builder);
                    }
                    // Process a Condition Group
                    else if (command.equalsIgnoreCase("conditiongroup")) {
                        String groupName = tokenString.popToken();
                        parseConditionGroup(groupName, reader.readSection(), builder);
                    }
                    // Check if this is a toggle for shortcuts.
                    else if (command.equalsIgnoreCase("shortcuts")) {
                        String fileName = tokenString.popToken();
                        if (fileName.isEmpty()) {
                            shortcuts.clear();
                        } else {
                            File shortcutFile = new File(source.getParent(), fileName);
                            useShortcuts(shortcutFile, reader.getLineNumber());
                        }
                    }
                    // Process an included file
                    else if (command.equalsIgnoreCase("include")) {
                        String fileName = tokenString.popToken();
                        processIncludedFile(fileName, builder, parents);
                    }
                    // Parse a rule starting with the pattern
                    else if (command.matches("match|catch|replace|rewrite")) {
                        String pattern = ShortCutManager.replace(logger, shortcuts, tokenString.getString());
                        parseRule(new Rule(pattern), reader.readSection(), builder, actionFactory);
                    }
                    // Parse a rule starting with the ID/Description
                    else if (command.matches("rule")) {
                        String id = tokenString.popToken();
                        String descr = tokenString.getString();
                        parseRule(new Rule(id, descr), reader.readSection(), builder, actionFactory);
                    }
                } catch (ParseException e) {
                    parserError(e.getErrorOffset(), e.getMessage(),builder.getConfigName());
                }

            }

            reader.close();

        } catch (IOException e) {
            String err = "IO Exception during processing: " + e.getMessage();
            logger.severe(err);
            return new InvalidChain(err);
        }
        return builder.build();
    }


    /* Private Parser Methods */

    private void parseRule(Rule rule,
                           List<NumberedLine> lines,
                           RuleChain.Builder builder,
                           ActionFactory factory) throws ParseException {

        for (NumberedLine line : lines) {
            TokenString tokenString = new TokenString(line.string);
            String command = tokenString.popToken();

            // rule <id> [description]
            if (command.equalsIgnoreCase("rule")) {
                rule.setId(tokenString.popToken()); // First argument is the ID
                rule.setDescription(tokenString.getString()); // Second argument is the Description
            } else if (command.equalsIgnoreCase("match")) {
                rule.setPattern(ShortCutManager.replace(logger, shortcuts, tokenString.getString()));
            }
            // conditions <conditiongroup>
            else if (command.equalsIgnoreCase("conditions")) {
                String groupName = tokenString.popToken();
                if (!rule.addConditions(builder.getConditionGroups().get(groupName))) {
                    throw new ParseException("Unable to find Condition Group: " + groupName, line.number);
                }
            }
            // actions <actiongroup>
            else if (command.equalsIgnoreCase("actions")) {
                String groupName = tokenString.popToken();
                if (!rule.addActions(builder.getActionGroups().get(groupName))) {
                    throw new ParseException("Unable to find Action Group: " + groupName, line.number);
                }
            }
            // then <action> [parameters]
            else if (command.equalsIgnoreCase("then")) {
                String actionName = tokenString.popToken();
                try {
                    rule.addAction(factory.getAction(actionName, tokenString.getString()));
                } catch (InvalidActionException ex) {
                    throw new ParseException("Error in action line: " + ex.getMessage(), line.number);
                }
            }
            // condition <parameters>
            else if (Condition.isCondition(command)) {
                // This is a condition.  Add a new condition to this rule.
                Condition newCondition = Condition.newCondition(tokenString.getOriginalString());
                if (!rule.addCondition(newCondition)) {
                    throw new ParseException("Could not parse condition: " + tokenString.getOriginalString(), line.number);
                }
            }
        }
        if (rule != null && rule.isValid()) {
            builder.append(rule);
            return;
        }

        throw new ParseException("Unable to parse a valid rule.", lines.get(0).number);

    }

    /**
     * Parse the provided strings into action objects, and add the group to the ruleChain.
     *
     * @param groupName A String containing the name of the group
     * @param lines     A list of Strings containing the actions to parse.
     */
    private void parseActionGroup(String groupName, List<NumberedLine> lines, RuleChain.Builder builder) throws ParseException {

        ArrayList<Action> actionGroup = new ArrayList<>();

        for (NumberedLine line : lines) {
            TokenString tString = new TokenString(line.string);
            String command = tString.popToken();

            if (command.equals("then")) command = tString.popToken();

            try {
                actionGroup.add(actionFactory.getAction(command, tString.getString()));
            } catch (InvalidActionException ex) {
                throw new ParseException("Error parsing action: " + ex.getMessage(), line.number);
            }
        }

        if (actionGroup.size() == 0) {
            throw new ParseException("Empty actionGroup found: " + groupName, lines.get(0).number);
        } else {
            try {
                builder.addActionGroup(groupName, actionGroup);
            } catch (InvalidObjectException e) {
                throw new ParseException(e.getMessage(), lines.get(0).number);
            }
        }

    }

    /**
     * Parse the provided strings into Condition objects, and add the group to the ruleChain.
     *  @param groupName A String containing the name of the group
     * @param lines     A list of Strings containing the Conditions to parse.
     * @param builder A RuleChain Builder object.
     */
    private void parseConditionGroup(String groupName, List<NumberedLine> lines, RuleChain.Builder builder) throws ParseException {

        ArrayList<Condition> conditionGroup = new ArrayList<>();

        for (NumberedLine line : lines) {
            TokenString tString = new TokenString(line.string);
            String command = tString.popToken();

            Condition thisCondition = Condition.newCondition(command, tString.getString());

            if (thisCondition != null) {
                conditionGroup.add(thisCondition);
            } else {
                throw new ParseException("Unable to parse condition: " + command + " " + tString.getString(), line.number);
            }
        }

        if (conditionGroup.size() == 0) {
            throw new ParseException("Empty Condition Group found: " + groupName, lines.get(0).number);
        } else {
            try {
                builder.addConditionGroup(groupName, conditionGroup);
            } catch (InvalidObjectException e) {
                throw new ParseException(e.getMessage(), lines.get(0).number);
            }

        }

    }

    private void processIncludedFile(String lineData, RuleChain.Builder parentBuilder, List<File> parents) {
        // Major change to the way this is done.  It used to be its own chain, which was linked.
        // Now, we are going to import each statement into this chain.  We will apply this chain's
        // shortcuts, actiongroups, etc. to the chain.  This will allow different chains to include
        // the same files, but have different actions.  Eg: a chat filter might want to kick a player
        // but an itemfilter would not kick the player, and instead destroy the item.

        logger.fine("Including chain: " + lineData + " in: " + parentBuilder.getConfigName());

        //TODO: Fix infinite loop problems.
//        if (getParents().contains(lineData))
//            throw new ParserException(lineNo, "Recursion error.  File: " + lineData + " has already been included.");

        File child = new File(lineData);

        parse(child, parents, parentBuilder);

    }

    // Updates parser with new shortcuts.
    private void useShortcuts(File shortcutFile, int lineNo) throws IOException, ParseException {
        shortcuts.clear();
        shortcuts.putAll(ShortCutManager.getInstance().getShortcutMap(shortcutFile));
        if (shortcuts.isEmpty()) {
            throw new ParseException("Could not load shortcuts file: " + shortcutFile.getPath(), lineNo);
        }
    }

    /**
     * The parserError method generates a standardized warning message in the
     * Minecraft console log.
     *
     * @param line  The line number that triggered the error.
     * @param error A String containing the error message.
     */
    private String parserError(int line, String error, String fileName) {
        return (String.format("Parser Error (%s:%d) %s", fileName, line, error));
    }

}