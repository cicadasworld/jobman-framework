@echo off

set gen=%GT_OUT_ROOT%\gtserver\winx64_vc10\bin\debug\plxn_fzdl2cppd.exe

%gen% /nologo /input:jobpdo.fzdl /target:basetype  /cop:../generated /ofn:jobpdo.fz

%gen% /nologo /input:jobsvc.fzdl /target:serviceproxy /cop:../generated /ofn:jobsvc.fz

del ..\generated\plxn_jobmansvcproxy.def
del ..\generated\scf_activator.h
del ..\generated\scf_activator.cpp
del ..\generated\scf_dllmain.cpp

