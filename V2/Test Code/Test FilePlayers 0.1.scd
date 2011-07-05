//run this first
Udef.loadAllFromDefaultDirectory.do(_.loadSynthDef(s))

// example
~path = SynthDef.synthDefDir.dirname+/+"/sounds/a11wlk01-44_1.aiff";

//buffer
//not looped
x = BufSndFile(~path, endFrame:44100*2.1, useChannels:[0]);
//looped 
x = BufSndFile(~path, startFrame: 44100*0.2, endFrame:44100*2.3, rate:2.0,
 loop: true, loopedDuration:7, fadeInTime:4, fadeOutTime:2);
// infinite loop. stop it with y.set(\gate,0)
x = BufSndFile(~path, startFrame: 44100*1.0, endFrame:44100*1.5, rate:1.0,
	loop: true, fadeInTime:4, fadeOutTime:2);

//disk
x = DiskSndFile(~path, startFrame: 44100*0.1, endFrame:44100*2, fadeInTime:0.2, fadeOutTime:0.2);
//looped
//note DiskSndFile when looping will ignore endFrame and startFrame on the second loop. 
x = DiskSndFile(~path, startFrame: 44100*2, rate:1.0,
 loop: true, loopedDuration:7, fadeInTime:0.1, fadeOutTime:2);
// infinite loop. stop it with y.set(\gate,0)
x = DiskSndFile(~path, startFrame: 44100*1.0, rate:1.0,
	loop: true, fadeInTime:4, fadeOutTime:2);

//play
(
y = x.makeUnit;
z = UChain(y,\output);
z.prepareAndStart(s);
)

//free synths and buffers
z.dispose;

//play and automatically dispose buffers on free
(
y = x.makeUnit;
y.disposeOnFree = true;
z = UChain(y,\output);
z.prepareAndStart(s)
)

//this should be empty
y.get(\bufnum).buffers

y.set(\gate,0)

// this will create Chain, allocate buffer, play, and when finishe, free the buffer.
y = x.play(s)

//RichBuffer
(
Udef(\testBuffer,{ |buf|
	Out.ar(0,BufAllpassN.ar(buf, Decay.ar(Dust.ar(1,0.5), 0.2, WhiteNoise.ar), 0.2, 3) )
});
x = U(\testBuffer,[\buf,RichBuffer(44100*1)]);
x.prepareAndStart(s)
)
