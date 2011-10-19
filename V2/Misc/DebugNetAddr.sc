DebugNetAddr : NetAddr {
	
	classvar <>debug = true;
	classvar <>postStatus = false;
	
	sendRaw { arg rawArray;
		if( debug ) { rawArray.postln };
		^super.sendRaw( rawArray );
	}
	sendMsg { arg ... args;
		if( debug && { postStatus or: {args[0].asSymbol !== '/status' } } ) { args.postln };
		^super.sendMsg( *args );
	}
	// warning: this primitive will fail to send if the bundle size is too large
	// but it will not throw an error.  this needs to be fixed
	sendBundle { arg time ... args;
		if( debug ) { ([ time ] ++ args).postln };
		^super.sendBundle( time, *args );
	}

	sendPosBundle { arg position ... args; // a 64 bit (double) position value
		if( debug ) { ([ position ] ++ args).postln };
		^super.sendPosBundle( position, *args );
	}
}