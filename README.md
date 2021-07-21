# In Memory File System
This repository is an exercise in building an in-memory File System. As more features are checked in, more documentation will be added here.

# Testing the program
## Start the File Server
Open a terminal and set the root of the project as the working directory. 
### Create the Gradle Wrapper
```bash
gradle wrapper
```
### Run the File Server
You can pick any free port, this example uses `4959`.
```bash
./gradlew run -DentryPoint=com.material.server.FileServer --args='4959'
```
Once you see  output like:
```bash
2021-07-20 19:42:00 INFO  [pool-1-thread-1] - Waiting for client connection... (at Server:40)
```

Then you're ready to start the client.

## Start the File Client
You can pick any free port, this example uses `4959`. Make sure the port is identical to the server port.
NOTE: You can run multiple clients against the server.
```bash
./gradlew run -DentryPoint=com.material.client.FileSystemClient --args='127.0.0.1 4959' -is --console=plain
```
once you see a message like:
```bash
2021-07-20 14:53:28 INFO  [main] - Starting file system client... connecting to server on 127.0.0.1:4959 (at main:30)
2021-07-20 14:53:28 INFO  [main] - > connected on: 62556 (at main:55)
```
The client has successfully connected. In the server terminal you'll see a message like:
```bash
2021-07-20 19:42:11 INFO  [pool-1-thread-1] - Client connected 64825 : 4959 (at Server:42)
2021-07-20 19:42:11 INFO  [pool-1-thread-1] - Handling client: 64825 (at Server:34)
```
denoting the client successfully connected.
### Login
by default, you're a guest user. In the client terminal, type a command to login to get started. `cpark` is an admin user
```bash
login cpark 1234
2021-07-20 19:44:16 INFO  [main] - > User login success: cpark / ADMIN (at FileSystemClient:61)
```
NOTE: You can add more users to `src/main/resources/users.txt` to test other user names.

### Use the File System
You can take a quick look at the `man` command to see all possible commands:
```bash
man
2021-07-20 19:45:29 INFO  [main] - User Manual for In-Memory File System ... (truncated for brevity)
```

### Logout
You can logout and login as a different user by simply typing
```bash
logout
```

## Running Tests
You can run JUnit Tests which have JaCoCo enabled for coverage monitoring with 
```bash
./gradlew test -is
```
or debug tests with 
```bash
./gradlew test -is --debug-jvm
```
you can pass `--debug-jvm` to tests, server, or client processes to debug their execution, but it will take some
finesse to debug both a server and client at the same time. E.g. creating multiple Remote Debug setups
in IDEA, and ensuring each process starts on a different jwp transport.

###
Tests include an automated harness which reads text files with commands to automate behavior, and test multiple session behavior.
You can tweak any of the input files to change the behavior that occurs during these tests.
