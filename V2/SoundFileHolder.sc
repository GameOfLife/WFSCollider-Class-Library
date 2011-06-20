// wslib 2010

AbstractSoundFile {
	
	// points to a soundfile and holds its specs, similar to SoundFile
	// aditional parameters for Buffer loading and playback settings
	// fully MVC aware
	
	var <path, <numFrames, <numChannels = 1, <sampleRate = 44100;
	var <startFrame = 0, endFrame;  // for buffer loading
	var <rate = 1;
	var <fadeInTime = 0.1, <fadeOutTime = 0.1;
	var <loop = false, <loopedDuration;
	
	var <>synths; // holder for the instant playback synths
	var <>buffers; // holder for all buffers
	
	*newBasic{ |path, numFrames, numChannels, sampleRate = 44100, startFrame = 0, endFrame, rate = 1,
	    fadeInTime = 0.1, fadeOutTime = 0.1,loop = false, loopedDuration |
		^super.newCopyArgs( path, numFrames, numChannels, sampleRate, startFrame, endFrame, rate,
		 fadeInTime, fadeOutTime, loop , loopedDuration );
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
	
	asBufSoundFile { ^this }
	asDiskSoundFile { ^DiskSoundFile.newCopyVars( this ); }
	
	// mvc aware setters
	
	path_ { |new, update = false|
		path = new ? path;
		this.changed( \path, path );
		if( update == true ) { this.prReadFromFile; };
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
	
	startFrame_ { |new|
		startFrame = (new ? 0).min(0);
		this.changed( \startFrame, startFrame );
	}
	
	endFrame_ { |new|
		endFrame = new.max(numFrames);
		this.changed( \endFrame, endFrame );
	}

	rate_ { |new|
		rate = new ? 1;
		this.changed( \rate, rate );
		synths.do( _.set( \rate, rate ) );
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
	
	framesToSeconds { |frames = 0|  ^frames !? { (frames / sampleRate) / rate } }
	secondsToFrames { |seconds = 0| ^seconds !? { seconds * rate * sampleRate } }
	
	startSecond 	{ ^this.framesToSeconds( this.startFrame ); }
	endSecond 	{ ^this.framesToSeconds(this.endFrame); }
	duration 		{ ^this.framesToSeconds(this.usedFrames); } // negative if unknown
	fileDuration 	{ ^this.framesToSeconds(this.numFrames); }
	
	startSecond_ { |startSecond = 0| this.startFrame = this.secondsToFrames( startSecond ); }
	endSecond_ { |endSecond = 0| this.endFrame = this.secondsToFrames( endFrame ); }
	duration_ { |duration| this.usedFrames = this.secondsToFrames( duration ) }
	fileDuration_ { |duration| this.numFrames = this.secondsToFrames( duration ); }
	
	// buffer creation methods
	
	makeBuffer { }
	
	addBuffer { |buf|
		buffers = buffers.add( buf );
		this.changed( \buffers, buffers );
	}
	
	removeBuffer { |buf|
		buffers.remove( buf );
		this.changed( \buffers, buffers );
	}
	
	freeBuffer { }
	
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

	wfsPrepare { |servers|
	    servers.do( this.makeBuffer(_) )
	}

	wfsDispose {
       this.freeAllBuffers
	}

	stop { this.freeSynths; }
	
	freeSynths { synths.do(_.free) }
	
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

	asControlInputFor { |server| ^this.currentBuffer(server) }

	makeWFSUnit {
        if( loop ){
	        if( loopedDuration.notNil ) {
                ^WFSUnit( ( this.wfsUnitNamePrefix++"FilePlayerLoop"++this.numPlayedChannels).asSymbol,
                    [\bufnum, this, \loopTime, this.duration, \i_eventDuration, loopedDuration, \i_fadeInTime, fadeInTime,  \i_fadeOutTime, fadeOutTime, \rate, rate])
            }{
                ^WFSUnit( (this.wfsUnitNamePrefix++"FilePlayerInfLoop"++this.numPlayedChannels).asSymbol,
                    [\bufnum, this, \loopTime, this.duration, \i_fadeInTime, fadeInTime,  \i_fadeOutTime, fadeOutTime, \rate, rate])
            }
        } {
            ^WFSUnit( (this.wfsUnitNamePrefix++"FilePlayer"++this.numPlayedChannels).asSymbol,
                [\bufnum, this, \i_duration, this.duration, \i_fadeInTime, fadeInTime,  \i_fadeOutTime, fadeOutTime, \speed, rate]);
        }
	}
	
}

BufSoundFile : AbstractSoundFile {

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

	play { |server, startOffset = 0, mul = 1, out = 0| // returns buffer, not synth
		var action = { |buf| // copied and modified from Buffer:play
			synths = synths.add(
				{ var player;
					player = PlayBuf.ar( buf.numChannels, buf,
						BufRateScale.kr( buf ) * \rate.kr(rate),
						loop: loop.binaryValue,
						doneAction: if( loop ) { 0 } { 2 });
					Out.ar( out, player * mul );
				}.play( buf.server ).freeAction_({ |synth|
						synths.remove( synth );
						this.freeBuffer( buf );
					})
			);
		};

		// TODO : replace with single synthdef instead of .play

		^this.makeBuffer( server, startOffset, action );
	}

    numPlayedChannels{
        ^if( useChannels.isNil ) {
	        numChannels
	    }{
	        useChannels.size
	    }
    }

    wfsUnitNamePrefix{ ^"buffer" }
}

DiskSoundFile : AbstractSoundFile {
	
	var <>diskBufferSize = 32768;

	*new{ |path, startFrame = 0, endFrame, rate = 1, fadeInTime = 0.1, fadeOutTime = 0.1, loop = false , loopedDuration| // path of existing file or SoundFile
		if( path.class == SoundFile ) {
			^this.newBasic( path.path, nil, nil, nil, startFrame, endFrame, rate, fadeInTime, fadeOutTime, loop, loopedDuration ).fromFile( path );
		} {
			^this.newBasic( path, nil, nil, nil, startFrame, endFrame, rate, fadeInTime, fadeOutTime, loop, loopedDuration ).fromFile;
		};
	}
	
	asBufSoundFile { ^BufSoundFile.newCopyVars( this ); }
	asDiskSoundFile { ^this }
	
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
			"DiskSoundFile:prReadBuffer : file not found".warn;
		};
	}
	
	freeBuffer { |buf, action|
		buf.checkCloseFree( action );
		buffers.remove( buf );
	}
	
	play { |server, startOffset = 0, mul = 1, out = 0| // only one at a time per server
		var action = { |buf|
			synths = synths.add( 
				{ var diskin;
					diskin = VDiskIn.ar( numChannels, buf, \rate.kr( rate ), loop.binaryValue ); 
					if( loop.booleanValue.not ) {
						Line.kr(0,1, this.duration - (startOffset / sampleRate), doneAction:2);
					};
					Out.ar( out, diskin * mul );
				}.play( buf.server ).freeAction_({ |synth|
				 	synths.remove( synth );
					this.freeBuffer( buf );
				});
			);
		};
		
		^this.makeBuffer( server, startOffset, action );
	}

    numPlayedChannels {
        ^numChannels
    }

    wfsUnitNamePrefix{ ^"disk" }
	
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