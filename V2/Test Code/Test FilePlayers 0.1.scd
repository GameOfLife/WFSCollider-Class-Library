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
z.prepareAndStart(s)

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
y.getArg(\bufnum).buffers







//Defs
(
~uni = \unipolar.asSpec;
10.do{ |i| 
	
	Udef( ("bufferFilePlayer"++(i+1)).asSymbol, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = PlayBuf.ar( i+1, bufnum, speed ); 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		UOut.ar( 0, source * env);
	}).setSpec(\i_fadeTime, \unipolar.asSpec)
	.setSpec(\i_fadeOutTime, \unipolar.asSpec)
	.loadSynthDef;


	Udef( ("diskFilePlayer"++(i+1)).asSymbol, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = VDiskIn.ar( i+1, bufnum, speed );
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		UOut.ar( 0, source * env );
	}).loadSynthDef;

	Udef( ("bufferFilePlayerLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, i_eventDuration, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		UOut.ar( 0, env * PlayBufCF.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	}).loadSynthDef;
	
	Udef( ("bufferFilePlayerInfLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1|
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14); 
		UOut.ar( 0, env * PlayBufCF.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	}).loadSynthDef;

	Udef( ("diskFilePlayerLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_eventDuration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		UOut.ar( 0, env * VDiskIn.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	}).loadSynthDef;
	
	Udef( ("diskFilePlayerInfLoop"++(i+1)).asSymbol, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_duration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1| 
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14);
		UOut.ar( 0, env * VDiskIn.ar( i+1, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	}).loadSynthDef;
};

Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] ).loadSynthDef;
)


//Generate Defs
(
~base = U.filenameSymbol.asString.dirname.dirname +/+ "UnitDefs/";
10.collect{ |i| 
	
	
	~names = [
		("bufferFilePlayer"++(i+1)).asSymbol,
		("diskFilePlayer"++(i+1)).asSymbol,
		("bufferFilePlayerLoop"++(i+1)).asSymbol,
		("bufferFilePlayerInfLoop"++(i+1)).asSymbol,
		("diskFilePlayerLoop"++(i+1)).asSymbol,
		("diskFilePlayerInfLoop"++(i+1)).asSymbol
	];	
	
	~defs = [{ |i| "Udef( \\bufferFilePlayer"++(i+1)++", { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		UOut.ar( 0, level * env * PlayBuf.ar("++( i+1)++", bufnum, speed ));
	})" },

	{ |i| "Udef( \\diskFilePlayer"++(i+1)++", { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		UOut.ar( 0, level * env * level * VDiskIn.ar("++( i+1)++", bufnum, speed ));
	})" },

	{ |i| "Udef( \\bufferFilePlayerLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, i_eventDuration, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		UOut.ar( 0, level * env * PlayBufCF.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	})" },
	
	{ |i| "Udef( \\bufferFilePlayerInfLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1|
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14); 
		UOut.ar( 0, level * env * PlayBufCF.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	})" },
	{ |i| "Udef( \\diskFilePlayerLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_eventDuration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		UOut.ar( 0, level * env * VDiskIn.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	})" },
	{ |i| "Udef( \\diskFilePlayerInfLoop"++(i+1)++", { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_duration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1| 
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14);
		UOut.ar( 0, level * env * VDiskIn.ar("++( i+1)++", bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	})" }
	];
	~names.do{ |name,j|
		f = File.open(~base++name++".scd","w");
		f.write(~defs[j].value(i));
		f.close
	}
};
)
Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] ).loadSynthDef;
)

U(\output)