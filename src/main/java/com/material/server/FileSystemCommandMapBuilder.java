package com.material.server;

import com.material.filesystem.File;
import com.material.filesystem.FileStreamReader;
import com.material.filesystem.FileSystem;
import com.material.filesystem.FileSystemTreeNode;
import com.material.filesystem.NodeType;
import com.material.filesystem.permissions.Permission;
import com.material.filesystem.user.User;
import com.material.filesystem.user.UserManager;
import com.material.filesystem.user.UserSecurityException;
import com.material.filesystem.user.UserType;
import com.material.filesystem.util.DataGenerator;
import com.material.filesystem.util.FileSystemNodeHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * This class maps commands provided by the {@link com.material.client.FileSystemClient} via user input
 * to actions on the {@link FileSystem}.
 *
 * When a user command is parsed, the name of the parsed command is used to select an action mapped by this object.
 * if the action does not exist, an error is printed.
 */
public class FileSystemCommandMapBuilder {
  private final FileSystem _fileSystem;
  private final UserManager _userManager;

  private final HashMap<String, String> _helpMap = new HashMap<>();

  public FileSystemCommandMapBuilder(FileSystem fileSystem, UserManager userManager) {
    _fileSystem = fileSystem;
    _userManager = userManager;
  }

  public Map<String, Function<ClientCommand, String>> build() {
    Map<String, Function<ClientCommand, String>> map = new HashMap<>();
    buildLoginCommand(map);
    buildLogoutCommand(map);
    buildReadCommand(map);
    buildFindCommand(map);
    buildWriteCommand(map);
    buildWriteRandomCommand(map);
    buildWriteFromFileCommand(map);
    buildMoveCommand(map);
    buildCopyCommand(map);
    buildRmCommand(map);
    buildRmDirCommand(map);
    buildTouchCommand(map);
    buildMakeDirCommand(map);
    buildSelectDirCommand(map);
    buildLsCommand(map);
    buildPwdCommand(map);
    buildHelpCommand(map);
    buildSetUserPermissionCommand(map);
    return map;
  }

