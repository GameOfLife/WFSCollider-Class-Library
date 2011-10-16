WFSPathBuffer : AbstractRichBuffer {
	
	classvar <>writeServers;
	
	// if a filepath is specified, the path is read from there
	// at playback. Otherwise wfsPath is used by sending it.
	// Using a filePath is safer, but of course it needs to
	// be saved and taken separately.
	
	// file frame 0 is used for format settings and should not be read for playback
	
	var <wfsPath, <filePath;
	var <>fileStartFrame = 1, <>fileEndFrame;
	var <startFrame = 0, endFrame;
	var <rate = 1;
	var <loop = false;
	
	*initClass {
		writeServers = [ Server.default ];
	}
	
	*new { |wfsPath, filePath|
		^super.new( wfsPath !? { wfsPath.positions.size } ? 0, 9 )
			.wfsPath_( wfsPath )
			.filePath_( filePath );
	}
	
	shallowCopy{
        ^this.class.new(wfsPath, filePath);
	}
	
	asControlInputFor { |server, startPos = 0| 
	    ^[ this.currentBuffer(server, startPos), startFrame, rate, loop.binaryValue ] 
	 }
	 
	wfsPath_ { |new|
		wfsPath = new ? wfsPath;
		this.changed( \wfsPath, wfsPath );
	}
	 
	filePath_ { |new|
		filePath = new.formatGPath;
		this.changed( \filePath, filePath );
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
		this.startFrame = wfsPath.indexAtTime( second );
	}
	
	startSecond {
		^wfsPath.timeAtIndex( startFrame );
	}
	
	name_ { |new|
		wfsPath.name = new.asString;
	}
	
	name { ^wfsPath.name }
	
	makeBuffer { |server, action, bufnum|
	    var buf;
	    if( filePath.notNil ) {
		    buf = this.readBuffer( server, action, bufnum );
	    } {
		    buf = this.sendBuffer( server, action, bufnum );
	    };
		this.addBuffer( buf );
		^buf;
	}
	
	readBuffer { |server, action, bufnum, path|
		path = path ? filePath;
		if( path.notNil ) {
			^Buffer.read( server, path.getGPath,
					fileStartFrame ? 1, fileEndFrame ? -1, action, bufnum );
		} {
			"WFSPathBuffer:readBuffer - no filePath specified".postln;
			^nil;
		}
	}
	
	sendBuffer { |server, action, bufnum, forWriting = false|
		var array, buf, sendFunc;
		array = wfsPath.asBufferArray( forWriting );
		sendFunc = { |buf|
			// use 0.02 wait time to remain sending at high traffic 
			// (only relevant for > 180 point wfsPaths)
			{ buf.sendCollection( array, 0, 0.02, action ); }.fork;
		};
		buf = Buffer.alloc( server, wfsPath.positions.size, 9, nil, bufnum );
		OSCresponderNode( server.addr, '/done', { |time, resp, msg, addr|
			if( msg == [ '/done', '/b_alloc', buf.bufnum ] ) {
				resp.remove;
				sendFunc.value( buf );
			};
		}).add;
		^buf;
	}
	
	writeFile { |servers, path|
		servers = (servers ? writeServers).asCollection;
		if( path.notNil ) {
			filePath = path.formatGPath;
		};
		servers.do({ |srv|
			this.writeBuffer( srv, filePath.getGPath );
		});
	}
	
	writeBuffer { |server, path, action|
		var buf, writeFunc, removeFunc;
		
		path = path ? filePath;
		
		writeFunc = { |buf|
			buf.write( path.getGPath, "aiff", "float32", -1, 0, false );
		};
		
		buf = this.sendBuffer( server, writeFunc );
		
		OSCresponderNode( server.addr, '/done', { |time, resp, msg, addr|
			if( msg == [ '/done', '/b_write', buf.bufnum ] ) {
				resp.remove;
				buf.free;
				action.value;
			};
		}).add; 
	}
	
}

+ WFSPath2 {
	
	asUnitArg { |unit|
		^WFSPathBuffer( this ).asUnitArg( unit );
	}
	
	asUnit {
		^U( \wfsPathPlayer, [ \wfsPath, this ] );
	}
}