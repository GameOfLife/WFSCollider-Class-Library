BufferCenter {
	classvar <>all, <>defaults;
	
	var <server, <>dict;
	
	/*
	dict:
		(
		Buffer: (
			\synths: [ ... ], // synths depending upon buffer 
			\startFrame: xxx, // start frame of sound file (default: 0)
			\numFrames: xxx,  // numFrames of sound file (default: -1)
			\channels: []    // default nil
			\cue: bool )	   // true if DiskIn cue buffer (default: false)
		)
	*/
	
	*forServer { |server| // use this instead of *new
		server = server ? Server.default;
		^all.detect({ |item| item.server == server }) ?? { this.new( server ) }	}
	
	*new { |server| ^super.newCopyArgs( server ? Server.default ).init }
	
	*initClass {
		defaults = IdentityDictionary[		
			\startFrame -> 0,
			\numFrames -> -1,
			\cue -> false
		];
	}
	
	init {
		dict = IdentityDictionary();
		all = all.add( this );
	}

	*bufferPerform { |selector ...args|
		var buf;
		buf = Buffer.perform( selector, *args );
		this.addBuffer( buf );
		^buf;
	}
	
	// doubles for Meta_Buffer methods
	*alloc { |server, numFrames, numChannels = 1, completionMessage, bufnum|
		^this.bufferPerform( \alloc, server, numFrames, numChannels, completionMessage, bufnum )
		}
	
	addBuffer { |buffer ...args| // args: arg pairs
		if( buffer.server == server )
			{ dict.put( buffer, IdentityDictionary().proto_( defaults ).putPairs( *args ) ); }
			{ this.class.addBuffer( buffer ); }
	}
	
	removeBuffer { |buffer| 
		if( buffer.server == server )
			{ dict.removeAt( buffer ); }
			{ this.class.removeBuffer( buffer ); }
	}
	
	*addBuffer { |buffer|
		this.forServer( buffer.server ).addBuffer( buffer );
	}
	
	*removeBuffer { |buffer|
		this.forServer( buffer.server ).removeBuffer( buffer );
	}
	
	
	
	
}