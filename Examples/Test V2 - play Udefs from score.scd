
(
WFS.startup


Udef.loadAllFromDefaultDirectory.do(_.loadSynthDef(s))


(
a = WFSScore.current.events[0].wfsSynth.getUChain;
a.units.postln;
a.prepareAndStart(s);
)
a.units


a.dispose