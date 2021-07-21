package com.material.server;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ClientInputProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ClientInputProcessor.class);

  public Map<String, Function<ClientCommand, String>> _fileSystemHandlerMap = new HashMap<>();

  public ClientInputProcessor(FileSystemCommandMapBuilder commandMapBuilder) {
    _fileSystemHandlerMap.putAll(commandMapBuilder.build());
  }

  public void onClientClosed() {
    // Naive way to ensure we logout the current user thread before we die.
    processInput("logout");
  }

  public String processInput(String clientInput) {
    CommandParser parser = new CommandParser();
    ClientCommand command = parser.parse(clientInput);

    LOG.debug("Client Command: " + command.getUserInput() + " - " + command.toString());

    if (_fileSystemHandlerMap.containsKey(command.getName())) {
      return _fileSystemHandlerMap.get(command.getName()).apply(command);
    } else {
      throw new IllegalArgumentException(
          "Command (" + command.getName() + ") was not a valid command, check your input and try again: '"
              + command.getUserInput() + "'");
    }
  }
}
