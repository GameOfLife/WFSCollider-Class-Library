// example
SynthDef.synthDefDir.dirname+/+"/sounds/a11wlk01-44_1.aiff";

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

//play
(
y = x.makeWFSUnit;
z = WFSChain(y,\output);
fork{
y.def.loadSynthDef;
//when synthdefs should synthdefs be sent ?
z.prepare(s);
s.sync;
z.start;
}
)
//free synths and buffers
z.dispose;




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
	}).setSpec(\i_fadeTime, \unipolar.asSpec)
	.setSpec(\i_fadeOutTime, \unipolar.asSpec)
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


WFSUnit.filenameSymbol.asString.dirname.d
//Generate Defs
(
~base = WFSUnit.filenameSymbol.asString.dirname +/+ "WFSUnitDefs";
10.collect{ |i| 
	
	
	~names = [
		("bufferFilePlayer"++(i+1)).asSymbol,
		("diskFilePlayer"++(i+1)).asSymbol,
		("bufferFilePlayerLoop"++(i+1)).asSymbol,
		("bufferFilePlayerInfLoop"++(i+1)).asSymbol,
		("diskFilePlayerLoop"++(i+1)).asSymbol,
		("diskFilePlayerInfLoop"++(i+1)).asSymbol
	];	
	
	~defs = [{ |i| "WFSUnitDef( \\bufferFilePlayer"++(i+1)++", { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = PlayBuf.ar("++( i+1)++", bufnum, speed ); 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		WFSUnitOut.ar( 0, source * env);
	})" },

	{ |i| "WFSUnitDef( \\diskFilePlayer"++(i+1)++", { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = VDiskIn.ar("++( i+1)++", bufnum, speed );
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		WFSUnitOut.ar( 0, source * env );
	})" },

	{ |i| "WFSUnitDef( \\bufferFilePlayerLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, i_eventDuration, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		WFSUnitOut.ar( 0, env * PlayBufCF.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	})" },
	
	{ |i| "WFSUnitDef( \\bufferFilePlayerInfLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1|
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14); 
		WFSUnitOut.ar( 0, env * PlayBufCF.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	})" },
	{ |i| "WFSUnitDef( \\diskFilePlayerLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_eventDuration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		WFSUnitOut.ar( 0, env * VDiskIn.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	})" },
	{ |i| "WFSUnitDef( \\diskFilePlayerInfLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_duration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1| 
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14);
		WFSUnitOut.ar( 0, env * VDiskIn.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	})" }
	];
	~names.do{ |name,j|
		f = File.open(~base++name++".scd","w");
		f.write(~defs[j].value(i));
		f.close
	}
};
)
WFSUnitDef( \output, { |bus = 0|
	Out.ar( bus, WFSUnitIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] ).loadSynthDef;
)

WFSUnit(\output)