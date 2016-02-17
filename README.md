# Coney

usage: Coney
 -ch <arg>   channel name
 -d          debug, default false
 -f <arg>    bots INI file
 -h <arg>    irc host, default irc.freenode.net
 -n <arg>    nick


INI files example:
*********************

[bot_nick1]
    type = php
    takeAll = true
    path = /home/bots/test1.php

[bot_foo_nick2]
    type = bsh
    path = bots/test2.bsh
