@echo off

set gen=%GT_OUT_ROOT%\gtserver\winx64_vc10\bin\debug\plxn_fzdl2javad.exe

%gen% /nologo /input:jobpdo.fzdl   /target:basetype /op:L:\gtcloud\gtcloud-jobman-core\src\main\java
%gen% /nologo /input:schedpdo.fzdl /target:basetype /op:L:\gtcloud\gtcloud-jobman-core\src\main\java
%gen% /nologo /input:dumpstate.fzdl /target:basetype /op:L:\gtcloud\gtcloud-jobman-core\src\main\java
