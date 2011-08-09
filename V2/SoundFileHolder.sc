// wslib 2010

AbstractRichBuffer {
    var <numFrames, <numChannels, <sampleRate;

	var <>buffers; // holder for all buffers
    var <unit;

    *new{ |numFrames, numChannels = 1, sampleRate = 44100|
        ^super.newCopyArgs(numFrames, numChannels, sampleRate)
    }

    shallowCopy{
        ^this.class.new(numFrames, numChannels, sampleRate)
	}

    numFrames_ { |new|
		numFrames = new;
		this.changed( \numFrames, numFrames );
	}

	numChannels_ { |new|
		numChannels = new;
		this.changed( \numChannels, numChannels );
	}

	sampleRate_ { |new|
		sampleRate = new ? 44100;
		this.changed( \sampleRate, sampleRate );
	}

    //BUFFER MANAGEMENT
	addBuffer { |buf|
		buffers = buffers.add( buf );
		this.changed( \buffers, buffers );
	}

	removeBuffer { |buf|
		buffers.remove( buf );
		this.changed( \buffers, buffers );
	}

	currentBuffers { |server| // returns all buffers if server == nil
		if( server.notNil ) {
			^buffers.select({ |item| item.server == server });
		};
		^buffers;
	}

	currentBuffer { |server|
	    ^this.currentBuffers(server)[0]
	}


	freeAllBuffers { |server|
	    if( server.notNil ) {
		    this.currentBuffers( server ).do( this.freeBuffer(_) )
		}{
		    this.buffers.do( this.freeBuffer(_) )
		}
	}

	resetBuffers { |server|
		this.currentBuffers( server ).do({ |buf|
			this.removeBuffer( buf );
		});
	}

	prepare { |servers, action|
	    this.resetBuffers;
	    action = MultiActionFunc( action );
	    servers.do( this.makeBuffer(_, action.getAction) )
	}

	dispose {
       this.freeAllBuffers
	}

	disposeFor { |server|
        this.freeAllBuffers(server)
	}

	asControlInputFor { |server| ^this.currentBuffer(server) }

	unit_ { |aUnit|
	    if(unit.isNil) {
	        unit = aUnit
	    } {
	        "Warning: ".postln;
	        this.cs.postln;
	        "is already being used by".postln;
	        unit.postln;
	    }
	}
}

RichBuffer : AbstractRichBuffer {

    *new{ |numFrames, numChannels = 1, sampleRate = 44100|
        ^super.new(numFrames, numChannels, sampleRate)
    }

	makeBuffer { |server, action, bufnum|
	    var buf;
		buf = Buffer.alloc(server, numFrames, numChannels, nil, bufnum );
		OSCresponderNode( server.addr, '/done', { |time, resp, msg, addr|
			if( msg == [ '/done', '/b_alloc', buf.bufnum ] ) {
				resp.remove;
				action.value( buf );
			};
		}).add;
		buffers = buffers.add( buf );
		^buf;
	}

	freeBuffer { |buf, action|
		buf.checkFree( action );
		buffers.remove( buf );
	}

	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* [
		    numFrames, numChannels, sampleRate]
		<<")"
	}

    storeOn { arg stream;
		stream << this.class.name << "(" <<* [
		    numFrames, numChannels, sampleRate]
		<<")"
	}
}

