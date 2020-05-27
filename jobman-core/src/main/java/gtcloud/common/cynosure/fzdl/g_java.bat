@echo off

set gen=%GT_OUT_ROOT%\gtserver\winx64_vc10\bin\debug\plxn_fzdl2javad.exe

set GTCLOUD_ROOT=L:\gtcloud

%gen% /nologo /input:P:\GTServer\platon\src\cynosure\pdo\autogen.fzdl /target:basetype /op:%GTCLOUD_ROOT%\gtcloud-common-extensions\src\main\java
