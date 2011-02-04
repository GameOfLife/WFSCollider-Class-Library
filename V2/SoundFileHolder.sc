// wslib 2010

SoundFileHolder {
	
	// points to a SoundFile and holds its specs
	// aditional parameters for Buffer loading settings
	
	var <>path, <>numFrames, <>numChannels = 1, <>sampleRate = 44100;
	var <>startFrame = 0, >endFrame, <>useChannels;  // for buffer loading
	var <>rate = 1, <>loop = false;
	
	var <>synths; // holder for the playback synths
	
	*new { |path, numFrames, numChannels, sampleRate|
		^super.newCopyArgs( path, numFrames, numChannels, sampleRate );
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
		if( soundfile.isNil or: { soundfile.isOpen.not } )
			{	soundfile = soundfile ?? { SoundFile.new }; 
			  	test = soundfile.openRead( path.standardizePath ) 
			};
		if( test )
			{	numFrames = soundfile.numFrames;
				numChannels = soundfile.numChannels;
				sampleRate = soundfile.sampleRate;
				^true;
			}
			{ ^false };
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
	
	// playback / buffer methods
	
	asBuffer { |server, action, bufnum|
		if( useChannels.notNil )
			{ ^Buffer.readChannel( server, path.standardizePath, 
					startFrame, this.usedFrames, useChannels, action, bufnum ); } 
			{ ^Buffer.read( server, path.standardizePath, 
					startFrame, this.usedFrames, action, bufnum );
			};
	}
	
	cueSoundFile {  arg server, action, bufnum, bufferSize=32768;
		// useChannels and endFrame doesn't work
		^Buffer.alloc(server, bufferSize, numChannels,
			{ arg buffer;
				buffer.readMsg(path, startFrame, bufferSize, 0, true,
					{|buf|["/b_query", buf.bufnum]});
			}).doOnInfo_(action).cache;
	}
	
	play { |server, mul = 1, out = 0, disk = false|
		if( disk )
			{ ^this.playDisk( server, mul, out ) }
			{ ^this.playBuffer( server, mul, out ) }; 
	}
	
	playBuffer { |server, mul = 1, out = 0| // returns buffer, not synth
		var action = { |buf| // copied and modified from Buffer:play
			synths = synths.add(	
				{ var player;
					player = PlayBuf.ar( buf.numChannels, buf,
						BufRateScale.kr( buf ) * rate,
						loop: loop.binaryValue, 
						doneAction: if( loop ) { 0 } { 2 });
					Out.ar( out, player * mul );
				}.play( buf.server ).freeAction_({ |synth| 
						synths.remove( synth );
						buf.checkFree; // clean up afterwards
					}) 
			); 
		};
		
		// TODO : replace with single synthdef instead of .play
		
		^this.asBuffer( server, action );
		}
	
	playDisk { |server, mul = 1, out = 0| // only one at a time per server
		
		var action = { |buf|
			synths = synths.add( { var diskin = VDiskIn.ar( 1, buf, rate ); 
				Env([1,1],[this.duration]).kr(2);
				Out.ar( out, diskin * mul );
			}.play( buf.server ).freeAction_({ |synth|
				 	synths.remove( synth );
					buf.checkCloseFree; 
				});
			);
		};
		
		^this.cueSoundFile( server, action );
	}
	
	freeSynths { synths.do(_.free) }
}