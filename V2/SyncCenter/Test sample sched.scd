



(
WFS.startup;
WFS.debugMode = true;
~soptions = ServerOptions()
.memSize_(8192*12)
.numAudioBusChannels_(150)
.numOutputBusChannels_(30)
.numInputBusChannels_(30)
.numWireBufs_(2048)
.sampleRate_( 44100 )
.blockAllocClass_( ContiguousBlockAllocator )
.device_("JackRouter");

~slaves = WFSServers.default.multiServers.collect(_.servers).flat;
~master = WFSServers.default.masterServer;

SyncCenter.inBus = 0;
SyncCenter.outBus = 0;
SyncCenter.master_(~master);

{
	~master.boot;
	2.wait;
	~slaves.do{ |sv|
		sv.options_(~soptions);
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
	
	SyncCenter.sendDefs;
	

}.fork;

)		

issues:

