// wslib 2010

SoundFileHolder {
	
	// points to a SoundFile and holds its specs
	// aditional parameters for Buffer loading settings
	
	var <>path, <>numFrames, <>numChannels = 1, <>sampleRate = 44100;
	var <>mode = 'buffer'; // or 'disk'
	var <>startFrame = 0, >endFrame, <>useChannels;  // for buffer loading
	var <rate = 1, <>loop = false;
	
	var <>diskBufferSize = 32768;
	
	var <>synths; // holder for the instant playback synths
	var <>buffers; // holder for all buffers
	
	*new { |path, numFrames, numChannels, sampleRate = 44100, mode = 'buffer'|
		^super.newCopyArgs( path, numFrames, numChannels, sampleRate, mode );
	}
	
	*fromFile { |path| // path of existing file or SoundFile
		if( path.class == SoundFile ) 
			{ ^this.new( path.path ).fromFile( path );  }
			{ ^this.new( path ).fromFile; };
	}
	
	fromFile { |soundfile|
		if( this.readFromFile( soundfile ).not )
			{ "%:initFromFile - could not open file '%'\n".postf( this.class, path.basename ) }
	}
	
	readFromFile { |soundfile|
		var test = true;
		if( soundfile.isNil or: { soundfile.isOpen.not } ) {
			soundfile = soundfile ?? { SoundFile.new }; 
			test = soundfile.openRead( path.standardizePath );
			soundfile.close; // close if it wasn't open
		};
		if( test ) {	
			numFrames = soundfile.numFrames;
			numChannels = soundfile.numChannels;
			sampleRate = soundfile.sampleRate;
			^true;
		} { 
			^false 
		};
	}
	
	rate_ { |newRate|
		rate = newRate ? 1;
		synths.do( _.set( \rate, rate ) );
	}
	
	endFrame { if( numFrames.notNil ) 
			{ ^(endFrame ? numFrames) % (numFrames+1) } 
			{ ^endFrame };
		 }
		 
	usedFrames { ^(this.endFrame ?? { startFrame - 1 }) - startFrame } // -1 if unknown or to end
	usedFrames_ { |frames = (-1)| // -1 means from startFrame to end 
			if( [-1, nil].includes(frames.asInt) )
				{ this.endFrame = nil; }
				{ this.endFrame = frames + startFrame };
	}
	
	framesToSeconds { |frames = 0|  ^frames !? { (frames / sampleRate) / rate } }
	secondsToFrames { |seconds = 0| ^seconds !? { seconds * rate * sampleRate } }
	
	startSecond 	{ ^this.framesToSeconds( this.startFrame ); }
	endSecond 	{ ^this.framesToSeconds(this.endFrame); }
	duration 		{ ^this.framesToSeconds(this.usedFrames); } // negative if unknown
	fileDuration 	{ ^this.framesToSeconds(this.numFrames); }
	
	startSecond_ { |startSecond = 0| startFrame = this.secondsToFrames( startSecond ); }
	endSecond_ { |endSecond = 0| endFrame = this.secondsToFrames( endFrame ); }
	duration_ { |duration| this.usedFrames = this.secondsToFrames( duration ) }
	fileDuration_ { |duration| this.numFrames = this.secondsToFrames( duration ); }
	
	asSoundFile { ^SoundFile.openRead(path) } // forgets settings
	
	// buffer creation methods
	
	prReadBuffer { |server, startOffset = 0, action, bufnum|
		if( useChannels.notNil ) { 
			^Buffer.readChannel( server, path.standardizePath, 
					startFrame + startOffset, this.usedFrames, useChannels, action, bufnum ); 
		} { 
			^Buffer.read( server, path.standardizePath, 
					startFrame + startOffset, this.usedFrames, action, bufnum );
		};
	}
	
	prCueSoundFile {  |server, startOffset = 0, action, bufnum| 
			// useChannels and endFrame not used
		var test = true;
		
		if( numChannels.isNil ) { 
			test = this.readFromFile; // get numchannels etc.
		};
		
		if( test ) {
			^Buffer.alloc(server, diskBufferSize, numChannels, { arg buffer;
				buffer.readMsg(path, startFrame + startOffset, diskBufferSize, 0, true, {|buf|
					["/b_query", buf.bufnum]
				});
			}).doOnInfo_(action).cache;
		} {
			"Buffer:prCueSoundfile : file not found".warn;
		};
	}
	
	makeBuffer { |server, startOffset, action, bufnum|
		var buf;
		if( mode == 'disk' ) {
			buf = this.prCueSoundFile( server, startOffset, action, bufnum );		} {
			buf = this.prReadBuffer( server, startOffset, action, bufnum );
		};
		buffers = buffers.add( buf );
		^buf;
	}
	
	freeBuffer { |buf, action|
		if( mode == 'disk' ) {
			buf.checkCloseFree( action );
		} {
			buf.checkFree( action );
		};
		buffers.remove( buf );
	}
	
	currentBuffers { |server|
		if( server.notNil ) {
			^buffers.select({ |item| item.server == server });
		};
		^buffers;
	}
	
	freeAllBuffers { |server|
		this.currentBuffers( server ).do( this.freeBuffer(_) );
	}
	
	resetBuffers { |server|
		this.currentBuffers( server ).do({ |buf|
			buffers.remove( buf );
		});
	}
	
	// playback methods
	
	play { |server, startOffset = 0, mul = 1, out = 0|
		if( mode == 'disk' )
			{ ^this.playDisk( server, startOffset, mul, out ) }
			{ ^this.playBuffer( server, startOffset, mul, out ) }; 
	}
	
	playBuffer { |server, startOffset = 0, mul = 1, out = 0| // returns buffer, not synth
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
	
	playDisk { |server, startOffset = 0, mul = 1, out = 0| // only one at a time per server
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
	
	stop {
		this.freeSynths;
	}
	
	freeSynths { synths.do(_.free) }
}