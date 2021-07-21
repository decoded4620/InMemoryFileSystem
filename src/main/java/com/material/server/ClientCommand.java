package com.material.server;

import java.util.ArrayList;
import java.util.StringJoiner;


public class ClientCommand {
  private final ArrayList<String> _commandArgs = new ArrayList<>();
  private final ArrayList<Character> _commandOpts = new ArrayList<>();
  private String _name;
  private String _userInput;

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
  }

  public String getUserInput() {
    return _userInput;
  }

  public void setUserInput(String userInput) {
    _userInput = userInput;
  }

  public void addArgument(String arg) {
    arg = arg.trim();

    if (arg.startsWith("'")) {
      // trim the quotes
      arg = arg.substring(1, arg.length() - 1);
    }

    _commandArgs.add(arg);
  }

  public void addOpts(String opts) {
    if (opts == null) {
      return;
    }

    opts = opts.trim();
    if (opts.charAt(0) != '-') {
      throw new IllegalArgumentException("Opts should have a leading '-' character");
    }

    for (int i = 1; i < opts.length(); i++) {
      _commandOpts.add(opts.charAt(i));
    }
  }

  public boolean hasArgAt(int argIdx) {
    return _commandArgs.size() > argIdx;
  }

  public String getArg(int argIdx) {
    if (_commandArgs.size() > argIdx) {
      return _commandArgs.get(argIdx);
    } else {
      throw new IndexOutOfBoundsException("Argument " + argIdx + " was out of bounds");
    }
  }

  public boolean hasOpt(Character optChar) {
    return _commandOpts.contains(optChar);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ClientCommand.class.getSimpleName() + "[", "]").add("_commandArgs=" + _commandArgs)
        .add("_commandOpts=" + _commandOpts)
        .add("_name='" + _name + "'")
        .toString();
  }
}
