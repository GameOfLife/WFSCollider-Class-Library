WFSPathBuffer : AbstractRichBuffer {
	
	var <wfsPath, <filePath;
	var <>fileStartFrame, <>fileEndFrame; // in case the file is shared with other paths
	var <startFrame = 0, endFrame;
	var <rate = 1;
	var <loop = false;
	
	*new { |wfsPath, filePath|
		^super.new( wfsPath !? { wfsPath.positions.size } ? 0, 9 )
			.wfsPath_( wfsPath )
			.filePath_( filePath );
	}
	
	asControlInputFor { |server, startPos = 0| 
	    ^[ this.currentBuffer(server, startPos), startFrame, rate, loop.binaryValue ] 
	 }
	 
	wfsPath_ { |new|
		wfsPath = new ? wfsPath;
		this.changed( \wfsPath, wfsPath );
	}
	 
	filePath_ { |new|
		filePath = new ? filePath;
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
		endFrame = new.min(numFrames);
		this.changed( \endFrame, endFrame );
	}
	
	endFrame { if( numFrames.notNil ) { 
			^(endFrame ? numFrames) % (numFrames+1) 
		} { 
			^endFrame 
		};
	}
	
	makeBuffer { |server, action, bufnum|
	    var buf;
	    if( filePath.notNil ) {
		    buf = this.readBuffer( server, action, bufnum );
	    } {
		    buf = this.sendBuffer( server, action, bufnum );
	    };
		buffers = buffers.add( buf );
		^buf;
	}
	
	readBuffer { |server, action, bufnum, path|
		path = path ? filePath;
		if( path.notNil ) {
			^Buffer.read( server, path.standardizePath,
					fileStartFrame ? 0, fileEndFrame ? -1, action, bufnum );
		} {
			"WFSPathBuffer:readBuffer - no filePath specified".postln;
			^nil;
		}
	}
	
	sendBuffer { |server, action, bufnum|
		var array, buf, sendFunc;
		array = wfsPath.asBufferArray;
		sendFunc = { |buf|
			// use 0.02 wait time to remain sending at high traffic 
			// (only relevant for > 180 point wfsPaths)
			buf.sendCollection( array, 0, 0.02, action ); 
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
	
	writeBuffer { |server, path, action|
		var buf, writeFunc, removeFunc;
		
		path = path ? filePath;
		
		writeFunc = { |buf|
			buf.write( path.standardizePath, "aiff", "float32", -1, 0, false );
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