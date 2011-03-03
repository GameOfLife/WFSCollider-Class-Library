(
WFS.startup;
WFS.debugMode = true;

~slaves = WFSServers.default.multiServers.collect(_.servers).flat;
~master = WFSServers.default.masterServer;

SyncCenter.inBus = 0;
SyncCenter.outBus = 0;

{	2.wait;
	~slaves.do{ |sv|
		sv.boot;
		2.wait;
	};
	
	while({ (~slaves++[~master]).collect(_.serverBooting).reduce('and')}) {
		1.wait;
	};
	"servers booted".postln;
	
	(2.collect{ |i|
		"jack_disconnect  system:capture_"++(i+1)++" scsynth:in"++(i+1)++" && "++
		"jack_disconnect  system:capture_"++(i+1)++" scsynth-01:in"++(i+1)++" && "++
		"jack_disconnect  system:capture_"++(i+1)++" scsynth-02:in"++(i+1)++" && "
	}.reduce('++') ++ "jack_connect scsynth:out1 scsynth-01:in1 && jack_connect scsynth:out1 scsynth-02:in1").runInTerminal;
		

}.fork;

)		

Server.killAll

SyncCenterGui.new

SyncCenter.serverCounts.pairsDo{ |a,b| [a,b].postln }
SyncCenter.servers.as(Array)


fork{ (SyncCenter.master.options.blockSize*SyncCenter.master.blockCount).postln; };
fork{ SyncCenter.getSchedulingSampleCountS(0,SyncCenter.master).postln };
SyncCenter.serverCounts.at(SyncCenter.master).value

SyncCenter.master