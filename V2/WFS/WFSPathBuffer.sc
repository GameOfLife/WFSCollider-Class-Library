WFSPathBuffer : AbstractRichBuffer {
	
	classvar <>writeServers;
	
	// if a filePath is specified, the path is read from there
	// at playback. Otherwise wfsPath is used by sending it.
	// Using a filePath is safer, but of course it needs to
	// be saved and taken separately.
	
	// wfsPath can also be a file path. In that case the gui doesn't
	// know the WFSPath, but it is still played on the server (if found)
	
	// file frame 0 is used for format settings and should not be read for playback
	
	var <wfsPath;
	var <>fileStartFrame = 1, <>fileEndFrame;
	var <startFrame = 0, endFrame;
	var <rate = 1;
	var <loop = false;
	
	*initClass {
		writeServers = [ Server.default ];
	}
	
	*new { |wfsPath|
		^super.new( nil, 9 ).wfsPath_( wfsPath )
	}
	
	shallowCopy{
        ^this.class.new(wfsPath);
	}
	
	asControlInputFor { |server, startPos = 0| 
	    ^[ this.currentBuffer(server, startPos), startFrame, rate, loop.binaryValue ] 
	 }
	 
	wfsPath_ { |new|
		wfsPath = (new ? wfsPath).asWFSPath2;
		if( wfsPath.isWFSPath2 ) {
			numFrames = wfsPath.positions.size;
		} {
			numFrames = nil;
		};
		this.changed( \wfsPath, wfsPath );
	}
	
	filePath { ^if( wfsPath.isWFSPath2 ) { wfsPath.filePath } { wfsPath }; }
	 
	filePath_ { |new|
		if( new.notNil ) {
			wfsPath = new.asWFSPath2;
			this.changed( \filePath, this.filePath );
		};
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
	
	startFrame_ { |new|
		startFrame = (new ? 0).max(0);
		this.changed( \startFrame, startFrame );
	}
	
	endFrame_ { |new|
		endFrame = new.min(wfsPath.positions.size);
		this.changed( \endFrame, endFrame );
	}
	
	endFrame { 
		if( numFrames.notNil ) { 
			^(endFrame ? numFrames) % (numFrames+1) 
		} { 
			^endFrame 
		};
	}
	
	startSecond_ { |second = 0|
		if( wfsPath.isWFSPath2 ) {
			this.startFrame = wfsPath.indexAtTime( second );
		} {
			"%-startSecond_ : can't set startSecond because path % is unknown"
				.format( this.class, wfsPath )
				.warn;
		};
	}
	
	startSecond {
		if( wfsPath.isWFSPath2 ) {
			^wfsPath.timeAtIndex( startFrame );
		} {
			"%-startSecond : can't get startSecond because path % is unknown"
				.format( this.class, wfsPath )
				.warn;
			^0;
		};
	}
	
	name_ { |new|
		wfsPath.name = new.asString;
	}
	
	name { ^wfsPath.name }
	
	makeBuffer { |server, action, bufnum|
	    var buf;
	    if( this.filePath.notNil ) {
		    buf = this.readBuffer( server, action, bufnum );
	    } {
		    buf = this.sendBuffer( server, action, bufnum );
	    };
		this.addBuffer( buf );
		^buf;
	}
	
	readBuffer { |server, action, bufnum, path|
		path = path ?? { this.filePath; };
		if( path.notNil ) {
			^Buffer.read( server, path.getGPath,
					fileStartFrame ? 1, fileEndFrame ? -1, action, bufnum );
		} {
			"WFSPathBuffer:readBuffer - no filePath specified".postln;
			action.value;
			^nil;
		}
	}
	
	sendBuffer { |server, action, bufnum, forWriting = false|
		var array, buf, sendFunc;
		if( wfsPath.isWFSPath2 ) {	
			array = wfsPath.asBufferArray( forWriting );
			sendFunc = { |buf|
				// use 0.02 wait time to remain sending at high traffic 
				// (only relevant for > 180 point wfsPaths)
				{ buf.sendCollection( array, 0, 0.02, action ); }.fork;
			};
			buf = Buffer.alloc( server, array.size / 9, 9, nil, bufnum );
			OSCresponderNode( server.addr, '/done', { |time, resp, msg, addr|
				if( msg == [ '/done', '/b_alloc', buf.bufnum ] ) {
					resp.remove;
					sendFunc.value( buf );
				};
			}).add;
			^buf;
		} {
			"WFSPathBuffer:sendBuffer - can't send, WFSPath2 unknown".postln;
			"\twill try to read the buffer instead from %\n".postf( wfsPath );
			^this.readBuffer( server, action, bufnum );
		};
	}
	
	writeFile { |servers, path|
		if( wfsPath.isWFSPath2 ) {
			servers = (servers ? writeServers).asCollection;
			if( path.notNil ) {
				wfsPath.filePath = path;
			};
			wfsPath.savedCopy = wfsPath.deepCopy;
			if( this.filePath.notNil ) {
				servers.do({ |srv|
					this.writeBuffer( srv, this.filePath );
				});
			} {
				"%-writeFile : can't write file because filePath is unknown"
					.format( this.class )
					.warn;
			};
		} {
			"%-writeFile : can't write file because path % is unknown"
				.format( this.class, wfsPath )
				.warn;
		};
	}
	
	writeBuffer { |server, path, action|
		var buf, writeFunc, removeFunc;
		
		path = path ? this.filePath;
		
		writeFunc = { |buf|
			buf.write( path.getGPath, "aiff", "float32", -1, 0, false );
		};
		
		buf = this.sendBuffer( server, writeFunc, forWriting: true );
		
		OSCresponderNode( server.addr, '/done', { |time, resp, msg, addr|
			if( msg == [ '/done', '/b_write', buf.bufnum ] ) {
				resp.remove;
				buf.free;
				action.value;
			};
		}).add; 
	}
	
	storeArgs { ^[ wfsPath ] }
	
}

+ WFSPath2 {
	
	asUnitArg { |unit|
		^WFSPathBuffer( this ).asUnitArg( unit );
	}
	
	asUnit {
		^U( \wfsPathPlayer, [ \wfsPath, this ] );
	}
}