AbstractSndFile : AbstractRichBuffer {
	
	// points to a Sndfile and holds its specs, similar to SoundFile
	// aditional parameters for Buffer loading and playback settings
	// fully MVC aware
	
	var <path;
	var <startFrame = 0, endFrame;  // for buffer loading
	var <rate = 1;
	var <fadeInTime = 0.1, <fadeOutTime = 0.1;
	var <loop = false, <loopedDuration;

	var <unit;
	
	*newBasic{ |path, numFrames, numChannels, sampleRate = 44100, startFrame = 0, endFrame, rate = 1,
	    fadeInTime = 0.1, fadeOutTime = 0.1,loop = false, loopedDuration |
		^super.new(numFrames, numChannels, sampleRate)
		    .initAbstractSndFile( path, startFrame, endFrame, rate,
		 fadeInTime, fadeOutTime, loop , loopedDuration );
	}

	shallowCopy{
        ^this.class.newBasic(path, numFrames, numChannels, sampleRate, startFrame, endFrame, rate,
	    fadeInTime, fadeOutTime,loop, loopedDuration)
	}

	initAbstractSndFile { |inPath, inStartFrame, inEndFrame, inRate,
		 inFadeInTime, inFadeOutTime, inLoop , inLoopedDuration|
		 path = inPath;
		 startFrame = inStartFrame;
		 endFrame = inEndFrame;
		 rate = inRate;
		 fadeInTime = inFadeInTime;
		 fadeOutTime = inFadeOutTime;
		 loop = inLoop;
		 loopedDuration = inLoopedDuration;
	}

	*buf{ ^BufSndFile }
	*disk{ ^DiskSndFile }
	*fromType{ |type|
	    ^switch(type)
	    {\buf}{BufSndFile}
	    {\disk}{DiskSndFile}
	}

	fromFile { |soundfile|
		if( this.prReadFromFile( soundfile ).not ) { 
			"%:initFromFile - could not open file '%'\n".postf( this.class, path.basename ) 
		}
	}
	
	prReadFromFile { |soundfile|
		var test = true;
		if( soundfile.isNil or: { soundfile.isOpen.not } ) {
			soundfile = soundfile ?? { SoundFile.new }; 
			test = soundfile.openRead( path.standardizePath );
			soundfile.close; // close if it wasn't open
		};
		if( test ) {	
			this.numFrames = soundfile.numFrames;
			this.numChannels = soundfile.numChannels;
			this.sampleRate = soundfile.sampleRate;
			^true;
		} { 
			^false 
		};
	}
	
	asSoundFile { // convert to normal soundfile
		^SoundFile( path )
			//.numFrames_( numFrames ? 0 )
			.instVarPut( \numFrames,  numFrames ? 0 )
			.numChannels_( numChannels ? 1 )
			.sampleRate_( sampleRate ? 44100 );
	} 
	
	asBufSndFile { ^this }
	asDiskSndFile { ^DiskSndFile.newCopyVars( this ); }
	
	// mvc aware setters
	
	path_ { |new, update = false|
		path = new ? path;
		this.changed( \path, path );
		if( update == true ) { this.prReadFromFile; };
	}

	startFrame_ { |new|
		startFrame = (new ? 0).max(0);
		this.changed( \startFrame, startFrame );
	}
	
	endFrame_ { |new|
		endFrame = new.min(numFrames);
		this.changed( \endFrame, endFrame );
	}

	rate_ { |new|
		rate = new ? 1;
		this.changed( \rate, rate );
	}
	
	loop_ { |new|
		loop = new ? false;
		this.changed( \loop, loop );
	}
	
	endFrame { if( numFrames.notNil ) { 
			^(endFrame ? numFrames) % (numFrames+1) 
		} { 
			^endFrame 
		};
	}
	
	// pseudo getter/setters
		 
	usedFrames { ^(this.endFrame ?? { startFrame - 1 }) - startFrame } // -1 if unknown or to end
	usedFrames_ { |frames = (-1)| // -1 means from startFrame to end 
		if( [-1, nil].includes(frames.asInt) ) { 
			this.endFrame = nil; 
		} { 
			this.endFrame = frames + startFrame 
		};
	}

	framesToSeconds { |frames = 0|  ^frames !? { (frames / (sampleRate ? 44100)) / rate } }
	secondsToFrames { |seconds = 0| ^seconds !? { seconds * rate * (sampleRate ? 44100) } }

	startSecond 	{ ^this.framesToSeconds( this.startFrame ); }
	endSecond 	{ ^this.framesToSeconds(this.endFrame); }
	duration 		{ ^this.framesToSeconds(this.usedFrames); } // negative if unknown
	fileDuration 	{ ^this.framesToSeconds(this.numFrames); }
	eventDuration { ^if(loop){loopedDuration}{this.duration} }
	
	startSecond_ { |startSecond = 0| this.startFrame = this.secondsToFrames( startSecond ); }
	endSecond_ { |endSecond = 0| this.endFrame = this.secondsToFrames( endFrame ); }
	duration_ { |duration| this.usedFrames = this.secondsToFrames( duration ) }
	fileDuration_ { |duration| this.numFrames = this.secondsToFrames( duration ); }
	
	// utilities
	
	plot { this.asSoundFile.plot; }
	
	checkDo { |action|
		var test = true;
		if( numFrames.isNil ) { 
			test = this.prReadFromFile; // get numFrames etc.
		};
		if( test ) { 
			^action.value 
		} {
			"%: file % not found".format( this.class, path.quote ).warn;
			^false;
		};
	}
	
	splice { |seconds|
		^this.checkDo({
			this.spliceFrames( this.secondsToFrames( seconds ) );
		});
	}
	
	spliceFrames { |frame = 0| // split into segments based on frame or frames relative to startFrame
		^this.checkDo({
			var positions, segments, usedFrames;
			usedFrames = this.usedFrames;
			positions = [ 0, usedFrames];
			frame.asCollection.do({ |item|
				if( positions.includes( item ).not && { item < usedFrames } ) {
					positions = positions.add( item );
				};
			});
			positions.sort;
			positions.doAdjacentPairs({ |a,b| segments = segments.add( [a,b] ); });
			if( segments.size > 1 ) {
				segments.collect({ |segment|
					this.copy
						.startFrame_( startFrame + segment[0] )
						.endFrame_( startFrame + segment[1] )
				});
			} { 
				[ this ];
			}
		});
	}

	makeUnit {
	    ^if( loop.not || loopedDuration.notNil ) {
	        ^MetaU(this.unitNamePrefix++"FilePlayer",[\numChannels,1,\loop,loop],
	         [\bufnum, this, \i_duration, this.eventDuration, \loopTime, this.duration,
	          \i_fadeInTime, fadeInTime,  \i_fadeOutTime, fadeOutTime, \rate, rate,])
	    } {
	        ^MetaU(this.unitNamePrefix++"FilePlayerLoopInf",[\numChannels,1,\loop,loop],
	        [\bufnum, this, \loopTime, this.duration,\i_fadeInTime, fadeInTime,
	        \i_fadeOutTime, fadeOutTime, \rate, rate,])

	    }
	}

	play{ |target|
	    var chain = UChain( this.copy.makeUnit.disposeOnFree_(true),\output);
	    chain.prepareAndStart(target);
	    ^chain
	}

    printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* [
		    path, numFrames, numChannels, sampleRate,
            startFrame, endFrame,
	        rate, fadeInTime, fadeOutTime,
            loop, loopedDuration
		]  <<")"
	}

    storeOn { arg stream;
		stream << this.class.name << "(" <<* [
		    path, numFrames, numChannels, sampleRate,
            startFrame, endFrame,
	        rate, fadeInTime, fadeOutTime,
            loop, loopedDuration
		]  <<")"
	}
	
}

