// wslib 2010

AbstractRichBuffer {
    var <numFrames, <numChannels, <sampleRate;

	var <>buffers; // holder for all buffers
    var <unit, <unitArgName;

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
	    ^this.currentBuffers(server).last;
	}
	
	freeAllBuffers { |server|
	    if( server.notNil ) {
		    this.currentBuffers( server ).do( this.freeBuffer(_) )
		}{
		    this.buffers.do( this.freeBuffer(_) )
		}
	}
	
	freeBufferFor { |server|
		if( server.notNil ) {
			this.freeBuffer( this.currentBuffers(server).first );
		};
	}

	resetBuffers { |server|
		this.currentBuffers( server ).do({ |buf|
			this.removeBuffer( buf );
		});
	}

	prepare { |servers, action|
	    //this.resetBuffers;
	    action = MultiActionFunc( action );
	    servers.do({ |server| this.makeBuffer(server, action: action.getAction) })
	}

	dispose {
       this.freeAllBuffers
	}

	disposeFor { |server|
        this.freeBufferFor(server)
	}

	asControlInputFor { |server| ^this.currentBuffer(server) }
	
	asUnitArg { |unit|
		this.unit = unit; ^this;
	}

	unit_ { |aUnit|
	    case { unit == aUnit } { 
		    // do nothing
		} {
		    unit.isNil 
		} {
	        unit = aUnit;
	        unitArgName = nil;
	    } {
	        "Warning:".postln;
	        this.postcs;
	        "\nis already being used by".postln;
	        unit.postln;
	    };
	}
	
	unitSet { // sets this object in the unit to enforce setting of the synths
		if( unit.notNil ) {	
			if( unitArgName.isNil ) {
				unitArgName = unit.findKeyForValue( this );
			};
			if( unitArgName.notNil ) {
				unit.set( unitArgName, this );
			};
		};
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
	var <loop = false;
		
	*newBasic{ |path, numFrames, numChannels, sampleRate = 44100, startFrame = 0, endFrame, 
		rate = 1, loop = false |
		^super.new(numFrames, numChannels, sampleRate)
		    .initAbstractSndFile( path, startFrame, endFrame, rate, loop );
	}

	shallowCopy{
        ^this.class.newBasic(path, numFrames, numChannels, sampleRate, startFrame, endFrame, rate, 
        		loop);
	}

	initAbstractSndFile { |inPath, inStartFrame, inEndFrame, inRate, inLoop|
		 path = inPath;
		 startFrame = inStartFrame;
		 endFrame = inEndFrame;
		 rate = inRate;
		 loop = inLoop;
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
			test = soundfile.openRead( path.asPathFromServer.standardizePath );
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
		^SoundFile( path.asPathFromServer )
			//.numFrames_( numFrames ? 0 )
			.instVarPut( \numFrames,  numFrames ? 0 )
			.numChannels_( numChannels ? 1 )
			.sampleRate_( sampleRate ? 44100 );
	} 
	
	// mvc aware setters
	
	path_ { |new, update = false|
		path = new ? path;
		this.changed( \path, path );
		if( update == true ) { this.prReadFromFile; };
	}
	
	basename { ^path !? { path.basename } }
	basename_ { |basename| 
		if( path.isNil ) {
			this.path = basename;
		} {
			this.path = path.dirname +/+ basename;
		};
	}
	
	dirname {  ^path !? { path.dirname } }
	dirname_ { |dirname| 
		if( path.isNil ) {
			this.path = dirname;
		} {
			this.path = dirname +/+ path.basename;
		};
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
		this.unitSet;
	}
	
	loop_ { |new|
		loop = new ? false;
		this.changed( \loop, loop );
		this.unitSet;
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
	eventDuration { ^if(loop){ inf }{ this.duration } }
	
	startSecond_ { |startSecond = 0| this.startFrame = this.secondsToFrames( startSecond ); }
	endSecond_ { |endSecond = 0| this.endFrame = this.secondsToFrames( endSecond ); }
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
	
	makeUnit { // to do: remove fadeInTime / fadeOutTime : they are now covered elsewhere
		^MetaU( this.unitNamePrefix++"Player", 
			[\numChannels, numChannels], 
			[\soundFile, this ] 
		);
	}

	makeUChain {
		var chain;
		this.unit = nil; // forget old unit
		chain = UChain( this.makeUnit, \output);
		if( loop.not ) { chain.setDur( this.eventDuration ) };
		^chain;
	}

	play{ |target|
	    var chain = this.makeUChain;
	    chain.prepareAndStart(target ? Server.default);
	    ^chain
	}

    printOn { arg stream;
		stream << this.class.name << "(" <<* [
		    	path, numFrames, numChannels, sampleRate,
            	startFrame, endFrame, rate, loop
		]  <<")"
	}

    storeOn { arg stream;
		stream << this.class.name << ".newBasic(" <<* [ // use newBasic to prevent file reading
		    path.quote, numFrames, numChannels, sampleRate,
             startFrame, endFrame, rate, loop
		]  <<")"
	}
	
}

BufSndFile : AbstractSndFile {

    var <useChannels, <>useStartPosForBuf = false;

	*new{ |path, startFrame = 0, endFrame, rate = 1, loop = false, useChannels | 
		// path of existing file or SoundFile
		if( path.class == SoundFile ) {
			^this.newBasic( path.path, nil, nil, nil, startFrame, endFrame, rate, loop )
				.useChannels_(useChannels).fromFile( path );
		} {
			^this.newBasic( path, nil, nil, nil, startFrame, endFrame, rate, loop )
				.useChannels_(useChannels).fromFile;
		};
	}
	
	asBufSndFile { ^this }
	asDiskSndFile { ^DiskSndFile.newCopyVars( this ); }

    useChannels_ { |new|
        useChannels = new;
        this.changed( \useChannels, useChannels );
    }
    
    asControlInputFor { |server, startPos = 0| 
	    ^[ this.currentBuffer(server, startPos), rate, loop.binaryValue ] 
	   }
	   
	// not used by Unit:
	asControlInput { 
		^this.asControlInputFor( Server.default ); // assume default server
	}
	
	asOSCArgEmbeddedArray { | array| ^this.asControlInput.asOSCArgEmbeddedArray(array) }

    makeBuffer { |server, startPos = 0, action, bufnum|
		var buf, addStartFrame = 0, localUsedFrames;
		
		localUsedFrames = this.usedFrames;
		
		if( useStartPosForBuf && { startPos != 0 } ) { 
			addStartFrame = this.secondsToFrames( startPos );
			if( localUsedFrames != -1 ) { 
				localUsedFrames - addStartFrame;
			};
		};
		
		if( useChannels.notNil ) {
			buf = Buffer.readChannel( server, path.standardizePath,
					startFrame + addStartFrame, localUsedFrames, useChannels, action, bufnum );
		} {
			buf = Buffer.read( server, path.standardizePath,
					startFrame + addStartFrame, localUsedFrames, action, bufnum );
		};
		buffers = buffers.add( buf );
		^buf;
	}

    freeBuffer { |buf, action|
		buf.checkFree( action );
		this.removeBuffer( buf );
	}

    unitNamePrefix{ ^"buffer" }
    
    u_waitTime { ^5 }
    
    storeOn { arg stream;
		stream << this.class.name << ".newBasic(" <<* [ // use newBasic to prevent file reading
		    path.quote, numFrames, numChannels, sampleRate,
             startFrame, endFrame, rate, loop
		]  << ")" << if( useChannels.notNil ) { ".useChannels(%)".format( useChannels ) } { "" };
	}

}

DiskSndFile : AbstractSndFile {
	
	var <>diskBufferSize = 32768;

	*new{ |path, startFrame = 0, endFrame, rate = 1, loop = false| 
		// path of existing file or SoundFile
		if( path.class == SoundFile ) {
			^this.newBasic( path.path, nil, nil, nil, startFrame, endFrame, rate, loop )
				.fromFile( path );
		} {
			^this.newBasic( path, nil, nil, nil, startFrame, endFrame, rate, loop).fromFile;
		};
	}
	
	asBufSndFile { ^BufSndFile.newCopyVars( this ); }
	asDiskSndFile { ^this }
	
	asControlInputFor { |server, startPos = 0| 
		^[ this.currentBuffer(server, startPos), rate, loop.binaryValue ] 
	}
	
	makeBuffer {  |server, startPos = 0, action, bufnum|  // startOffset in seconds
	    //endFrame not used
		var test = true;
		var buf, addStartFrame = 0;
		
		if( numChannels.isNil ) { 
			test = this.prReadFromFile; // get numchannels etc.
		};
		
		if( test ) {
			if( startPos != 0 ) { addStartFrame = this.secondsToFrames( startPos ) };
			buf = Buffer.alloc(server, diskBufferSize, numChannels, { arg buffer;
				buffer.readMsg(path, startFrame + addStartFrame, 
					diskBufferSize, 0, true, {|buf|
						["/b_query", buf.bufnum]
					}
				);
			}).doOnInfo_(action).cache;
			buffers = buffers.add( buf );
			^buf;
		} {
			"DiskSndFile:prReadBuffer : file not found".warn;
		};
	}
	
	 freeBuffer { |buf, action|
		buf.checkCloseFree( action );
		this.removeBuffer( buf );
	}

    unitNamePrefix{ ^"disk" }
    
    u_waitTime { ^1 }
	
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

+ String {
	asUnitArg { |unit|
		^BufSndFile( this ).asUnitArg( unit );
	}
	
	asPathFromServer { 
		
		// a relative path on the server is not the same as on the lang for standalone apps.
		// this method always returns a path that points to same file on both server and lang

		var scdir;
		if( [ $/, $~ ].includes( this[0] ).not ) {
			scdir = String.scDir;
			if( scdir != File.getcwd ) { // <- means this is a standalone app
				^scdir +/+ this;	
			} {
				^this
			};
		} {
			^this;
		}	
	}
}