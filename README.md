Shoptopia
=======================

Shoptopia is a lightweight showcase plugin solution for the Skytopia Skyblock server. 

**Please note:** This plugin will not work outside of Skytopia due to the usage of multiple
 parts of the Skyblock plugin. Please feel free to submit changes or improvements to the code, however!

Contributors
------------
* [lavuh](https://github.com/lavuh) (Plugin development)
* [JacquiRose](https://github.com/JacquiRose) (Plugin integration, testing)

Features
--------
- Admin showcases are defined in `/plugins/Shoptopia/shops.xml`. They have unlimited stock.
- Player-created showcases are stored in a database table, which persists through server restarts.
- Showcase item drops cannot be interacted with, and are regularly checked for tampering.

Compiling
---------
Compiling is not recommended at this current stage. You will need to use [Maven](https://maven.apache.org/) to compile:
* Spigot/CraftBukkit libraries from [BuildTools](https://www.spigotmc.org/wiki/buildtools/)
* sk89q-command-framework libraries from [our fork](https://github.com/skytopia/sk89q-command-framework)
* Floating-Anvil libraries, which are not open source. There is no API currently.

Once you have these libraries compiled, on your commandline, type the following.
```
cd /path/to/Shoptopia
mvn clean install
```
Maven automatically downloads the other required dependencies.
Output JAR will be placed in the `/target` folder which can be then put into the plugins folder.
