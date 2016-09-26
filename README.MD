# Events

[![Build Status](https://travis-ci.org/csh/bukkit-events-api.svg?branch=master)](https://travis-ci.org/csh/bukkit-events-api)
[![Coverage Status](https://coveralls.io/repos/github/Fireflies/bukkit-events-api/badge.svg?branch=master)](https://coveralls.io/github/Fireflies/bukkit-events-api?branch=master)

A fancy library for fancy event handling.

## Table of Contents

- [Usage](#usage)
- [License](#license)
- [Examples](#examples)
    - [Basic Handlers](#basic-handlers)
    - [Time Based Handling](#time-based-handling)
    - [Conditional Handling](#conditional-handling)

## Usage

**You must add the [Sonatype snapshot repository](https://oss.sonatype.org/content/repositories/snapshots) to your build tool of choice.**

You may import this library by adding the following snippets to your build configuration, if you want to see your build tool of choice
listed here then I'm happy to accept PRs.

This is not a standalone plugin so you must include the classes in your JAR.

##### Gradle

You can use John Engelman's "[Shadow](https://github.com/johnrengelman/shadow)" plugin, or you could setup a task manually for a little
practice. :wink:

```groovy
dependencies {
    compile group: 'ninja.smirking', name: 'bukkit-events-api', version: '1.0-SNAPSHOT'
}
```

##### Maven

You must use the `maven-shade-plugin` plugin to include the classes in your final JAR.

```xml
<dependencies>
  <dependency>
    <groupId>ninja.smirking</groupId>
    <artifactId>bukkit-events-api</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
  </dependency>
</dependencies>
```

## License

Events is released to the public under the BSD "Simplified" (2 Clause) license.
If you are shading this as a dependency with the intentions of distributing it
then you will be happy to know that the license is pre-packaged.

```
Copyright (c) 2016 Connor Spencer Harries
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## Examples

These examples will not be spectacularly complex, they're just here to demonstrate how you could use this
library to add a little spice to your event handling. It may be of interest to developers to know that
all public methods in the `Events` class will return a `Listener` that can be unregistered using the
traditional procedure.

### Basic Handlers

When registering event handlers through the `Events` class you may specify whether you want to observe
a single event or multiple events of the given type. For observing a single event we use the `observe`
method and for observing multiple events we use `observeAll`.

Singular event example:
```java
Events.observe(PlayerLoginEvent.class, event -> {
    event.getPlayer().getServer().getLogger().log(Level.INFO, "Woohoo, that's our first player for today!");
});
```

Multiple events example:
```java
AtomicInteger counter = new AtomicInteger();
Events.observeAll(PlayerLoginEvent.class, event -> {
    event.getPlayer().getServer().getLogger().log(Level.INFO, "Woohoo, {0} players have logged in today!", new Object[] {
        counter.incrementAndGet();
    });
});
```

### Time Based Handling

Events also allows you to specify handlers which will unregister themselves after a certain
amount of time, it's really rather nifty.

```java
Events.observeFor(PlayerJoinEvent.class, event -> {
    event.getPlayer().sendMessage("Double XP is active for the next few hours!");
}, 6L, TimeUnit.HOURS);
```

It's also possible to get the remaining time (in milliseconds) by using a `BiConsumer` in the
`observeFor` call instead.

### Conditional Handling

You can also tell Events that you only want to handle certain events of the given type by
providing a `Predicate` which is tested prior to the event being passed to the handler.

```java
// Bukkit imports omitted

import ninja.smirking.events.bukkit.Events;

public class MaintenancePlugin extends JavaPlugin {
    private boolean maintenanceEnabled;
    private String maintenanceMotd;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.maintenanceMotd = getConfig().getString("motd", "Server is currently in maintenance mode.");
        Events.observeIf(ServerListPingEvent.class, event -> event.setMotd(maintenanceMotd), event -> maintenanceEnabled);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("maintenance-toggle")) {
            maintenanceEnabled = !maintenanceEnabled;
        } else {
            reloadConfig();
            this.maintenanceMotd = getConfig().getString("motd", maintenanceMotd);
        }
        return true;
    }
}
```
