SynthDefManager {
	classvar <>dict;
	
	*initClass {
		dict = ();
	}
	
	*load { |target, def, completionMsg, dir|
		var server, bytes;
		server = target.asTarget.server;
		if( dict[ server ].isNil or: { dict[ server ].includes( def ).not } ) {
			if( server.isLocal ) {
				def.load( server, completionMsg, dir );
			} {
				this.prSend( server, completionMsg, dir );
			};
			(dict[ server ] ? []).removeAllSuchThat({ |item| item.name == def.name });
			dict[ server ] = dict[ server ].add( def );
		};
	}
	
	*send { |target, def, completionMsg, dir|
		var server, bytes;
		server = target.asTarget.server;
		if( dict[ server ].isNil or: { dict[ server ].includes( def ).not } ) {
			this.prSend( server, def, completionMsg );
		};
		(dict[ server ] ? []).removeAllSuchThat({ |item| item.name == def.name });
		dict[ server ] = dict[ server ].add( def );
	}
	
	*prSend { |server, def, completionMsg|
		var bytes;
		bytes = def.asBytes;
		if(bytes.size > (65535 div: 4)) {
			"synthdef may have been too large to send to remote server".warn;
		};
		server.sendMsg("/d_recv", bytes, completionMsg);
	}
	
	*reset { |target|
		if( target.isNil ) {
			dict = ();
		} {
			dict[ target.asTarget.server ] = nil;
		};
	}
	
	*removeDef { |def, target|
		if( target.isNil ) {
			dict.values.do(_.remove(def));
		} {
			dict[ target.asTarget.server ].remove(def);
		};
	}
	
}