  private void buildLoginCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("login")) {
      _helpMap.put("login", "login USER PASS - Login as a user");
      commandMap.put("login", (command) -> {
        validateCommandCountAtLeast(command, 2);
        String arg1 = command.getArg(0);
        String arg2 = command.getArg(1);
        try {
          User user = _userManager.login(arg1, arg2);
          return "User login success: " + user.getUsername() + " / " + user.getUserType();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildLogoutCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("logout")) {
      _helpMap.put("logout", "logout the current user");
      commandMap.put("logout", (command) -> {
        try {
          User user = _userManager.currentUser();
          _userManager.logout();
          return "User logout success: " + user.getUsername() + " / " + user.getUserType();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildReadCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("read")) {
      _helpMap.put("read", "read DEST_FILE [FROM_POS] [TOTAL_BYTES] [-OPT] - Read contents a file if it exists. \n"
          + " Options: \n\t -s -- read the contents as a stream"
          + " \n\t -t -- truncate the contents and return only a size value");
      commandMap.put("read", (command) -> {
        validateCommandCountAtLeast(command, 1);
        String arg1 = command.getArg(0);

        Path filePath = Paths.get(arg1);
        boolean relative = !arg1.startsWith("/");
        try {
          FileSystemTreeNode node = _fileSystem.getNodeAtPath(filePath, relative);

          if (node.getNodeType() == NodeType.FILE) {
            File file = (File) node.getFileSystemObject();

            // get the stat position
            int start = command.hasArgAt(1) ? Integer.parseInt(command.getArg(1)) : 0;

            // get the end position
            int len = command.hasArgAt(2) ? Integer.parseInt(command.getArg(2)) : file.size();

            // TODO NOTE: this is naive to return the entire stream to the client caller, can be improved
            if (command.hasOpt('s')) {
              try {
                FileStreamReader reader = new FileStreamReader(new ByteArrayOutputStream(file.size()), 4);
                file.readContentStream(reader, start, len);
                return command.hasOpt('t') ? reader.getBytes().length + " bytes" : new String(reader.getBytes());
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            } else {
              if (start == 0 && len == file.size()) {
                return new String(file.getContents());
              }

              if (start + len > file.size()) {
                throw new IllegalArgumentException(
                    "File size " + file.size() + " was less than the length to read: " + len);
              } else {
                return new String(Arrays.copyOfRange(file.getContents(), start, start + len));
              }
            }
          } else {
            throw new UnsupportedOperationException("Cannot read contents of a directory");
          }
        } catch (FileNotFoundException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildWriteCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("write")) {
      _helpMap.put("write",
          "write TO_FILE CONTENT [FROM_IDX] [-OPT] - Write to a file" + "\n Options: \n\t -s -- write as a stream");
      commandMap.put("write", (command) -> {
        validateCommandCountAtLeast(command, 2);
        String arg1 = command.getArg(0);

        try {
          FileSystemTreeNode destinationNode = _fileSystem.getNodeAtPath(Paths.get(arg1), !arg1.startsWith("/"));

          if (destinationNode.getNodeType() == NodeType.FILE) {
            File file = (File) destinationNode.getFileSystemObject();

            String arg2 = command.hasArgAt(1) ? command.getArg(1) : "";
            int startWritePos = command.hasArgAt(2) ? Integer.parseInt(command.getArg(2)) : file.size();
            byte[] writeBytes = arg2.getBytes(StandardCharsets.UTF_8);

            if (command.hasOpt('s')) {
              // we will always write the whole input buffer, no reason to complicate it.
              file.writeContentStream(new ByteArrayInputStream(writeBytes, 0, writeBytes.length), startWritePos);
            } else {
              file.setContents(writeBytes);
            }
            return "Write " + writeBytes.length + " chars of content to /" + destinationNode.getPath().toString();
          } else {
            throw new UnsupportedOperationException("Cannot write contents to  Directory, only Files");
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildWriteRandomCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("writeRandom")) {
      _helpMap.put("writeRandom", "write TO_FILE SIZE [FROM_IDX] [-OPT] - Write to a file SIZE random bytes"
          + "\n Options: \n\t -s -- write as a stream");
      commandMap.put("writeRandom", (command) -> {
        validateCommandCountAtLeast(command, 2);
        String arg1 = command.getArg(0);

        try {
          FileSystemTreeNode destinationNode = _fileSystem.getNodeAtPath(Paths.get(arg1), !arg1.startsWith("/"));

          if (destinationNode.getNodeType() == NodeType.FILE) {
            File file = (File) destinationNode.getFileSystemObject();

            String arg2 = command.hasArgAt(1) ? command.getArg(1) : "1";
            int startWritePos = command.hasArgAt(2) ? Integer.parseInt(command.getArg(2)) : file.size();
            byte[] writeBytes = DataGenerator.randomArray(Integer.parseInt(arg2));

            if (command.hasOpt('s')) {
              // we will always write the whole input buffer, no reason to complicate it.
              file.writeContentStream(new ByteArrayInputStream(writeBytes, 0, writeBytes.length), startWritePos);
            } else {
              file.setContents(writeBytes);
            }
            return "Write Random" + writeBytes.length + " chars of content to /" + destinationNode.getPath().toString();
          } else {
            throw new UnsupportedOperationException("Cannot write contents to  Directory, only Files");
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildWriteFromFileCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("writeFromFile")) {
      _helpMap.put("writeFromFile", "writeFromFile TO_FILE LOCAL_DISK_FILE_NAME [FROM_IDX] [-OPT] - Write to a file"
          + "\n Options: \n\t -s -- write as a stream");
      commandMap.put("writeFromFile", (command) -> {
        validateCommandCountAtLeast(command, 2);
        String arg1 = command.getArg(0);
        try {
          FileSystemTreeNode destinationNode = _fileSystem.getNodeAtPath(Paths.get(arg1), !arg1.startsWith("/"));

          if (destinationNode.getNodeType() == NodeType.FILE) {
            File file = (File) destinationNode.getFileSystemObject();

            String localFilePath = command.hasArgAt(1) ? command.getArg(1) : "";
            int startWritePos = command.hasArgAt(2) ? Integer.parseInt(command.getArg(2)) : file.size();

            java.io.File f = new java.io.File(localFilePath);

            if (!f.exists()) {
              throw new FileNotFoundException("Could not find local file: " + f.getAbsolutePath());
            } else if (!f.canRead()) {
              throw new FileSystemException("Could not read local file: " + f.getAbsolutePath());
            }

            StringBuilder writeContent = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(f)) {
              int content;
              // reads a byte at a time, if it reached end of the file, returns -1
              while ((content = fis.read()) != -1) {
                writeContent.append((char) content);
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
            byte[] writeBytes = writeContent.toString().getBytes(StandardCharsets.UTF_8);

            if (command.hasOpt('s')) {
              // we will always write the whole input buffer, no reason to complicate it.
              file.writeContentStream(new ByteArrayInputStream(writeBytes, 0, writeBytes.length), startWritePos);
            } else {
              file.setContents(writeBytes);
            }
            return "Write " + writeBytes.length + " chars of content to /" + destinationNode.getPath().toString();
          } else {
            throw new UnsupportedOperationException("Cannot write contents to  Directory, only Files");
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildMoveCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("mv")) {
      _helpMap.put("mv", "mv SOURCE DEST [-OPT] - Move a file or folder from SOURCE to DEST"
          + "\n Options: \n\t -f -- overwrite existing file(s) or dir(s)");
      commandMap.put("mv", (command) -> {
        validateCommandCountAtLeast(command, 2);
        String arg1 = command.getArg(0);
        String arg2 = command.getArg(1);
        try {
          FileSystemTreeNode movedNode =
              _fileSystem.moveNodeTo(Paths.get(arg1), Paths.get(arg2), !arg1.startsWith("/"), !arg2.startsWith("/"),
                  command.hasOpt('f'));
          return "moved node from: " + arg1 + " to /" + movedNode.getPath().toString();
        } catch (FileNotFoundException | FileAlreadyExistsException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildCopyCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("cp")) {
      _helpMap.put("cp", "cp SOURCE DEST [-OPT] - Copy a file or folder from SOURCE to DEST"
          + "\n Options: \n\t -f -- overwrite existing file(s). When copying directories, contents are merged if"
          + " there exists another directory at the same destination level as the source directory is being copied to.");
      commandMap.put("cp", (command) -> {
        validateCommandCountAtLeast(command, 2);
        String arg1 = command.getArg(0);
        String arg2 = command.getArg(1);
        try {
          FileSystemTreeNode movedNode =
              _fileSystem.copyNode(Paths.get(arg1), Paths.get(arg2), !arg1.startsWith("/"), !arg2.startsWith("/"),
                  command.hasOpt('f'));
          return "copied node from: " + arg1 + " to /" + movedNode.getPath().toString();
        } catch (FileNotFoundException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildTouchCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("touch")) {
      _helpMap.put("touch",
          "touch DEST_FILE [SIZE] [-OPT] - Create a file if not already existing. If size is specified, generate random content of the specified size. \n"
              + " Options: \n\t -R, -r -- create non-existent dirs leading up to file");
      commandMap.put("touch", (command) -> {
        validateCommandCountAtLeast(command, 1);
        String arg1 = command.getArg(0);
        Path filePath = Paths.get(arg1);
        boolean relative = !arg1.startsWith("/");
        try {
          if (_fileSystem.nodeExists(filePath, relative)) {
            return "file exists at /" + _fileSystem.getNodeAtPath(Paths.get(arg1), relative);
          }
          FileSystemTreeNode createdNode = _fileSystem.createNodeAtPath(filePath, NodeType.FILE, relative,
              command.hasOpt('r') || command.hasOpt('R'));
          return "created file at /" + createdNode.getPath().toString();
        } catch (FileNotFoundException | FileAlreadyExistsException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildFindCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("find")) {
      _helpMap.put("find", "find MATCH_REG_EX [-OPT] - find one or more files matching MATCH_REG_EX"
          + "\n Options: \n\t -a -- find all files matching, if not supplied only first file is returned \n\t -l -- print verbose info for each result");
      commandMap.put("find", (command) -> {
        validateCommandCountAtLeast(command, 1);
        String arg1 = command.getArg(0);
        boolean findAll = command.hasOpt('a');
        Collection<FileSystemTreeNode> results;
        try {
          if (findAll) {
            results = _fileSystem.findAllNodesMatching(Pattern.compile(arg1));
          } else {
            results = Collections.singleton(_fileSystem.findFirstNodeMatching(Pattern.compile(arg1)));
          }

          boolean isVerbose = command.hasOpt('l');
          StringBuilder resultString = new StringBuilder("\n");
          results.forEach(
              result -> FileSystemNodeHelper.getNodeInfo(_userManager, result, resultString, isVerbose, true));
          return resultString.toString();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildRmCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("rm")) {
      _helpMap.put("rm", "rm DEST_FILE - Remove a file if it exists.");
      commandMap.put("rm", (command) -> {
        validateCommandCountAtLeast(command, 1);
        String arg1 = command.getArg(0);
        Path filePath = Paths.get(arg1);
        boolean relative = !arg1.startsWith("/");
        try {
          if (_fileSystem.nodeExists(filePath, relative)) {
            FileSystemTreeNode existingNode = _fileSystem.getNodeAtPath(filePath, relative);

            if (existingNode.getNodeType() == NodeType.DIRECTORY) {
              return filePath.toString() + " is a directory, use rmDir to remove it";
            } else {
              if (_fileSystem.removeNodeAtPath(filePath, relative)) {
                return "removed file " + filePath;
              } else {
                return "file not removed " + filePath;
              }
            }
          } else {
            throw new FileNotFoundException("File " + filePath + " does not exist");
          }
        } catch (FileNotFoundException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildRmDirCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("rmDir")) {
      _helpMap.put("rmDir", "rmDir DEST_DIR [-OPT] - Remove a directory if it exists. \n"
          + " Options: \n\t -R, -r -- recursively remove");
      commandMap.put("rmDir", (command) -> {
        validateCommandCountAtLeast(command, 1);
        String arg1 = command.getArg(0);
        Path filePath = Paths.get(arg1);
        boolean relative = !arg1.startsWith("/");
        try {
          if (_fileSystem.nodeExists(filePath, relative)) {
            FileSystemTreeNode existingNode = _fileSystem.getNodeAtPath(filePath, relative);

            if (existingNode.getNodeType() == NodeType.FILE) {
              return filePath.toString() + " is a file, use rm to remove it";
            } else if (existingNode.getNodeType() == NodeType.DIRECTORY) {
              if (existingNode.getChildren().isEmpty() || command.hasOpt('R') || command.hasOpt('r')) {
                if (_fileSystem.removeNodeAtPath(filePath, relative)) {
                  return "removed directory " + filePath.toString();
                } else {
                  return "directory not removed " + filePath.toString();
                }
              } else {
                return "Directory " + filePath.toString() + " is not empty, use -R to recursively remove the files";
              }
            } else {
              throw new UnsupportedOperationException("Only files and directories can be removed");
            }
          } else {
            throw new FileNotFoundException("File " + filePath + " does not exist");
          }
        } catch (FileNotFoundException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildMakeDirCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("mkDir")) {
      _helpMap.put("mkDir", "mkDir DEST_DIR [-OPT] - Make a directory\n"
          + "Options: \n\t -R, -r -- create non-existent dirs leading up to file");
      commandMap.put("mkDir", (command) -> {
        validateCommandCountAtLeast(command, 1);
        String arg1 = command.getArg(0);

        try {
          FileSystemTreeNode createdNode =
              _fileSystem.createNodeAtPath(Paths.get(arg1), NodeType.DIRECTORY, !arg1.startsWith("/"),
                  command.hasOpt('r') || command.hasOpt('R'));
          return "created directory: " + arg1 + " to /" + createdNode.getPath().toString();
        } catch (FileNotFoundException | FileAlreadyExistsException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildSelectDirCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("cd")) {
      _helpMap.put("cd", "cd DEST_DIR [-OPT] - Change working directory");
      commandMap.put("cd", (command) -> {
        validateCommandCountAtLeast(command, 1);

        String arg1 = command.getArg(0);
        try {
          _fileSystem.selectWorkingNode(Paths.get(arg1), !arg1.startsWith("/"));
          return "working dir set to: /" + _fileSystem.getWorkingNode().getPath().toString();
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void buildLsCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("ls")) {
      _helpMap.put("ls", "ls - List files / folders in the current working directory");
      commandMap.put("ls", (command) -> {

        FileSystemTreeNode nodeToList;
        if (command.hasArgAt(0)) {
          Path lsPath = Paths.get(command.getArg(0));
          try {
            nodeToList = _fileSystem.getNodeAtPath(lsPath, command.getArg(0).startsWith("/"));
          } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
          }
        } else {
          nodeToList = _fileSystem.getWorkingNode();
        }
        StringBuilder childrenNames = new StringBuilder("\n");

        // NOTE - doesn't handle '.' yet.
        if (!nodeToList.isRootNode()) {
          childrenNames.append("..");
        }

        boolean isVerbose = command.hasOpt('l');
        nodeToList.getChildren()
            .forEach(child -> FileSystemNodeHelper.getNodeInfo(_userManager, child, childrenNames, isVerbose, false));

        return childrenNames.toString();
      });
    }
  }

  private void buildPwdCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("pwd")) {
      _helpMap.put("pwd", "pwd - Print the working directory");
      commandMap.put("pwd", (command) -> '/' + _fileSystem.getWorkingNode().getPath().toString());
    }
  }

  private void buildHelpCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("man")) {
      _helpMap.put("man", "man [COMMAND] [-OPT] - Show the help. If COMMAND specified, only show help for that command."
          + "\n Options: \n\t -v -- show verbose help");
      commandMap.put("man", (command) -> {
        if (command.hasArgAt(0)) {
          return _helpMap.get(command.getArg(0));
        } else {
          StringBuilder manual = new StringBuilder();
          manual.append("User Manual for In-Memory File System\n\n");
          _helpMap.forEach((cmd, help) -> manual.append(help).append("\n"));
          return manual.toString();
        }
      });
    }
  }

  private void buildSetUserPermissionCommand(Map<String, Function<ClientCommand, String>> commandMap) {
    if (!commandMap.containsKey("setUserPermission")) {
      _helpMap.put("setUserPermission",
          "setUserPermission FILE_OR_DIR USER_OR_USER_TYPE PERMS [-OPT] - Set permissions for a user or user "
              + "type on a file or a directory\n"
              + "Options: \n\t -r -- Recursively set permissions on all files and folders below FILE_OR_DIR "
              + "\n\t -t -- Sets the permission on a UserType. The user type must be ADMIN, USER, or GUEST");
      commandMap.put("setUserPermission", (command) -> {
        validateCommandCountAtLeast(command, 3);

        String arg1 = command.getArg(0);

        try {
          FileSystemTreeNode node = _fileSystem.getNodeAtPath(Paths.get(arg1), !arg1.startsWith("/"));
          UserType userType = _userManager.currentUser().getUserType();
          if (node.getOwner() != _userManager.currentUser() && userType != UserType.ADMIN
              && userType != UserType.ROOT) {
            throw new UserSecurityException("Only admin and root users can set user permissions");
          }

          String userNameOrType = command.getArg(1);
          String permissionStr = command.getArg(2);
          String[] permSet = permissionStr.split(",");

          List<Permission> permissionList = new ArrayList<>();

          Arrays.stream(permSet)
              .forEach(permString -> permissionList.add(Permission.valueOf(permString.toUpperCase())));

          boolean recursive = command.hasOpt('r');
          if (command.hasOpt('t')) {
            final UserType inputUserType = UserType.valueOf(userNameOrType);
            permissionList.forEach(permission -> node.setUserTypePermission(inputUserType, permission, recursive));
          } else {
            User inputUser = _userManager.getUser(userNameOrType);
            permissionList.forEach(permission -> node.setUserPermission(inputUser, permission, recursive));
          }

          return "Updated permissions for " + userNameOrType + " to " + permissionList.toString();
        } catch (FileNotFoundException ex) {
          throw new RuntimeException(ex);
        }
      });
    }
  }

  private void validateCommandCountAtLeast(ClientCommand command, int count) {
    if (!command.hasArgAt(count - 1)) {
      throw new RuntimeException("Input required " + count + " args, command: " + command.getUserInput());
    }
  }
}
