# ProtectOwnBlocks
All block that player placed are protected by that player.

1.13 ~ 1.19.4 spigot(or paper) servers are fully supported for now.
(Development focused to 1.19.4 for now.)

1.8 ~ 1.12.2 spigot server works but there is problem with "onBlockBreak" event. (when you set "restrict_interactions: true" on config.yml) 
I think I won't fix this. But if you want to fix it, Pull Request is welcome.

1.7.10- servers does not work. If you want to support, Pull Request is welcome.

Command : 
 - /pob on : After you type this command and then place blocks, those blocks will protected and will not breaked from other player.(This is enabled by default)
 - /pob off : After you type this command and place blocks, blocks will not be protected.(Blocks that placed before this command typed, those blocks will still protected.)

Feature
 - All block that player placed are protected by that player.(Save with uuid)
 - You can disable specific world from protecting blocks when placing
 - Many exploit are blocked(Tnt exploit, water/lava exploit, Piston Exploit etc.)


Reload is not supported for now(Even with a plugin that only reloads each plugin), there is a plan for reload only config.yml
So, do not try reload this plugin.
