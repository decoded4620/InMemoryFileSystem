package com.material.server;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This is a naive parser which matches the following types of commands
 * 1. command [-opts]
 * 2. command "/path/arg/1" path/arg/2 [-opts]
 * 3. command "/path/arg/1" [-opts]
 * 4. command "/path" start end [-opts]
 *
 *
 * so we can have commands
 * ls
 * touch file1.txt [-f]
 * mkDir dir1.txt [-fr]
 * mv a/b c/d [-f]
 * rm a/b [-f]
 * rmDir a/b [-rf]
 * cd a/b/c
 * cd ../../
 */
public class CommandParser {
  final String tripleArgRegex =
      "^([\\w]+)[ ]+(?:('.*?')|([^\\s]*))[ ]+(?:('.*?')|([^\\s]*))[ ]+(?:('.*?')|([^\\s]*))(?:(?:[ ]+?)(\\-[\\w]+))?$";
  final String doubleArgRegex = "^([\\w]+)[ ]+(?:('.*?')|([^\\s]*))[ ]+(?:('.*?')|([^\\s]*))(?:(?:[ ]+?)(\\-[\\w]+))?$";
  final String singleArgRegex = "^([\\w]+)[ ]+(?:('.*?')|([^\\s]*))(?:(?:[ ]+?)(\\-[\\w]+))?$";
  final String zeroArgRegex = "^([\\w]+)(([ ]+?)(\\-[\\w]+))?$";

  private final Pattern tripleArgCommandWithOpts = Pattern.compile(tripleArgRegex);
  private final Pattern doubleArgCommandWithOpts = Pattern.compile(doubleArgRegex);
  private final Pattern singleArgCommandWithOpts = Pattern.compile(singleArgRegex);
  private final Pattern zerArgCommandWithOpts = Pattern.compile(zeroArgRegex);

  public ClientCommand parse(String input) {
    ClientCommand command = new ClientCommand();
    command.setUserInput(input);
    Matcher tripleArgMatcher = tripleArgCommandWithOpts.matcher(input);
    Matcher doubleArgMatcher = doubleArgCommandWithOpts.matcher(input);
    Matcher singleArgMatcher = singleArgCommandWithOpts.matcher(input);
    Matcher zeroArgMatcher = zerArgCommandWithOpts.matcher(input);

    List<Matcher> matchers = new ArrayList<>();
    matchers.add(zeroArgMatcher);
    matchers.add(singleArgMatcher);
    matchers.add(doubleArgMatcher);
    matchers.add(tripleArgMatcher);

    int expectedArgs = 0;
    boolean matched = false;
    for (Matcher matcher : matchers) {
      if (matcher.matches()) {
        matched = true;

        List<String> commandParts = new ArrayList<>();

        for (int i = 1; i <= matcher.groupCount(); i++) {
          String match = matcher.group(i);

          if (match == null || "".equals(match)) {
            continue;
          }

          commandParts.add(matcher.group(i));
        }

        if (commandParts.isEmpty()) {
          throw new IllegalArgumentException("Expected at least one argument for a command");
        }

        command.setName(commandParts.get(0));

        int i = 1;
        for (; i <= expectedArgs && i < commandParts.size(); i++) {
          command.addArgument(commandParts.get(i));
        }

        // naive way to add opts
        if (commandParts.size() > i) {
          command.addOpts(commandParts.get(i));
        }
        break;
      }

      expectedArgs++;
    }

    if (!matched) {
      throw new IllegalArgumentException("Command " + input + " was malformed");
    }

    return command;
  }
}
