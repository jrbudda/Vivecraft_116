Vivecraft for Minecraft 1.14
=========

This readme is intended for developers. For downloads and gameplay instructions please see the [official website](http://www.vivecraft.org/)


Using this Repository
========
 
 Vivecraft uses patches to avoid distributing Minecraft code. The build scripts are in Python 2.X.
 
 - Fork, checkout or download the repo using your Git method of choice.
 - Install Java JDK 1.8. The Java JRE will NOT work.
 - Set the JAVA_HOME environment variable to the JDK directory
 - Add %JAVA_HOME%\bin to your PATH environment variable
 - Install Python 2.7.x (NOT 3.x). Be sure to tick the 'add python to your PATH' option during install. [Download from python.org](https://www.python.org/downloads/)
 - Open a command prompt and navigate to the repo directory
 - Run install.bat
 
The install process (install.py) does a number of things:
 - It downloads MCP (Minecraft coder's pack) and extracts it to the \mcp9xx\ directory.
 - It downloads a ton of dependencies.
 - It merges Optifine into vanilla minecraft jar.
 - It decompiles and deobfuscates the combined minecraft/optifine into \mcp9xx\src\.minecraft_orig_nofix\
 - It applies any patches found in \mcppatches\ and copies the result to\mcp9xx\src\.minecraft_orig\
 - It applies all the patches found in \patches\ and copies the result to \mcp9xx\src\minecraft\. 
 - It copies all code files found in \src\ to \mcp9xx\src\minecraft\. 
 - It copies all files found in \assets\ to \mcp9xx\src\assets\.
 This directory is now the full 'Vivecraft' codebase.
 
If you use Eclipse you can open the workspace found in \mcp9xx\eclipse. You will have to correct the library path for the vanilla jar and realms jar, and also add libraries for JRift, json, asm, and launchwrapper, all of these can be found in the root /lib folder. To run the game from eclipse you also have to attach natives to the lwjgl jar (from lib/natives).

Make all changes to the game in the \mcp9xx\src\minecraft directory.

To update code to Github:
========
 - run getchanges.bat. This compares mcp9xx\src\minecraft to mcp9xx\src\minecraft_orig. patches are generated for modified files and copied to \patches\. Whole new files are copied to \src\.
 - Push to Github.
 
To build an installer:
========
 - run build.bat. This runs getchanges, build, and then create_install. Basically it takes the new files and patches and creates a jar. And then it uses the code and jsons found in \installer\ to make an installer.exe.

To update changes from github
========
  - After pulling changes from github run applychanges.bat. This backs up mcp9xx\src\minecraft to mcp9xx\src\minecraft_bak, and starts over by applying all patches in \patches\ to mcp9xx\src\minecraft_orig, and copies the result o mcp9xx\src\minecraft
  
There is no Forge.