BufSndFile : AbstractSndFile {

    var <useChannels;

	*new{ |path, startFrame = 0, endFrame, rate = 1, fadeInTime = 0.1, fadeOutTime = 0.1, loop = false, loopedDuration, useChannels | // path of existing file or SoundFile
		if( path.class == SoundFile ) {
			^this.newBasic( path.path, nil, nil, nil, startFrame, endFrame, rate, fadeInTime, fadeOutTime, loop, loopedDuration ).useChannels_(useChannels).fromFile( path );
		} {
			^this.newBasic( path, nil, nil, nil, startFrame, endFrame, rate, fadeInTime, fadeOutTime, loop, loopedDuration ).useChannels_(useChannels).fromFile;
		};
	}

    useChannels_ { |new|
        useChannels = new;
        this.changed( \useChannels, useChannels );
    }

    makeBuffer { |server, startOffset = 0, action, bufnum|
		var buf;
		if( useChannels.notNil ) {
			buf = Buffer.readChannel( server, path.standardizePath,
					startFrame + startOffset, this.usedFrames, useChannels, action, bufnum );
		} {
			buf = Buffer.read( server, path.standardizePath,
					startFrame + startOffset, this.usedFrames, action, bufnum );
		};
		buffers = buffers.add( buf );
		^buf;
	}

    freeBuffer { |buf, action|
		buf.checkFree( action );
		this.removeBuffer( buf );
	}

    unitNamePrefix{ ^"buffer" }

}

DiskSndFile : AbstractSndFile {
	
	var <>diskBufferSize = 32768;

	*new{ |path, startFrame = 0, endFrame, rate = 1, fadeInTime = 0.1, fadeOutTime = 0.1, loop = false , loopedDuration| // path of existing file or SoundFile
		if( path.class == SoundFile ) {
			^this.newBasic( path.path, nil, nil, nil, startFrame, endFrame, rate, fadeInTime, fadeOutTime, loop, loopedDuration ).fromFile( path );
		} {
			^this.newBasic( path, nil, nil, nil, startFrame, endFrame, rate, fadeInTime, fadeOutTime, loop, loopedDuration ).fromFile;
		};
	}
	
	asBufSndFile { ^BufSndFile.newCopyVars( this ); }
	asDiskSndFile { ^this }
	
	makeBuffer {  |server, startOffset = 0, action, bufnum| 
	    //endFrame not used
		var test = true;
		var buf;
		
		if( numChannels.isNil ) { 
			test = this.prReadFromFile; // get numchannels etc.
		};
		
		if( test ) {
			buf = Buffer.alloc(server, diskBufferSize, numChannels, { arg buffer;
				buffer.readMsg(path, startFrame + startOffset, diskBufferSize, 0, true, {|buf|
					["/b_query", buf.bufnum]
				});
			}).doOnInfo_(action).cache;
			buffers = buffers.add( buf );
			^buf;
		} {
			"DiskSndFile:prReadBuffer : file not found".warn;
		};
	}

    unitNamePrefix{ ^"disk" }
	
}

+ Object {
	*newCopyVars { |obj, exclude|  // assumes object with similar keys, a subclass etc
		var correspondingVars, new;
		exclude = exclude.asCollection;
		new = this.newCopyArgs(); // set nothing, init nothing
		this.instVarNames.select({ |item| 
			exclude.includes( item ).not && {
				obj.class.instVarNames.includes( item );
			};
		}).do({ |item|
			new.instVarPut( item, obj.instVarAt( item ).copy );
		});
		^new;		
	}
}