@echo off

set gen=%GT_OUT_ROOT%\gtserver\winx64_vc14\bin\debug\plxn_fzdl2javad.exe

set GTCLOUD_ROOT=L:\gtcloud-20190715-refact

%gen% /nologo /input:mission.fzdl  ^
      /target:basetype /op:%GTCLOUD_ROOT%\gtcloud-jobman-core\src\main\java
