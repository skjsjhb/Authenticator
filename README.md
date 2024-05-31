# Authenticator

Login helper for Minecraft. Displays login page using JCEF and outputs the authorization code.

## Purpose

This program utilizes JCEF to create a Chromium web viewer. It then opens the official Minecraft login page and
prompt the user to login. The authorization code is printed to `stdout` once login complete.

Some reasons you might want to use this:

- Supports multiple accounts (by changing the cache ID).
- Reliable (the embedded Chromium comes with the latest features).
- Works in China (by utilizing a local mirror).
- Easy to embed (the JAR file is around 7 MiB).

Some reasons you might **NOT** want to use this:

- If you hate to have multiple Chromiums on your (or your customers') machine.
- If the environment is not capable of downloading files of size around 100 MiB (the browser).
- If you think printing to `stdout` is dangerous.

## Usage

This program is mainly released as fat JARs. To prompt the user to login, run:

```
java -jar Authenticator.jar [cacheId] [useMirrorCN]
```

Where `Authenticator.jar` is the path to the downloaded JAR. For the arguments:

- `cacheId`: Sets the user data path of this session. Can be used to "forget" cached accounts.
- `useMirrorCN`: Uses an alternative mirror in China. Set the value to `mirror` to enable.

On macOS you'll need some extra JVM flags:

```
--add-opens java.desktop/sun.awt=ALL-UNNAMED
--add-opens java.desktop/sun.lwawt=ALL-UNNAMED
--add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED
```

The output (`stdout`) looks like:

```
==== DO NOT SHARE THE CODE BELOW ====
Code=M.XXXX_BAY.1.2.12345678-0000-aaaa-1234-111111111111
==== DO NOT SHARE THE CODE ABOVE ====
```

There may be some extra stuff printed by the JCEF framework. However, you just need to extract the line that starts
with `Code=`. If there is no such line, then the program has crashed, or the user has closed the window without logging
in.

Here is a simple example of retrieving the code using Node.js:

```js
const cp = require("child_process");
cp.exec(`java -jar /path/to/authenticator.jar myCacheId`, (_, stdout) => {
    const code = stdout.split("\n").find(it => it.startsWith("Code="))?.slice(5);
    // Use the code
});
```

## License and Disclaimer

![WTFPL](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-4.png)

Copyright (C) 2024 Ted "skjsjhb" Gao.

Licensed under WTFPL, version 2.

This program is created in the hope that it will be useful, but WITHOUT ANY WARRANTY. Use at your own risk.

The output is not encrypted. It's the caller's responsibility to protect the `stdout` of this program from malware.

This program by itself does not collect nor send any data. Chromium and Microsoft web services might collect extra
informaton when browsing using this program. See their privacy policies for details.

This program is not affiliated with Microsoft or Mojang.