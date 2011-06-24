// example

//Defs
(
~uni = \unipolar.asSpec;
10.do{ |i| 
	
	WFSUnitDef( ("bufferFilePlayer"++(i+1)).asSymbol, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = PlayBuf.ar( i+1, bufnum, speed ); 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		WFSUnitOut.ar( 0, source * env);
	}).setSpec(\i_fadeTime, ~uni)
	.setSpec(\i_fadeOutTime, ~uni)
	.loadSynthDef;


	WFSUnitDef( ("diskFilePlayer"++(i+1)).asSymbol, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = VDiskIn.ar( i+1, bufnum, speed );
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		WFSUnitOut.ar( 0, source * env );
	}).loadSynthDef;

	WFSUnitDef( ("bufferFilePlayerLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, i_eventDuration, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		WFSUnitOut.ar( 0, env * PlayBufCF.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	}).loadSynthDef;
	
	WFSUnitDef( ("bufferFilePlayerInfLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1|
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14); 
		WFSUnitOut.ar( 0, env * PlayBufCF.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	}).loadSynthDef;

	WFSUnitDef( ("diskFilePlayerLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_eventDuration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		WFSUnitOut.ar( 0, env * VDiskIn.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	}).loadSynthDef;
	
	WFSUnitDef( ("diskFilePlayerInfLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_duration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1| 
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14);
		WFSUnitOut.ar( 0, env * VDiskIn.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	}).loadSynthDef;
};

WFSUnitDef( \output, { |bus = 0|
	Out.ar( bus, WFSUnitIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] ).loadSynthDef;
)


~path = "/Users/miguelnegrao/Development/SuperCollider/scgit/supercollider/build/SuperCollider/sounds/a11wlk01-44_1.aiff";

//buffer
//not looped
x = BufSoundFile(~path, endFrame:44100*2.1, useChannels:[0]);
//looped 
x = BufSoundFile(~path, startFrame: 44100*0.2, endFrame:44100*2.3, rate:2.0,
 loop: true, loopedDuration:7, fadeInTime:4, fadeOutTime:2);
// infinite loop. stop it with y.set(\gate,0)
x = BufSoundFile(~path, startFrame: 44100*1.0, endFrame:44100*1.5, rate:1.0,
	loop: true, fadeInTime:4, fadeOutTime:2);

//disk
x = DiskSoundFile(~path, startFrame: 44100*0.1, endFrame:44100*2, fadeInTime:0.2, fadeOutTime:0.2);
//looped
//note DiskSoundFile when looping will ignore endFrame and startFrame on the second loop. 
x = DiskSoundFile(~path, startFrame: 44100*2, rate:1.0,
 loop: true, loopedDuration:7, fadeInTime:0.1, fadeOutTime:2);
// infinite loop. stop it with y.set(\gate,0)
x = DiskSoundFile(~path, startFrame: 44100*1.0, rate:1.0,
	loop: true, fadeInTime:4, fadeOutTime:2);
x.dump


(
y = x.makeWFSUnit;
z = WFSChain(y,\output);
z.prepareAndStart(s);
)
//free synths and buffers
z.dispose;
y.args.inspect
//freAction not being called sometimes...
y.def.argSpecs.inspect
y.def.getSpec(\bufnum)
x.dump
y.synths
y.verbose

y.gui
y.set(\rate,0.1)

x = WFSUnitDef( \test, { |bla| } );

x.specs[0] // -> ControlSpec(0, 1, 'linear', 0, 0.0, "")
0.nextPowerOfTwo
s.queryAllNodes(true)

x.dump
x.inspect
y.getArg(\loopTime)
y.setArg(\loopTime,3)
y.synths[0].trace

y
y.du
y.def.dump
y.def.argSpecs[8].spec.dump

x = WFSUnitDef( ("bufferFilePlayerLoop1").asSymbol, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, i_startOffset = 0, i_eventDuration, loopTime, trig| 
		Line.kr(0,1, i_eventDuration , doneAction:14);
		WFSUnitOut.ar( 0, PlayBufCF.ar( 1, bufnum, speed, Impulse.kr(loopTime.reciprocal), loop:1) );
	})
x.synthDef.allControlNames.inspect

WFSUnitDef.fromName(\bufferFilePlayerLoop1).dump

WFSUnitDef.fromName(\diskFilePlayerLoop1).argSpecs.inspect

r = y.getArg(\bufnum)
r.duration
y.getArg(\i_duration)
y.getArg(\bufnum).duration

z.start


y.synths[0].trace


y.getArg(\bufnum).buffers





(
SynthDef(\env_help, { | out, gate = 0 |
    var z;
    z = EnvGen.kr(Env.new([0,1,0],[2,2],releaseNode:1 ), gate, doneAction:14) * SinOsc.ar(440, 0, 0.1);
    Out.ar(out, z)
}).add;
)

a = Synth(\env_help);


// turn on
a.set(\gate, 1);

// turn off
a.set(\gate, 0);



























// verbose
//Buffer
(
a = BufSoundFile("/Users/miguelnegrao/Desktop/test123.aiff", endFrame:44100*10, useChannels:[0]);
a.makeBuffer;
)
a.makeWFSUnit(\looped)

a.buffers
(
//y = WFSUnit(\bufferFilePlayer1, [\bufnum, a, \i_duration, a.duration, \i_fadeInTime, 0.1, \i_fadeOutTime, 0.1 ]);
y = a.makeWFSUnit(\looped);
z = WFSChain(y,\output);
z.start
)
z.stop
y.synths

//Disk
(
b = DiskSoundFile("/Users/miguelnegrao/Desktop/test123.aiff");
b.makeBuffer
)
b.numChannels
(
//y = WFSUnit(\diskFilePlayer2, [\bufnum, b, \i_duration, b.duration ]);
y = b.makeWFSUnit;
z = WFSChain(y,\output);
z.start
)

z.stop
y.start
y.synths

//condensed

y.getArg(\bufnum).buffers

z = WFSChain(y,\output)
z.start
z.stop
y.gui
y.start
y.stop
y.free
y.synths[0].trace


x = WFSUnitDef( \filePlayerLoop, { |buf, numChannels, speed, loop| 
 var diskin;
	diskin = VDiskIn.ar( numChannels, buf, speed, loop ); 
	if( loop.booleanValue.not ) {
		Line.kr(0,1, this.duration - (startOffset / sampleRate), doneAction:14);
	};
	Out.ar( out, diskin * mul );
})


(
x = WFSUnitDef( \sine, { |freq = 440, amp = 0.1| 
	Out.ar( 0, SinOsc.ar( freq, 0, amp ) ) 
} ).loadSynthDef;
)

y = WFSUnit(\sine)
y.start
y.synths
y.stop
f =  { \hello.kr }
f.play

(
x = WFSUnitDef( \sine, { |freq = 440, amp = 0.1| 
	Out.ar( 0, SinOsc.ar( freq, 0, amp ) ) 
} ).loadSynthDef;
)



y = WFSUnit( \sine, [ \freq, 880 ] );
y.start
y.setArg(\freq,200)
y.synths
y.gui;

y.def.loadSynthDef;

y.start;
y.stop;
(
Server.local.makeWindow;
s = Server.local;
Server.default = Server.local;
)

(
// a styled gui in user-defined window
w = Window( y.defName, Rect( 300,25,200,200 ) ).front;
w.addFlowLayout;
RoundView.useWithSkin( ( 
	labelWidth: 40, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.25)
), { 
	SmoothButton( w, 16@16 )
		.label_( ['power', 'power'] )
		.hiliteColor_( Color.green.alpha_(0.5) )
		.action_( [ { y.start }, { y.stop } ] )
		.value_( (y.synths.size > 0).binaryValue );
	y.gui( w );
});
)

WFSUnitIn.ar(0)
WFSUnitIn.getControlName( 'o', 'ar', 0)
~bc = BufferCenter(s)
~bc.inspect
~bc.read("/Users/miguelnegrao/Desktop/test123.aiff")

a = DiskSoundFile.fromFile("/Users/miguelnegrao/Desktop/test123.aiff")
a.makeBuffer
a.buffers
a.plot
a.inspect
a.path
~bc.dict