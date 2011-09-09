//supernova version

(
Server.program = "cd % && exec ./supernova -U plugins".format(String.scDir.quote);

~soptions = ServerOptions()
.memSize_(8192*12)
.numAudioBusChannels_(150)
.numOutputBusChannels_(2)
.numInputBusChannels_(30)
.numWireBufs_(2048)
.sampleRate_( 44100 )
.blockAllocClass_( ContiguousBlockAllocator );
//.device_("JackRouter");

~slaves = [
        Server(\slave1,NetAddr("localhost", 57456),~soptions),
        Server(\slave2,NetAddr("localhost", 57458),~soptions)
];

~master = Server(\master,NetAddr("localhost", 57368),~soptions);

(~slaves++[~master]).do(_.makeWindow);

SyncCenter.inBus = 0;
SyncCenter.outBus = 0;
SyncCenter.addAll(~slaves);
SyncCenter.master_(~master);


{
        ~master.boot;
        2.wait;
        ~slaves[0].boot;
        2.wait;
        ~slaves[1].boot;
        
        while({ ~master.serverBooting || ~slaves[0].serverBooting || ~slaves[1].serverBooting }) {
                1.wait;
        };
        "servers booted".postln;
        
      "jack_connect supernova:output_1 supernova-01:input_1 && jack_connect supernova:output_1 supernova-02:input_1".runInTerminal;
        
        SyncCenter.sendDefs;
        

}.fork;

)
//turns on more posting of info
SyncCenter.verbose = true
//do the remote sync between all servers
SyncCenter.remoteSync;
//after multiple remoteSync the values outputed by this should always be hte same
SyncCenter.serverCounts.collect{ |x| x.value - SyncCenter.current.localCount }


//canceling sine-waves
//if the sync is working then these two sinewaves will cancel.
~slaves.do({ arg server;
        SynthDef("sine", { arg out=0, freq=110.0, amp=0.1;
                OffsetOut.ar(out, SinOsc.ar(freq, 0, amp).dup);
        }).send(server);
});


OSCBundle.new.add([ "/s_new", "sine", 2000, 1, 1, 'freq', 1000, 'amp', 0.25 ]).syncedSend(~slaves[0],1);
OSCBundle.new.add([ "/s_new", "sine", 2000, 1, 1, 'freq', 1000, 'amp', -0.25 ]).syncedSend(~slaves[1],2);
//kill slave1 or slave2 and you will hear sine wave again.

//this number should be the same between syncs
SyncCenter.serverCounts.collect{ |x| x.value - SyncCenter.current.localCount }
