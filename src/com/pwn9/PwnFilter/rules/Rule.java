package com.pwn9.PwnFilter.rules;

import com.pwn9.PwnFilter.FilterState;
import com.pwn9.PwnFilter.PwnFilter;
import com.pwn9.PwnFilter.rules.action.Action;
import com.pwn9.PwnFilter.rules.action.ActionFactory;
import com.pwn9.PwnFilter.util.LimitedRegexCharSequence;
import com.pwn9.PwnFilter.util.Patterns;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule object
 * <p/>
 * <P>Each Rule has a single match Pattern, an ArrayList of {@link Condition}'s and an ArrayList of {@link com.pwn9.PwnFilter.rules.action.Action}'s</P>
 * TODO: Finish docs
 */
public class Rule implements ChainEntry {
    final Pattern pattern;
//    String name; // TODO: Give rules names for logs and troubleshooting
    ArrayList<Condition> conditions = new ArrayList<Condition>();
    ArrayList<Action> actions = new ArrayList<Action>();
    ArrayList<String> includeEvents = new ArrayList<String>();
    ArrayList<String> excludeEvents = new ArrayList<String>();

        /* Constructors */

    // All rules must have a matchStr, hence no parameter-less constructor.
    public Rule(String matchStr) {
        this.pattern = Patterns.compilePattern(matchStr);
    }

    /* Methods */

    /**
     * apply this action to the current message / event.  May trigger other bukkit events.
     * @param state A FilterState object for this event.
     * @return true if action was taken, false if not.
     */
    public boolean apply(FilterState state) {

        // Check if action matches the current state of the message

        if (PwnFilter.debugMode.compareTo(PwnFilter.DebugModes.high) >= 0) {
            PwnFilter.logger.info("Testing Pattern: " + pattern.toString() + " on string: " + state.message.getPlainString());
        }

            LimitedRegexCharSequence limitedRegexCharSequence = new LimitedRegexCharSequence(state.message.getPlainString(),1000);
            final Matcher matcher = pattern.matcher(limitedRegexCharSequence);
        // If we don't match, return immediately with the original message
        try {
            if (!matcher.find()) return false;
        } catch (RuntimeException ex) {
            PwnFilter.logger.severe("Regex match timed out! Regex: " + pattern.toString());
            PwnFilter.logger.severe("Failed string was: " + limitedRegexCharSequence);
        }

        state.pattern = pattern;

        // If Match, log it and then check any conditions.
        state.addLogMessage("|" + state.eventType.toString() +  "| MATCH <" +
                state.playerName + "> " + state.message.getPlainString());

        PwnFilter.matchTracker.increment(); // Update Match Statistics

        for (Condition c : conditions) {
            // This checks that EVERY condition is met (conditions are AND)
            if (!c.check(state)) {
                state.addLogMessage("CONDITION not met <"+ c.flag.toString()+
                        " " + c.type.toString()+" " + c.parameters + "> " + state.getOriginalMessage());
                return false;
            }

        }

        // If we get this far, execute the actions
        for (Action a : actions) {
            a.execute(state);
        }
        return true;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * When we are building a new rule chain (eg, by reloading a file), this routine parses
     * out a line (eg: "then replace") and adds it to this rule.
     * @param command the first part of the line, eg: "then" or "ignore"
     * @param parameterString the remainder of the line eg: "kick" or "user tremor77"
     * @return true on success, false on failure
     */
    public boolean addLine(String command, final String parameterString) {

        command = command.toLowerCase();

        if (command.matches("then")) {
            // This is an action.  Try to add a new action with its parameters.

            Action newAction = ActionFactory.getActionFromString(parameterString);
            if (newAction != null) {
                actions.add(newAction);
                return true;
            } else {
                return false;
            }

        }
        else if (command.matches("events")) {
            String[] parts = parameterString.split("[\\s|,]");

            if (parts[0].matches("not")) {
                for (int i = 1; i < parts.length ; i++ ) {
                    excludeEvents.add(parts[i].toUpperCase());
                }
            } else {
                for (String event : parts ) {
                    includeEvents.add(event.toUpperCase());
                }
            }

            return true;
        }

        else if ( Condition.isCondition(command) )  {
            // This is a condition.  Add a new condition to this rule.
            Condition newCondition = Condition.newCondition(command,parameterString);
            return newCondition != null && conditions.add(newCondition);
        }

        // This line isn't a condition or an action...
        return false;
    }


    public boolean isValid() {
        // Check that we have a valid pattern and at least one action
        return this.pattern != null && this.actions != null;
    }

    public String toString() {
        return pattern.toString();
    }

    @Override
    public Set<String> getPermissionList() {
        TreeSet<String> permList = new TreeSet<String>();

        for (Condition c : conditions) {
            if (c.type == Condition.CondType.permission) {
                Collections.addAll(permList, c.parameters.split("\\|"));
            }
        }

        return permList;
    }
}